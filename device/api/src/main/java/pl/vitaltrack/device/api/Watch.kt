package pl.vitaltrack.device.api

import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.model.WatchCapabilities
import pl.vitaltrack.core.model.WatchState

/** Abstrakcja urządzenia — UI nigdy nie widzi GATT. */
interface WatchAdapter {
    val caps: WatchCapabilities
    val state: Obs<WatchState>
    val auth: Obs<AuthState>

    fun connect(d: DiscoveredWatch, key: ByteArray?): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun liveHr(): Int
    fun batteryBlocking(): Int?
    fun infoBlocking(): Map<String, String>
    fun activityRawBlocking(): List<Pair<String, ByteArray>>
    fun findWatch()
    fun testNotify()
    fun setOption(cmd: ByteArray, label: String)
    fun syncAlarms(a: List<M.Alarm>)
    fun setSyncLauncher(l: (() -> Unit)?) {}
    fun addListener(l: Events)
    fun removeListener(l: Events)
}

interface Events {
    fun onStatus(t: String) {}
    fun onScan(s: Boolean, m: String) {}
    fun onDevices(d: List<M.BleDevice>) {}
    fun onConnected() {}
    fun onDisconnected() {}
    fun onAuth(s: AuthState) {}
    fun onHeart(bpm: Int, ts: Long) {}
    fun onBattery(p: Int) {}
    fun onInfo(i: Map<String, String>) {}
    fun onVendor(svc: String, chr: String, hex: String) {}
    fun onSyncDone(ok: Boolean, summary: String) {}
}

data class DiscoveredWatch(val name: String, val address: String, val rssi: Int,
                           val services: List<String> = emptyList())

sealed interface SyncResult {
    data class Success(val summary: String, val records: Int) : SyncResult
    data class Failure(val error: pl.vitaltrack.core.model.ConnectionError, val detail: String) : SyncResult
}

interface WatchFactory {
    fun supports(d: DiscoveredWatch): Boolean
    fun create(d: DiscoveredWatch): WatchAdapter
}

class WatchRegistry(private val fs: List<WatchFactory>) {
    fun create(d: DiscoveredWatch): WatchAdapter? = fs.firstOrNull { it.supports(d) }?.create(d)
}
