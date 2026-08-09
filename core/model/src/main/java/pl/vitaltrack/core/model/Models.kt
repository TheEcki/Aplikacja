package pl.vitaltrack.core.model

/** Modele domenowe. Timestampy: epoch-millis UTC; data lokalna wyliczana przy prezentacji. */
object M {
    data class Day(
        val date: String,
        var steps: Int = 0,
        var calories: Int = 0,
        var moveMinutes: Int = 0,
        var cardioPoints: Int = 0,
        var distanceM: Int = 0,
        var sleepMin: Int = 0,
        var deepMin: Int = 0,
        var lightMin: Int = 0,
        var remMin: Int = 0,
        var awakeMin: Int = 0,
        var restingHr: Int = 0,
        var avgHr: Int = 0,
        var maxHr: Int = 0,
        var battery: Int = -1,
    )

    data class HrSample(val ts: Long, val bpm: Int)
    data class BatterySample(val ts: Long, val percent: Int)
    data class SyncLogEntry(val id: Long, val ts: Long, val level: String, val message: String)
    data class BleDevice(val name: String, val address: String, val rssi: Int)

    data class Workout(
        var id: Long = 0,
        var type: String = "RUNNING",
        var startTs: Long = 0,
        var endTs: Long = 0,
        var durationSec: Int = 0,
        var distanceM: Int = 0,
        var avgHr: Int = 0,
        var maxHr: Int = 0,
        var calories: Int = 0,
        var avgPaceSecPerKm: Int = 0,
        var source: String = "gps",
        var route: MutableList<DoubleArray> = mutableListOf(),
    ) {
        fun durationSec(): Long = when {
            durationSec > 0 -> durationSec.toLong()
            endTs > startTs -> (endTs - startTs) / 1000
            else -> 0
        }
    }

    data class GoalSet(
        val steps: Int = 10000,
        val moveMinutes: Int = 30,
        val sleepMin: Int = 480,
        val workoutsPerWeek: Int = 3,
    )

    enum class Origin { DEVICE, HEALTH_CONNECT, PHONE_SENSOR, CALCULATED, IMPORTED }
    enum class Quality { GOOD, FAIR, POOR, UNKNOWN }

    data class Alarm(
        val index: Int,
        var enabled: Boolean = false,
        var hour: Int = 7,
        var minute: Int = 0,
        var repeatMask: Int = 0,
    ) {
        fun daysLabel(): String {
            if (repeatMask == 0) return "Codziennie"
            val d = arrayOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd")
            val sb = StringBuilder()
            for (i in 0 until 7) if ((repeatMask and (1 shl i)) != 0) sb.append(d[i]).append(" ")
            return sb.toString().trim()
        }
    }

    object AlarmCodec {
        const val MAX = 5
        fun encode(a: List<Alarm>): String = a.joinToString("|") {
            "${if (it.enabled) 1 else 0},${it.hour},${it.minute},${it.repeatMask}"
        }
        fun decode(s: String?): List<Alarm> {
            val out = (0 until MAX).map { Alarm(it) }.toMutableList()
            if (s.isNullOrEmpty()) return out
            for ((i, p) in s.split("|").withIndex()) {
                if (i >= MAX) break
                try {
                    val f = p.split(",")
                    val a = out[i]
                    a.enabled = f[0].toInt() == 1
                    a.hour = f[1].toInt()
                    a.minute = f[2].toInt()
                    a.repeatMask = f[3].toInt()
                } catch (ignored: Exception) {}
            }
            return out
        }
        fun hm(h: Int, m: Int): String = String.format(java.util.Locale.US, "%02d:%02d", h, m)
    }
}

object WorkoutTypes {
    val ALL = listOf("RUNNING", "WALKING", "CYCLING", "STRENGTH", "OTHER")
    val LABELS = mapOf("RUNNING" to "Bieganie", "WALKING" to "Chód", "CYCLING" to "Rower",
        "STRENGTH" to "Siłownia", "OTHER" to "Inne")
    fun label(t: String?): String = LABELS[t] ?: "Trening"
    fun time(sec: Long): String {
        val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}

sealed class ConnectionError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data object BluetoothDisabled : ConnectionError("Bluetooth jest wyłączony")
    data object PermissionDenied : ConnectionError("Brak uprawnień Bluetooth")
    data object DeviceUnavailable : ConnectionError("Urządzenie niedostępne")
    data object AuthenticationFailed : ConnectionError("Autoryzacja nieudana")
    data object ConnectionTimeout : ConnectionError("Przekroczono czas połączenia")
    data object ProtocolError : ConnectionError("Błąd protokołu")
    data object DatabaseError : ConnectionError("Błąd bazy danych")
    data object HealthConnectUnavailable : ConnectionError("Health Connect niedostępny")
    data class Unknown(val t: Throwable) : ConnectionError("Nieznany błąd", t)
}

sealed interface WatchState {
    data object Disconnected : WatchState
    data object Scanning : WatchState
    data class Connecting(val name: String) : WatchState
    data object Authenticating : WatchState
    data object Connected : WatchState
    data class Error(val reason: ConnectionError) : WatchState
}

enum class AuthState { NONE, KEY_SENT, CHALLENGE, RESPONSE_SENT, OK, FAIL }

data class WatchCapabilities(
    val steps: Boolean = false, val calories: Boolean = false, val distance: Boolean = false,
    val heartRate: Boolean = false, val continuousHeartRate: Boolean = false,
    val restingHeartRate: Boolean = false, val sleep: Boolean = false,
    val sleepStages: Boolean = false, val spo2: Boolean = false, val stress: Boolean = false,
    val hrv: Boolean = false, val workouts: Boolean = false, val gpsTracks: Boolean = false,
    val battery: Boolean = false,
) {
    companion object { val UNVERIFIED = WatchCapabilities() }
}
