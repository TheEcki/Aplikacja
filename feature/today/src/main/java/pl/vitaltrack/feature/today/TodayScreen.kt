package pl.vitaltrack.feature.today

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import pl.vitaltrack.core.common.HrHistory
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.Screens
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import pl.vitaltrack.core.model.Estimates
import pl.vitaltrack.core.model.M
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dziś — poranne podsumowanie (wzorzec Zepp): hero z krokami i celem,
 * siatka kart metryk (HR, sen, kalorie, dystans), wykres 7 dni i 24 h HR.
 */
class TodayScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var ring: W.Ring? = null
    private var hr: MetricBox? = null
    private var bat: MetricBox? = null
    private var sleep: MetricBox? = null
    private var cal: MetricBox? = null
    private var dist: MetricBox? = null
    private var move: MetricBox? = null
    private var hrSpark: W.Spark? = null
    private var bars: W.Bars? = null
    private var line: W.Line? = null
    private var syncChip: TextView? = null

    /** Wynik karty metryki: duża wartość + podpis. */
    private class MetricBox(val value: TextView, val sub: TextView)

    private val TAG_METRIC = 0x0A110001

    init {
        orientation = VERTICAL
        build(ctx)
    }

    private fun build(c: Context) {
        setPadding(M3.dp(16f), M3.dp(10f), M3.dp(16f), M3.dp(120f))

        val hero = W.hero(c)
        val heroCol = W.col(c)
        hero.addView(heroCol)
        val h = SimpleDateFormat("HH", Locale.US).format(Date()).toIntOrNull() ?: 12
        val greet = when { h < 6 -> "Dobranoc"; h < 12 -> "Dzień dobry"; h < 18 -> "Miłego dnia"; else -> "Dobry wieczór" }
        heroCol.addView(W.title(c, greet))
        heroCol.addView(W.body(c, niceDate(), 13f, M3.onSurfaceVariant))
        val row = W.row(c)
        row.addView(W.spacer(c))
        syncChip = W.med(c, "—", 12f, M3.onSurfaceVariant)
        row.addView(syncChip)
        heroCol.addView(row, W.mlp(0, 8, 0, 0))
        addView(hero, W.mlp(0, 0, 0, 0))

        val ringCard = W.card(c)
        val ringCol = W.col(c)
        ringCard.addView(ringCol)
        ringCol.addView(W.section(c, Hub.strings.steps()))
        ring = W.Ring(c)
        ringCol.addView(ring, 0, LinearLayout.LayoutParams(M3.dp(210f), M3.dp(220f)))
        addView(ringCard, W.mlp(0, 12, 0, 0))

        fun metricRow(icon: Int, label: String): LinearLayout {
            val r = W.row(c)
            r.gravity = Gravity.TOP
            val box = metricCard(c, icon, label)
            r.addView(box, W.weight())
            return r
        }

        addView(metricRow(W.Icon.HEART, Hub.strings.heartRate()).apply {
            hr = (getChildAt(0) as LinearLayout).getTag(TAG_METRIC) as MetricBox
        }, W.mlp(0, 12, 0, 0))
        addView(metricRow(W.Icon.BOLT, "Bateria").apply {
            bat = (getChildAt(0) as LinearLayout).getTag(TAG_METRIC) as MetricBox
        }, W.mlp(0, 12, 0, 0))
        addView(metricRow(W.Icon.MOON, Hub.strings.sleep()).apply {
            sleep = (getChildAt(0) as LinearLayout).getTag(TAG_METRIC) as MetricBox
        }, W.mlp(0, 12, 0, 0))
        addView(metricRow(W.Icon.FLAME, Hub.strings.calories()).apply {
            cal = (getChildAt(0) as LinearLayout).getTag(TAG_METRIC) as MetricBox
        }, W.mlp(0, 12, 0, 0))
        addView(metricRow(W.Icon.BOOT, Hub.strings.distance()).apply {
            dist = (getChildAt(0) as LinearLayout).getTag(TAG_METRIC) as MetricBox
        }, W.mlp(0, 12, 0, 0))
        addView(metricRow(W.Icon.CLOCK, Hub.strings.move()).apply {
            move = (getChildAt(0) as LinearLayout).getTag(TAG_METRIC) as MetricBox
        }, W.mlp(0, 12, 0, 0))

        val week = W.card(c)
        val weekCol = W.col(c)
        week.addView(weekCol)
        weekCol.addView(W.section(c, Hub.strings.steps() + " — 7 dni"))
        bars = W.Bars(c)
        bars!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(140f))
        weekCol.addView(bars, W.mlp(0, 4, 0, 0))
        addView(week, W.mlp(0, 12, 0, 0))

        val hrCard = W.card(c)
        val hrCol = W.col(c)
        hrCard.addView(hrCol)
        hrCol.addView(W.section(c, Hub.strings.heartRate() + " — 24 h"))
        line = W.Line(c)
        line!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(140f))
        hrCol.addView(line)
        addView(hrCard, W.mlp(0, 12, 0, 0))
    }

    private fun metricCard(c: Context, icon: Int, label: String): android.widget.FrameLayout {
        val card = W.card(c)
        val col = W.col(c)
        card.addView(col)
        val head = W.row(c)
        val ic = W.Icon(c)
        ic.setIcon(icon); ic.setColor(M3.accent)
        ic.layoutParams = LinearLayout.LayoutParams(M3.dp(22f), M3.dp(22f))
        head.addView(ic)
        head.addView(W.body(c, "  $label", 12.5f, M3.onSurfaceVariant))
        col.addView(head)
        val v = W.bold(c, "—", 30f, M3.onSurface)
        col.addView(v, W.mlp(0, 4, 0, 0))
        val sub = W.body(c, "", 11.5f, M3.onSurfaceVariant)
        col.addView(sub)
        card.setTag(TAG_METRIC, MetricBox(v, sub))
        return card
    }

    private fun niceDate(): String =
        SimpleDateFormat("EEEE, d MMMM", Locale("pl", "PL")).format(Date())

    fun refresh() {
        val d = Hub.data
        val today = Time.today()
        val day: M.Day? = d.day(today)
        val steps = day?.steps ?: 0
        val goal = Hub.settings.getInt(SettingsPort.Keys.GOAL_STEPS, 10000)

        ring?.setData(if (goal > 0) steps.toFloat() / goal else 0f,
            String.format(Locale.US, "%,d", steps), "z " + String.format(Locale.US, "%,d", goal))

        val last = Hub.settings.getLong(SettingsPort.Keys.LAST_SYNC, 0L)
        syncChip?.text = if (last > 0) "sync " + Time.hhmm(last) else "brak sync"

        val hrSample = d.latestHr()
        if (hrSample != null && System.currentTimeMillis() - hrSample.ts < 15 * 60 * 1000) {
            hr?.value?.text = hrSample.bpm.toString()
            hr?.sub?.text = Estimates.zoneOf(hrSample.bpm, maxHr()).name + " · " + Time.hhmm(hrSample.ts)
        } else {
            hr?.value?.text = "—"
            hr?.sub?.text = Hub.strings.emptyHr()
        }
        hrSpark?.setData(HrHistory.values(), M3.danger)

        val battery = Hub.settings.getInt(SettingsPort.Keys.BATTERY, -1)
        bat?.value?.text = if (battery < 0) "—" else "$battery%"

        sleep?.value?.text = if (day != null && day.sleepMin > 0)
            String.format(Locale.US, "%dh %02dmin", day.sleepMin / 60, day.sleepMin % 60) else "—"
        cal?.value?.text = day?.calories?.toString() ?: "—"
        dist?.value?.text = if (day != null) String.format(Locale.US, "%.1f km", day.distanceM / 1000.0) else "—"
        move?.value?.text = (day?.moveMinutes?.toString() ?: "—") + " min"

        val vals = IntArray(7); val labels = Array(7) { "" }
        for (i in 0 until 7) {
            val date = Time.addDays(today, -(6 - i))
            vals[i] = d.day(date)?.steps ?: 0
            labels[i] = date.substring(5)
        }
        bars?.setData(vals, labels)

        val start = Time.startOfDay(System.currentTimeMillis())
        val samples = d.hr(start - Time.DAY, System.currentTimeMillis())
        val bins = FloatArray(24)
        for (s in samples) {
            val idx = ((s.ts - (start - Time.DAY)) / Time.HOUR).toInt()
            if (idx in 0 until 24 && s.bpm > bins[idx]) bins[idx] = s.bpm.toFloat()
        }
        val mh = maxHr()
        line?.setZone(mh * 0.6f, mh * 0.85f)
        line?.setData(bins, arrayOf("00", "06", "12", "18", "24"), Hub.strings.emptyHr())
    }

    private fun maxHr(): Int = Estimates.maxHr(
        Hub.settings.getInt(SettingsPort.Keys.MAX_HR, 0),
        Hub.settings.getInt(SettingsPort.Keys.AGE, 30))
}
