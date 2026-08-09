package pl.vitaltrack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Time
import pl.vitaltrack.core.model.Estimates
import pl.vitaltrack.core.model.M
import pl.vitaltrack.core.model.WorkoutTypes
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/** Usługa pierwszoplanowa — trening GPS. */
class WorkoutService : Service(), LocationListener {

    private val handler = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private var lm: LocationManager? = null
    private var type = "RUNNING"
    private var start = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L
    private var paused = false
    private var dist = 0.0
    private var last: Location? = null
    private val route = ArrayList<DoubleArray>()
    private var kcal = 0
    private var gps = false
    private var hrSum = 0
    private var hrN = 0

    private val tick = object : Runnable {
        override fun run() {
            if (!paused) {
                val hr = Hub.watch.liveHr()
                if (hr > 0) { hrSum += hr; hrN++
                    val s = Hub.settings
                    kcal += maxOf(0, Math.round(Estimates.kcalPerMinute(hr,
                        s.getInt(pl.vitaltrack.core.common.SettingsPort.Keys.AGE, 30),
                        s.getInt(pl.vitaltrack.core.common.SettingsPort.Keys.WEIGHT, 70),
                        s.getInt(pl.vitaltrack.core.common.SettingsPort.Keys.GENDER, 1) == 1) / 60.0).toInt())
                }
            }
            val el = if (paused) pausedAt - start - pausedTotal else System.currentTimeMillis() - start - pausedTotal
            val pace = if (dist >= 500) el / 1000.0 / (dist / 1000.0) else 0.0
            for (l in listeners) l.onTick(el, dist.toInt(), pace, Hub.watch.liveHr(), kcal, paused, gps)
            notify(el)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        NotificationChannel(CHANNEL, "Trening", NotificationManager.IMPORTANCE_LOW).let {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(it)
        }
        wake = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vital:workout")
        wake?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY
        when (intent.action) {
            ACTION_START -> {
                type = intent.getStringExtra(EXTRA_TYPE) ?: "RUNNING"
                start = System.currentTimeMillis(); pausedTotal = 0; paused = false
                dist = 0.0; kcal = 0; hrSum = 0; hrN = 0; route.clear(); last = null
                startForeground(1, notif("Trening trwa…"))
                try {
                    if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0f, this)
                } catch (ignored: Exception) {}
                handler.post(tick)
            }
            ACTION_PAUSE -> if (!paused) { pausedAt = System.currentTimeMillis(); paused = true }
            ACTION_RESUME -> if (paused) { pausedTotal += System.currentTimeMillis() - pausedAt; paused = false }
            ACTION_STOP -> { stopTracking(); stopForeground(true); stopSelf(); return START_NOT_STICKY }
        }
        return START_STICKY
    }

    override fun onLocationChanged(loc: Location) {
        if (paused || loc == null) return
        if (!loc.hasAccuracy() || loc.accuracy <= 0 || loc.accuracy > 50) return
        gps = true
        val pt = doubleArrayOf(loc.latitude, loc.longitude, loc.time.toDouble())
        val l = last
        if (l != null) {
            val d = l.distanceTo(loc)
            if (d > 1.0f) { dist += d; route.add(pt); for (x in listeners) x.onRoute(pt[0], pt[1], loc.time) }
        } else { route.add(pt); for (x in listeners) x.onRoute(pt[0], pt[1], loc.time) }
        last = loc
    }

    override fun onStatusChanged(p: String?, s: Int, b: android.os.Bundle?) {}
    override fun onProviderEnabled(p: String) {}
    override fun onProviderDisabled(p: String) {}

    private fun stopTracking() {
        handler.removeCallbacks(tick)
        try { lm?.removeUpdates(this) } catch (ignored: Exception) {}
        wake?.let { if (it.isHeld) it.release() }
        val now = System.currentTimeMillis()
        val el = if (paused) pausedAt - start - pausedTotal else now - start - pausedTotal
        if (el > 10_000) {
            val w = M.Workout()
            w.type = type; w.startTs = start; w.endTs = now
            w.durationSec = (el / 1000).toInt(); w.distanceM = dist.toInt()
            w.avgHr = if (hrN > 0) hrSum / hrN else 0; w.calories = kcal
            w.avgPaceSecPerKm = if (dist >= 500) (el / 1000.0 / (dist / 1000.0)).toInt() else 0
            w.source = "gps"; w.route = route
            val id = Hub.data.insertWorkout(w)
            val today = Time.today()
            val d = Hub.data.day(today) ?: M.Day(today)
            d.distanceM += w.distanceM; d.calories += w.calories; d.moveMinutes += w.durationSec / 60
            Hub.data.upsertDay(d)
            Hub.data.log("OK", "Zapisano trening #$id")
        }
        for (l in listeners) { l.onTick(0, 0, 0.0, 0, 0, false, false); l.onEnded() }
    }

    private fun notif(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("VitalTrack — " + WorkoutTypes.label(type))
            .setContentText(text).setOngoing(true).setContentIntent(pi).build()
    }

    private fun notify(el: Long) {
        val txt = String.format(Locale.US, "%.2f km · %s", dist / 1000.0, WorkoutTypes.time(el / 1000))
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, notif(txt))
    }

    companion object {
        const val ACTION_START = "pl.vitaltrack.START"
        const val ACTION_PAUSE = "pl.vitaltrack.PAUSE"
        const val ACTION_RESUME = "pl.vitaltrack.RESUME"
        const val ACTION_STOP = "pl.vitaltrack.STOP"
        const val EXTRA_TYPE = "type"
        const val CHANNEL = "workout"
        private val listeners = CopyOnWriteArrayList<Listener>()

        interface Listener {
            fun onTick(el: Long, dist: Int, pace: Double, hr: Int, kcal: Int, paused: Boolean, gps: Boolean) {}
            fun onRoute(lat: Double, lon: Double, ts: Long) {}
            fun onEnded() {}
        }

        fun addListener(l: Listener) = listeners.add(l)
        fun removeListener(l: Listener) = listeners.remove(l)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
