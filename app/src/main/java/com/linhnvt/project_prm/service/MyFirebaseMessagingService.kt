package com.linhnvt.project_prm.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.linhnvt.project_prm.R
import com.linhnvt.project_prm.ui.MainActivity
import com.linhnvt.project_prm.utils.Constant


class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val FIREBASE_MESSAGE_ID = 29
        private const val KEY_MESSAGE_CONTENT = "message_content"
        private const val NOTIFICATION_TITLE = "My Spotify"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        sendNotification(remoteMessage.data[KEY_MESSAGE_CONTENT])
    }

    override fun onNewToken(token: String) {
        Log.d(Constant.COMMON_TAG, "App's token: $token")
    }

    private fun sendNotification(messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, MainActivity.NORMAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText("$messageBody")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify(FIREBASE_MESSAGE_ID, builder.build())
        }
    }
}