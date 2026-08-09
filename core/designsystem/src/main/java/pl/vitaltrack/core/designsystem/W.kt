package pl.vitaltrack.core.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

/** Komponenty Material 3 (offline) + wykresy canvas na tokenach M3. */
object W {

    fun text(c: Context, s: String, sp: Float, color: Int, tf: android.graphics.Typeface): TextView =
        TextView(c).apply {
            text = s; setTextSize(sp); setTextColor(color); typeface = tf
            includeFontPadding = false; setLineSpacing(0f, 1.05f)
        }

    fun body(c: Context, s: String, sp: Float, color: Int) = text(c, s, sp, color, M3.regular)
    fun med(c: Context, s: String, sp: Float, color: Int) = text(c, s, sp, color, M3.medium)
    fun semi(c: Context, s: String, sp: Float, color: Int) = text(c, s, sp, color, M3.semi)
    fun bold(c: Context, s: String, sp: Float, color: Int) = text(c, s, sp, color, M3.bold)
    fun title(c: Context, s: String) = text(c, s, 22f, M3.onSurface, M3.semi)
    fun section(c: Context, s: String): TextView =
        text(c, s.uppercase(Locale.ROOT), 11.5f, M3.onSurfaceVariant, M3.medium).apply {
            letterSpacing = 0.08f
            setPadding(M3.dp(4f), M3.dp(10f), 0, M3.dp(2f))
        }

    fun card(c: Context): FrameLayout {
        val f = FrameLayout(c)
        val g = GradientDrawable().apply {
            setCornerRadius(M3.dp(M3.Shape.MEDIUM.toFloat()).toFloat())
            setColor(M3.card); setStroke(M3.dp(1f), M3.border)
        }
        f.background = g
        f.setPadding(M3.dp(16f), M3.dp(16f), M3.dp(16f), M3.dp(16f))
        return f
    }

    fun hero(c: Context): FrameLayout {
        val f = FrameLayout(c)
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, M3.hero())
        g.setCornerRadius(M3.dp(M3.Shape.XL.toFloat()).toFloat())
        f.background = g
        f.setPadding(M3.dp(20f), M3.dp(20f), M3.dp(20f), M3.dp(20f))
        return f
    }

    fun ripple(v: View) {
        try {
            val content = v.background
            v.background = RippleDrawable(
                ColorStateList.valueOf(M3.layer(M3.surface, M3.onSurface, M3.Layer.PRESSED)),
                content, null)
        } catch (ignored: Exception) {}
    }

    fun col(c: Context): LinearLayout = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
    fun row(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
    }
    fun scroll(c: Context): ScrollView = ScrollView(c).apply {
        isFillViewport = true; clipToPadding = false
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
    }
    fun mlp(l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(M3.dp(l.toFloat()), M3.dp(t.toFloat()), M3.dp(r.toFloat()), M3.dp(b.toFloat()))
        }
    fun weight(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, -2, 1f)
    fun spacer(c: Context): View = View(c).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) }

    fun back(c: Context, title: String, onBack: () -> Unit): LinearLayout {
        val r = row(c)
        r.minimumHeight = M3.dp(56f)
        val b = text(c, "‹", 26f, M3.onSurface, M3.semi).apply {
            gravity = Gravity.CENTER; minimumWidth = M3.dp(48f); minimumHeight = M3.dp(48f)
            isClickable = true
        }
        ripple(b); b.setOnClickListener { onBack() }
        r.addView(b); r.addView(title(c, title), LinearLayout.LayoutParams(0, -2, 1f))
        return r
    }

    fun item(c: Context, icon: Int, label: String, sub: String?, onClick: () -> Unit): LinearLayout {
        val r = row(c)
        r.setPadding(M3.dp(2f), M3.dp(12f), M3.dp(2f), M3.dp(12f))
        val ic = Icon(c).apply { setIcon(icon); setColor(M3.accent) }
        ic.layoutParams = LinearLayout.LayoutParams(M3.dp(24f), M3.dp(24f))
        r.addView(ic)
        val mid = col(c)
        mid.addView(body(c, label, 15f, M3.onSurface))
        if (sub != null) mid.addView(body(c, sub, 12f, M3.onSurfaceVariant))
        mid.setPadding(M3.dp(16f), 0, 0, 0)
        r.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        r.isClickable = true; ripple(r); r.setOnClickListener { onClick() }
        return r
    }

    fun div(c: Context): View = View(c).apply { setBackgroundColor(M3.border) }

    fun btn(c: Context, label: String, primary: Boolean = true): Button {
        val b = Button(c)
        b.text = label; b.textSize = 14f; b.isAllCaps = false
        b.typeface = M3.medium; b.includeFontPadding = false
        b.minimumHeight = M3.dp(48f)
        b.setPadding(M3.dp(24f), 0, M3.dp(24f), 0)
        val g = GradientDrawable().apply {
            setCornerRadius(M3.dp(24f).toFloat())
            if (primary) { setColor(M3.primary); b.setTextColor(M3.onPrimary) }
            else { setColor(M3.surface); setStroke(M3.dp(1f), M3.outline); b.setTextColor(M3.primary) }
        }
        b.background = RippleDrawable(ColorStateList.valueOf(M3.layer(
            if (primary) M3.primary else M3.surface,
            if (primary) M3.onPrimary else M3.primary, M3.Layer.PRESSED)), g, g)
        return b
    }

    fun chip(c: Context, label: String, selected: Boolean): TextView {
        val t = med(c, label, 13f, if (selected) M3.onSecondaryContainer else M3.onSurfaceVariant)
        val g = GradientDrawable().apply {
            setCornerRadius(M3.dp(M3.Shape.SMALL.toFloat()).toFloat())
            if (selected) setColor(M3.secondaryContainer)
            else { setColor(M3.surface); setStroke(M3.dp(1f), M3.outlineVariant) }
        }
        t.background = g
        t.setPadding(M3.dp(16f), M3.dp(9f), M3.dp(16f), M3.dp(9f))
        t.gravity = Gravity.CENTER
        return t
    }

    fun input(c: Context, hint: String, value: String?): EditText {
        val e = EditText(c)
        e.hint = hint; e.setText(value ?: ""); e.textSize = 14f
        e.typeface = M3.regular; e.setTextColor(M3.onSurface); e.setHintTextColor(M3.onSurfaceVariant)
        e.setSingleLine(true)
        val g = GradientDrawable().apply {
            setCornerRadius(M3.dp(M3.Shape.SMALL.toFloat()).toFloat())
            setColor(M3.surfaceContainerHigh); setStroke(M3.dp(1f), M3.outlineVariant)
        }
        e.background = g
        e.setPadding(M3.dp(16f), M3.dp(12f), M3.dp(16f), M3.dp(12f))
        return e
    }

    class Icon(c: Context) : View(c) {
        companion object {
            const val DASH = 0; const val WATCH = 1; const val PLAY = 2; const val SHARE = 3
            const val GEAR = 4; const val HEART = 5; const val FLAME = 6; const val MOON = 7
            const val BOOT = 8; const val BOLT = 9; const val PULSE = 10; const val SYNC = 11
            const val PAUSE = 12; const val MAP = 13; const val TRASH = 14; const val CAL = 15
            const val TARGET = 16; const val CHECK = 17; const val INFO = 18; const val POWER = 19
            const val ARROW = 20; const val DROP = 21; const val CLOCK = 22; const val CHART = 23
            const val PHONE = 24; const val LOCK = 25; const val EYE = 26; const val SEARCH = 27
            const val STAR = 28; const val SUN = 29; const val MOON_F = 30; const val FILE = 31
            const val BELL = 32; const val PERSON = 33
        }

        private var icon = DASH
        private var color = M3.onSurfaceVariant
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        fun setIcon(i: Int) { icon = i; invalidate() }
        fun setColor(c: Int) { color = c; invalidate() }

        override fun onDraw(canvas: Canvas) {
            val s = minOf(width.toFloat(), height.toFloat())
            canvas.save(); canvas.translate((width - s) / 2f, (height - s) / 2f)
            p.color = color; p.style = Paint.Style.STROKE
            p.strokeWidth = s * 0.072f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
            val path = Path()
            fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float) =
                canvas.drawRoundRect(RectF(s * l, s * t, s * r, s * b), s * rad, s * rad, p)
            when (icon) {
                DASH -> { rr(0.24f, 0.24f, 0.42f, 0.42f, 0.04f); rr(0.58f, 0.24f, 0.76f, 0.42f, 0.04f)
                    rr(0.24f, 0.58f, 0.42f, 0.76f, 0.04f); rr(0.58f, 0.58f, 0.76f, 0.76f, 0.04f) }
                WATCH -> { path.addRoundRect(RectF(s * 0.2f, s * 0.1f, s * 0.8f, s * 0.9f), s * 0.1f, s * 0.1f, Path.Direction.CW)
                    canvas.drawPath(path, p); canvas.drawLine(s * 0.3f, s * 0.1f, s * 0.3f, s * 0.04f, p)
                    canvas.drawLine(s * 0.7f, s * 0.1f, s * 0.7f, s * 0.04f, p)
                    canvas.drawLine(s * 0.3f, s * 0.9f, s * 0.3f, s * 0.96f, p); canvas.drawLine(s * 0.7f, s * 0.9f, s * 0.7f, s * 0.96f, p) }
                PLAY -> { val t = Path(); t.moveTo(s * 0.3f, s * 0.16f); t.lineTo(s * 0.8f, s * 0.5f)
                    t.lineTo(s * 0.3f, s * 0.84f); t.close(); canvas.drawPath(t, p) }
                PAUSE -> { rr(0.28f, 0.16f, 0.44f, 0.84f, 0.05f); rr(0.56f, 0.16f, 0.72f, 0.84f, 0.05f) }
                SHARE -> { path.moveTo(s * 0.26f, s * 0.38f); path.lineTo(s * 0.68f, s * 0.16f)
                    path.lineTo(s * 0.78f, s * 0.52f); path.lineTo(s * 0.26f, s * 0.38f); path.close()
                    canvas.drawPath(path, p); canvas.drawCircle(s * 0.7f, s * 0.76f, s * 0.11f, p)
                    canvas.drawCircle(s * 0.3f, s * 0.62f, s * 0.11f, p) }
                GEAR -> { canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.17f, p); canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.33f, p)
                    for (i in 0 until 8) { val a = Math.PI * 2 * i / 8 + Math.PI / 8
                        canvas.drawLine(s * 0.5f + Math.cos(a).toFloat() * s * 0.33f, s * 0.5f + Math.sin(a).toFloat() * s * 0.33f,
                            s * 0.5f + Math.cos(a).toFloat() * s * 0.44f, s * 0.5f + Math.sin(a).toFloat() * s * 0.44f, p) } }
                HEART -> { path.moveTo(s * 0.5f, s * 0.84f); path.cubicTo(s * 0.06f, s * 0.5f, s * 0.2f, s * 0.1f, s * 0.5f, s * 0.38f)
                    path.cubicTo(s * 0.8f, s * 0.1f, s * 0.94f, s * 0.5f, s * 0.5f, s * 0.84f); canvas.drawPath(path, p) }
                FLAME -> { path.moveTo(s * 0.5f, s * 0.08f); path.cubicTo(s * 0.76f, s * 0.34f, s * 0.84f, s * 0.5f, s * 0.8f, s * 0.66f)
                    path.cubicTo(s * 0.76f, s * 0.84f, s * 0.6f, s * 0.92f, s * 0.5f, s * 0.9f)
                    path.cubicTo(s * 0.34f, s * 0.88f, s * 0.22f, s * 0.76f, s * 0.2f, s * 0.6f)
                    path.cubicTo(s * 0.18f, s * 0.48f, s * 0.28f, s * 0.4f, s * 0.38f, s * 0.5f)
                    path.cubicTo(s * 0.4f, s * 0.32f, s * 0.42f, s * 0.2f, s * 0.5f, s * 0.08f); canvas.drawPath(path, p) }
                MOON -> { path.addArc(RectF(s * 0.1f, s * 0.1f, s * 0.9f, s * 0.9f), -70f, 300f); path.close(); canvas.drawPath(path, p) }
                SUN -> { canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.18f, p)
                    for (i in 0 until 8) { val a = Math.PI * 2 * i / 8
                        canvas.drawLine(s * 0.5f + Math.cos(a).toFloat() * s * 0.3f, s * 0.5f + Math.sin(a).toFloat() * s * 0.3f,
                            s * 0.5f + Math.cos(a).toFloat() * s * 0.4f, s * 0.5f + Math.sin(a).toFloat() * s * 0.4f, p) } }
                BOOT -> { path.moveTo(s * 0.24f, s * 0.72f); path.lineTo(s * 0.24f, s * 0.38f); path.lineTo(s * 0.42f, s * 0.26f)
                    path.lineTo(s * 0.6f, s * 0.38f); path.lineTo(s * 0.6f, s * 0.72f); canvas.drawPath(path, p)
                    canvas.drawArc(RectF(s * 0.22f, s * 0.6f, s * 0.8f, s * 0.92f), 0f, 180f, false, p) }
                BOLT -> { path.moveTo(s * 0.56f, s * 0.06f); path.lineTo(s * 0.3f, s * 0.56f); path.lineTo(s * 0.48f, s * 0.56f)
                    path.lineTo(s * 0.42f, s * 0.94f); path.lineTo(s * 0.7f, s * 0.4f); path.lineTo(s * 0.52f, s * 0.4f)
                    path.close(); canvas.drawPath(path, p) }
                PULSE -> { path.moveTo(s * 0.06f, s * 0.5f); path.lineTo(s * 0.3f, s * 0.5f); path.lineTo(s * 0.4f, s * 0.26f)
                    path.lineTo(s * 0.58f, s * 0.74f); path.lineTo(s * 0.68f, s * 0.5f); path.lineTo(s * 0.94f, s * 0.5f); canvas.drawPath(path, p) }
                SYNC -> { canvas.drawArc(RectF(s * 0.14f, s * 0.14f, s * 0.86f, s * 0.86f), 20f, 300f, false, p)
                    path.moveTo(s * 0.14f, s * 0.4f); path.lineTo(s * 0.32f, s * 0.4f); path.lineTo(s * 0.18f, s * 0.6f); path.close(); canvas.drawPath(path, p) }
                MAP -> { path.moveTo(s * 0.14f, s * 0.2f); path.lineTo(s * 0.36f, s * 0.12f); path.lineTo(s * 0.64f, s * 0.2f)
                    path.lineTo(s * 0.86f, s * 0.12f); path.lineTo(s * 0.86f, s * 0.8f); path.lineTo(s * 0.64f, s * 0.88f)
                    path.lineTo(s * 0.36f, s * 0.8f); path.lineTo(s * 0.14f, s * 0.88f); path.close(); canvas.drawPath(path, p)
                    canvas.drawLine(s * 0.36f, s * 0.12f, s * 0.36f, s * 0.8f, p); canvas.drawLine(s * 0.64f, s * 0.2f, s * 0.64f, s * 0.88f, p) }
                TRASH -> { path.moveTo(s * 0.24f, s * 0.32f); path.lineTo(s * 0.76f, s * 0.32f); path.lineTo(s * 0.7f, s * 0.86f)
                    path.lineTo(s * 0.3f, s * 0.86f); path.close(); canvas.drawPath(path, p)
                    canvas.drawLine(s * 0.14f, s * 0.32f, s * 0.86f, s * 0.32f, p); canvas.drawLine(s * 0.38f, s * 0.14f, s * 0.62f, s * 0.14f, p)
                    canvas.drawLine(s * 0.4f, s * 0.46f, s * 0.4f, s * 0.72f, p); canvas.drawLine(s * 0.6f, s * 0.46f, s * 0.6f, s * 0.72f, p) }
                CAL -> { rr(0.12f, 0.24f, 0.88f, 0.88f, 0.08f); canvas.drawLine(s * 0.14f, s * 0.44f, s * 0.86f, s * 0.44f, p)
                    canvas.drawLine(s * 0.34f, s * 0.12f, s * 0.34f, s * 0.3f, p); canvas.drawLine(s * 0.66f, s * 0.12f, s * 0.66f, s * 0.3f, p) }
                TARGET -> { canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.34f, p); canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.18f, p)
                    canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.05f, p) }
                CHECK -> { path.moveTo(s * 0.2f, s * 0.52f); path.lineTo(s * 0.42f, s * 0.74f); path.lineTo(s * 0.82f, s * 0.28f); canvas.drawPath(path, p) }
                INFO -> { canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.36f, p); canvas.drawCircle(s * 0.5f, s * 0.34f, s * 0.04f, p)
                    canvas.drawLine(s * 0.5f, s * 0.46f, s * 0.5f, s * 0.68f, p) }
                POWER -> { path.moveTo(s * 0.5f, s * 0.14f); path.lineTo(s * 0.5f, s * 0.5f); canvas.drawPath(path, p)
                    canvas.drawArc(RectF(s * 0.18f, s * 0.2f, s * 0.82f, s * 0.86f), 135f, 270f, false, p) }
                ARROW -> { path.moveTo(s * 0.22f, s * 0.5f); path.lineTo(s * 0.78f, s * 0.5f); canvas.drawPath(path, p)
                    path.moveTo(s * 0.58f, s * 0.26f); path.lineTo(s * 0.8f, s * 0.5f); path.lineTo(s * 0.58f, s * 0.74f); canvas.drawPath(path, p) }
                DROP -> { path.moveTo(s * 0.5f, s * 0.1f); path.cubicTo(s * 0.74f, s * 0.38f, s * 0.82f, s * 0.5f, s * 0.82f, s * 0.6f)
                    path.cubicTo(s * 0.82f, s * 0.78f, s * 0.68f, s * 0.9f, s * 0.5f, s * 0.9f)
                    path.cubicTo(s * 0.32f, s * 0.9f, s * 0.18f, s * 0.78f, s * 0.18f, s * 0.6f)
                    path.cubicTo(s * 0.18f, s * 0.5f, s * 0.26f, s * 0.38f, s * 0.5f, s * 0.1f); canvas.drawPath(path, p) }
                CLOCK -> { canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.36f, p); canvas.drawLine(s * 0.5f, s * 0.26f, s * 0.5f, s * 0.5f, p)
                    canvas.drawLine(s * 0.5f, s * 0.5f, s * 0.66f, s * 0.62f, p) }
                CHART -> { canvas.drawLine(s * 0.16f, s * 0.84f, s * 0.84f, s * 0.84f, p)
                    path.moveTo(s * 0.2f, s * 0.7f); path.lineTo(s * 0.36f, s * 0.5f); path.lineTo(s * 0.5f, s * 0.62f)
                    path.lineTo(s * 0.66f, s * 0.32f); path.lineTo(s * 0.8f, s * 0.2f); canvas.drawPath(path, p) }
                PHONE -> { rr(0.26f, 0.08f, 0.74f, 0.92f, 0.08f); canvas.drawLine(s * 0.42f, s * 0.8f, s * 0.58f, s * 0.8f, p) }
                LOCK -> { rr(0.18f, 0.44f, 0.82f, 0.86f, 0.06f); path.moveTo(s * 0.32f, s * 0.44f); path.lineTo(s * 0.32f, s * 0.3f)
                    path.cubicTo(s * 0.32f, s * 0.18f, s * 0.68f, s * 0.18f, s * 0.68f, s * 0.3f); path.lineTo(s * 0.68f, s * 0.44f); canvas.drawPath(path, p) }
                EYE -> { path.moveTo(s * 0.06f, s * 0.5f); path.cubicTo(s * 0.22f, s * 0.26f, s * 0.5f, s * 0.16f, s * 0.78f, s * 0.5f)
                    path.cubicTo(s * 0.5f, s * 0.84f, s * 0.22f, s * 0.74f, s * 0.06f, s * 0.5f); path.close(); canvas.drawPath(path, p)
                    canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.13f, p) }
                SEARCH -> { canvas.drawCircle(s * 0.42f, s * 0.42f, s * 0.26f, p); canvas.drawLine(s * 0.62f, s * 0.62f, s * 0.88f, s * 0.88f, p) }
                STAR -> { for (i in 0 until 10) { val a = -Math.PI / 2 + i * Math.PI / 5
                    val r = if (i % 2 == 0) s * 0.42f else s * 0.18f
                    val x = s * 0.5f + Math.cos(a).toFloat() * r; val y = s * 0.5f + Math.sin(a).toFloat() * r
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; path.close(); canvas.drawPath(path, p) }
                BELL -> { path.moveTo(s * 0.5f, s * 0.12f); path.cubicTo(s * 0.68f, s * 0.12f, s * 0.74f, s * 0.32f, s * 0.76f, s * 0.5f)
                    path.cubicTo(s * 0.78f, s * 0.62f, s * 0.8f, s * 0.7f, s * 0.86f, s * 0.78f); path.lineTo(s * 0.14f, s * 0.78f)
                    path.cubicTo(s * 0.2f, s * 0.7f, s * 0.22f, s * 0.62f, s * 0.24f, s * 0.5f)
                    path.cubicTo(s * 0.26f, s * 0.32f, s * 0.32f, s * 0.12f, s * 0.5f, s * 0.12f); path.close(); canvas.drawPath(path, p)
                    canvas.drawLine(s * 0.38f, s * 0.84f, s * 0.62f, s * 0.84f, p) }
                FILE -> { path.moveTo(s * 0.26f, s * 0.1f); path.lineTo(s * 0.62f, s * 0.1f); path.lineTo(s * 0.8f, s * 0.28f)
                    path.lineTo(s * 0.8f, s * 0.9f); path.lineTo(s * 0.26f, s * 0.9f); path.close(); canvas.drawPath(path, p)
                    canvas.drawLine(s * 0.62f, s * 0.1f, s * 0.62f, s * 0.28f, p); canvas.drawLine(s * 0.62f, s * 0.28f, s * 0.8f, s * 0.28f, p)
                    canvas.drawLine(s * 0.38f, s * 0.5f, s * 0.68f, s * 0.5f, p); canvas.drawLine(s * 0.38f, s * 0.64f, s * 0.68f, s * 0.64f, p) }
                PERSON -> { canvas.drawCircle(s * 0.5f, s * 0.3f, s * 0.16f, p)
                    path.moveTo(s * 0.18f, s * 0.88f); path.cubicTo(s * 0.2f, s * 0.62f, s * 0.36f, s * 0.5f, s * 0.5f, s * 0.5f)
                    path.cubicTo(s * 0.64f, s * 0.5f, s * 0.8f, s * 0.62f, s * 0.82f, s * 0.88f); canvas.drawPath(path, p) }
            }
            canvas.restore()
        }
    }

    // ------------------------------------------------------------- wykresy

    class Ring(c: Context) : View(c) {
        private var progress = 0f; private var shown = 0f
        private var value = ""; private var sub = ""
        private val track = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arc = Paint(Paint.ANTI_ALIAS_FLAG)
        private val t1 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val t2 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        init {
            track.style = Paint.Style.STROKE; track.strokeWidth = M3.dp(14f).toFloat(); track.strokeCap = Paint.Cap.ROUND
            arc.style = Paint.Style.STROKE; arc.strokeWidth = M3.dp(14f).toFloat(); arc.strokeCap = Paint.Cap.ROUND
            t1.textAlign = Paint.Align.CENTER; t2.textAlign = Paint.Align.CENTER
        }

        fun setData(p: Float, v: String, s: String) {
            progress = p.coerceIn(0f, 1f); value = v; sub = s
            ValueAnimator.ofFloat(shown, progress).apply {
                duration = M3.Motion.MEDIUM
                interpolator = android.view.animation.DecelerateInterpolator(2f)
                addUpdateListener { shown = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            val size = minOf(width.toFloat(), height.toFloat())
            val cx = width / 2f; val cy = height / 2f
            val r = size / 2f - M3.dp(12f)
            track.color = M3.primaryContainer
            rect.set(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(rect, -90f, 360f, false, track)
            arc.shader = SweepGradient(cx, cy, intArrayOf(M3.accent, M3.accent2, M3.accent), floatArrayOf(0f, 0.55f, 1f))
            canvas.drawArc(rect, -90f, 360f * shown, false, arc)
            val ty = cy - M3.dp(6f)
            t1.color = M3.onSurface; t1.typeface = M3.semi; t1.textSize = M3.sp(30f).toFloat()
            canvas.drawText(value, cx, ty, t1)
            t2.color = M3.onSurfaceVariant; t2.typeface = M3.regular; t2.textSize = M3.sp(12.5f).toFloat()
            canvas.drawText(sub, cx, ty + M3.dp(22f), t2)
        }
    }

    class Bars(c: Context) : View(c) {
        private var values = IntArray(0); private var labels = arrayOf<String>()
        private val bar = Paint(Paint.ANTI_ALIAS_FLAG); private val lbl = Paint(Paint.ANTI_ALIAS_FLAG)
        private val valP = Paint(Paint.ANTI_ALIAS_FLAG); private val avg = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            lbl.color = M3.onSurfaceVariant; lbl.typeface = M3.medium; lbl.textSize = M3.sp(10.5f).toFloat(); lbl.textAlign = Paint.Align.CENTER
            valP.color = M3.onSurfaceVariant; valP.typeface = M3.semi; valP.textSize = M3.sp(10f).toFloat(); valP.textAlign = Paint.Align.CENTER
            avg.color = M3.secondary; avg.style = Paint.Style.STROKE; avg.strokeWidth = M3.dp(1.4f).toFloat()
            avg.pathEffect = DashPathEffect(floatArrayOf(M3.dp(5f).toFloat(), M3.dp(5f).toFloat()), 0f)
        }

        fun setData(v: IntArray, l: Array<String>) { values = v; labels = l; invalidate() }

        override fun onDraw(canvas: Canvas) {
            if (values.isEmpty()) return
            val w = width.toFloat(); val h = height.toFloat()
            val top = M3.dp(18f); val bot = M3.dp(16f); val ch = h - top - bot
            val slot = w / values.size; val bw = minOf(M3.dp(20f).toFloat(), slot * 0.5f)
            var max = 1; var sum = 0
            for (v in values) { max = maxOf(max, v); sum += v }
            val av = sum / values.size.toFloat(); val ay = top + ch - ch * av / max
            for (i in values.indices) {
                val bh = if (values[i] == 0) M3.dp(3f).toFloat() else maxOf(M3.dp(5f).toFloat(), ch * values[i] / max)
                val cx = slot * i + slot / 2
                bar.shader = LinearGradient(0f, top + ch - bh, 0f, top + ch, intArrayOf(M3.accent2, M3.accent), null, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(RectF(cx - bw / 2, top + ch - bh, cx + bw / 2, top + ch), bw / 2, bw / 2, bar)
                if (values[i] > 0) canvas.drawText(short(values[i]), cx, top + ch - bh - M3.dp(5f), valP)
                if (labels.size > i && labels[i].isNotEmpty()) canvas.drawText(labels[i], cx, h - M3.dp(2f), lbl)
            }
            if (av > 0) canvas.drawLine(M3.dp(2f).toFloat(), ay, w - M3.dp(2f), ay, avg)
        }

        fun short(v: Int): String = if (v >= 1000) String.format(Locale.US, "%.1fk", v / 1000.0) else v.toString()
    }

    class Line(c: Context) : View(c) {
        private var values = FloatArray(0); private var xl = arrayOf<String>(); private var empty = ""
        private var zLo = -1f; private var zHi = -1f
        private val line = Paint(Paint.ANTI_ALIAS_FLAG); private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val grid = Paint(Paint.ANTI_ALIAS_FLAG); private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
        private val zone = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            line.style = Paint.Style.STROKE; line.strokeWidth = M3.dp(2.6f).toFloat()
            line.strokeCap = Paint.Cap.ROUND; line.strokeJoin = Paint.Join.ROUND
            grid.color = M3.border; grid.strokeWidth = M3.dp(1f).toFloat()
            txt.color = M3.onSurfaceVariant; txt.typeface = M3.medium; txt.textSize = M3.sp(10f).toFloat()
        }

        fun setData(v: FloatArray, l: Array<String>, e: String) { values = v; xl = l; empty = e; invalidate() }
        fun setZone(lo: Float, hi: Float) { zLo = lo; zHi = hi; invalidate() }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (values.size < 2) { txt.textAlign = Paint.Align.CENTER; canvas.drawText(empty, w / 2, h / 2, txt); txt.textAlign = Paint.Align.LEFT; return }
            var max = 1f; for (v in values) max = maxOf(max, v)
            var min = Float.MAX_VALUE; for (v in values) min = minOf(min, v)
            if (max - min < 20) { min = maxOf(0f, min - 8); max += 8 }
            val pl = M3.dp(26f).toFloat(); val pr = M3.dp(8f).toFloat()
            val pt = M3.dp(10f).toFloat(); val pb = M3.dp(16f).toFloat()
            val cw = w - pl - pr; val chh = h - pt - pb
            if (zLo > 0) {
                val yh = pt + chh - chh * (minOf(zHi, max) - min) / (max - min)
                val yl = pt + chh - chh * (maxOf(zLo, min) - min) / (max - min)
                zone.color = M3.withAlpha(M3.accent, 16)
                canvas.drawRect(pl, yh, w - pr, yl, zone)
            }
            for (i in 0 until 3) {
                val gv = min + (max - min) * (i + 1) / 4f
                val y = pt + chh - chh * (gv - min) / (max - min)
                canvas.drawLine(pl, y, w - pr, y, grid)
                canvas.drawText(Math.round(gv).toString(), M3.dp(2f).toFloat(), y + M3.dp(3.5f), txt)
            }
            val p = Path(); val pf = Path()
            for (i in values.indices) {
                val x = pl + cw * i / (values.size - 1)
                val y = pt + chh - chh * (values[i] - min) / (max - min)
                if (i == 0) { p.moveTo(x, y); pf.moveTo(x, y) } else {
                    val x0 = pl + cw * (i - 1) / (values.size - 1)
                    val y0 = pt + chh - chh * (values[i - 1] - min) / (max - min)
                    val mx = (x0 + x) / 2f
                    p.cubicTo(mx, y0, mx, y, x, y); pf.cubicTo(mx, y0, mx, y, x, y)
                }
            }
            line.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(M3.accent2, M3.accent), null, Shader.TileMode.CLAMP)
            canvas.drawPath(p, line)
            pf.lineTo(w - pr, h - pb); pf.lineTo(pl, h - pb); pf.close()
            fill.shader = LinearGradient(0f, pt, 0f, h - pb, intArrayOf(M3.withAlpha(M3.accent, 52), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawPath(pf, fill)
            for (i in xl.indices) { val x = pl + cw * i / (xl.size - 1); canvas.drawText(xl[i], x, h - M3.dp(2f), txt) }
        }
    }

    class Spark(c: Context) : View(c) {
        private var values = FloatArray(0); private var color = M3.accent
        private val line = Paint(Paint.ANTI_ALIAS_FLAG)
        init { line.style = Paint.Style.STROKE; line.strokeWidth = M3.dp(2f).toFloat(); line.strokeCap = Paint.Cap.ROUND; line.strokeJoin = Paint.Join.ROUND }
        fun setData(v: FloatArray, c: Int) { values = v; color = c; invalidate() }
        override fun onDraw(canvas: Canvas) {
            if (values.size < 2) return
            val w = width.toFloat(); val h = height.toFloat(); val pad = M3.dp(4f)
            var max = 1f; for (v in values) max = maxOf(max, v)
            var min = Float.MAX_VALUE; for (v in values) min = minOf(min, v)
            if (max - min < 8) { min = maxOf(0f, min - 4); max += 4 }
            val p = Path()
            for (i in values.indices) {
                val x = pad + (w - 2 * pad) * i / (values.size - 1)
                val y = pad + (h - 2 * pad) - (h - 2 * pad) * (values[i] - min) / (max - min)
                if (i == 0) p.moveTo(x, y) else { val x0 = pad + (w - 2 * pad) * (i - 1) / (values.size - 1)
                    val y0 = pad + (h - 2 * pad) - (h - 2 * pad) * (values[i - 1] - min) / (max - min)
                    p.cubicTo((x0 + x) / 2f, y0, (x0 + x) / 2f, y, x, y) }
            }
            line.color = color; canvas.drawPath(p, line)
        }
    }

    class Gauge(c: Context) : View(c) {
        private var value = 0f; private var max = 60f
        private val track = Paint(Paint.ANTI_ALIAS_FLAG); private val arc = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        init { track.style = Paint.Style.STROKE; track.strokeWidth = M3.dp(10f).toFloat(); track.strokeCap = Paint.Cap.ROUND
            arc.style = Paint.Style.STROKE; arc.strokeWidth = M3.dp(10f).toFloat(); arc.strokeCap = Paint.Cap.ROUND }
        fun setValue(v: Float, m: Float) { value = v.coerceIn(0f, m); max = maxOf(1f, m); invalidate() }
        override fun onDraw(canvas: Canvas) {
            val size = minOf(width.toFloat(), height.toFloat())
            val cx = width / 2f; val cy = height / 2f + M3.dp(4f); val r = size / 2f - M3.dp(12f)
            rect.set(cx - r, cy - r, cx + r, cy + r)
            track.color = M3.primaryContainer
            canvas.drawArc(rect, 150f, 240f, false, track)
            arc.shader = SweepGradient(cx, cy, intArrayOf(M3.info, M3.accent, M3.good, M3.warn, M3.danger), floatArrayOf(0f, 0.3f, 0.55f, 0.8f, 1f))
            canvas.drawArc(rect, 150f, 240f * (value / max), false, arc)
        }
    }

    class Stage(c: Context) : View(c) {
        private var frac = FloatArray(0); private var colors = IntArray(0)
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun setData(f: FloatArray, c: IntArray) { frac = f; colors = c; invalidate() }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            var total = 0f; for (f in frac) total += f
            if (total <= 0 || colors.isEmpty()) return
            var x = 0f
            for (i in frac.indices) {
                if (frac[i] <= 0) continue
                val seg = w * frac[i] / total
                p.color = colors[i % colors.size]
                val end = x + maxOf(seg, M3.dp(3f).toFloat())
                canvas.drawRoundRect(RectF(x, 0f, end, h), h / 2, h / 2, p); x = end
            }
        }
    }

    class Route(c: Context) : View(c) {
        private var points: List<DoubleArray> = emptyList()
        private val path = Paint(Paint.ANTI_ALIAS_FLAG); private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        init { path.style = Paint.Style.STROKE; path.strokeWidth = M3.dp(3f).toFloat(); path.strokeCap = Paint.Cap.ROUND; path.strokeJoin = Paint.Join.ROUND; dot.style = Paint.Style.FILL }
        fun setPoints(p: List<DoubleArray>) { points = p ?: emptyList(); invalidate() }
        override fun onDraw(canvas: Canvas) {
            if (points.size < 2) return
            val w = width.toFloat(); val h = height.toFloat(); val pad = M3.dp(16f)
            var mnLat = 1e9; var mxLat = -1e9; var mnLon = 1e9; var mxLon = -1e9
            for (p in points) { mnLat = minOf(mnLat, p[0]); mxLat = maxOf(mxLat, p[0]); mnLon = minOf(mnLon, p[1]); mxLon = maxOf(mxLon, p[1]) }
            val spLat = maxOf(mxLat - mnLat, 1e-6); val spLon = maxOf(mxLon - mnLon, 1e-6)
            val p = Path()
            for (i in points.indices) {
                val x = pad + ((points[i][1] - mnLon) / spLon).toFloat() * (w - 2 * pad)
                val y = pad + ((mxLat - points[i][0]) / spLat).toFloat() * (h - 2 * pad)
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            path.shader = LinearGradient(0f, 0f, w, h, intArrayOf(M3.accent2, M3.accent), null, Shader.TileMode.CLAMP)
            canvas.drawPath(p, path)
            fun pt(i: Int): Pair<Float, Float> = Pair(pad + ((points[i][1] - mnLon) / spLon).toFloat() * (w - 2 * pad),
                pad + ((mxLat - points[i][0]) / spLat).toFloat() * (h - 2 * pad))
            dot.color = M3.good; val s = pt(0); canvas.drawCircle(s.first, s.second, M3.dp(5f).toFloat(), dot)
            dot.color = M3.danger; val e = pt(points.size - 1); canvas.drawCircle(e.first, e.second, M3.dp(5f).toFloat(), dot)
        }
    }

    class Dot(c: Context) : View(c) {
        private var color = M3.accent
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun setColor(c: Int) { color = c; invalidate() }
        override fun onDraw(canvas: Canvas) {
            p.color = color
            val r = minOf(width.toFloat(), height.toFloat()) / 2f
            canvas.drawCircle(width / 2f, height / 2f, r, p)
        }
    }

    class Bar(c: Context) : View(c) {
        private var progress = 0f; private var color = M3.accent
        private val track = Paint(Paint.ANTI_ALIAS_FLAG); private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fun setProgress(p: Float) { progress = p.coerceIn(0f, 1f); invalidate() }
        fun setColor(c: Int) { color = c; invalidate() }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val r = h / 2f
            track.color = M3.surfaceContainerHighest
            canvas.drawRoundRect(RectF(0f, 0f, w, h), r, r, track)
            val fw = w * progress
            if (fw > M3.dp(2f)) {
                fill.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(M3.mix(color, M3.accent, 0.4f), color), null, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(RectF(0f, 0f, fw, h), r, r, fill)
            }
        }
    }
}
