package pl.vitaltrack.core.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object Permissions {
    fun btOn(c: Context): Boolean =
        (c.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled ?: false

    fun scan(c: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        c.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    fun connect(c: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
        c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun canScan(c: Context): String? {
        if (!btOn(c)) return "Bluetooth jest wyłączony."
        if (!scan(c)) return "Brak uprawnienia BLUETOOTH_SCAN."
        if (Build.VERSION.SDK_INT < 31 &&
            c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return "Android 11 i starsze wymagają lokalizacji do skanowania."
        return null
    }
}
