package de.markus_thielker.streamplus.ui

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.markus_thielker.streamplus.core.Chatbot
import de.markus_thielker.streamplus.core.ChatbotStatus
import de.markus_thielker.streamplus.core.UIComponent
import de.markus_thielker.streamplus.core.components.InputDialogObject
import de.markus_thielker.streamplus.core.components.MessageDialogObject
import de.markus_thielker.streamplus.core.components.ValueNotEnteredException
import de.markus_thielker.streamplus.ui.views.NavigationView
import de.markus_thielker.streamplus.ui.views.commands.commandsView
import de.markus_thielker.streamplus.ui.views.dashboard.dashboardView
import de.markus_thielker.streamplus.ui.views.settings.settingsView
import kotlinx.coroutines.delay
import java.io.File

fun main() { Application().run() }

class Application : UIComponent {

    // navigation component
    private val navigationView = mutableStateOf(NavigationView.Dashboard)

    // chatbot components
    private val chatbotButtonText = mutableStateOf("Start Chatbot")
    private var chatbot = Chatbot(this)

    // input dialog components
    private var inputDialog = mutableStateOf(InputDialogObject(title = "", button = ""))
    private var inputDialogVisible = mutableStateOf(false)
    private var inputDialogContent = mutableStateOf("")

    // message dialog components
    private var messageDialog = mutableStateOf(MessageDialogObject(title = ""))
    private var messageDialogVisible = mutableStateOf(false)

    fun run() {

        // check for files and create missing ones
        checkAppDataStructure()

        // create theme
        val lightColors = lightColors(
            primary = Color(0xFFFF4500),
            primaryVariant = Color(0xFFFF3300),
            onPrimary = Color.White
        )

        // show main window
        Window(
            title = "StreamPlus",
            size = IntSize(1280, 720),
            onDismissRequest = { if (chatbot.status == ChatbotStatus.Running) chatbot.disconnect() }
        ) {

            // base theme
            MaterialTheme(
                colors = lightColors,
            ) {

                Row {

                    // menu bar
                    sideMenu()

                    // input dialog if visible = true
                    if (inputDialogVisible.value)
                        showInputDialog()

                    // message dialog if visible = true
                    if (messageDialogVisible.value)
                        showMessageDialog()

                    // navigation host
                    when (navigationView.value) {
                        NavigationView.Dashboard -> dashboardView()
                        NavigationView.Commands -> commandsView()
                        NavigationView.Settings -> settingsView()
                    }
                }
            }
        }
    }

    private fun checkAppDataStructure() {

        // get AppData path
        val appDataPath = System.getenv("AppData")

        // create directory if not existing
        val dir = File("$appDataPath\\StreamPlus")
        if (!dir.exists()) dir.mkdir()

        // create file if not existing
        val file = File("$appDataPath\\StreamPlus\\credentials.json")
        if (file.createNewFile()) file.writeText("{\"Streamer\":{\"accessToken\":\"\",\"refreshToken\":\"\"},\"Chatbot\":{\"accessToken\":\"\",\"refreshToken\":\"\"}}")
    }

    @Composable
    fun sideMenu() {

        // base with elevation
        Card(
            elevation = 5.dp,
            modifier = Modifier.fillMaxHeight(),
            shape = RoundedCornerShape(0),
            backgroundColor = MaterialTheme.colors.background
        ) {

            // split in two regions
            Column {

                // region at top (navigation points)
                Column(
                    modifier = Modifier.width(200.dp),
                    verticalArrangement = Arrangement.Top
                ) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    menuButton(
                        title = "Dashboard",
                        onClick = { navigationView.value = NavigationView.Dashboard }
                    )

                    menuButton(
                        title = "Commands",
                        onClick = { navigationView.value = NavigationView.Commands }
                    )

                    menuButton(
                        title = "Settings",
                        onClick = { navigationView.value = NavigationView.Settings }
                    )
                }

                // region at bottom (chatbot controls)
                Column(
                    modifier = Modifier.width(200.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {

                    menuButton(
                        title = chatbotButtonText.value,
                        onClick = {

                            when (chatbot.status) {

                                ChatbotStatus.Running -> {
                                    chatbot.disconnect()
                                }

                                ChatbotStatus.Stopped -> {
                                    chatbot.connect()
                                }

                                else -> {
                                }
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                }
            }
        }
    }

    override fun updateChatbotState(status : ChatbotStatus) {

        when (status) {

            ChatbotStatus.Startup -> chatbotButtonText.value = "Chatbot starting..."
            ChatbotStatus.Running -> chatbotButtonText.value = "Stop Chatbot"
            ChatbotStatus.Shutdown -> chatbotButtonText.value = "Chatbot stopping..."
            ChatbotStatus.Stopped -> chatbotButtonText.value = "Start Chatbot"
        }
    }

    override suspend fun requestInputDialog(dialog : InputDialogObject) : String {

        // clear input field value
        inputDialogContent.value = ""

        // replace state dialog -> overwritten with visible = true
        inputDialog.value = dialog
        inputDialogVisible.value = true

        // while dialog is visible wait here
        while (inputDialogVisible.value) delay(200)

        // check if input was confirmed with button click or closed another way
        if (!inputDialog.value.confirmed) throw ValueNotEnteredException()

        // return entered value
        return inputDialogContent.value
    }

    override fun requestMessageDialog(dialog : MessageDialogObject) {
        messageDialog.value = dialog
        messageDialogVisible.value = true
    }

    @Composable
    fun showInputDialog() {

        Dialog(
            onDismissRequest = { inputDialogVisible.value = false },
            properties = DialogProperties(size = IntSize(400, 300)),
            content = {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = inputDialog.value.title,
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = inputDialog.value.text,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextField(
                        value = inputDialogContent.value,
                        onValueChange = {
                            inputDialogContent.value = it
                        },
                        label = { Text(inputDialog.value.hint) },
                        modifier = Modifier
                            .size(400.dp, 55.dp)
                            .clip(shape = RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            inputDialog.value.confirmed = true
                            inputDialogVisible.value = false
                        },
                        content = { Text(text = inputDialog.value.button) },
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        )
    }

    @Composable
    fun showMessageDialog() {

        AlertDialog(
            title = { Text(messageDialog.value.title) },
            text = { Text(messageDialog.value.text) },
            onDismissRequest = { messageDialogVisible.value = false },
            modifier = Modifier.clip(shape = RoundedCornerShape(5.dp)),
            confirmButton = {
                Button(
                    onClick = { messageDialogVisible.value = false },
                    content = { Text(text = "Okay") },
                    modifier = Modifier.padding(16.dp)
                )
            },
        )
    }
}

@Composable
fun menuButton(title : String, onClick : () -> Unit) {

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0),
        onClick = onClick
    ) {
        Text(title)
    }
}