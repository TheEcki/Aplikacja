package pl.vitaltrack.feature.trends

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import java.util.Locale

/** Trendy — 7/30/90/365 dni: kroki, sen, tętno spoczynkowe. */
class TrendsScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var period = 7
    private var bars: W.Bars? = null
    private var sleep: W.Line? = null
    private var resting: W.Line? = null
    private var summary: TextView? = null
    private val chips = ArrayList<TextView>()

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        addView(W.back(c, Hub.strings.trends()) { nav.back() })
        setPadding(M3.dp(16f), 0, M3.dp(16f), M3.dp(120f))
        val row = W.row(c)
        val labels = arrayOf("7", "30", "90", "365")
        for (i in labels.indices) {
            val ch = W.chip(c, labels[i] + " dni", i == 0)
            row.addView(ch, W.weight()); chips.add(ch)
            ch.setOnClickListener { period = labels[i].toInt(); for (k in chips.indices) style(chips[k], k == i); refresh() }
        }
        addView(row, W.mlp(0, 10, 0, 0))
        val card = W.card(c)
        val col = W.col(c)
        card.addView(col)
        col.addView(W.section(c, "Podsumowanie"))
        summary = W.semi(c, "", 14.5f, M3.onSurface)
        col.addView(summary, W.mlp(0, 4, 0, 0))
        col.addView(W.section(c, Hub.strings.steps()), W.mlp(0, 8, 0, 0))
        bars = W.Bars(c)
        bars!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(150f))
        col.addView(bars, W.mlp(0, 4, 0, 0))
        col.addView(W.section(c, "Sen (min)"), W.mlp(0, 8, 0, 0))
        sleep = W.Line(c)
        sleep!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(100f))
        col.addView(sleep, W.mlp(0, 4, 0, 0))
        col.addView(W.section(c, "Tętno spoczynkowe"), W.mlp(0, 8, 0, 0))
        resting = W.Line(c)
        resting!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(100f))
        col.addView(resting, W.mlp(0, 4, 0, 0))
        addView(card, W.mlp(0, 12, 0, 0))
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
        val step = if (period > 90) 7 else 1
        val n = period / step
        val today = Time.today()
        val vals = IntArray(n); val sl = FloatArray(n); val rs = FloatArray(n)
        val labels = Array(n) { "" }
        var sSum = 0; var slSum = 0; var slCnt = 0; var rSum = 0; var rCnt = 0
        for (i in 0 until n) {
            val from = Time.addDays(today, -(period - i * step))
            val to = Time.addDays(from, step - 1)
            var s = 0; var sle = 0; var r = 0; var rc = 0
            var date = from
            while (date <= to) {
                val d = Hub.data.day(date)
                if (d != null) { s += d.steps; sle += d.sleepMin; if (d.restingHr > 0) { r += d.restingHr; rc++ } }
                date = Time.addDays(date, 1)
            }
            vals[i] = s; sl[i] = sle.toFloat(); rs[i] = if (rc > 0) r.toFloat() / rc else 0f
            labels[i] = if (step == 1) from.substring(5) else from.substring(5, 7)
            sSum += s; slSum += sle; if (sle > 0) slCnt++
            if (rc > 0) { rSum += r; rCnt++ }
        }
        bars?.setData(vals, labels)
        sleep?.setData(sl, labels, "brak danych")
        resting?.setData(rs, labels, "brak danych")
        summary?.text = String.format(Locale.US, "Kroki: %,d · Śr. sen: %s · Śr. HR spocz.: %s",
            sSum, if (slCnt > 0) mm(slSum / slCnt) else "—",
            if (rCnt > 0) (rSum / rCnt).toString() else "—")
    }

    private fun mm(min: Int): String = String.format(Locale.US, "%dh%02d", min / 60, min % 60)
}
