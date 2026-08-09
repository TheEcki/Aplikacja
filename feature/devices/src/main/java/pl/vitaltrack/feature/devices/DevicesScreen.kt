package pl.vitaltrack.feature.devices

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import pl.vitaltrack.core.common.Hex
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.model.WatchState
import pl.vitaltrack.device.amazfit.Codec

/** Urządzenia — parowanie (skan→klucz→połącz), status, bateria, alarmy, log. */
class DevicesScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var status: TextView? = null
    private var name: TextView? = null
    private var mac: TextView? = null
    private var batV: TextView? = null
    private var fwV: TextView? = null
    private var keyInput: EditText? = null
    private var list: LinearLayout? = null
    private var logList: LinearLayout? = null
    private var scanBtn: android.widget.Button? = null

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        setPadding(M3.dp(16f), M3.dp(10f), M3.dp(16f), M3.dp(120f))

        val hero = W.hero(c)
        val col = W.col(c)
        hero.addView(col)
        val top = W.row(c)
        val ic = W.Icon(c); ic.setIcon(W.Icon.WATCH); ic.setColor(M3.accent)
        ic.layoutParams = LinearLayout.LayoutParams(M3.dp(28f), M3.dp(28f))
        top.addView(ic)
        top.addView(W.semi(c, "  Amazfit Bip 6", 18f, M3.onSurface))
        top.addView(W.spacer(c))
        status = W.med(c, "nie połączono", 12.5f, M3.onSurfaceVariant)
        top.addView(status)
        col.addView(top)
        name = W.semi(c, "—", 20f, M3.onSurface)
        col.addView(name, W.mlp(0, 10, 0, 0))
        mac = W.body(c, "—", 12f, M3.onSurfaceVariant)
        col.addView(mac)
        val meta = W.row(c)
        meta.addView(W.body(c, "Bateria  ", 12f, M3.onSurfaceVariant))
        batV = W.semi(c, "—", 13f, M3.onSurface)
        meta.addView(batV)
        meta.addView(W.body(c, "   FW  ", 12f, M3.onSurfaceVariant))
        fwV = W.semi(c, "—", 13f, M3.onSurface)
        meta.addView(fwV)
        col.addView(meta, W.mlp(0, 8, 0, 0))
        addView(hero, W.mlp(0, 0, 0, 0))

        // klucz parowania
        addView(W.section(c, Hub.strings.key()))
        val keyCard = W.card(c)
        val keyCol = W.col(c)
        keyCard.addView(keyCol)
        keyCol.addView(W.body(c, "32 znaki HEX z konta Zepp — szyfrowane Android Keystore.",
            12f, M3.onSurfaceVariant))
        val keyRow = W.row(c)
        keyInput = W.input(c, "0011…eEff", if (Hub.secrets.hasKey()) "••••••••••••••••••••••••••••••••" else "")
        keyInput!!.typeface = Typeface.MONOSPACE
        keyInput!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        keyRow.addView(keyInput, LinearLayout.LayoutParams(0, -2, 1f))
        keyCol.addView(keyRow, W.mlp(0, 8, 0, 0))
        val saveKey = W.btn(c, Hub.strings.saveKey(), true)
        saveKey.setOnClickListener {
            if (Hub.watch.saveKey(keyInput!!.text.toString().trim()))
                Toast.makeText(ctx, "Klucz zapisany (Keystore).", Toast.LENGTH_SHORT).show()
            else Toast.makeText(ctx, "Klucz musi mieć 32 znaki HEX.", Toast.LENGTH_LONG).show()
        }
        keyCol.addView(saveKey, W.mlp(0, 10, 0, 0))
        addView(keyCard, W.mlp(0, 2, 0, 0))

        // akcje
        val actions = W.row(c)
        scanBtn = W.btn(c, "Skanuj")
        scanBtn!!.setOnClickListener {
            if (Hub.watch.isScanning()) { Hub.watch.stopScan(); return@setOnClickListener }
            Hub.watch.canScan().let { if (it != null) { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); return@setOnClickListener } }
            list?.removeAllViews()
            list?.addView(W.body(ctx, "Skanowanie… (20 s)", 12f, M3.onSurfaceVariant))
            Hub.watch.scan()
        }
        val connect = W.btn(c, Hub.strings.connect(), true)
        connect.setOnClickListener {
            val m = Hub.settings.getString(SettingsPort.Keys.MAC, "")
            if (m.isEmpty()) { Toast.makeText(ctx, "Najpierw zeskanuj i wybierz zegarek.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            Hub.watch.connect(m)
        }
        val sync = W.btn(c, Hub.strings.sync())
        sync.setOnClickListener { Hub.watch.sync() }
        val disc = W.btn(c, Hub.strings.disconnect(), false)
        disc.setOnClickListener { Hub.watch.disconnect() }
        for (b in listOf(scanBtn, connect, sync, disc)) {
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            lp.setMargins(M3.dp(3f), 0, M3.dp(3f), 0)
            actions.addView(b, lp)
        }
        addView(actions, W.mlp(0, 12, 0, 0))

        // akcje urządzenia
        val devCard = W.card(c)
        val devCol = W.col(c)
        devCard.addView(devCol)
        devCol.addView(W.item(ctx, W.Icon.CLOCK, "Alarmy zegarka", "5 alarmów · Huami 0x0003") { alarms() })
        devCol.addView(W.div(ctx))
        devCol.addView(W.item(ctx, W.Icon.GEAR, "Ustawienia zegarka", "Podświetlenie, jednostki, powiadomienia") { options() })
        devCol.addView(W.div(ctx))
        devCol.addView(W.item(ctx, W.Icon.SEARCH, "Znajdź zegarek", "Wibracja 3 s") { Hub.watch.findWatch() })
        devCol.addView(W.div(ctx))
        devCol.addView(W.item(ctx, W.Icon.BELL, "Test powiadomienia", "ANS") { Hub.watch.testNotify() })
        devCol.addView(W.div(ctx))
        devCol.addView(W.item(ctx, W.Icon.TRASH, Hub.strings.forget(), "Usuwa zegarek i klucz") { forget() })
        addView(devCard, W.mlp(0, 12, 0, 0))

        // znalezione
        addView(W.section(c, "Znalezione urządzenia"))
        val found = W.card(c)
        list = W.col(c)
        found.addView(list)
        list?.addView(W.body(ctx, "Skanowanie wykrywa Amazfit/Huami/Zepp (nazwa lub FEE1).",
            12f, M3.onSurfaceVariant))
        addView(found, W.mlp(0, 2, 0, 0))

        // log
        addView(W.section(c, "Log synchronizacji"))
        val logCard = W.card(c)
        logList = W.col(c)
        logCard.addView(logList)
        addView(logCard, W.mlp(0, 2, 0, 0))
    }

    private fun forget() {
        AlertDialog.Builder(ctx)
            .setTitle(Hub.strings.forget())
            .setMessage("Usunie zegarek i klucz parowania (Keystore). Dane zdrowotne zostaną.")
            .setPositiveButton("Usuń") { _, _ -> Hub.watch.forgetDevice(); refresh() }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun alarms() {
        val alarms = M.AlarmCodec.decode(Hub.settings.getString(SettingsPort.Keys.ALARMS, ""))
        val listV = W.col(ctx)
        for (a in alarms) {
            val row = W.row(ctx)
            row.setPadding(0, M3.dp(6f), 0, M3.dp(6f))
            row.addView(W.semi(ctx, M.AlarmCodec.hm(a.hour, a.minute), 18f,
                if (a.enabled) M3.onSurface else M3.textFaint))
            val mid = W.col(ctx)
            mid.addView(W.body(ctx, a.daysLabel(), 11.5f, M3.onSurfaceVariant))
            row.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
            mid.setPadding(M3.dp(10f), 0, 0, 0)
            val sw = W.chip(ctx, if (a.enabled) "WŁ" else "WYŁ", a.enabled)
            sw.setOnClickListener { a.enabled = !a.enabled; save(alarms); alarms() }
            row.addView(sw)
            row.isClickable = true
            row.setOnClickListener {
                TimePickerDialog(ctx, { _, h, m -> a.hour = h; a.minute = m; save(alarms); alarms() },
                    a.hour, a.minute, true).show()
            }
            listV.addView(row)
        }
        AlertDialog.Builder(ctx)
            .setTitle("Alarmy")
            .setView(listV)
            .setPositiveButton("Wyślij na zegarek") { _, _ -> Hub.watch.syncAlarms(alarms) }
            .setNegativeButton("Zamknij", null)
            .show()
    }

    private fun save(a: List<M.Alarm>) {
        Hub.settings.putString(SettingsPort.Keys.ALARMS, M.AlarmCodec.encode(a))
    }

    private fun options() {
        data class Opt(val label: String, val on: ByteArray, val off: ByteArray, val key: String)
        val items = listOf(
            Opt("Podświetlenie przy uniesieniu", Codec.Opt.LIFT_ON, Codec.Opt.LIFT_OFF, "watch_lift"),
            Opt("Powiadomienie o celu kroków", Codec.Opt.GOAL_ON, Codec.Opt.GOAL_OFF, "watch_goal"),
            Opt("Powiadomienie o rozłączeniu", Codec.Opt.DISC_ON, Codec.Opt.DISC_OFF, "watch_disc"),
            Opt("Pokazuj dzwoniącego", Codec.Opt.CALLER_ON, Codec.Opt.CALLER_OFF, "watch_caller"),
            Opt("Jednostki metryczne", Codec.Opt.METRIC_ON, Codec.Opt.METRIC_OFF, "watch_metric"),
            Opt("Zegar 24 h", Codec.Opt.T24_ON, Codec.Opt.T24_OFF, "watch_t24"),
        )
        val listV = W.col(ctx)
        for (o in items) {
            val row = W.row(ctx)
            val initial = Hub.settings.getBool(o.key, true)
            row.addView(W.body(ctx, o.label, 13f, M3.onSurface), LinearLayout.LayoutParams(0, -2, 1f))
            val sw = W.chip(ctx, if (initial) "WŁ" else "WYŁ", initial)
            row.addView(sw)
            row.setPadding(0, M3.dp(6f), 0, M3.dp(6f))
            row.isClickable = true
            row.setOnClickListener {
                val now = !Hub.settings.getBool(o.key, true)
                Hub.settings.putBool(o.key, now)
                Hub.watch.setOption(if (now) o.on else o.off, o.label)
                options()
            }
            listV.addView(row)
        }
        AlertDialog.Builder(ctx).setTitle("Ustawienia zegarka").setView(listV)
            .setPositiveButton("Zamknij", null).show()
    }

    fun onDeviceFound(d: M.BleDevice) {
        val row = W.row(ctx)
        row.setPadding(0, M3.dp(7f), 0, M3.dp(7f))
        val ic = W.Icon(ctx); ic.setIcon(W.Icon.WATCH); ic.setColor(M3.textSec)
        ic.layoutParams = LinearLayout.LayoutParams(M3.dp(22f), M3.dp(22f))
        row.addView(ic)
        val mid = W.col(ctx)
        mid.addView(W.semi(ctx, d.name, 14f, M3.onSurface))
        mid.addView(W.body(ctx, Hex.maskMac(d.address), 11f, M3.textFaint))
        row.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(W.body(ctx, "RSSI " + d.rssi, 11.5f, M3.textSec))
        row.isClickable = true; W.ripple(row)
        row.setOnClickListener {
            Hub.settings.putString(SettingsPort.Keys.MAC, d.address)
            Hub.settings.putString(SettingsPort.Keys.NAME, d.name)
            Toast.makeText(ctx, "Zapamiętano ${d.name}", Toast.LENGTH_SHORT).show()
            Hub.watch.connect(d.address)
        }
        list?.addView(row)
    }

    fun setScanningUi(on: Boolean) { scanBtn?.text = if (on) "Stop" else "Skanuj" }

    fun refreshLog() {
        val log = logList ?: return
        log.removeAllViews()
        for (e in Hub.data.syncLog(12)) {
            val row = W.row(ctx)
            val color = when (e.level) { "ERROR" -> M3.danger; "WARN" -> M3.warn; "OK" -> M3.good; else -> M3.textFaint }
            val dot = W.Dot(ctx); dot.setColor(color)
            dot.layoutParams = LinearLayout.LayoutParams(M3.dp(6f), M3.dp(6f))
            row.addView(dot)
            row.addView(W.body(ctx, "  " + Time.hhmm(e.ts), 11f, M3.textFaint))
            val msg = W.body(ctx, "  ${e.message}", 12.5f, if (e.level == "ERROR") M3.danger else M3.textSec)
            msg.maxLines = 3
            row.addView(msg)
            log.addView(row)
            log.addView(W.div(ctx), LinearLayout.LayoutParams(-1, 1))
        }
        if (Hub.data.syncLog(1).isEmpty())
            log.addView(W.body(ctx, "Brak wpisów — połącz zegarek i Sync.", 12f, M3.textFaint))
    }

    fun refresh() {
        val s = Hub.settings
        val macS = s.getString(SettingsPort.Keys.MAC, "")
        status?.text = when (Hub.watch.state.value) {
            is WatchState.Connecting -> "łączenie…"
            is WatchState.Authenticating -> "autoryzacja…"
            is WatchState.Connected -> "połączono"
            is WatchState.Error -> "błąd"
            is WatchState.Scanning -> "skanowanie…"
            else -> if (macS.isNotEmpty()) "zapamiętano" else "nie połączono"
        }
        status?.setTextColor(if (Hub.watch.isConnected()) M3.good else M3.textFaint)
        name?.text = if (macS.isNotEmpty()) s.getString(SettingsPort.Keys.NAME, "Zegarek") else "Brak zegarka"
        mac?.text = if (macS.isNotEmpty()) Hex.maskMac(macS) else "—"
        val b = s.getInt(SettingsPort.Keys.BATTERY, -1)
        batV?.text = if (b < 0) "—" else "$b%"
        val fw = s.getString(SettingsPort.Keys.FW, "")
        fwV?.text = if (fw.isNotEmpty()) fw else "—"
        refreshLog()
    }
}
