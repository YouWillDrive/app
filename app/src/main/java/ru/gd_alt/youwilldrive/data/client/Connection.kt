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
import ru.gd_alt.youwilldrive.models.Notification
import ru.gd_alt.youwilldrive.models.User

class ConnectionNotInitializedException(message: String) : IllegalStateException(message)

object Connection {
    private var _cl: SurrealDBClient? = null
    val cl: SurrealDBClient
        get() = _cl ?: throw ConnectionNotInitializedException("Connection.cl not initialized. Call Connection.initialize(context) first.")

    private const val NOTIFICATION_CHANNEL_ID = "surreal_live_updates_channel"
    private const val NOTIFICATION_CHANNEL_NAME = "SurrealDB Live Updates"
    private const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for SurrealDB live query updates."
    private const val NOTIFICATION_ID_BASE = 1000

    /**
     * Initializes the SurrealDB client and sets up the live query with notification callback.
     * This should be called from an Android Context, ideally Application.onCreate().
     * @param context Application context.
     * @param clientScope CoroutineScope tied to the lifecycle (e.g., Application scope).
     */
    fun initialize(context: Context, clientScope: CoroutineScope) {
        val appContext = context.applicationContext

        // Create the notification channel (safe to call multiple times)
        createNotificationChannel(appContext)

        // Define the live query callback using the application context
        val notificationCallback = LiveUpdateCallback.Async { liveUpdate ->
            Log.d("SurrealLiveQuery", "Received a Notification: $liveUpdate")

            when (liveUpdate) {
                is LiveQueryUpdate.Create -> {}
                is LiveQueryUpdate.Update -> return@Async
                is LiveQueryUpdate.Delete -> return@Async
                is LiveQueryUpdate.Unknown -> return@Async
            }

            val notification = Notification.fromDictionary(liveUpdate.data["notification"] as Map<String, Any?>)
            val user = User.fromDictionary(liveUpdate.data["receiver"] as Map<String, Any?>)
            Log.d("SurrealLiveQuery", "Notification: $notification")
            Log.d("SurrealLiveQuery", "User: $user")

            val myId = DataStoreManager(appContext).getUserId().firstOrNull()
            Log.d("SurrealLiveQuery", "My ID: $myId")

            if (DataStoreManager(appContext).getUserId().firstOrNull() != user.id) {
                return@Async
            }

            val title = notification.title
            val content = notification.message
            val notificationId = NOTIFICATION_ID_BASE + liveUpdate.data["id"].hashCode()
            showNotification(appContext, title, content, notificationId)
            notification.received = true
            notification.update()
        }

        clientScope.launch {
            try {
                _cl = SurrealDBClient.create(
                    "ws://87.242.117.89:5457/rpc",
                    "root",
                    "iwilldrive",
                    attemptReconnect = true
                ) { client ->
                    // This is the onConnectCallback, runs after successful connection/reconnection and sign-in
                    Log.i("SurrealLiveQuery", "(re)Connected to SurrealDB!")
                    try {
                        client.use("main", "main") // Re-establish namespace and database
                        Log.i("SurrealLiveQuery", "Used namespace/database 'main'")
                        Log.i("SurrealLiveQuery", "Remote DB version: ${client.version()}")

                        // Start the live query using the defined notificationCallback
                        // doNotReproduce = false allows the client's internal logic to restart it
                        // on subsequent reconnections within the client's restartLiveQueries method.
                        val warCrime = "SELECT (SELECT * FROM ->users)[0] as receiver, (SELECT * FROM <-notifications)[0] as notification FROM is_for"
                        client.liveQuery("LIVE $warCrime", null, notificationCallback, doNotReproduce = false)
                        Log.i("SurrealLiveQuery", "Started/restarted live query for notifications connection table.")
                        Log.i("SurrealLiveQuery", "Waiting for live updates from test table…\n")
                        Log.i("SurrealLiveQuery", "Fetching unreceived notifications…")

                        val myId = DataStoreManager(appContext).getUserId().firstOrNull()

                        if (myId == null) {
                            Log.i("SurrealLiveQuery", "No user ID found. Not fetching unreceived notifications. Ready!")
                            return@create
                        }

                        val warCrimeRefined = "SELECT * FROM (SELECT (SELECT * FROM ->users)[0] as receiver, (SELECT * FROM <-notifications)[0] as notification FROM is_for) WHERE notification.received = false && receiver.id = \$receiverId"
                        val notificationsData = (client.query(warCrimeRefined,
                            mapOf("receiverId" to RecordID(myId.split(":")[0], myId.split(":")[1]))
                        ) as List<Map<String, Any?>>)[0]["result"] as List<Map<String, Any?>>

                        if (notificationsData.isEmpty()) {
                            Log.i("SurrealLiveQuery", "No unreceived notifications.")
                        } else {
                            for (pendingNotificationData in notificationsData) {
                                Log.d("SurrealLiveQuery", "Pending Notification Data: $pendingNotificationData")
                                val notification =
                                    Notification.fromDictionary(pendingNotificationData["notification"] as Map<String, Any?>)

                                if (notification.received) {
                                    Log.i("SurrealLiveQuery", "Notification already received. Skipping.")
                                    continue
                                }

                                Log.d("SurrealLiveQuery", "Pending Notification: $notification")

                                val title = notification.title
                                val content = notification.message
                                val notificationId =
                                    NOTIFICATION_ID_BASE + pendingNotificationData["id"].hashCode()
                                showNotification(appContext, title, content, notificationId)
                                notification.received = true
                                notification.update(client)
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
        // Create the NotificationChannel, but only on API 26+
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance).apply {
            description = NOTIFICATION_CHANNEL_DESCRIPTION
        }
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
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