package com.wanderlust.gdpricemonitor.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.wanderlust.gdpricemonitor.data.alert.AlertRuleStore

/**
 * 后台检测调度：用 AlarmManager 精确闹钟(setExactAndAllowWhileIdle)按设定的分钟间隔触发
 * [AlertAlarmReceiver]，从而真正支持小于 15 分钟的周期（WorkManager 周期任务最短被系统限制为 15 分钟）。
 *
 * 注意：
 * - Android 12+(S) 需 SCHEDULE_EXACT_ALARM 权限；无权限时退化为非精确闹钟(setAndAllowWhileIdle)。
 * - 深度 Doze 下精确闹钟仍可能被轻微延迟，但会触发。
 * - 闹钟不跨重启持久化，重启后需重新打开 App 才会再次排程。
 */
object PriceAlertScheduler {

    private const val ACTION_ALARM = "com.wanderlust.gdpricemonitor.action.ALERT_TICK"
    private const val REQ_CODE = 1001

    /** 排定下一次后台检测（间隔 minutes 分钟）。 */
    fun schedule(context: Context, minutes: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context)
        am.cancel(pi)
        val intervalMs = minutes.coerceAtLeast(1) * 60_000L
        val triggerAt = SystemClock.elapsedRealtime() + intervalMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // 无精确闹钟权限：退化为非精确，仍会在窗口内触发
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            }
        } else {
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlertAlarmReceiver::class.java).apply { action = ACTION_ALARM }
        return PendingIntent.getBroadcast(
            context,
            REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
