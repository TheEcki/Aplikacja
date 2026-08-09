package pl.vitaltrack

import android.content.Context
import pl.vitaltrack.core.bluetooth.Permissions
import pl.vitaltrack.core.bluetooth.Scanner
import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.common.WatchListener
import pl.vitaltrack.core.common.WatchPort
import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.model.WatchCapabilities
import pl.vitaltrack.core.model.WatchState
import pl.vitaltrack.core.security.Vault
import pl.vitaltrack.core.settings.SettingsRepo
import pl.vitaltrack.core.sync.Engine
import pl.vitaltrack.device.api.DiscoveredWatch
import pl.vitaltrack.device.api.SyncResult
import pl.vitaltrack.device.api.WatchAdapter
import pl.vitaltrack.device.api.WatchRegistry
import java.util.concurrent.CopyOnWriteArrayList

/** WatchPort — fasada dla UI: skaner + registry + adapter + sync. */
class DeviceRepository(
    private val ctx: Context,
    private val settings: SettingsRepo,
    private val vault: Vault,
    private val registry: WatchRegistry,
    private val engine: Engine,
) : WatchPort {

    override val state: Obs<WatchState> = Obs(WatchState.Disconnected)
    override val auth: Obs<AuthState> = Obs(AuthState.NONE)
    override val caps: WatchCapabilities
        get() = adapter?.caps ?: WatchCapabilities.UNVERIFIED

    private val ls = CopyOnWriteArrayList<WatchListener>()
    private val scanner = Scanner(ctx)
    private var adapter: WatchAdapter? = null

    override fun addListener(l: WatchListener) { ls.add(l) }
    override fun removeListener(l: WatchListener) { ls.remove(l) }
    private fun status(t: String) { for (l in ls) l.onStatus(t) }

    override fun canScan(): String? = Permissions.canScan(ctx)

    override fun scan(): Boolean {
        scanner.setSavedMac(settings.getString(SettingsPort.Keys.MAC, ""))
        scanner.setListener(object : Scanner.Listener {
            override fun onScan(on: Boolean, msg: String) {
                state.set(if (on) WatchState.Scanning else WatchState.Disconnected)
                for (l in ls) l.onScanState(on, msg)
            }
            override fun onFound(list: List<M.BleDevice>) { for (l in ls) l.onDevices(list) }
            override fun onError(msg: String) { for (l in ls) l.onStatus(msg) }
        })
        return scanner.start()
    }

    override fun stopScan() = scanner.stop()
    override fun isScanning(): Boolean = scanner.isScanning()

    override fun connect(mac: String): Boolean {
        if (!Permissions.btOn(ctx)) { status("Bluetooth wyłączony."); return false }
        val d = DiscoveredWatch(settings.getString(SettingsPort.Keys.NAME, "Zegarek"), mac, 0)
        val a = registry.create(d) ?: run { status("Brak adaptera."); return false }
        adapter?.removeListener(bridge)
        adapter = a
        a.addListener(bridge)
        a.setSyncLauncher { sync() }
        return a.connect(d, vault.loadKey())
    }

    override fun disconnect() { adapter?.disconnect() }
    override fun isConnected(): Boolean = adapter?.isConnected() == true
    override fun liveHr(): Int = adapter?.liveHr() ?: 0

    override fun sync(): Boolean {
        val a = adapter ?: run { status("Nie połączono."); return false }
        if (engine.isRunning()) { status("Synchronizacja już trwa…"); return true }
        engine.run(a) { r ->
            when (r) {
                is SyncResult.Success -> for (l in ls) l.onSyncDone(true, r.summary)
                is SyncResult.Failure -> { for (l in ls) l.onSyncDone(false, r.detail); status(r.detail) }
            }
        }
        return true
    }

    override fun findWatch() = adapter?.findWatch() ?: status("Nie połączono.")
    override fun testNotify() = adapter?.testNotify() ?: status("Nie połączono.")
    override fun setOption(cmd: ByteArray, label: String) { adapter?.setOption(cmd, label) }
    override fun syncAlarms(a: List<M.Alarm>) { adapter?.syncAlarms(a) }

    override fun saveKey(hex: String): Boolean {
        if (!vault.saveKey(hex)) return false
        settings.putBool(SettingsPort.Keys.HAS_KEY, true)
        return true
    }

    override fun forgetDevice() {
        adapter?.disconnect()
        settings.forget()
        vault.clearKey()
        state.set(WatchState.Disconnected)
        status("Usunięto zegarek i klucz.")
    }

    private val bridge = object : pl.vitaltrack.device.api.Events {
        override fun onStatus(t: String) { for (l in ls) l.onStatus(t) }
        override fun onScan(on: Boolean, m: String) { for (l in ls) l.onScanState(on, m) }
        override fun onDevices(d: List<M.BleDevice>) { for (l in ls) l.onDevices(d) }
        override fun onConnected() { state.set(WatchState.Connected); for (l in ls) l.onConnected() }
        override fun onDisconnected() { state.set(WatchState.Disconnected); for (l in ls) l.onDisconnected() }
        override fun onAuth(s: AuthState) { auth.set(s); for (l in ls) l.onAuth(s) }
        override fun onHeart(bpm: Int, ts: Long) { for (l in ls) l.onHeart(bpm, ts) }
        override fun onBattery(p: Int) { for (l in ls) l.onBattery(p) }
        override fun onInfo(i: Map<String, String>) { for (l in ls) l.onInfo(i) }
        override fun onVendor(svc: String, chr: String, hex: String) {
            for (l in ls) l.onStatus("Char ${chr.substring(4, 8).uppercase()} = ${hex.take(64)}")
        }
        override fun onSyncDone(ok: Boolean, summary: String) { for (l in ls) l.onSyncDone(ok, summary) }
    }
}
