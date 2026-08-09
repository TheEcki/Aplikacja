package pl.vitaltrack.core.healthconnect

import pl.vitaltrack.core.model.M

/** Czyste kontrakty Health Connect (JVM) — testowane bez Androida. */
interface HcStatusPort {
    val isAvailable: Boolean
    val reason: String?
}

sealed interface HcResult {
    data class Ok(val n: Int) : HcResult
    data class Failed(val error: HcError, val detail: String) : HcResult
}

sealed class HcError(message: String) : Exception(message) {
    object Unavailable : HcError("Health Connect niedostępny w tym buildzie")
    object PermissionDenied : HcError("Brak zgód")
    class SyncFailed(val msg: String) : HcError(msg)
}

interface HcExport : HcStatusPort {
    fun exportAll(daily: List<M.Day>, hr: List<M.HrSample>, ws: List<M.Workout>): HcResult
}
