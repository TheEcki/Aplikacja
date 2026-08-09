package pl.vitaltrack.core.bluetooth

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import pl.vitaltrack.core.model.M

/** Skanowanie BLE — akceptacja: nazwa Amazfit/Huami/Zepp, usługa FEE1, zapisany MAC. */
class Scanner(private val ctx: Context) {

    interface Listener {
        fun onScan(on: Boolean, msg: String)
        fun onFound(list: List<M.BleDevice>)
        fun onError(msg: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private var listener: Listener? = null
    private val found = ArrayList<M.BleDevice>()
    private val seen = HashSet<String>()
    @Volatile private var scanning = false
    private var savedMac = ""

    fun setListener(l: Listener?) { listener = l }
    fun setSavedMac(m: String) { savedMac = m.lowercase() }
    fun isScanning(): Boolean = scanning

    fun start(): Boolean {
        if (!Permissions.scan(ctx) || !Permissions.btOn(ctx)) return false
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
        val scanner = bm.adapter?.bluetoothLeScanner ?: return false
        if (scanning) return true
        found.clear(); seen.clear(); scanning = true
        listener?.onScan(true, "Skanuję… (20 s)")
        try {
            scanner.startScan(null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                cb)
        } catch (e: SecurityException) { scanning = false; listener?.onError("Brak uprawnień."); return false }
        catch (e: Exception) { scanning = false; listener?.onError("Błąd skanowania."); return false }
        main.postDelayed({
            stop()
            if (found.isEmpty()) listener?.onError(
                "Nie znaleziono zegarka. Sprawdź: Bluetooth, zasięg, odparowanie z systemu.")
        }, 20_000)
        return true
    }

    fun stop() {
        if (!scanning) return
        try {
            val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.bluetoothLeScanner?.stopScan(cb)
        } catch (ignored: Exception) {}
        scanning = false
        listener?.onScan(false, "")
    }

    private val cb = object : ScanCallback() {
        override fun onScanResult(t: Int, r: ScanResult) {
            val dev = r.device ?: return
            val name = safeName(dev)
            val addr = dev.address
            val fee1 = r.scanRecord?.serviceUuids?.any { it.uuid.toString() == "0000fee1-0000-1000-8000-00805f9b34fb" } == true
            if (!nameMatch(name) && !fee1 && !addr.equals(savedMac, true)) return
            if (!seen.add(addr)) return
            val label = name ?: if (fee1) "Urządzenie Huami/Amazfit" else "Urządzenie BLE"
            found.add(M.BleDevice(label, addr, r.rssi))
            main.post { listener?.onFound(ArrayList(found)) }
        }

        override fun onScanFailed(code: Int) {
            scanning = false
            listener?.onScan(false, "Błąd skanowania (kod $code).")
        }
    }

    private fun nameMatch(n: String?): Boolean {
        if (n.isNullOrEmpty()) return false
        val s = n.lowercase()
        return s.contains("amazfit") || s.contains("huami") || s.contains("zepp") ||
            s.contains("bip") || s.contains("gtr") || s.contains("gts") ||
            s.contains("band") || s.contains("watch") || s.contains("active") ||
            s.contains("balance") || s.contains("trex")
    }

    private fun safeName(dev: android.bluetooth.BluetoothDevice): String? = try {
        if (!Permissions.connect(ctx)) null else dev.name
    } catch (e: SecurityException) { null }
}
