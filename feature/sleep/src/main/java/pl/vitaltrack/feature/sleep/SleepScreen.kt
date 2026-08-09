package pl.vitaltrack.feature.sleep

import android.content.Context
import android.widget.LinearLayout
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import pl.vitaltrack.core.model.M
import java.util.Locale

/** Sen — podsumowanie ostatniej nocy + 7 dni; uczciwy empty state. */
class SleepScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    private var value: android.widget.TextView? = null
    private var sub: android.widget.TextView? = null
    private var stage: W.Stage? = null
    private var legend: LinearLayout? = null
    private var bars: W.Bars? = null

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        addView(W.back(c, Hub.strings.sleep()) { nav.back() })
        setPadding(M3.dp(16f), 0, M3.dp(16f), M3.dp(120f))
        val card = W.card(c)
        val col = W.col(c)
        card.addView(col)
        col.addView(W.section(c, "Ostatnia noc"))
        value = W.bold(c, "—", 34f, M3.onSurface)
        col.addView(value, W.mlp(0, 4, 0, 0))
        sub = W.body(c, "", 12f, M3.onSurfaceVariant)
        col.addView(sub)
        stage = W.Stage(c)
        stage!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(10f))
        col.addView(stage, W.mlp(0, 12, 0, 0))
        legend = W.row(c)
        col.addView(legend, W.mlp(0, 8, 0, 0))
        addView(card, W.mlp(0, 10, 0, 0))

        val week = W.card(c)
        val weekCol = W.col(c)
        week.addView(weekCol)
        weekCol.addView(W.section(c, "Sen — 7 dni (min)"))
        bars = W.Bars(c)
        bars!!.layoutParams = LinearLayout.LayoutParams(-1, M3.dp(140f))
        weekCol.addView(bars, W.mlp(0, 4, 0, 0))
        addView(week, W.mlp(0, 12, 0, 0))
    }

    fun refresh() {
        val day = latest()
        if (day != null) {
            value?.text = String.format(Locale.US, "%dh %02dmin", day.sleepMin / 60, day.sleepMin % 60)
            sub?.text = "noc z " + day.date
            stage?.setData(
                floatArrayOf(day.deepMin.toFloat(), day.lightMin.toFloat(), day.remMin.toFloat(), day.awakeMin.toFloat()),
                intArrayOf(M3.info, M3.accent, M3.accent2, M3.border))
            legend?.removeAllViews()
            for ((label, color) in listOf(
                "głęboki " + mm(day.deepMin) to M3.info,
                "lekki " + mm(day.lightMin) to M3.accent,
                "REM " + mm(day.remMin) to M3.accent2,
                "czuwanie " + mm(day.awakeMin) to M3.textFaint)) {
                val l = W.row(ctx)
                val dot = W.Dot(ctx); dot.setColor(color)
                dot.layoutParams = LinearLayout.LayoutParams(M3.dp(7f), M3.dp(7f))
                l.addView(dot)
                l.addView(W.body(ctx, "  $label", 10.5f, M3.textSec))
                l.setPadding(0, 0, M3.dp(10f), 0)
                legend?.addView(l)
            }
        } else {
            value?.text = "—"
            sub?.text = Hub.strings.emptySleep()
            stage?.setData(FloatArray(0), IntArray(0))
            legend?.removeAllViews()
        }
        val today = Time.today()
        val vals = IntArray(7); val labels = Array(7) { "" }
        for (i in 0 until 7) {
            val date = Time.addDays(today, -(6 - i))
            vals[i] = Hub.data.day(date)?.sleepMin ?: 0
            labels[i] = date.substring(5)
        }
        bars?.setData(vals, labels)
    }

    private fun latest(): M.Day? {
        for (d in Hub.data.lastDays(14)) if (d.sleepMin > 0) return d
        return null
    }

    private fun mm(min: Int): String = String.format(Locale.US, "%dh%02d", min / 60, min % 60)
}
