package pl.vitaltrack.core.common

import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.model.WatchCapabilities
import pl.vitaltrack.core.model.WatchState

/** Porty — implementacje w modułach infrastruktury, kompozycja w :app. */

interface SettingsPort {
    fun getString(k: String, def: String): String
    fun putString(k: String, v: String)
    fun getInt(k: String, def: Int): Int
    fun putInt(k: String, v: Int)
    fun getLong(k: String, def: Long): Long
    fun putLong(k: String, v: Long)
    fun getBool(k: String, def: Boolean): Boolean
    fun putBool(k: String, v: Boolean)
    fun hasDevice(): Boolean = getString(Keys.MAC, "").isNotEmpty()

    object Keys {
        const val MAC = "device_mac"
        const val NAME = "device_name"
        const val HAS_KEY = "has_auth_key"
        const val LAST_SYNC = "last_sync_ts"
        const val AGE = "profile_age"
        const val GENDER = "profile_gender"
        const val WEIGHT = "profile_weight_kg"
        const val HEIGHT = "profile_height_cm"
        const val RESTING = "profile_resting_hr"
        const val MAX_HR = "profile_max_hr"
        const val DARK = "dark_mode"
        const val ACCENT = "accent_index"
        const val AUTO_CONNECT = "auto_connect"
        const val AUTO_SYNC = "auto_sync"
        const val ONBOARDED = "onboarded_v1"
        const val ALARMS = "alarms_v1"
        const val GOAL_STEPS = "goal_steps"
        const val GOAL_MOVE = "goal_move_min"
        const val GOAL_SLEEP = "goal_sleep_min"
        const val GOAL_WORKOUTS = "goal_workouts_week"
        const val BATTERY = "last_battery"
        const val BATTERY_TS = "last_battery_ts"
        const val FW = "last_fw"
        const val MODEL = "last_model"
    }
}

interface DataPort {
    fun day(date: String): M.Day?
    fun upsertDay(d: M.Day)
    fun lastDays(n: Int): List<M.Day>
    fun insertHr(ts: Long, bpm: Int)
    fun insertHrBulk(s: List<Pair<Long, Int>>)
    fun hr(from: Long, to: Long): List<M.HrSample>
    fun latestHr(): M.HrSample?
    fun hrStats(from: Long, to: Long): Triple<Int, Int, Int>
    fun workouts(n: Int): List<M.Workout>
    fun insertWorkout(w: M.Workout): Long
    fun deleteWorkout(id: Long)
    fun insertBattery(ts: Long, p: Int)
    fun battery(n: Int): List<M.BatterySample>
    fun log(level: String, msg: String)
    fun syncLog(n: Int): List<M.SyncLogEntry>
    fun clearLog()
    fun clearAll()
}

interface SecretsPort {
    fun hasKey(): Boolean
    fun saveKey(hex: String): Boolean
    fun loadKey(): ByteArray?
    fun clearKey()
}

interface WatchListener {
    fun onStatus(t: String) {}
    fun onScanState(s: Boolean, m: String) {}
    fun onDevices(d: List<M.BleDevice>) {}
    fun onConnected() {}
    fun onDisconnected() {}
    fun onAuth(s: AuthState) {}
    fun onHeart(bpm: Int, ts: Long) {}
    fun onBattery(p: Int) {}
    fun onInfo(i: Map<String, String>) {}
    fun onSyncDone(ok: Boolean, summary: String) {}
}

interface WatchPort {
    val state: Obs<WatchState>
    val auth: Obs<AuthState>
    val caps: WatchCapabilities
    fun canScan(): String?
    fun scan(): Boolean
    fun stopScan()
    fun isScanning(): Boolean
    fun connect(mac: String): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun liveHr(): Int
    fun sync(): Boolean
    fun findWatch()
    fun testNotify()
    fun setOption(cmd: ByteArray, label: String)
    fun syncAlarms(a: List<M.Alarm>)
    fun saveKey(hex: String): Boolean
    fun forgetDevice()
    fun addListener(l: WatchListener)
    fun removeListener(l: WatchListener)
}

interface SyncPort {
    fun run(adapter: pl.vitaltrack.device.api.WatchAdapter, onResult: (pl.vitaltrack.device.api.SyncResult) -> Unit)
    fun isRunning(): Boolean
    fun cancel()
}

interface StringsPort {
    fun appName(): String
    fun tabToday(): String
    fun tabActivity(): String
    fun tabHealth(): String
    fun tabDevices(): String
    fun tabProfile(): String
    fun steps(): String
    fun heartRate(): String
    fun sleep(): String
    fun calories(): String
    fun distance(): String
    fun move(): String
    fun cardio(): String
    fun workouts(): String
    fun trends(): String
    fun goals(): String
    fun sync(): String
    fun connect(): String
    fun disconnect(): String
    fun forget(): String
    fun key(): String
    fun saveKey(): String
    fun noData(): String
    fun unsupported(): String
    fun exportData(): String
    fun emptyHr(): String
    fun emptySleep(): String
}

interface Navigator {
    fun showTab(i: Int, animate: Boolean = true)
    fun open(screen: Int, animate: Boolean = true)
    fun back()
    fun refresh()
}

object Screens {
    const val TAB_TODAY = 0
    const val TAB_ACTIVITY = 1
    const val TAB_HEALTH = 2
    const val TAB_DEVICES = 3
    const val TAB_PROFILE = 4
    const val HR = 10
    const val SLEEP = 11
    const val TRENDS = 12
    const val WORKOUTS = 13
    const val GOALS = 14
    const val ONBOARD = 20
}

object Hub {
    lateinit var settings: SettingsPort; private set
    lateinit var data: DataPort; private set
    lateinit var secrets: SecretsPort; private set
    lateinit var watch: WatchPort; private set
    lateinit var sync: SyncPort; private set
    lateinit var strings: StringsPort; private set
    lateinit var hcStatus: pl.vitaltrack.core.healthconnect.HcStatusPort; private set

    fun init(s: SettingsPort, d: DataPort, sec: SecretsPort, w: WatchPort,
             sy: SyncPort, st: StringsPort, hc: pl.vitaltrack.core.healthconnect.HcStatusPort) {
        settings = s; data = d; secrets = sec; watch = w; sync = sy; strings = st; hcStatus = hc
    }
    fun ok(): Boolean = ::settings.isInitialized
}

object HrHistory {
    private const val MAX = 90
    private val buf = ArrayList<Float>()
    fun push(bpm: Int) { synchronized(buf) { buf.add(bpm.toFloat()); while (buf.size > MAX) buf.removeAt(0) } }
    fun values(): FloatArray = synchronized(buf) { FloatArray(buf.size) { buf[it] } }
}

enum class SyncPhase { CONNECT, AUTH, INFO, BATTERY, RANGE, DOWNLOAD, PARSE, VALIDATE, DEDUPE, STORE, AGGREGATE, HC, DONE }
