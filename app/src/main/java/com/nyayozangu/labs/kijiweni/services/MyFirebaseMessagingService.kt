package com.nyayozangu.labs.kijiweni.services

import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.helpers.*
import com.nyayozangu.labs.kijiweni.models.ChatMessage
import java.util.*
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import com.nyayozangu.labs.kijiweni.activities.MainActivity


const val CHANNEL_ID = "main_channel"

private var chat: ChatMessage? = null
private val common = Common

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        //check if the message contains data payload
        if (remoteMessage?.data?.size!! > 0) {
            val data = remoteMessage.data
            val type = data[CHAT_MESSAGE_TYPE]
            val chatId = data[CHAT_ID]
            getMessageDetails(chatId, type)
        }else{
            Log.d(TAG, "data has no payload")
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: ${remoteMessage.notification?.body}")
            val message = remoteMessage.notification?.body
            val title = getString(R.string.default_notification_title)
            if (message != null) createNotification(title, message)
        }
    }

    private fun getMessageDetails(chatId: String?, type: String?) {
        if (chatId != null) {
            val chatRef = common.database().collection(CHATS).document(chatId)
            chatRef.get().addOnSuccessListener {
                if (it.exists()) {
                    chat = it.toObject(ChatMessage::class.java)
                    val userId = chat?.user_id
                    if (userId != null && userId != common.currentUserUid()) {
                        val username = chat?.username
                        val message = chat?.message
                        //todo if chat has image, show image in notification
                        val contentTitle = getString(R.string.new_message_text)
                        when (type) {
                            NEW_TEXT_MESSAGE -> {
                                val contentMessage = "$username: $message"
                                createNotification(contentTitle, contentMessage)
                            }
                            NEW_IMAGE_MESSAGE -> {
                                val contentMessage = "$username has posted a new photo"
                                createNotification(contentTitle, contentMessage)
                            }else -> {

                            createNotification(title = getString(R.string.default_notification_title),
                                    message = getString(R.string.new_message_text))
                        }
                        }
                    }

                }
            }
                    .addOnFailureListener {
                        Log.e(TAG, "failed to show notification: ${it.message}")
                    }
        }
    }

    private fun createNotification(title: String?, message: String) {

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle( NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                //todo add a reply button

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManagerO = getSystemService(NotificationManager::class.java)
            notificationManagerO.createNotificationChannel(channel)
        }else{
            val notificationManager = NotificationManagerCompat.from(this)
            val notificationId = (0..1000000).random()
            notificationManager.notify(notificationId, mBuilder.build())
        }
    }
    private fun ClosedRange<Int>.random() = Random().nextInt((endInclusive + 1) - start) +  start
}
