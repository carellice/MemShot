package com.memshot.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.memshot.app.MainActivity
import com.memshot.app.R
import com.memshot.app.analysis.Categories
import com.memshot.app.data.Shot

object Notifications {
    private const val CHANNEL = "imports"
    const val EXTRA_SHOT_ID = "shot_id"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Screenshot salvati", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Un avviso discreto quando MemShot salva e cataloga un nuovo screenshot"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun imported(context: Context, shots: List<Shot>) {
        if (shots.isEmpty()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val first = shots.first()
        val tags = shots.flatMap { it.tags }.distinct().take(3)
            .joinToString(" · ") { Categories.label(it) }
        val title = if (shots.size == 1) "Screenshot salvato in MemShot" else "${shots.size} screenshot salvati in MemShot"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (shots.size == 1) putExtra(EXTRA_SHOT_ID, first.id)
        }
        val pi = PendingIntent.getActivity(
            context, first.id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val thumb = runCatching {
            BitmapFactory.decodeFile(first.filePath, BitmapFactory.Options().apply { inSampleSize = 4 })
        }.getOrNull()
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(tags.ifEmpty { "Catalogato e pronto da cercare" })
            .setLargeIcon(thumb)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setColor(0xFF6C4DFF.toInt())
            .setTimeoutAfter(15_000)
            .build()
        NotificationManagerCompat.from(context).notify(4242, n)
    }
}
