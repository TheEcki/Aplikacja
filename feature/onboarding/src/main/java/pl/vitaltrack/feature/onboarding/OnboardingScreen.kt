package pl.vitaltrack.feature.onboarding

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.Screens
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W

/** Onboarding — krótki przewodnik (można pominąć). */
class OnboardingScreen(private val ctx: Context, private val nav: Navigator) : LinearLayout(ctx) {

    init { orientation = VERTICAL; build(ctx) }

    private fun build(c: Context) {
        setPadding(M3.dp(20f), M3.dp(30f), M3.dp(20f), M3.dp(40f))
        val logo = W.bold(c, "VitalTrack", 30f, M3.onSurface)
        logo.gravity = Gravity.CENTER
        addView(logo)
        val tag = W.body(c, "Twój Amazfit Bip 6 bez aplikacji Zepp", 14f, M3.textSec)
        tag.gravity = Gravity.CENTER
        addView(tag, W.mlp(0, 4, 0, 0))

        addView(step(c, "1", "Sparuj zegarek w aplikacji Zepp i pobierz klucz parowania (32 znaki HEX)."), W.mlp(0, 22, 0, 0))
        addView(step(c, "2", "Odepnij zegarek z systemowego Bluetooth telefonu i zamknij Zepp."), W.mlp(0, 12, 0, 0))
        addView(step(c, "3", "W VitalTrack: wklej klucz (Urządzenia), Skanuj, Połącz, Sync."), W.mlp(0, 12, 0, 0))

        val note = W.body(c, "Wszystko działa lokalnie — bez serwerów, bez konta. " +
            "Dane i klucz nigdy nie opuszczają telefonu.", 12f, M3.textFaint)
        note.setLineSpacing(0f, 1.2f)
        addView(note, W.mlp(0, 18, 0, 0))

        val start = W.btn(c, "Zacznij", true)
        start.setOnClickListener {
            Hub.settings.putBool(SettingsPort.Keys.ONBOARDED, true)
            nav.showTab(Screens.TAB_TODAY)
        }
        addView(start, W.mlp(0, 22, 0, 0))
    }

    private fun step(c: Context, n: String, text: String): LinearLayout {
        val l = W.row(c)
        val num = W.semi(c, n, 14f, M3.onAccent)
        val g = android.graphics.drawable.GradientDrawable().apply {
            setCornerRadius(M3.dp(18f).toFloat()); setColor(M3.accent)
        }
        num.background = g
        num.gravity = Gravity.CENTER
        num.minimumWidth = M3.dp(36f); num.minimumHeight = M3.dp(36f)
        l.addView(num)
        val t = W.body(c, "  $text", 13.5f, M3.onSurface)
        t.setLineSpacing(0f, 1.2f)
        l.addView(t)
        return l
    }
}
