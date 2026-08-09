package pl.vitaltrack

import android.content.Context
import pl.vitaltrack.core.common.StringsPort

/** i18n — zasoby (values PL + values-en). */
class StringsImpl(c: Context) : StringsPort {
    private val ctx = c.applicationContext
    override fun appName() = ctx.getString(R.string.app_name)
    override fun tabToday() = ctx.getString(R.string.tab_today)
    override fun tabActivity() = ctx.getString(R.string.tab_activity)
    override fun tabHealth() = ctx.getString(R.string.tab_health)
    override fun tabDevices() = ctx.getString(R.string.tab_devices)
    override fun tabProfile() = ctx.getString(R.string.tab_profile)
    override fun steps() = ctx.getString(R.string.steps)
    override fun heartRate() = ctx.getString(R.string.heart_rate)
    override fun sleep() = ctx.getString(R.string.sleep)
    override fun calories() = ctx.getString(R.string.calories)
    override fun distance() = ctx.getString(R.string.distance)
    override fun move() = ctx.getString(R.string.move)
    override fun cardio() = ctx.getString(R.string.cardio)
    override fun workouts() = ctx.getString(R.string.workouts)
    override fun trends() = ctx.getString(R.string.trends)
    override fun goals() = ctx.getString(R.string.goals)
    override fun sync() = ctx.getString(R.string.sync)
    override fun connect() = ctx.getString(R.string.connect)
    override fun disconnect() = ctx.getString(R.string.disconnect)
    override fun forget() = ctx.getString(R.string.forget)
    override fun key() = ctx.getString(R.string.key)
    override fun saveKey() = ctx.getString(R.string.save_key)
    override fun noData() = ctx.getString(R.string.no_data)
    override fun unsupported() = ctx.getString(R.string.unsupported)
    override fun exportData() = ctx.getString(R.string.export)
    override fun emptyHr() = ctx.getString(R.string.empty_hr)
    override fun emptySleep() = ctx.getString(R.string.empty_sleep)
}
