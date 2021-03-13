package de.markus_thielker.streamplus.server

import de.markus_thielker.streamplus.shared.network.TwitchTokenRequest
import de.markus_thielker.streamplus.shared.network.TwitchTokenResponse
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import khttp.responses.Response
import org.json.JSONObject

private const val clientId = "xa54hwj838r3y1ou3da65e8nlu4b6r"
private const val clientSecret = "*****" // client secret hidden for security reasons
private const val redirectUri = "https://imtherayze.com/authentication"

fun main(args : Array<String>) : Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * This function specifies the main module of the ktor-server by installing basic statusPages and contentNegotiation
 * as well as setting the routing for the supported requests.
 *
 * @author Markus Thielker
 *
 * */
fun Application.module() {

    install(StatusPages) {
        exception<Throwable> { e ->
            call.respondText(e.localizedMessage, ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    install(ContentNegotiation) {
        gson()
    }

    routing {

        post("/validate") {

            // get object from request payload
            val request = call.receive<TwitchTokenRequest>()

            // set url for login validation request
            val url = "https://id.twitch.tv/oauth2/token?" +
                    "client_id=$clientId&" +
                    "client_secret=$clientSecret&" +
                    "code=${request.token}&" +
                    "grant_type=authorization_code&" +
                    "redirect_uri=$redirectUri"

            postAndRespond(url, this)
        }

        post("/refresh") {

            // get object from request payload
            val request = call.receive<TwitchTokenRequest>()

            // set url for access token refresh request
            val url = "https://id.twitch.tv/oauth2/token?" +
                    "client_id=$clientId&" +
                    "client_secret=$clientSecret&" +
                    "refresh_token=${request.token}&" +
                    "grant_type=refresh_token&" +
                    "redirect_uri=$redirectUri"

            postAndRespond(url, this)
        }
    }
}

/**
 * This functions sends a post request to the passed url, parses access- and refresh tokens
 * to a json object and returns it as response to the initial call.
 *
 * @param url The url with all internal parameters, the request gets send to.
 * @param connection The pipeline to the initial call.
 *
 * @author Markus Thielker
 *
 * */
private suspend fun postAndRespond(url : String, connection : PipelineContext<Unit, ApplicationCall>) {

    // post request and get result as json-object
    val response : Response = khttp.post(url)
    val obj : JSONObject = response.jsonObject

    // get values from received response JSON string
    val accessToken = obj["access_token"] as String
    val refreshToken = obj["refresh_token"] as String

    // send json-string as call response
    connection.call.respond(TwitchTokenResponse(accessToken, refreshToken))
}