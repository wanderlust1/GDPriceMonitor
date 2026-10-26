package com.wanderlust.gdpricemonitor.ui.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 系统通知发送（Android 8+ 必须建通知渠道）。
 */
object NotificationSender {

    private const val CHANNEL_ID = "price_alert_channel"
    private const val CHANNEL_NAME = "金价预警"

    /** 确保通知渠道存在（Android 8+）。在 Application 与 Worker 中都会调用。 */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "国际金价跌破 / 涨破阈值时提醒"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun send(context: Context, symbol: String, title: String, text: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        // 同一品种用固定通知 id，避免重复堆积；跌破/涨破共用该 id（后者覆盖前者）
        NotificationManagerCompat.from(context).notify(symbol.hashCode(), notification)
    }
}
