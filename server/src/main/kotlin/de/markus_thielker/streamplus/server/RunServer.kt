package de.markus_thielker.streamplus.server

import java.net.ServerSocket
import kotlin.concurrent.thread

fun main() {

    val serverSocket = ServerSocket(5050)

    var running = true

    thread {
        while (running) {
            val client = serverSocket.accept()
            thread { ConnectionHandler(client = client) }
        }
    }

    while (running) {

        when(readLine()) {
            "exit" -> {
                println("Stopping Server...")
                running = false
            }
            else -> { println("Unknown command! - Type \"exit\" to stop the server") }
        }
    }

    println("Server stopped.")
}