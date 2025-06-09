package ru.gd_alt.youwilldrive.data.client

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.gd_alt.youwilldrive.data.DataStoreManager
import ru.gd_alt.youwilldrive.data.models.RecordID
import ru.gd_alt.youwilldrive.models.Chat
import ru.gd_alt.youwilldrive.models.Message
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.User
import ru.gd_alt.youwilldrive.models.fetchRelatedList
import ru.gd_alt.youwilldrive.models.fetchRelatedSingle

class ConnectionNotInitializedException(message: String) : IllegalStateException(message)

object Connection {
    private var _cl: SurrealDBClient? = null
    val cl: SurrealDBClient
        get() = _cl ?: throw ConnectionNotInitializedException("Connection.cl not initialized. Call Connection.initialize(context) first.")

    private const val NOTIFICATION_CHANNEL_ID = "surreal_live_updates_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "SurrealDB Live Updates"
    private const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for SurrealDB live query updates."
    private const val NOTIFICATION_ID_BASE = 1000

    fun initialize(context: Context, clientScope: CoroutineScope) {
        val appContext = context.applicationContext

        createNotificationChannel(appContext)

        clientScope.launch {
            try {
                _cl = SurrealDBClient.create(
                    "ws://87.242.117.89:5457/rpc",
                    "root",
                    "iwilldrive",
                    attemptReconnect = true
                ) { client ->
                    Log.i("SurrealLiveQuery", "(re)Connected to SurrealDB!")
                    try {
                        client.use("main", "main")
                        Log.i("SurrealLiveQuery", "Used namespace/database 'main'")
                        Log.i("SurrealLiveQuery", "Remote DB version: ${client.version()}")

                        val myUserId = DataStoreManager(appContext).getUserId().firstOrNull()
                        val currentUser = myUserId?.let { User.fromId(it) }

                        if (currentUser == null) {
                            Log.i("SurrealLiveQuery", "No user ID found or user not found. Skipping live query setup for notifications and messages. Ready!")
                            return@create
                        }

                        val notificationCallback = LiveUpdateCallback.Async { liveUpdate ->
                            Log.d("SurrealLiveQuery", "Received a Notification Live Update: $liveUpdate")

                            if (liveUpdate !is LiveQueryUpdate.Create) {
                                return@Async
                            }

                            val notification = Notification.fromDictionary(liveUpdate.data["notification"] as Map<String, Any?>)
                            val receiver = User.fromDictionary(liveUpdate.data["receiver"] as Map<String, Any?>)

                            if (receiver.id != currentUser.id) {
                                Log.d("SurrealLiveQuery", "Notification for different user (${receiver.id}), skipping.")
                                return@Async
                            }

                            Log.d("SurrealLiveQuery", "Processing Notification: $notification")

                            val title = notification.title
                            val content = notification.message
                            val notificationId = NOTIFICATION_ID_BASE + notification.id.hashCode()

                            showNotification(appContext, title, content, notificationId)
                            if (!notification.received) {
                                notification.received = true
                                notification.update(client)
                            }
                        }

                        val notificationWarCrime = "SELECT (SELECT * FROM ->users)[0] as receiver, (SELECT * FROM <-notifications)[0] as notification FROM is_for"
                        client.liveQuery("LIVE $notificationWarCrime", null, notificationCallback, doNotReproduce = false)
                        Log.i("SurrealLiveQuery", "Started/restarted live query for notifications connection table.")

                        val messageCallback = LiveUpdateCallback.Async { liveUpdate ->
                            Log.d("SurrealLiveQuery", "Received a Message Live Update: $liveUpdate")

                            if (liveUpdate !is LiveQueryUpdate.Create) {
                                return@Async
                            }

                            val messageRecordId = (liveUpdate.data["in"] as? RecordID)?.toString() ?: run {
                                Log.w("SurrealLiveQuery", "Message update data missing 'in' (message RecordID): ${liveUpdate.data}")
                                return@Async
                            }
                            val chatRecordId = (liveUpdate.data["out"] as? RecordID)?.toString() ?: run {
                                Log.w("SurrealLiveQuery", "Message update data missing 'out' (chat RecordID): ${liveUpdate.data}")
                                return@Async
                            }

                            try {
                                val dbMessage = Message.fromId(messageRecordId)
                                if (dbMessage == null) {
                                    Log.w("SurrealLiveQuery", "Could not fetch message with ID: $messageRecordId")
                                    return@Async
                                }

                                val sender = dbMessage.fetchRelatedSingle("sent_by", User::fromId)
                                val chatFromDb = Chat.fromId(chatRecordId)

                                val isMyChat = chatFromDb?.fetchRelatedList("participates", User::fromId, true)?.any { it.id == currentUser.id } ?: false

                                if (sender?.id != currentUser.id && isMyChat) {
                                    val title = "Новое сообщение от ${(sender?.name + " " + sender?.surname[0] + ".")}"
                                    val content = dbMessage.text.take(100) + if (dbMessage.text.length > 100) "..." else ""
                                    val notificationId = NOTIFICATION_ID_BASE + messageRecordId.hashCode()

                                    showNotification(appContext, title, content, notificationId)
                                    if (!dbMessage.delivered) {
                                        dbMessage.delivered = true
                                        dbMessage.update(client)
                                    }
                                }

                            } catch (e: Exception) {
                                Log.e("SurrealLiveQuery", "Error processing message live update: ${e.message}", e)
                            }
                        }

                        client.liveQuery("LIVE SELECT * FROM belongs_to", null, messageCallback, doNotReproduce = false)
                        Log.i("SurrealLiveQuery", "Started/restarted live query for message 'belongs_to' connection table.")

                        Log.i("SurrealLiveQuery", "Fetching unreceived notifications…")
                        val warCrimeRefined = "SELECT * FROM (SELECT (SELECT * FROM ->users)[0] as receiver, (SELECT * FROM <-notifications)[0] as notification FROM is_for) WHERE notification.received = false && receiver.id = \$receiverId"
                        val notificationsData = (client.query(warCrimeRefined,
                            mapOf("receiverId" to RecordID(currentUser.id.split(":")[0], currentUser.id.split(":")[1]))
                        ) as List<Map<String, Any?>>)[0]["result"] as List<Map<String, Any?>>

                        if (notificationsData.isEmpty()) {
                            Log.i("SurrealLiveQuery", "No unreceived notifications.")
                        } else {
                            for (pendingNotificationData in notificationsData) {
                                Log.d("SurrealLiveQuery", "Pending Notification Data: $pendingNotificationData")
                                val notification = Notification.fromDictionary(pendingNotificationData["notification"] as Map<String, Any?>)

                                if (notification.received) {
                                    Log.i("SurrealLiveQuery", "Notification already received. Skipping.")
                                    continue
                                }

                                Log.d("SurrealLiveQuery", "Pending Notification: $notification")

                                val title = notification.title
                                val content = notification.message
                                val notificationId = NOTIFICATION_ID_BASE + notification.id.hashCode()
                                showNotification(appContext, title, content, notificationId)
                                notification.received = true
                                notification.update(client)
                            }
                        }

                        Log.i("SurrealLiveQuery", "Fetching unreceived messages…")
                        val missedMessagesQuery = """
                            SELECT *, (->sent_by->users)[0] AS sender, (->belongs_to->chats)[0] AS chat
                            FROM messages
                            WHERE delivered = false
                            AND sender.id != \$myUserId
                            AND id IN (SELECT in FROM belongs_to WHERE out IN (SELECT in FROM participates WHERE out = \$myUserId));
                        """
                        val missedMessagesDataResult = (client.query(missedMessagesQuery,
                            mapOf("myUserId" to RecordID(currentUser.id.split(":")[0], currentUser.id.split(":")[1]))
                        ) as List<Map<String, Any?>>)[0]["result"] as List<Map<String, Any?>>

                        if (missedMessagesDataResult.isEmpty()) {
                            Log.i("SurrealLiveQuery", "No unreceived messages.")
                        } else {
                            for (missedMessageData in missedMessagesDataResult) {
                                Log.d("SurrealLiveQuery", "Pending Message Data: $missedMessageData")

                                val message = Message.fromDictionary(missedMessageData)
                                val sender = User.fromDictionary(missedMessageData["sender"] as Map<String, Any?>)

                                if (sender.id == currentUser.id) {
                                    Log.w("SurrealLiveQuery", "Self-sent message found in 'missed' batch, skipping: ${message.id}")
                                    continue
                                }

                                Log.d("SurrealLiveQuery", "Pending Message: $message from ${sender.name}")

                                val title = "Новое сообщение от ${sender.name} ${sender.surname[0]}."
                                val content = message.text.take(100) + if (message.text.length > 100) "..." else ""
                                val notificationId = NOTIFICATION_ID_BASE + message.id.hashCode()

                                showNotification(appContext, title, content, notificationId)

                                message.delivered = true
                                message.update(client)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("SurrealLiveQuery", "Error during onConnect setup (use/liveQuery): ${e.message}", e)
                    }
                }
                Log.i("SurrealLiveQuery", "SurrealDBClient created successfully.")
            } catch (e: Exception) {
                Log.e("SurrealLiveQuery", "Failed to create SurrealDBClient: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance).apply {
            description = NOTIFICATION_CHANNEL_DESCRIPTION
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(context: Context, title: String, content: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                }
                return
            }
            notify(notificationId, builder.build())
        }
    }
}