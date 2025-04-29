package ru.gd_alt.youwilldrive.data.client

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset

object Connection {
    val notificationChannel: NotificationChannel = NotificationChannel(
        "main",
        "Main",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Main channel"
    }

    val cl = runBlocking {
        return@runBlocking SurrealDBClient.create(
            "ws://87.242.117.89:5457/rpc",
            "root",
            "iwilldrive",
            true
        ) { cl ->
            cl.use("main", "main")
        }
    }
}

fun main() {
    val client = Connection.cl
    runBlocking {
        val result = client.version()
        println(result)
        println(client.query("SELECT * FROM time::now()"))
        println((((client.query("SELECT * FROM 'александр'") as List<*>)[0] as Map<*, *>)["result"] as List<*>)[0])
        println(Charset.defaultCharset())
        client.close()
    }
}