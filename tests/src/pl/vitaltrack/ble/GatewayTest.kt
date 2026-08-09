package pl.vitaltrack.ble

import pl.vitaltrack.core.bluetooth.GattGateway
import pl.vitaltrack.core.bluetooth.GattTransport
import pl.vitaltrack.core.bluetooth.GattCallback
import pl.vitaltrack.core.model.ConnectionError
import pl.vitaltrack.core.model.WatchState
import pl.vitaltrack.core.testing.Assert
import pl.vitaltrack.core.testing.Test
import java.util.concurrent.Executors

class GatewayTest {
    private fun await(c: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < end) { if (c()) return true; Thread.sleep(10) }
        return c()
    }

    @Test fun connectAndRead() {
        val t = Fake()
        val g = GattGateway(t, opTimeoutMs = 2000)
        val events = mutableListOf<String>()
        g.setListener(object : GattGateway.Listener {
            override fun onConnected() { events.add("c"); g.discover() }
            override fun onDisconnected() {}
            override fun onServices() { events.add("s") }
            override fun onChanged(char: String, v: ByteArray) {}
            override fun onRead(char: String, v: ByteArray) {}
            override fun onWrite(char: String) {}
            override fun onMtu(m: Int) {}
        })
        g.connect("AA:BB:CC:DD:EE:FF")
        Assert.assertTrue(await { events.contains("s") })
        Assert.assertTrue(t.session.contains("connect(AA:BB:CC:DD:EE:FF)"))
        var r: ByteArray? = null; var st = -1
        g.read("00002a19-0000-1000-8000-00805f9b34fb") { v, s -> r = v; st = s }
        Assert.assertTrue(await { st == 0 })
        Assert.assertEquals(88, r!![0].toInt() and 0xff)
    }

    @Test fun timeoutFails() {
        val g = GattGateway(Silent(), opTimeoutMs = 300)
        g.connect("AA")
        var st = -1
        g.read("00002a19-0000-1000-8000-00805f9b34fb") { _, s -> st = s }
        Assert.assertTrue(await { st == 133 || st == -1 })
        Assert.assertTrue(await { g.state.value is WatchState.Error })
        Assert.assertTrue((g.state.value as WatchState.Error).reason is ConnectionError.ConnectionTimeout)
    }

    private class Fake : GattTransport {
        val session = StringBuilder()
        private val ex = Executors.newSingleThreadExecutor { Thread(it, "f").apply { isDaemon = true } }
        private var cb: GattCallback? = null
        override fun setCallback(c: GattCallback?) { cb = c }
        override fun connect(a: String): Boolean { session.append("connect($a)\n"); ex.execute { Thread.sleep(10); cb?.onConnected() }; return true }
        override fun disconnect() { ex.execute { cb?.onDisconnected() } }
        override fun close() {}
        override fun discoverServices(): Boolean { session.append("discover\n"); ex.execute { Thread.sleep(5); cb?.onServices(0) }; return true }
        override fun services(): List<String> = listOf("0000fee1-0000-1000-8000-00805f9b34fb", "0000180f-0000-1000-8000-00805f9b34fb")
        override fun chars(s: String): List<String> = if (s.contains("180f")) listOf("00002a19-0000-1000-8000-00805f9b34fb") else emptyList()
        override fun props(s: String, c: String): Int = 2
        override fun setNotify(c: String, on: Boolean): Boolean = true
        override fun read(c: String): Boolean { session.append("read\n"); ex.execute { Thread.sleep(5); cb?.onRead(c, byteArrayOf(88), 0) }; return true }
        override fun write(c: String, v: ByteArray, t: Int): Boolean { ex.execute { Thread.sleep(5); cb?.onWrite(c, 0) }; return true }
        override fun mtu(m: Int): Boolean = true
    }

    private class Silent : GattTransport {
        override fun setCallback(c: GattCallback?) {}
        override fun connect(a: String): Boolean = true
        override fun disconnect() {}
        override fun close() {}
        override fun discoverServices(): Boolean = true
        override fun services(): List<String> = emptyList()
        override fun chars(s: String): List<String> = emptyList()
        override fun props(s: String, c: String): Int = 0
        override fun setNotify(c: String, on: Boolean): Boolean = true
        override fun read(c: String): Boolean = true
        override fun write(c: String, v: ByteArray, t: Int): Boolean = true
        override fun mtu(m: Int): Boolean = true
    }
}
