package pl.vitaltrack.core.bluetooth

/** Transport GATT — abstrakcja do testów (FakeGattTransport na JVM). */
interface GattTransport {
    fun connect(address: String): Boolean
    fun disconnect()
    fun close()
    fun discoverServices(): Boolean
    fun services(): List<String>
    fun chars(service: String): List<String>
    fun props(service: String, char: String): Int
    fun setNotify(char: String, on: Boolean): Boolean
    fun read(char: String): Boolean
    fun write(char: String, v: ByteArray, type: Int): Boolean
    fun mtu(m: Int): Boolean
    fun setCallback(cb: GattCallback?)
}

interface GattCallback {
    fun onConnected()
    fun onDisconnected()
    fun onServices(status: Int)
    fun onChanged(char: String, v: ByteArray)
    fun onRead(char: String, v: ByteArray, status: Int)
    fun onWrite(char: String, status: Int)
    fun onMtu(m: Int, status: Int)
}

object GattWrite { const val DEFAULT = 2; const val NO_RESPONSE = 4 }
object GattStatus { const val OK = 0; const val TIMEOUT = 133 }
object GattProps { const val READ = 0x02; const val WRITE = 0x08; const val NOTIFY = 0x10 }
