package de.markus_thielker.streamplus.core

import de.markus_thielker.streamplus.core.twitch.account.TwitchAccount
import de.markus_thielker.streamplus.core.twitch.account.TwitchAccountRole
import de.markus_thielker.streamplus.core.twitch.message.TwitchMessage
import de.markus_thielker.streamplus.core.twitch.message.TwitchMessageChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.properties.Delegates

class Chatbot(val uiComponent : UIComponent) {

    var status by Delegates.observable(ChatbotStatus.Stopped) { _, _, newValue -> uiComponent.updateChatbotState(newValue) }

    private val streamer = TwitchAccount(this, TwitchAccountRole.Streamer)
    private val chatbot = TwitchAccount(this, TwitchAccountRole.Chatbot)

    private lateinit var client : Socket
    private lateinit var reader : BufferedReader
    private lateinit var writer : BufferedWriter

    /**
     * The chatbot connects it's both twitch accounts, validates their login and if successful joins the streamer-accounts chat.
     *
     * @author Markus Thielker
     *
     * */
    fun connect() {

        CoroutineScope(Dispatchers.Default).launch {

            // update status and trigger button text
            status = ChatbotStatus.Startup

            // move account connection to IO scope
            withContext(Dispatchers.IO) {

                // connect both twitch accounts (chat bot first for browser session reasons)
                chatbot.connect()
                if (chatbot.isConnected) streamer.connect()

            }

            if (chatbot.isConnected && streamer.isConnected) {
                // update status and trigger button text
                status = ChatbotStatus.Running

                // finally join streamers chat
                joinChat()
            }
            else {
                status = ChatbotStatus.Stopped
            }
        }
    }

    /**
     * This function is called to send a message to the chat, the bot is connected to.
     *
     * @author Markus Thielker
     *
     * */
    private fun sendMessage(message : String) {
        writer.write("PRIVMSG #${streamer.username} :$message \n")
        writer.flush()
        println("#automated# ${chatbot.username}: $message".trimIndent())
    }

    /**
     * This function is called to disconnect the chatbot from the chat.
     *
     * @author Markus Thielker
     *
     * */
    fun disconnect() {

        status = ChatbotStatus.Shutdown

        sendMessage("/me disconnected [Kotlin]")

        writer.write("PART #${streamer.username}\n")
        writer.flush()

        reader.close()
        writer.close()
        client.close()

        status = ChatbotStatus.Stopped
    }

    /**
     * This function uses the previously validated Twitch accounts to connect the chatbot to the streamers chat and starts listening to it.
     *
     * @author Markus Thielker
     *
     * */
    private fun joinChat() {

        // connect to twitch chat server
        client = Socket("irc.chat.twitch.tv", 6667)

        // initialize input and output streams to chat server
        reader = BufferedReader(InputStreamReader(client.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))

        thread {

            writer.write("PASS oauth:${chatbot.accessToken}\n")
            writer.flush()
            writer.write("NICK ${chatbot.username}\n")
            writer.flush()
            writer.write("JOIN #${streamer.username}\n")
            writer.flush()

            // enable twitch irc functions
            writer.write("CAP REQ :twitch.tv/tags \n")
            writer.flush()
            writer.write("CAP REQ :twitch.tv/commands \n")
            writer.flush()

            sendMessage("/me connected [Kotlin]")

            while (status == ChatbotStatus.Running) {

                val messageRaw = reader.readLine()

                if (messageRaw == "PING :tmi.twitch.tv") {
                    writer.write("PONG :tmi.twitch.tv\r\n")
                    writer.flush()
                    continue
                }

                val messageValueHolder = hashMapOf<String, Any>()

                // get the channel from raw message (public chat or whisper)
                try {

                    val channelIndex = messageRaw.toLowerCase().indexOf(".tmi.twitch.tv ") + 15
                    val channelString = messageRaw.substring(startIndex = channelIndex, endIndex = channelIndex + 7)

                    messageValueHolder["channel"] = when (channelString) {
                        "PRIVMSG" -> TwitchMessageChannel.Public
                        "WHISPER" -> TwitchMessageChannel.Private
                        else -> continue
                    }

                } catch (ignore : Exception) {
                    continue
                }

                // public chat -> get the author and message from raw message
                if (messageValueHolder["channel"] == TwitchMessageChannel.Public) {

                    // get the author from raw message
                    try {

                        val authorIndex = messageRaw.toLowerCase().indexOf(";display-name=") + 14
                        val authorIndexEnd = messageRaw.toLowerCase().indexOf(";emotes=")
                        val authorString = messageRaw.substring(startIndex = authorIndex, endIndex = authorIndexEnd)

                        messageValueHolder["author"] = authorString

                    } catch (ignore : Exception) {
                        continue
                    }

                    // get the message from raw message
                    try {

                        val messageIndex = messageRaw.toLowerCase().indexOf(" #${streamer.username} :") + (4 + streamer.username.length)
                        val messageString = messageRaw.substring(startIndex = messageIndex)

                        messageValueHolder["message"] = messageString

                    } catch (ignore : Exception) {
                        continue
                    }

                }
                // whisper -> get the author and message from raw message
                else {
                    continue
                }

                // get the messageId from raw message
                try {

                    val colorIndex = messageRaw.toLowerCase().indexOf(";color=") + 7
                    val colorIndexEnd = messageRaw.toLowerCase().indexOf(";display-name=")
                    val colorString = messageRaw.substring(startIndex = colorIndex, endIndex = colorIndexEnd)

                    messageValueHolder["color"] = colorString

                } catch (ignore : Exception) {
                    continue
                }

                // get the messageId from raw message
                try {

                    val messageIdIndex = messageRaw.toLowerCase().indexOf(";id=") + 4
                    val messageIdIndexEnd = messageRaw.toLowerCase().indexOf(";mod=")
                    val messageIdString = messageRaw.substring(startIndex = messageIdIndex, endIndex = messageIdIndexEnd)

                    messageValueHolder["messageId"] = messageIdString

                } catch (ignore : Exception) {
                    continue
                }

                // get if user is broadcaster from raw message
                try {

                    messageValueHolder["broadcaster"] = messageValueHolder["author"] == streamer.displayName

                } catch (ignore : Exception) {
                    continue
                }

                // get if user is moderator from raw message
                try {

                    val moderatorIndex = messageRaw.toLowerCase().indexOf(";mod=") + 5
                    val moderatorIndexEnd = messageRaw.toLowerCase().indexOf(";room-id=")
                    val moderatorString = messageRaw.substring(startIndex = moderatorIndex, endIndex = moderatorIndexEnd)

                    messageValueHolder["moderator"] = moderatorString == "1"

                } catch (ignore : Exception) {
                    continue
                }

                // get if user is subscriber from raw message
                try {

                    val subscriberIndex = messageRaw.toLowerCase().indexOf(";subscriber=") + 12
                    val subscriberIndexEnd = messageRaw.toLowerCase().indexOf(";tmi-sent-ts=")
                    val subscriberString = messageRaw.substring(startIndex = subscriberIndex, endIndex = subscriberIndexEnd)

                    messageValueHolder["subscriber"] = subscriberString == "1"

                } catch (ignore : Exception) {
                    continue
                }

                // create Twitch message object from previously parsed values
                val message = TwitchMessage(

                    messageValueHolder["channel"] as TwitchMessageChannel,
                    messageValueHolder["messageId"] as String,
                    messageValueHolder["color"] as String,

                    messageValueHolder["author"] as String,
                    messageValueHolder["message"] as String,

                    messageValueHolder["broadcaster"] as Boolean,
                    messageValueHolder["moderator"] as Boolean,
                    messageValueHolder["subscriber"] as Boolean
                )

                println(message)

                // TODO: start message processing
            }
        }
    }
}