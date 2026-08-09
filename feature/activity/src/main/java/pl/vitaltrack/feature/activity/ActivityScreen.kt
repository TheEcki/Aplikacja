package pl.vitaltrack.feature.activity

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import java.util.Locale

/** Aktywność — kroki / dystans / kalorie na dzień (7/30 dni). */
class ActivityScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var period = 7
    private var bars: W.Bars? = null
    private var calBars: W.Bars? = null
    private var summary: TextView? = null

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        setPadding(M3.dp(16f), M3.dp(10f), M3.dp(16f), M3.dp(120f))
        val chips = W.row(c)
        val c7 = W.chip(c, "7 dni", true)
        val c30 = W.chip(c, "30 dni", false)
        chips.addView(c7, W.weight()); chips.addView(c30, W.weight())
        addView(chips)
        c7.setOnClickListener { period = 7; style(c7, true); style(c30, false); refresh() }
        c30.setOnClickListener { period = 30; style(c30, true); style(c7, false); refresh() }

        val card = W.card(c)
        val col = W.col(c)
        card.addView(col)
        col.addView(W.section(c, "Podsumowanie"))
        summary = W.semi(c, "", 15f, M3.onSurface)
        col.addView(summary, W.mlp(0, 4, 0, 0))
        col.addView(W.section(c, Hub.strings.steps()), W.mlp(0, 8, 0, 0))
        bars = W.Bars(c)
        bars!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(150f))
        col.addView(bars, W.mlp(0, 4, 0, 0))
        col.addView(W.section(c, Hub.strings.calories()), W.mlp(0, 8, 0, 0))
        calBars = W.Bars(c)
        calBars!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(120f))
        col.addView(calBars, W.mlp(0, 4, 0, 0))
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
        val days = Hub.data.lastDays(period)
        val steps = IntArray(days.size); val cals = IntArray(days.size); val labels = Array(days.size) { "" }
        var sSum = 0; var cSum = 0; var mSum = 0; var dSum = 0
        for (i in days.indices) {
            val d = days[days.size - 1 - i]
            sSum += d.steps; cSum += d.calories; mSum += d.moveMinutes; dSum += d.distanceM
            steps[i] = d.steps; cals[i] = d.calories; labels[i] = d.date.substring(5)
        }
        bars?.setData(steps, labels)
        calBars?.setData(cals, labels)
        summary?.text = String.format(Locale.US,
            "%s: %,d · %s: %.1f km · %s: %,d · %s: %d min",
            Hub.strings.steps(), sSum, Hub.strings.distance(), dSum / 1000.0,
            Hub.strings.calories(), cSum, Hub.strings.move(), mSum)
    }
}
