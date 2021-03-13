package de.markus_thielker.streamplus.core.twitch.account

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.markus_thielker.streamplus.shared.network.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.*

/**
 * This class represents the instance of a Twitch account. It can connect to Twitch api, login and validate all tokens by calling the connect() function.
 * The generated tokes are stored inside a file on the local machine and are reused on the next connection attempt of the account role.
 *
 * @param role The passed account role decides about the requested scopes and which stores tokens are used.
 *
 * @author Markus Thielker
 *
 * */
class TwitchAccount(private val role : TwitchAccountRole) {

    // connection state
    var isConnected = false

    // frontend information
    var username : String = ""
    var displayName : String = ""
    var userId : String = ""

    // backend information
    var accessToken = ""
    private var refreshToken = ""

    // constant values, set for object wide reuse
    private val clientId = "xa54hwj838r3y1ou3da65e8nlu4b6r"
    private val redirectUri = "https://imtherayze.com/authentication"

    private val httpClient = HttpClient(Apache) {
        install(feature = JsonFeature) {
            serializer = GsonSerializer {
                serializeNulls()
                disableHtmlEscaping()
            }
        }
    }

    /**
     * This function is called when the twitch account shall connect to the twitch api application.
     * The result of the operation is returned in objects variable isConnected.
     *
     * @author Markus Thielker
     *
     * */
    suspend fun connect() : Boolean {

        // init variable for account tokens
        val accounts : HashMap<TwitchAccountRole, HashMap<String, String>>

        // get AppData path
        val appDataPath = System.getenv("AppData")

        val file = File("$appDataPath\\StreamPlus\\credentials.json")

        // read string and convert to map object afterwards
        file.bufferedReader().use { reader ->

            // get credentials object from json file
            accounts = Gson().fromJson(reader.readLine(), object : TypeToken<HashMap<TwitchAccountRole, HashMap<String, String>>>() {}.type)

            // get stored tokens from file
            accessToken = accounts[role]!!["accessToken"] as String
            refreshToken = accounts[role]!!["refreshToken"] as String
        }

        // check if any of the tokens is missing
        if (accessToken.isEmpty() || refreshToken.isEmpty()) {

            // open authentication in browser to get temporary authorization code
            accessToken = logIntoTwitchAccount()

            // validate current authorization code temporary stored in "accessToken"-variable
            isConnected = validateLogin()

        } else isConnected = refreshAccessToken()

        // update stored account tokens to file
        CoroutineScope(Dispatchers.IO).launch {

            // update tokens in map
            accounts[role]!!["accessToken"] = accessToken
            accounts[role]!!["refreshToken"] = refreshToken

            // convert map to json sting
            val writeString = Gson().toJson(accounts)

            // write string to file
            // TODO: add file encryption to protect tokens
            file.writeText(writeString)
        }

        // validate the current accessToken
        validateAccessToken()

        // get the missing user data
        getUserData()

        // return if connection was successful
        return false

        // TODO: run background validation control thread to react on access revocations
    }

    /**
     * This function is called to log in to a twitch account and get the authorization code.
     *
     * @return created authorization code for twitch login.
     *
     * @author Markus Thielker
     *
     * */
    private fun logIntoTwitchAccount() : String {

        // set url to authentication page
        val url = "https://id.twitch.tv/oauth2/authorize?client_id=$clientId&redirect_uri=$redirectUri&response_type=code&force_verify=true&scope="

        // decide required scopes, differing from account role
        val scopes =
            if (role == TwitchAccountRole.Streamer) "analytics:read:games%20bits:read%20channel:read:subscriptions%20user:edit%20moderation:read%20user:edit:broadcast%20user:read:broadcast%20channel:moderate"
            else "chat:edit%20chat:read%20whispers:read%20whispers:edit%20channel:moderate%20channel_editor"

        // open browser with previously defined parameters
        Desktop.getDesktop().browse(URI("$url$scopes"))

        // get user input of code due to missing GUI implementation
        // TODO: request authorization code in UI dialog
        print("Enter code ($role): ")
        return readLine() ?: ""
    }

    /**
     * This function is called to validate the login, using the authorization code and generate the accessToken and refreshToken.
     *
     * @return returns true, if the request was successful.
     *
     * @author Markus Thielker
     *
     * */
    private suspend fun validateLogin() : Boolean {

        // post request -> validate login via custom server
        val response = httpClient.post<TwitchTokenResponse> {
            url("http://server.markus-thielker.de:8080/validate")
            contentType(ContentType.Application.Json)
            body = TwitchTokenRequest(token = accessToken)
        }

        // get values from received response JSON string
        accessToken = response.accessToken
        refreshToken = response.refreshToken

        // TODO: implement request-error recognition

        return true
    }

    /**
     * This function is called to refresh the restored accessToken, using the refreshToken.
     *
     * @return returns true, if the request was successful.
     *
     * @author Markus Thielker
     *
     * */
    private suspend fun refreshAccessToken() : Boolean {

        // post request -> refresh access token via custom server
        val response = httpClient.post<TwitchTokenResponse> {
            url("http://server.markus-thielker.de:8080/refresh")
            contentType(ContentType.Application.Json)
            body = TwitchTokenRequest(token = refreshToken)
        }

        // get values from received response JSON string
        accessToken = response.accessToken
        refreshToken = response.refreshToken

        // TODO: implement request-error recognition

        return true
    }

    /**
     * This function is called to validate the currently set accessToken.
     * It also fetches the username and userId.
     *
     * @author Markus Thielker
     *
     * */
    private suspend fun validateAccessToken() {

        // get request -> validate access token using Twitch API
        val response = httpClient.get<TwitchTokenValidation> {
            url("https://id.twitch.tv/oauth2/validate")
            header("Authorization", "OAuth $accessToken")
        }

        // get username (lowercase) and userId from response
        username = response.login
        userId = response.user_id
    }

    /**
     * This function is called to get the missing user information.
     * It fetches the displayName and email.
     *
     * @author Markus Thielker
     *
     * */
    private suspend fun getUserData() {

        // get request -> request user data using Twitch API
        val response = httpClient.get<String> {
            url("https://api.twitch.tv/helix/users?id=$userId")
            header("Authorization", "Bearer $accessToken")
            header("Client-Id", clientId)
        }

        // parse value from response string
        displayName = response.split("\"")[13]

        // close http client
        httpClient.close()
    }

    override fun toString() : String {
        return "AccountDebug [$role]: userId: $userId, username: $username, displayName: $displayName, accessToken: $accessToken, refreshToken: $refreshToken"
    }
}