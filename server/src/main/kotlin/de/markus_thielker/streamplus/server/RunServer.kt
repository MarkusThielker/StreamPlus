package de.markus_thielker.streamplus.server

import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main() {

    // open server socket
    val serverSocket = ServerSocket(5050)

    // set running var for exit
    var running = true

    // open new thread accepting and managing connection requests
    thread {
        while (running) {

            // accept and route connections to new thread
            val client = serverSocket.accept()
            thread { ConnectionHandler(client = client) }
        }
    }

    // read server commands in main thread
    while (running) {

        print("Command: ")

        when(readLine()) {

            // stop server for command "exit"
            "exit" -> {
                println("Stopping Server...")
                running = false
            }

            // else -> return that input didn't match any valid command
            else -> { println("Unknown command! - Type \"exit\" to stop the server") }
        }
    }

    // stops all running threads
    exitProcess(0)
}