package pl.vitaltrack.sync

import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.common.SyncPhase
import pl.vitaltrack.core.healthconnect.HcError
import pl.vitaltrack.core.healthconnect.HcExport
import pl.vitaltrack.core.healthconnect.HcResult
import pl.vitaltrack.core.model.AuthState
import pl.vitaltrack.core.model.ConnectionError
import pl.vitaltrack.core.model.M
import pl.vitaltrack.device.api.SyncResult
import pl.vitaltrack.core.model.WatchCapabilities
import pl.vitaltrack.core.model.WatchState
import pl.vitaltrack.core.sync.Pipeline
import pl.vitaltrack.core.sync.Store
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test
import pl.vitaltrack.device.api.DiscoveredWatch
import pl.vitaltrack.device.api.WatchAdapter

class PipelineTest {

    private class FakeWatch : WatchAdapter {
        override val caps = WatchCapabilities(heartRate = true, battery = true)
        override val state = Obs<WatchState>(WatchState.Disconnected)
        override val auth = Obs<AuthState>(AuthState.NONE)
        var info = mapOf("model" to "Amazfit Bip 6", "wersja FW" to "1.0.0.0")
        var battery: Int? = 78
        var raw = emptyList<Pair<String, ByteArray>>()
        fun connect() { state.set(WatchState.Connected) }
        fun failAuth() { auth.set(AuthState.FAIL) }
        override fun connect(d: DiscoveredWatch, key: ByteArray?): Boolean = true
        override fun disconnect() { state.set(WatchState.Disconnected) }
        override fun isConnected(): Boolean = state.value is WatchState.Connected
        override fun liveHr(): Int = 0
        override fun batteryBlocking(): Int? = battery
        override fun infoBlocking(): Map<String, String> = info
        override fun activityRawBlocking(): List<Pair<String, ByteArray>> = raw
        override fun findWatch() {}
        override fun testNotify() {}
        override fun setOption(cmd: ByteArray, label: String) {}
        override fun syncAlarms(a: List<M.Alarm>) {}
        override fun addListener(l: pl.vitaltrack.device.api.Events) {}
        override fun removeListener(l: pl.vitaltrack.device.api.Events) {}
    }

    private class MemStore : Store {
        val log = mutableListOf<String>()
        val state = HashMap<String, String>()
        val days = HashMap<String, M.Day>()
        val hr = mutableListOf<M.HrSample>()
        var batteryN = 0
        override fun log(level: String, msg: String) { log.add("[$level] $msg") }
        override fun getState(k: String): String? = state[k]
        override fun putState(k: String, v: String) { state[k] = v }
        override fun day(date: String): M.Day? = days[date]
        override fun upsert(d: M.Day) { days[d.date] = d }
        override fun hrRange(from: Long, to: Long): List<M.HrSample> = hr.filter { it.ts in from..to }
        override fun battery(ts: Long, p: Int) { batteryN++ }
        override fun days(n: Int): List<M.Day> = days.values.sortedByDescending { it.date }.take(n)
        override fun ws(n: Int): List<M.Workout> = emptyList()
    }

    private class MemSettings : pl.vitaltrack.core.sync.Settings {
        val map = HashMap<String, String>()
        override fun putString(k: String, v: String) { map[k] = v }
        override fun putInt(k: String, v: Int) { map[k] = v.toString() }
        override fun putLong(k: String, v: Long) { map[k] = v.toString() }
    }

    private class FakeHc(val available: Boolean) : HcExport {
        override val isAvailable = available
        override val reason = if (available) null else "brak"
        override fun exportAll(daily: List<M.Day>, hr: List<M.HrSample>, ws: List<M.Workout>): HcResult =
            if (available) HcResult.Ok(daily.size) else HcResult.Failed(HcError.Unavailable, "brak")
    }

    private fun run(w: FakeWatch, s: MemStore = MemStore(), se: MemSettings = MemSettings(),
                    hc: HcExport = FakeHc(false)): Pair<SyncResult, List<pl.vitaltrack.core.sync.SyncProgress>> {
        val p = Pipeline(s, se, hc)
        val prog = mutableListOf<pl.vitaltrack.core.sync.SyncProgress>()
        var r: SyncResult? = null
        p.run(w, { prog.add(it) }, { r = it })
        return r!! to prog
    }

    @Test fun happyPath() {
        val w = FakeWatch().apply { connect() }
        val s = MemStore(); val se = MemSettings()
        val (r, prog) = run(w, s, se)
        Assert.assertTrue(r is SyncResult.Success)
        Assert.assertEquals(1, s.batteryN)
        Assert.assertEquals("78", se.map[Pipeline.Keys.BATTERY])
        Assert.assertEquals("ok", s.state[Pipeline.State.STATUS])
        Assert.assertEquals(SyncPhase.CONNECT, prog.first().phase)
        Assert.assertEquals(SyncPhase.DONE, prog.last().phase)
        Assert.assertTrue(s.log.any { it.contains("pierwsza") })
    }

    @Test fun deviceUnavailable() {
        val w = FakeWatch()
        w.state.set(WatchState.Error(ConnectionError.DeviceUnavailable))
        val (r, _) = run(w)
        Assert.assertTrue(r is SyncResult.Failure)
        Assert.assertTrue((r as SyncResult.Failure).error is ConnectionError.DeviceUnavailable)
    }

    @Test fun authFailed() {
        val w = FakeWatch().apply { connect(); failAuth() }
        val (r, _) = run(w)
        Assert.assertTrue(r is SyncResult.Failure)
        Assert.assertTrue((r as SyncResult.Failure).error is ConnectionError.AuthenticationFailed)
    }

    @Test fun rawCounted() {
        val w = FakeWatch().apply { connect(); raw = listOf("c1" to byteArrayOf(1, 2)) }
        val s = MemStore()
        val (r, _) = run(w, s)
        Assert.assertTrue(r is SyncResult.Success)
        Assert.assertTrue(s.log.any { it.contains("1 surowych") })
    }

    @Test fun incremental() {
        val w = FakeWatch().apply { connect() }
        val s = MemStore(); s.state[Pipeline.State.LAST_SYNCED] = "123456789"
        run(w, s)
        Assert.assertTrue(s.log.any { it.contains("przyrostowo") })
    }

    @Test fun hcSkippedNotFailed() {
        val w = FakeWatch().apply { connect() }
        val s = MemStore()
        val (r, _) = run(w, s)
        Assert.assertTrue(r is SyncResult.Success)
        Assert.assertTrue(s.log.any { it.contains("HC niedostępny") })
    }
}
