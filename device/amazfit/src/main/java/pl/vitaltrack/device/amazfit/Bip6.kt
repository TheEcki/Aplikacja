package pl.vitaltrack.device.amazfit

import android.content.Context
import android.os.Handler
import android.os.Looper
import pl.vitaltrack.core.bluetooth.AndroidGatt
import pl.vitaltrack.core.bluetooth.GattGateway
import pl.vitaltrack.core.bluetooth.GattProps
import pl.vitaltrack.core.bluetooth.GattStatus
import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.model.ConnectionError
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.model.WatchCapabilities
import pl.vitaltrack.core.model.WatchState
import pl.vitaltrack.device.api.DiscoveredWatch
import pl.vitaltrack.device.api.Events
import pl.vitaltrack.device.api.WatchAdapter
import pl.vitaltrack.device.api.WatchFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Adapter Amazfit Bip 6 (Zepp OS). Potwierdzone elementy: GATT, handshake
 * Huami (FEE1/00000009, AES-128-ECB), tętno na żywo, bateria, info, alarmy,
 * ustawienia, ANS. Historia aktywności — UNVERIFIED (docs/BLE_PROTOCOL.md).
 */
class Bip6(private val ctx: Context) : WatchAdapter {

    override val caps = WatchCapabilities(heartRate = true, continuousHeartRate = true, battery = true)
    override val state: Obs<WatchState> = Obs(WatchState.Disconnected)
    override val auth: Obs<AuthState> = Obs(AuthState.NONE)

    private val ls = CopyOnWriteArrayList<Events>()
    private val main = Handler(Looper.getMainLooper())
    private var g: GattGateway? = null
    private var key: ByteArray? = null
    @Volatile private var live = 0
    private var watchdog: Runnable? = null

    override fun addListener(l: Events) { ls.add(l) }
    override fun removeListener(l: Events) { ls.remove(l) }
    private fun status(t: String) { for (l in ls) l.onStatus(t) }
    private fun vendor(s: String, c: String, h: String) { for (l in ls) l.onVendor(s, c, h) }

    override fun connect(d: DiscoveredWatch, key: ByteArray?): Boolean {
        this.key = key
        closeInternal()
        val gw = GattGateway(AndroidGatt(ctx), opTimeoutMs = 10_000) { status("GATT: $it") }
        g = gw

        gw.setListener(object : GattGateway.Listener {
            override fun onConnected() { status("Połączono — usługi…"); gw.discover() }
            override fun onDisconnected() {
                auth.set(AuthState.NONE); status("Rozłączono.")
                for (l in ls) l.onDisconnected()
            }
            override fun onServices() { authFlow(gw) }
            override fun onChanged(char: String, v: ByteArray) {
                when (char) {
                    Profile.AUTH -> handleAuth(gw, v)
                    Profile.HR_MEAS -> Codec.heartRate(v)?.let { hr ->
                        if (hr > 0) { live = hr; val now = System.currentTimeMillis(); for (l in ls) l.onHeart(hr, now) }
                    }
                    else -> vendor(svcOf(gw, char), char, hex(v))
                }
            }
            override fun onRead(char: String, v: ByteArray) {
                when {
                    char == Profile.BAT_LEVEL && v.isNotEmpty() -> for (l in ls) l.onBattery(v[0].toInt() and 0xff)
                    char.startsWith("00002a") -> for (l in ls) l.onInfo(mapOf(Profile.infoName(char) to String(v).trim()))
                    else -> vendor(svcOf(gw, char), char, hex(v))
                }
            }
            override fun onWrite(char: String) {}
            override fun onMtu(m: Int) {}
        })

        state.set(WatchState.Connecting(d.name.ifEmpty { "Zegarek" }))
        gw.connect(d.address)
        watchdog?.let { main.removeCallbacks(it) }
        watchdog = Runnable {
            if (state.value !is WatchState.Connected) {
                gw.disconnect(); state.set(WatchState.Error(ConnectionError.ConnectionTimeout))
            }
        }
        main.postDelayed(watchdog!!, 20_000)
        return true
    }

    private fun closeInternal() {
        watchdog?.let { main.removeCallbacks(it) }; watchdog = null
        try { g?.close() } catch (ignored: Exception) {}
        g = null; auth.set(AuthState.NONE)
    }

    override fun disconnect() {
        closeInternal(); state.set(WatchState.Disconnected); status("Rozłączono.")
        for (l in ls) l.onDisconnected()
    }

    override fun isConnected(): Boolean = g?.isConnected() == true

    private fun svcOf(gw: GattGateway, char: String): String {
        for (s in gw.services()) if (gw.chars(s).contains(char)) return s
        return "?"
    }

    private fun authFlow(gw: GattGateway) {
        val k = key
        if (k == null || k.size != 16) {
            status("Brak klucza — tryb ograniczony (HR/bateria/info).")
            auth.set(AuthState.NONE); standard(gw); return
        }
        if (!gw.chars(Profile.HUAMI).contains(Profile.AUTH)) {
            status("Brak FEE1/auth — tryb ograniczony."); auth.set(AuthState.NONE); standard(gw); return
        }
        gw.setNotify(Profile.AUTH)
        state.set(WatchState.Authenticating)
        auth.set(AuthState.KEY_SENT)
        status("Autoryzacja Huami…")
        gw.write(Profile.AUTH, Auth.keyPacket(k))
        main.postDelayed({ if (auth.value != AuthState.OK) gw.write(Profile.AUTH, Auth.challengeRequest()) }, 500)
    }

    private fun handleAuth(gw: GattGateway, p: ByteArray) {
        when (val r = Auth.parse(p)) {
            is Auth.Packet.KeyAccepted -> { status("Klucz przyjęty…"); auth.set(AuthState.CHALLENGE); gw.write(Profile.AUTH, Auth.challengeRequest()) }
            is Auth.Packet.Challenge -> {
                val k = key ?: return
                try { gw.write(Profile.AUTH, Auth.challengeResponse(r.random, k)); status("AES wysłane…"); auth.set(AuthState.RESPONSE_SENT) }
                catch (e: Exception) { status("Błąd AES"); auth.set(AuthState.FAIL) }
            }
            is Auth.Packet.Success -> { status("Autoryzacja OK ✓"); auth.set(AuthState.OK); state.set(WatchState.Connected); standard(gw) }
            is Auth.Packet.KeyChange -> { status("Zegarek żąda zmiany klucza."); auth.set(AuthState.FAIL) }
            is Auth.Packet.Rejected -> { status("Autoryzacja odrzucona."); auth.set(AuthState.FAIL) }
            is Auth.Packet.Unknown -> vendor(Profile.HUAMI, Profile.AUTH, hex(p))
        }
    }

    private fun standard(gw: GattGateway) {
        if (gw.chars(Profile.HR).contains(Profile.HR_MEAS)) gw.setNotify(Profile.HR_MEAS)
        if (gw.chars(Profile.BAT).contains(Profile.BAT_LEVEL)) gw.read(Profile.BAT_LEVEL)
        for (c in listOf(Profile.INFO_MANUF, Profile.INFO_MODEL, Profile.INFO_FW, Profile.INFO_SN, Profile.INFO_HW))
            if (gw.chars(Profile.INFO).contains(c)) gw.read(c)
    }

    override fun liveHr(): Int = live

    override fun batteryBlocking(): Int? {
        val gw = g ?: return null
        var r: Int? = null
        gw.read(Profile.BAT_LEVEL) { v, s -> if (s == GattStatus.OK && v != null && v.isNotEmpty()) r = v[0].toInt() and 0xff }
        return r
    }

    override fun infoBlocking(): Map<String, String> {
        val gw = g ?: return emptyMap()
        val out = HashMap<String, String>()
        for (c in listOf(Profile.INFO_MANUF, Profile.INFO_MODEL, Profile.INFO_FW, Profile.INFO_SN, Profile.INFO_HW))
            gw.read(c) { v, s -> if (s == GattStatus.OK && v != null) out[Profile.infoName(c)] = String(v).trim() }
        return out
    }

    override fun activityRawBlocking(): List<Pair<String, ByteArray>> {
        val gw = g ?: return emptyList()
        val out = ArrayList<Pair<String, ByteArray>>()
        val targets = Profile.ACTS.toMutableList()
        for (c in gw.chars(Profile.HUAMI)) {
            if (c !in targets && (gw.props(Profile.HUAMI, c) and GattProps.READ) != 0) targets.add(c)
        }
        for (c in targets) gw.read(c) { v, s ->
            if (s == GattStatus.OK && v != null && v.isNotEmpty()) { out.add(c to v); vendor(Profile.HUAMI, c, hex(v)) }
        }
        return out
    }

    override fun findWatch() {
        val gw = g ?: run { status("Nie połączono."); return }
        if (!gw.chars(Profile.ALERT_SVC).contains(Profile.ALERT_LEVEL)) { status("Brak Immediate Alert."); return }
        gw.write(Profile.ALERT_LEVEL, byteArrayOf(0x02)); status("Znajdź: wibracja 3 s…")
        main.postDelayed({ gw.write(Profile.ALERT_LEVEL, byteArrayOf(0x00)) }, 3000)
    }

    override fun testNotify() {
        val gw = g ?: run { status("Nie połączono."); return }
        if (!gw.chars(Profile.ANS).contains(Profile.NEW_ALERT)) { status("Brak ANS."); return }
        gw.write(Profile.NEW_ALERT, Codec.notifyPacket("Test VitalTrack"))
        status("Test powiadomienia wysłany.")
    }

    override fun setOption(cmd: ByteArray, label: String) {
        val gw = g ?: run { status("Nie połączono."); return }
        if (!gw.chars(Profile.HUAMI).contains(Profile.CONFIG)) { status("Brak kanału 0x0003."); return }
        gw.write(Profile.CONFIG, cmd); status("$label → ${hex(cmd)}")
    }

    override fun syncAlarms(a: List<M.Alarm>) {
        val gw = g ?: run { status("Nie połączono — zapisano lokalnie."); return }
        if (!gw.chars(Profile.HUAMI).contains(Profile.CONFIG)) { status("Brak kanału 0x0003."); return }
        for (al in a) gw.write(Profile.CONFIG, Codec.alarmPacket(al))
        status("Alarmy: ${a.size} pakietów.")
    }

    private fun hex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) sb.append(String.format("%02x", x.toInt() and 0xff))
        return sb.toString()
    }
}

class Bip6Factory(private val ctx: Context) : WatchFactory {
    override fun supports(d: DiscoveredWatch): Boolean {
        if (d.services.any { it.equals(Profile.HUAMI, true) }) return true
        val n = d.name.lowercase()
        return n.contains("amazfit") || n.contains("huami") || n.contains("zepp") || n.contains("bip")
    }
    override fun create(d: DiscoveredWatch): WatchAdapter = Bip6(ctx)
}
