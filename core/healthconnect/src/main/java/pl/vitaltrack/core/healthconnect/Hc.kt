package pl.vitaltrack.core.healthconnect

import android.content.Context
import android.content.Intent
import pl.vitaltrack.core.model.M

interface HcGateway : HcExport {
    fun openApp(c: Context): Boolean
}

/** Uczciwy status: offline build bez androidx → Unavailable, nigdy atrapa. */
class UnavailableHc : HcGateway {
    override val isAvailable = false
    override val reason = "Ten build nie zawiera androidx.health.connect (offline, bez Maven). Kod integracji: docs/HEALTH_CONNECT.md"
    override fun exportAll(daily: List<M.Day>, hr: List<M.HrSample>, ws: List<M.Workout>): HcResult =
        HcResult.Failed(HcError.Unavailable, reason)
    override fun openApp(c: Context): Boolean = try {
        val i = c.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
        if (i != null) { c.startActivity(i); true } else false
    } catch (e: Exception) { false }
}

object HcIntents {
    fun launch(c: Context): Intent =
        c.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
            ?: Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata") }
}
