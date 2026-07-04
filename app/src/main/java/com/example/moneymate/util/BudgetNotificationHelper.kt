package com.example.moneymate.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.moneymate.R
import com.example.moneymate.domain.model.BudgetProgress
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object BudgetNotificationHelper {
    private const val CHANNEL_ID = "moneymate_budget_channel"
    private const val CHANNEL_NAME = "Cảnh báo ngân sách MoneyMate"

    private val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    })

    fun showExceededNotification(context: Context, progress: BudgetProgress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi chi tiêu vượt ngân sách danh mục"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val rootIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            progress.budget.id.toInt(),
            rootIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exceeded = abs(progress.remainingAmount)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Vượt ngân sách ${progress.budget.categoryTitle}")
            .setContentText("Bạn đã vượt ${formatter.format(exceeded)} đ trong tháng này.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(progress.budget.id.toInt() + 9000, notification)
    }
}
