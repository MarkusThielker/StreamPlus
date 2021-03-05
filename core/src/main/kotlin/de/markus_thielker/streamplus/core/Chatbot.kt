package de.markus_thielker.streamplus.core

import de.markus_thielker.streamplus.core.twitch.account.TwitchAccount
import de.markus_thielker.streamplus.core.twitch.account.TwitchAccountRole
import de.markus_thielker.streamplus.core.twitch.message.TwitchMessageChannel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.properties.Delegates

class Chatbot(private val statusChangedListener : () -> Unit) {

    var status by Delegates.observable(ChatbotStatus.Stopped) { _, _, _ -> statusChangedListener.invoke() }

    private val streamer = TwitchAccount(TwitchAccountRole.Streamer)
    private val chatbot = TwitchAccount(TwitchAccountRole.Chatbot)

    private lateinit var client : Socket
    private lateinit var reader : BufferedReader
    private lateinit var writer : BufferedWriter

    /**
     * The chatbot connects it's both twitch accounts, validates their login and if successful joins the streamer-accounts chat.
     *
     * @author Markus +\last name/+
     *
     * */
    fun connect() {

        thread {

            // update status and trigger button text
            status = ChatbotStatus.Startup

            // connect both twitch accounts (chatbot first for browser session reasons)
            chatbot.connect()
            streamer.connect()

            // update status and trigger button text
            status = ChatbotStatus.Running

            // TODO: check if account are connected

            // finally join streamers chat
            joinChat()
        }
    }

    /**
     * This function is called
     *
     * @author Markus +\last name/+
     *
     * */
    private fun sendMessage(message : String) {
        writer.write("PRIVMSG #${streamer.username} :$message \n")
        writer.flush()
        println("#automated# ${chatbot.username}: $message".trimIndent())
    }

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

                val message = hashMapOf<String, Any>()

                // raw message string
                // @badge-info=;badges=broadcaster/1;client-nonce=7beb95da93799b822e379540868dc4b6;color=#FF4500;display-name=ImTheRayze;emotes=;flags=;id=0893b61e-3cab-4309-8710-65317a726197;mod=0;room-id=152068123;subscriber=0;tmi-sent-ts=1614052536358;turbo=0;user-id=152068123;user-type= :imtherayze!imtherayze@imtherayze.tmi.twitch.tv PRIVMSG #imtherayze :Hey was geht ab

                // TODO: react to PING message

                try {

                    val channelIndex = messageRaw.toLowerCase().indexOf(".tmi.twitch.tv ", ignoreCase = true)
                    val channelString = messageRaw.substring(startIndex = channelIndex, endIndex = channelIndex + 7)

                    message["channel"] = when (channelString) {
                        "PRIVMSG" -> TwitchMessageChannel.Public
                        "WHISPER" -> TwitchMessageChannel.Private
                        else -> continue
                    }

                } catch (ignore : Exception) {
                    break
                }

                // TODO: get message author

                // TODO: get message content

                // TODO: get message messageID

                // TODO: get message roles (broadcaster, subscriber, vip, moderator)

                // TODO: start message processing
            }
        }
    }
}