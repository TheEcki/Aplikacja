package pl.vitaltrack.core.bluetooth

import pl.vitaltrack.core.common.Obs
import pl.vitaltrack.core.model.ConnectionError
import pl.vitaltrack.core.model.WatchState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Serializowana kolejka GATT z timeoutami (jedna operacja na raz; timeout
 * retry raz, potem błąd połączenia). Czysty JVM — testowany.
 */
class GattGateway(
    private val t: GattTransport,
    private val opTimeoutMs: Long = 10_000,
    private val log: (String) -> Unit = {},
) : GattCallback {

    val state: Obs<WatchState> = Obs(WatchState.Disconnected)

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onServices()
        fun onChanged(char: String, v: ByteArray)
        fun onRead(char: String, v: ByteArray)
        fun onWrite(char: String)
        fun onMtu(m: Int)
    }

    private var listener: Listener? = null
    fun setListener(l: Listener?) { listener = l }

    private val tasks = LinkedBlockingQueue<Runnable>()
    @Volatile private var running = false

    fun connect(address: String) {
        state.set(WatchState.Connecting(address))
        start()
        post {
            try { if (!t.connect(address)) fail(ConnectionError.DeviceUnavailable) }
            catch (e: Exception) { fail(ConnectionError.Unknown(e)) }
        }
    }

    fun disconnect() {
        post {
            if (!running) return@post
            running = false; t.disconnect()
            state.set(WatchState.Disconnected); listener?.onDisconnected()
        }
    }

    fun close() { post { running = false; t.close() } }
    fun discover() { post { try { t.discoverServices() } catch (e: Exception) { fail(ConnectionError.ProtocolError) } } }
    fun post(task: () -> Unit) { tasks.add(Runnable { task() }) }

    fun read(char: String, cb: (ByteArray?, Int) -> Unit = { _, _ -> }) {
        post {
            if (!running) return@post
            var attempts = 0
            while (attempts < 2) {
                attempts++
                val latch = CountDownLatch(1)
                synchronized(lock) {
                    pendingRead.set(ReadResult(char, null, GattStatus.TIMEOUT)); readLatch = latch
                }
                try {
                    if (!t.read(char)) { synchronized(lock) { readLatch = null }; cb(null, GattStatus.TIMEOUT); return@post }
                } catch (e: Exception) { synchronized(lock) { readLatch = null }; cb(null, GattStatus.TIMEOUT); return@post }
                val done = latch.await(opTimeoutMs, TimeUnit.MILLISECONDS)
                val r = synchronized(lock) { readLatch = null; pendingRead.get() }
                if (done && r.char == char && r.status == GattStatus.OK) { cb(r.v, r.status); return@post }
                log("read timeout #$attempts ${short(char)}")
            }
            cb(null, GattStatus.TIMEOUT); fail(ConnectionError.ConnectionTimeout)
        }
    }

    fun write(char: String, v: ByteArray, type: Int = GattWrite.DEFAULT) {
        post {
            if (!running) return@post
            var attempts = 0
            while (attempts < 2) {
                attempts++
                val latch = CountDownLatch(1)
                synchronized(lock) {
                    pendingWrite.set(WriteResult(char, GattStatus.TIMEOUT)); writeLatch = latch
                }
                try {
                    if (!t.write(char, v, type)) { synchronized(lock) { writeLatch = null }; fail(ConnectionError.ProtocolError); return@post }
                } catch (e: Exception) { synchronized(lock) { writeLatch = null }; fail(ConnectionError.Unknown(e)); return@post }
                val done = latch.await(opTimeoutMs, TimeUnit.MILLISECONDS)
                val r = synchronized(lock) { writeLatch = null; pendingWrite.get() }
                if (done && r.char == char && r.status == GattStatus.OK) return@post
                log("write timeout #$attempts ${short(char)}")
            }
            fail(ConnectionError.ConnectionTimeout)
        }
    }

    fun setNotify(char: String) { post { try { t.setNotify(char, true) } catch (e: Exception) { log("notify: ${e.message}") } } }
    fun requestMtu(m: Int) { post { try { t.mtu(m) } catch (ignored: Exception) {} } }
    fun isConnected(): Boolean = state.value is WatchState.Connected
    fun services(): List<String> = try { t.services() } catch (e: Exception) { emptyList() }
    fun chars(s: String): List<String> = try { t.chars(s) } catch (e: Exception) { emptyList() }
    fun props(s: String, c: String): Int = try { t.props(s, c) } catch (e: Exception) { 0 }

    private data class ReadResult(val char: String, val v: ByteArray?, val status: Int)
    private data class WriteResult(val char: String, val status: Int)
    private val pendingRead = AtomicReference(ReadResult("", null, GattStatus.TIMEOUT))
    private val pendingWrite = AtomicReference(WriteResult("", GattStatus.TIMEOUT))
    private val lock = Any()
    private var readLatch: CountDownLatch? = null
    private var writeLatch: CountDownLatch? = null

    override fun onConnected() { state.set(WatchState.Connecting("connected")); listener?.onConnected() }
    override fun onDisconnected() { running = false; tasks.clear(); state.set(WatchState.Disconnected); listener?.onDisconnected() }
    override fun onServices(status: Int) { if (status == GattStatus.OK) listener?.onServices() else fail(ConnectionError.ProtocolError) }
    override fun onChanged(char: String, v: ByteArray) { listener?.onChanged(char, v) }
    override fun onRead(char: String, v: ByteArray, status: Int) {
        synchronized(lock) { pendingRead.set(ReadResult(char, v, status)); readLatch?.countDown() }
        listener?.onRead(char, v)
    }
    override fun onWrite(char: String, status: Int) {
        synchronized(lock) { pendingWrite.set(WriteResult(char, status)); writeLatch?.countDown() }
        listener?.onWrite(char)
    }
    override fun onMtu(m: Int, status: Int) { listener?.onMtu(m) }

    private fun fail(e: ConnectionError) {
        log("fail: ${e.message}")
        running = false; tasks.clear()
        try { t.disconnect() } catch (ignored: Exception) {}
        state.set(WatchState.Error(e))
    }

    private fun start() {
        if (running) return
        running = true
        Thread({ runLoop() }, "vital-gatt").apply { isDaemon = true }.start()
    }

    private fun runLoop() {
        while (running) {
            try {
                val task = tasks.poll(100, TimeUnit.MILLISECONDS) ?: continue
                try { task.run() } catch (e: Exception) { log("task: $e") }
            } catch (ignored: InterruptedException) { break }
        }
    }

    private fun short(u: String): String = if (u.length >= 8) u.substring(4, 8).uppercase() else u

    init { t.setCallback(this) }
}
