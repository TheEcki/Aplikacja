package pl.vitaltrack.core.sync

import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.common.SyncPhase
import pl.vitaltrack.core.common.SyncPort
import pl.vitaltrack.core.model.ConnectionError
import pl.vitaltrack.device.api.SyncResult
import pl.vitaltrack.device.api.WatchAdapter
import java.util.concurrent.atomic.AtomicBoolean

/** Silnik synchronizacji — wątek + obserwowalny postęp. */
class Engine(
    private val pipeline: Pipeline,
) : SyncPort {
    val progress: Obs<SyncProgress> = Obs(SyncProgress(SyncPhase.CONNECT, "gotowy"))
    private val running = AtomicBoolean(false)
    override fun isRunning(): Boolean = running.get()
    override fun cancel() { pipeline.cancel() }

    override fun run(adapter: WatchAdapter, onResult: (SyncResult) -> Unit) {
        if (!running.compareAndSet(false, true)) {
            onResult(SyncResult.Failure(ConnectionError.ProtocolError, "Synchronizacja już trwa"))
            return
        }
        Thread({
            try {
                pipeline.run(adapter, { progress.set(it) }, { r -> onResult(r); running.set(false) })
            } catch (e: Exception) {
                onResult(SyncResult.Failure(ConnectionError.Unknown(e), e.message ?: "Błąd"))
                running.set(false)
            }
        }, "vital-sync").apply { isDaemon = true }.start()
    }
}
