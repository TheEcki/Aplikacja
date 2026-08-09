package pl.vitaltrack.device.amazfit

/** GATT dla Amazfit/Huami/Zepp OS — potwierdzone elementy (docs/BLE_PROTOCOL.md). */
object Profile {
    const val HR = "0000180d-0000-1000-8000-00805f9b34fb"
    const val HR_MEAS = "00002a37-0000-1000-8000-00805f9b34fb"
    const val BAT = "0000180f-0000-1000-8000-00805f9b34fb"
    const val BAT_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"
    const val INFO = "0000180a-0000-1000-8000-00805f9b34fb"
    const val INFO_MANUF = "00002a29-0000-1000-8000-00805f9b34fb"
    const val INFO_MODEL = "00002a24-0000-1000-8000-00805f9b34fb"
    const val INFO_SN = "00002a25-0000-1000-8000-00805f9b34fb"
    const val INFO_FW = "00002a26-0000-1000-8000-00805f9b34fb"
    const val INFO_HW = "00002a27-0000-1000-8000-00805f9b34fb"
    const val ALERT_SVC = "00001802-0000-1000-8000-00805f9b34fb"
    const val ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb"
    const val ANS = "00001811-0000-1000-8000-00805f9b34fb"
    const val NEW_ALERT = "00002a46-0000-1000-8000-00805f9b34fb"
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"

    const val HUAMI = "0000fee1-0000-1000-8000-00805f9b34fb"
    const val AUTH = "00000009-0000-3512-2118-0009af100700"
    const val CONFIG = "00000003-0000-3512-2118-0009af100700"
    const val ACT1 = "00000007-0000-3512-2118-0009af100700"   // UNVERIFIED format
    const val ACT2 = "00000006-0000-3512-2118-0009af100700"   // UNVERIFIED format
    const val ACT3 = "00000001-0000-3512-2118-0009af100700"   // UNVERIFIED format
    val ACTS = listOf(ACT1, ACT2, ACT3)

    fun short(u: String): String = u.substring(4, 8).uppercase()
    fun infoName(u: String): String = when (u) {
        INFO_MANUF -> "producent"; INFO_MODEL -> "model"; INFO_FW -> "wersja FW"
        INFO_SN -> "numer seryjny"; INFO_HW -> "wersja HW"; else -> short(u)
    }
}
