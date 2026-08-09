package pl.vitaltrack.feature.profile

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.Screens
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import java.io.File
import java.util.Locale

/** Profil — dane, motyw, jednostki, eksport, Health Connect, prywatność. */
class ProfileScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var age: EditText? = null
    private var weight: EditText? = null
    private var height: EditText? = null
    private var resting: EditText? = null
    private var maxHr: EditText? = null
    private var files: LinearLayout? = null

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        setPadding(M3.dp(16f), M3.dp(10f), M3.dp(16f), M3.dp(120f))

        addView(W.section(c, "Profil"))
        val prof = W.card(c)
        val profCol = W.col(c)
        prof.addView(profCol)
        age = num(c, "Wiek (lata)", Hub.settings.getInt(SettingsPort.Keys.AGE, 30).toString())
        weight = num(c, "Waga (kg)", Hub.settings.getInt(SettingsPort.Keys.WEIGHT, 70).toString())
        height = num(c, "Wzrost (cm)", Hub.settings.getInt(SettingsPort.Keys.HEIGHT, 175).toString())
        resting = num(c, "Tętno spoczynkowe (bpm)", Hub.settings.getInt(SettingsPort.Keys.RESTING, 60).toString())
        maxHr = num(c, "HRmax (0 = wzór)", Hub.settings.getInt(SettingsPort.Keys.MAX_HR, 0).toString())
        for (e in listOf(age, weight, height, resting, maxHr)) profCol.addView(e, W.mlp(0, 6, 0, 0))
        val save = W.btn(c, "Zapisz profil", true)
        save.setOnClickListener { saveProfile() }
        profCol.addView(save, W.mlp(0, 10, 0, 0))
        addView(prof, W.mlp(0, 2, 0, 0))

        addView(W.section(c, Hub.strings.goals()))
        val goal = W.card(c)
        val goalCol = W.col(c)
        goal.addView(goalCol)
        goalCol.addView(W.item(ctx, W.Icon.TARGET, Hub.strings.goals(), "Kroki · sen · treningi") {
            nav.open(Screens.GOALS)
        })
        addView(goal, W.mlp(0, 2, 0, 0))

        addView(W.section(c, "Wygląd"))
        val look = W.card(c)
        val lookCol = W.col(c)
        look.addView(lookCol)
        val themeRow = W.row(c)
        val dark = W.chip(c, "Ciemny", M3.dark)
        val light = W.chip(c, "Jasny", !M3.dark)
        themeRow.addView(dark, W.weight()); themeRow.addView(light, W.weight())
        lookCol.addView(themeRow)
        dark.setOnClickListener { Hub.settings.putBool(SettingsPort.Keys.DARK, true); nav.refresh() }
        light.setOnClickListener { Hub.settings.putBool(SettingsPort.Keys.DARK, false); nav.refresh() }
        addView(look, W.mlp(0, 2, 0, 0))

        addView(W.section(c, Hub.strings.exportData()))
        val exp = W.card(c)
        val expCol = W.col(c)
        exp.addView(expCol)
        val export = W.btn(c, "Eksportuj (CSV/JSON) i udostępnij", true)
        export.setOnClickListener { doExport() }
        expCol.addView(export)
        files = W.col(c)
        expCol.addView(files)
        addView(exp, W.mlp(0, 2, 0, 0))

        addView(W.section(c, "Health Connect"))
        val hc = W.card(c)
        val hcCol = W.col(c)
        hc.addView(hcCol)
        hcCol.addView(W.body(c, if (Hub.hcStatus.isAvailable) "Health Connect dostępny"
            else "Health Connect: " + (Hub.hcStatus.reason ?: "niedostępny"),
            12.5f, if (Hub.hcStatus.isAvailable) M3.good else M3.textSec))
        val open = W.btn(c, "Otwórz Health Connect", false)
        open.setOnClickListener {
            try { ctx.startActivity(pl.vitaltrack.core.healthconnect.HcIntents.launch(ctx)) }
            catch (e: Exception) { Toast.makeText(ctx, "Zainstaluj Health Connect.", Toast.LENGTH_LONG).show() }
        }
        hcCol.addView(open, W.mlp(0, 8, 0, 0))
        addView(hc, W.mlp(0, 2, 0, 0))

        addView(W.section(c, "Prywatność"))
        val priv = W.card(c)
        val privCol = W.col(c)
        priv.addView(privCol)
        val clear = W.btn(ctx, "Wyczyść wszystkie dane", false)
        clear.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Wyczyścić dane?")
                .setMessage("Usunie metryki, tętno, treningi i log. Ustawienia i klucz zostaną.")
                .setPositiveButton("Wyczyść") { _, _ -> Hub.data.clearAll(); nav.refresh() }
                .setNegativeButton("Anuluj", null).show()
        }
        privCol.addView(clear)
        addView(priv, W.mlp(0, 2, 0, 0))

        addView(W.section(c, "O aplikacji"))
        val about = W.card(c)
        val aboutCol = W.col(c)
        about.addView(aboutCol)
        aboutCol.addView(W.semi(c, "VitalTrack 1.0", 15f, M3.onSurface))
        aboutCol.addView(W.body(c, "Natywna aplikacja do synchronizacji Amazfit Bip 6 (BLE, Huami).\n" +
            "Bez serwerów — dane i klucz pozostają na telefonie.", 12f, M3.textSec), W.mlp(0, 6, 0, 0))
        val note = W.row(c)
        val info = W.Icon(c); info.setIcon(W.Icon.INFO); info.setColor(M3.info)
        info.layoutParams = LinearLayout.LayoutParams(M3.dp(16f), M3.dp(16f))
        note.addView(info)
        note.addView(W.body(ctx, "  To nie jest wyrób medyczny. VO₂max i kalorie to szacunki.",
            11.5f, M3.textFaint))
        aboutCol.addView(note, W.mlp(0, 8, 0, 0))
        addView(about, W.mlp(0, 2, 0, 0))
    }

    private fun num(c: Context, hint: String, value: String): EditText =
        W.input(c, hint, value).apply { inputType = InputType.TYPE_CLASS_NUMBER }

    private fun saveProfile() {
        val s = Hub.settings
        try { s.putInt(SettingsPort.Keys.AGE, maxOf(5, age!!.text.toString().trim().toInt())) } catch (ignored: Exception) {}
        try { s.putInt(SettingsPort.Keys.WEIGHT, maxOf(20, weight!!.text.toString().trim().toInt())) } catch (ignored: Exception) {}
        try { s.putInt(SettingsPort.Keys.HEIGHT, maxOf(80, height!!.text.toString().trim().toInt())) } catch (ignored: Exception) {}
        try { s.putInt(SettingsPort.Keys.RESTING, maxOf(30, resting!!.text.toString().trim().toInt())) } catch (ignored: Exception) {}
        try { s.putInt(SettingsPort.Keys.MAX_HR, maxOf(0, maxHr!!.text.toString().trim().toInt())) } catch (ignored: Exception) {}
        Toast.makeText(ctx, "Profil zapisany.", Toast.LENGTH_SHORT).show()
        nav.refresh()
    }

    private fun doExport() {
        val dir = File(ctx.getExternalFilesDir(null), "export").apply { mkdirs() }
        val list = ArrayList<File>()
        try {
            list.add(Export.daily(ctx, dir))
            list.add(Export.hr(ctx, dir))
            list.add(Export.workouts(ctx, dir))
            list.add(Export.json(ctx, dir))
        } catch (ignored: Exception) {}
        files?.removeAllViews()
        for (f in list) {
            val row = W.row(ctx)
            row.addView(W.body(ctx, f.name, 12.5f, M3.textSec))
            row.addView(W.spacer(ctx))
            row.addView(W.body(ctx, size(f.length()), 11f, M3.textFaint))
            files?.addView(row)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                ArrayList(list.map { Uri.parse("content://pl.vitaltrack.files/" + it.name) }))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Udostępnij dane VitalTrack"))
    }

    private fun size(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> String.format(Locale.US, "%.1f kB", b / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
    }
}

/** Eksport CSV/JSON — bez klucza. */
object Export {
    fun daily(c: Context, dir: File): File {
        val f = File(dir, "vitaltrack_daily.csv")
        java.io.FileWriter(f).use { w ->
            w.write("date,steps,calories,move_minutes,cardio_points,distance_m,sleep_min,resting_hr,avg_hr,max_hr,battery\n")
            for (d in Hub.data.lastDays(90)) {
                w.write(String.format(Locale.US, "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                    d.date, d.steps, d.calories, d.moveMinutes, d.cardioPoints, d.distanceM,
                    d.sleepMin, d.restingHr, d.avgHr, d.maxHr, d.battery))
            }
        }
        return f
    }

    fun hr(c: Context, dir: File): File {
        val f = File(dir, "vitaltrack_heart_rate.csv")
        java.io.FileWriter(f).use { w ->
            w.write("timestamp_utc_ms,iso_utc,bpm\n")
            for (h in Hub.data.hr(0, Long.MAX_VALUE))
                w.write(h.ts.toString() + "," + Time.isoUtc(h.ts) + "," + h.bpm + "\n")
        }
        return f
    }

    fun workouts(c: Context, dir: File): File {
        val f = File(dir, "vitaltrack_workouts.csv")
        java.io.FileWriter(f).use { w ->
            w.write("id,type,start_iso,duration_sec,distance_m,avg_hr,max_hr,calories\n")
            for (x in Hub.data.workouts(200))
                w.write(String.format(Locale.US, "%d,%s,%s,%d,%d,%d,%d,%d\n",
                    x.id, x.type, Time.isoUtc(x.startTs), x.durationSec, x.distanceM,
                    x.avgHr, x.maxHr, x.calories))
        }
        return f
    }

    fun json(c: Context, dir: File): File {
        val f = File(dir, "vitaltrack_backup.json")
        java.io.FileWriter(f).use { w ->
            val sb = StringBuilder()
            sb.append("{\"app\":\"VitalTrack\",\"exported\":\"").append(Time.isoUtc(System.currentTimeMillis()))
                .append("\",\"daily\":[")
            var first = true
            for (d in Hub.data.lastDays(90)) {
                if (!first) sb.append(",")
                first = false
                sb.append("{\"date\":\"").append(d.date).append("\",\"steps\":").append(d.steps)
                    .append(",\"calories\":").append(d.calories)
                    .append(",\"sleepMin\":").append(d.sleepMin)
                    .append(",\"restingHr\":").append(d.restingHr).append("}")
            }
            sb.append("],\"heartRate\":[")
            first = true
            for (h in Hub.data.hr(0, Long.MAX_VALUE)) {
                if (!first) sb.append(",")
                first = false
                sb.append("{\"ts\":").append(h.ts).append(",\"bpm\":").append(h.bpm).append("}")
            }
            sb.append("],\"workouts\":[")
            first = true
            for (x in Hub.data.workouts(200)) {
                if (!first) sb.append(",")
                first = false
                sb.append("{\"id\":").append(x.id).append(",\"type\":\"").append(x.type)
                    .append("\",\"start\":").append(x.startTs).append("}")
            }
            sb.append("]}\n")
            w.write(sb.toString())
        }
        return f
    }
}
