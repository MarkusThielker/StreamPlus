package de.markus_thielker.streamplus.core.twitch.account

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import khttp.responses.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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
    private val clientSecret = "*****" // client secret hidden for security reasons
    private val redirectUri = "https://imtherayze.com/authentication"

    /**
     * This function is called when the twitch account shall connect to the twitch api application.
     * The result of the operation is returned in objects variable isConnected.
     *
     * @author Markus Thielker
     *
     * */
    fun connect() : Boolean {

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
        print("Enter code ($role) : ")
        return Scanner(System.`in`).nextLine()
    }

    /**
     * This function is called to validate the login, using the authorization code and generate the accessToken and refreshToken.
     *
     * @return returns true, if the request was successful.
     *
     * @author Markus Thielker
     *
     * */
    private fun validateLogin() : Boolean {

        // set url for login validation request
        val url = "https://id.twitch.tv/oauth2/token?client_id=$clientId&client_secret=$clientSecret&code=$accessToken&grant_type=authorization_code&redirect_uri=$redirectUri"

        // post request and get result as json-object
        val response : Response = khttp.post(url)
        val obj : JSONObject = response.jsonObject

        // get values from received response JSON string
        accessToken = obj["access_token"] as String // -> overwrite temporary code with final token
        refreshToken = obj["refresh_token"] as String

        // TODO: implement request-error recognition

        // return true
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
    private fun refreshAccessToken() : Boolean {

        // set url for access token refresh request
        val url = "https://id.twitch.tv/oauth2/token?client_id=$clientId&client_secret=$clientSecret&refresh_token=$refreshToken&grant_type=refresh_token&redirect_uri=$redirectUri"

        // post request and get result as json-object
        val response : Response = khttp.post(url)
        val obj : JSONObject = response.jsonObject

        // get values from received response JSON string
        accessToken = obj["access_token"] as String
        refreshToken = obj["refresh_token"] as String

        // TODO: implement request-error recognition

        // return true
        return true
    }

    /**
     * This function is called to validate the currently set accessToken.
     * It also fetches the username and userId.
     *
     * @author Markus Thielker
     *
     * */
    private fun validateAccessToken() {

        // set url for token validation request
        val url = "https://id.twitch.tv/oauth2/validate"

        // post request and get result as json-object
        val response : Response = khttp.get(url = url, headers = mapOf("Authorization" to "OAuth $accessToken"))
        val obj : JSONObject = response.jsonObject

        // get username (lowercase) and userId from response
        username = obj["login"] as String
        userId = obj["user_id"] as String
    }

    /**
     * This function is called to get the missing user information.
     * It fetches the displayName and email.
     *
     * @author Markus Thielker
     *
     * */
    private fun getUserData() {

        // set url for user data request
        val url = "https://api.twitch.tv/helix/users?id=$userId"

        // post request and get result as json-object
        val response : Response = khttp.get(url = url, headers = mapOf("Authorization" to "Bearer $accessToken", "Client-ID" to clientId))
        val obj : JSONObject = response.jsonObject

        // get displayName from response
        val tmp = obj["data"] as JSONArray
        val tmpTmp = tmp[0] as JSONObject

        displayName = tmpTmp["display_name"] as String
    }

    override fun toString() : String {
        return "AccountDebug [$role]: userId: $userId, username: $username, displayName: $displayName, accessToken: $accessToken, refreshToken: $refreshToken"
    }
}