package pl.vitaltrack.feature.heartrate

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import pl.vitaltrack.core.model.Estimates
import pl.vitaltrack.core.model.M
import java.util.Locale

/** Tętno — dzień/tydzień/miesiąc, min/max/śr, strefy. */
class HeartScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var period = "day"
    private var chart: W.Line? = null
    private var summary: TextView? = null
    private var zones: W.Stage? = null
    private var zoneLegend: LinearLayout? = null

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        addView(W.back(c, Hub.strings.heartRate()) { nav.back() })
        setPadding(M3.dp(16f), 0, M3.dp(16f), M3.dp(120f))
        val chips = W.row(c)
        val d = W.chip(c, "Dzień", true); val w = W.chip(c, "Tydzień", false); val m = W.chip(c, "Miesiąc", false)
        for (ch in listOf(d, w, m)) chips.addView(ch, W.weight())
        addView(chips, W.mlp(0, 10, 0, 0))
        d.setOnClickListener { period = "day"; style(d, true); style(w, false); style(m, false); refresh() }
        w.setOnClickListener { period = "week"; style(w, true); style(d, false); style(m, false); refresh() }
        m.setOnClickListener { period = "month"; style(m, true); style(d, false); style(w, false); refresh() }

        val card = W.card(c)
        val col = W.col(c)
        card.addView(col)
        col.addView(W.section(c, "Podsumowanie"))
        summary = W.semi(c, "", 15f, M3.onSurface)
        col.addView(summary, W.mlp(0, 4, 0, 0))
        chart = W.Line(c)
        chart!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(170f))
        col.addView(chart, W.mlp(0, 4, 0, 0))
        addView(card, W.mlp(0, 12, 0, 0))

        val zoneCard = W.card(c)
        val zoneCol = W.col(c)
        zoneCard.addView(zoneCol)
        zoneCol.addView(W.section(c, "Strefy"))
        zones = W.Stage(c)
        zones!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(10f))
        zoneCol.addView(zones, W.mlp(0, 6, 0, 0))
        zoneLegend = W.row(c)
        zoneCol.addView(zoneLegend, W.mlp(0, 8, 0, 0))
        addView(zoneCard, W.mlp(0, 12, 0, 0))
    }

    private fun style(chip: TextView, sel: Boolean) {
        val g = android.graphics.drawable.GradientDrawable().apply {
            setCornerRadius(M3.dp(M3.Shape.SMALL.toFloat()).toFloat())
            if (sel) setColor(M3.secondaryContainer) else { setColor(M3.surface); setStroke(M3.dp(1f), M3.outlineVariant) }
        }
        chip.background = g
        chip.setTextColor(if (sel) M3.onSecondaryContainer else M3.onSurfaceVariant)
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        val mh = Estimates.maxHr(Hub.settings.getInt(SettingsPort.Keys.MAX_HR, 0),
            Hub.settings.getInt(SettingsPort.Keys.AGE, 30))
        val (samples, from, to, count, binMs, labels) = when (period) {
            "week" -> {
                val start = Time.startOfDay(now - 6 * Time.DAY)
                Quad(Hub.data.hr(start, now), start, now, 24 * 7, Time.HOUR, { i -> "d" + (i / 24 + 1) })
            }
            "month" -> {
                val start = Time.startOfDay(now - 29 * Time.DAY)
                Quad(Hub.data.hr(start, now), start, now, 30, Time.DAY, { i -> Time.localDate(start + i * Time.DAY).substring(5) })
            }
            else -> {
                val start = Time.startOfDay(now)
                Quad(Hub.data.hr(start, now), start, now, 24, Time.HOUR, { i -> String.format("%02d", i) })
            }
        }
        val bins = FloatArray(count)
        for (s in samples) {
            val idx = ((s.ts - from) / binMs).toInt()
            if (idx in 0 until count && s.bpm > bins[idx]) bins[idx] = s.bpm.toFloat()
        }
        val lbl = Array(count) { labels(it) }
        chart?.setZone(mh * 0.6f, mh * 0.85f)
        chart?.setData(bins, lbl, if (samples.isEmpty()) Hub.strings.emptyHr() else "")

        val st = Hub.data.hrStats(from, to)
        val resting = Hub.settings.getInt(SettingsPort.Keys.RESTING, 0)
        summary?.text = String.format(Locale.US, "śr. %d · min %d · max %d bpm%s",
            st.first, st.second, st.third, if (resting > 0) " · spoczynkowe $resting" else "")

        // czas w strefach (dziś)
        val today = Hub.data.hr(Time.startOfDay(now), now)
        if (today.size >= 2) {
            val minutes = IntArray(5)
            var lastB = -1L; var lastZ = -1
            for (s in today) {
                val b = Time.minuteBucket(s.ts)
                val z = Estimates.zoneOf(s.bpm, mh)
                val zi = Estimates.ZONES.indexOfFirst { it.name == z.name }
                if (lastB >= 0 && b - lastB <= 2 && lastZ >= 0 && zi >= 0) minutes[zi] += (b - lastB).toInt()
                lastB = b; lastZ = zi
            }
            val colors = intArrayOf(c(M3, Estimates.ZONES[0]), c(M3, Estimates.ZONES[1]),
                c(M3, Estimates.ZONES[2]), c(M3, Estimates.ZONES[3]), c(M3, Estimates.ZONES[4]))
            zones?.setData(minutes.map { it.toFloat() }.toFloatArray(), colors)
            zoneLegend?.removeAllViews()
            for (i in 0 until 5) {
                val l = W.row(ctx)
                val dot = W.Dot(ctx); dot.setColor(colors[i])
                dot.layoutParams = LinearLayout.LayoutParams(M3.dp(7f), M3.dp(7f))
                l.addView(dot)
                l.addView(W.body(ctx, "  ${Estimates.ZONES[i].name} ${minutes[i]} min", 10.5f, M3.textSec))
                l.setPadding(0, 0, M3.dp(8f), 0)
                zoneLegend?.addView(l)
            }
        }
    }

    private data class Quad(val list: List<M.HrSample>, val from: Long, val to: Long,
                            val count: Int, val bin: Long, val label: (Int) -> String)

    private fun c(m: M3, z: Estimates.Zone): Int = try {
        android.graphics.Color.parseColor(z.colorHex)
    } catch (e: Exception) { M3.textSec }
}
