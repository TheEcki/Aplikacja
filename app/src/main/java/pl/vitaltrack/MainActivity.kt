package pl.vitaltrack

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.Navigator
import pl.vitaltrack.core.common.Screens
import pl.vitaltrack.core.common.SettingsPort
import pl.vitaltrack.core.designsystem.M3
import pl.vitaltrack.core.designsystem.W
import pl.vitaltrack.core.model.M
import pl.vitaltrack.feature.activity.ActivityScreen
import pl.vitaltrack.feature.devices.DevicesScreen
import pl.vitaltrack.feature.heartrate.HeartScreen
import pl.vitaltrack.feature.onboarding.OnboardingScreen
import pl.vitaltrack.feature.profile.ProfileScreen
import pl.vitaltrack.feature.sleep.SleepScreen
import pl.vitaltrack.feature.today.TodayScreen
import pl.vitaltrack.feature.trends.TrendsScreen
import java.util.Stack

/** Single-activity: 5 zakładek + pod-ekrany; M3 NavigationBar. */
class MainActivity : Activity(), Navigator {

    private var content: FrameLayout? = null
    private var bar: LinearLayout? = null
    private var current = -1
    private val stack = Stack<Int>()
    private var pending: Runnable? = null
    private var lastDash = 0L

    private lateinit var today: TodayScreen
    private lateinit var activity: ActivityScreen
    private lateinit var devices: DevicesScreen
    private lateinit var profile: ProfileScreen
    private lateinit var heart: HeartScreen
    private lateinit var sleep: SleepScreen
    private lateinit var trends: TrendsScreen
    private lateinit var onboarding: OnboardingScreen

    private val listener = object : pl.vitaltrack.core.common.WatchListener {
        override fun onScanState(on: Boolean, m: String) { runOnUiThread { devices.setScanningUi(on); devices.refresh() } }
        override fun onDevices(d: List<M.BleDevice>) { runOnUiThread { for (x in d) devices.onDeviceFound(x) } }
        override fun onConnected() { runOnUiThread { refresh() } }
        override fun onDisconnected() { runOnUiThread { refresh() } }
        override fun onHeart(bpm: Int, ts: Long) { runOnUiThread { refreshDash() } }
        override fun onBattery(p: Int) { runOnUiThread { refresh() } }
        override fun onStatus(t: String) { runOnUiThread { devices.refresh() } }
        override fun onSyncDone(ok: Boolean, summary: String) { runOnUiThread { refresh() } }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            if (!Hub.ok()) { showFatal(App.startupError ?: RuntimeException("Hub nie zainicjalizowany")); return }
            M3.apply(this)
            window.decorView.setBackgroundColor(M3.bg)
            Hub.watch.addListener(listener)
            build()
            insets()
            permissions()
            refresh()
            if (!Hub.settings.getBool(SettingsPort.Keys.ONBOARDED, false)) open(Screens.ONBOARD, false)
            else showTab(Screens.TAB_TODAY, false)
        } catch (t: Throwable) { showFatal(t) }
    }

    override fun onResume() { super.onResume(); refresh() }
    override fun onDestroy() { Hub.watch.removeListener(listener); super.onDestroy() }
    override fun onBackPressed() { if (!stack.isEmpty()) back() else super.onBackPressed() }

    private fun showFatal(t: Throwable) {
        val text = "VitalTrack nie może wystartować.\n\n" +
            android.os.Build.MODEL + " · Android " + android.os.Build.VERSION.RELEASE +
            "\n\n" + android.util.Log.getStackTraceString(t)
        App.writeCrash(this, text)
        val tv = TextView(this).apply {
            this.text = text; textSize = 11f; setTextIsSelectable(true); setPadding(24, 24, 24, 24)
        }
        setContentView(ScrollViewWrap(this, tv))
    }

    private fun build() {
        val root = FrameLayout(this)
        root.setBackgroundColor(M3.bg)
        content = FrameLayout(this)
        root.addView(content, FrameLayout.LayoutParams(-1, -1))

        bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            val g = GradientDrawable().apply {
                setCornerRadius(M3.dp(28f).toFloat()); setColor(M3.surfaceContainer)
            }
            background = g
            elevation = M3.dp(3f).toFloat()
            setPadding(M3.dp(8f), M3.dp(8f), M3.dp(8f), M3.dp(8f))
        }
        val blp = FrameLayout.LayoutParams(-1, M3.dp(80f))
        blp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        blp.leftMargin = M3.dp(12f); blp.rightMargin = M3.dp(12f); blp.bottomMargin = M3.dp(12f)
        root.addView(bar, blp)

        today = TodayScreen(this, this)
        activity = ActivityScreen(this, this)
        devices = DevicesScreen(this, this)
        profile = ProfileScreen(this, this)
        heart = HeartScreen(this, this)
        sleep = SleepScreen(this, this)
        trends = TrendsScreen(this, this)
        onboarding = OnboardingScreen(this, this)

        tab(Screens.TAB_TODAY, W.Icon.DASH, Hub.strings.tabToday())
        tab(Screens.TAB_ACTIVITY, W.Icon.BOOT, Hub.strings.tabActivity())
        tab(Screens.TAB_HEALTH, W.Icon.HEART, Hub.strings.tabHealth())
        tab(Screens.TAB_DEVICES, W.Icon.WATCH, Hub.strings.tabDevices())
        tab(Screens.TAB_PROFILE, W.Icon.PERSON, Hub.strings.tabProfile())
        setContentView(root)
    }

    private fun tab(idx: Int, icon: Int, label: String) {
        val b = bar ?: return
        val tab = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(0, M3.dp(4f), 0, M3.dp(2f))
            setOnClickListener { showTab(idx, true) }
        }
        val pill = FrameLayout(this)
        val pg = GradientDrawable().apply {
            setCornerRadius(M3.dp(16f).toFloat()); setColor(M3.secondaryContainer)
        }
        pill.background = pg
        val ic = W.Icon(this)
        ic.setIcon(icon)
        ic.layoutParams = FrameLayout.LayoutParams(M3.dp(24f), M3.dp(24f), Gravity.CENTER)
        pill.addView(ic)
        pill.layoutParams = LinearLayout.LayoutParams(M3.dp(64f), M3.dp(32f))
        tab.addView(pill)
        val t = W.med(this, label, 11f, M3.onSurfaceVariant)
        t.gravity = Gravity.CENTER
        tab.addView(t, LinearLayout.LayoutParams(-1, -2))
        b.addView(tab, LinearLayout.LayoutParams(0, -1, 1f))
        tab.setTag(R.id.tag_text, t)
        tab.setTag(R.id.tag_icon, ic)
        tab.setTag(R.id.tag_pill, pill)
    }

    private fun screenFor(id: Int): View = when (id) {
        Screens.TAB_TODAY -> today; Screens.TAB_ACTIVITY -> activity
        Screens.TAB_HEALTH -> heart; Screens.TAB_DEVICES -> devices
        Screens.TAB_PROFILE -> profile; Screens.HR -> heart; Screens.SLEEP -> sleep
        Screens.TRENDS -> trends; Screens.ONBOARD -> onboarding
        else -> today
    }

    override fun showTab(i: Int, animate: Boolean) {
        if (current == i && stack.isEmpty()) return
        if (i in 0..4) { stack.clear(); current = i }
        render(screenFor(i), animate, false)
        highlight(i)
        when (i) { Screens.TAB_TODAY -> refreshDash(); Screens.TAB_ACTIVITY -> activity.refresh()
            Screens.TAB_DEVICES -> devices.refresh(); Screens.TAB_PROFILE -> refresh() }
    }

    override fun open(screen: Int, animate: Boolean) {
        if (current >= 0) stack.push(current)
        render(screenFor(screen), animate, true)
        when (screen) { Screens.HR -> heart.refresh(); Screens.SLEEP -> sleep.refresh(); Screens.TRENDS -> trends.refresh() }
    }

    override fun back() {
        if (stack.isEmpty()) return
        val p = stack.pop()
        if (p in 0..4) showTab(p, true) else render(screenFor(p), true, false)
    }

    override fun refresh() {
        try { today.refresh(); activity.refresh(); devices.refresh() } catch (ignored: Exception) {}
    }

    private fun refreshDash() {
        val now = System.currentTimeMillis()
        if (current == Screens.TAB_TODAY && stack.isEmpty() && now - lastDash > 2000) {
            lastDash = now; today.refresh()
        }
    }

    private fun render(screen: View, animate: Boolean, hideBar: Boolean) {
        val c = content ?: return
        (screen.parent as? ViewGroup)?.removeView(screen)
        val sc = W.scroll(this)
        sc.addView(screen)
        sc.alpha = if (animate) 0f else 1f
        sc.translationX = if (animate) M3.dp(26f).toFloat() else 0f
        c.addView(sc)
        if (animate) {
            sc.animate().alpha(1f).translationX(0f).setDuration(220).start()
            val old = if (c.childCount > 1) c.getChildAt(0) else null
            if (old != null && old !== sc) old.animate().alpha(0f).setDuration(160)
                .withEndAction { if (old.parent != null) c.removeView(old) }.start()
        }
        while (c.childCount > 2) c.removeViewAt(0)
        bar?.visibility = if (hideBar) View.GONE else View.VISIBLE
    }

    private fun highlight(i: Int) {
        val b = bar ?: return
        for (k in 0 until b.childCount) {
            val t = b.getChildAt(k)
            val tv = t.getTag(R.id.tag_text) as? TextView
            val ic = t.getTag(R.id.tag_icon) as? W.Icon
            val pill = t.getTag(R.id.tag_pill) as? View
            val active = k == i
            ic?.setColor(if (active) M3.onSecondaryContainer else M3.onSurfaceVariant)
            tv?.setTextColor(if (active) M3.onSurface else M3.onSurfaceVariant)
            tv?.typeface = if (active) M3.semi else M3.medium
            pill?.visibility = if (active) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun insets() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                val si = insets.getInsets(android.view.WindowInsets.Type.systemBars() or
                    android.view.WindowInsets.Type.displayCutout())
                content?.setPadding(0, si.top, 0, 0)
                val blp = bar?.layoutParams as? FrameLayout.LayoutParams
                if (blp != null) { blp.bottomMargin = si.bottom + M3.dp(12f); bar?.layoutParams = blp }
                v.requestLayout(); insets
            }
        } else { window.statusBarColor = M3.bg; window.navigationBarColor = M3.bg }
    }

    private fun permissions() {
        val need = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.BLUETOOTH_SCAN)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) need.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 7)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        val p = pending
        if (p != null && results.all { it == PackageManager.PERMISSION_GRANTED }) { pending = null; p.run() }
        else pending = null
    }

    private class ScrollViewWrap(c: Activity, v: View) : android.widget.ScrollView(c) { init { addView(v) } }
}
