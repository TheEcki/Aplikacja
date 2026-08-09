package pl.vitaltrack.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.UUID

/** Platformowa implementacja GattTransport nad BluetoothGatt. */
class AndroidGatt(private val ctx: Context) : GattTransport {

    private var gatt: BluetoothGatt? = null
    private var cb: GattCallback? = null

    override fun setCallback(c: GattCallback?) { cb = c }

    override fun connect(address: String): Boolean = try {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager ?: return false
        val dev = bm.adapter?.getRemoteDevice(address) ?: return false
        gatt = dev.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE)
        gatt != null
    } catch (e: Exception) { false }

    override fun disconnect() { try { gatt?.disconnect() } catch (ignored: Exception) {} }
    override fun close() { try { gatt?.close() } catch (ignored: Exception) {}; gatt = null }
    override fun discoverServices(): Boolean = try { gatt?.discoverServices() ?: false } catch (e: Exception) { false }

    override fun services(): List<String> = gatt?.services?.map { it.uuid.toString() } ?: emptyList()

    override fun chars(service: String): List<String> {
        val g = gatt ?: return emptyList()
        return g.getService(UUID.fromString(service))?.characteristics?.map { it.uuid.toString() } ?: emptyList()
    }

    override fun props(service: String, char: String): Int {
        val g = gatt ?: return 0
        return g.getService(UUID.fromString(service))?.getCharacteristic(UUID.fromString(char))?.properties ?: 0
    }

    override fun setNotify(char: String, on: Boolean): Boolean = try {
        val g = gatt ?: return false
        val c = find(char) ?: return false
        g.setCharacteristicNotification(c, on)
        if (on) {
            val d = c.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) ?: return true
            d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(d)
        }
        true
    } catch (e: Exception) { false }

    override fun read(char: String): Boolean = try {
        val g = gatt ?: return false; val c = find(char) ?: return false; g.readCharacteristic(c)
    } catch (e: Exception) { false }

    override fun write(char: String, v: ByteArray, type: Int): Boolean = try {
        val g = gatt ?: return false; val c = find(char) ?: return false
        c.value = v; c.writeType = type; g.writeCharacteristic(c)
    } catch (e: Exception) { false }

    override fun mtu(m: Int): Boolean = try { gatt?.requestMtu(m) ?: false } catch (e: Exception) { false }

    private fun find(char: String): BluetoothGattCharacteristic? {
        val g = gatt ?: return null
        for (s in g.services) s.getCharacteristic(UUID.fromString(char))?.let { return it }
        return null
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> cb?.onConnected()
                BluetoothProfile.STATE_DISCONNECTED -> cb?.onDisconnected()
            }
        }
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) { cb?.onServices(status) }
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            cb?.onChanged(c.uuid.toString(), c.value ?: return)
        }
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            cb?.onRead(c.uuid.toString(), c.value ?: ByteArray(0), status)
        }
        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            cb?.onWrite(c.uuid.toString(), status)
        }
        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) { cb?.onMtu(mtu, status) }
    }
}
