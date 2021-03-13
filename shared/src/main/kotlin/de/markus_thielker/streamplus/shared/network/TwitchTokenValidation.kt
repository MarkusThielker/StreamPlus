package de.markus_thielker.streamplus.shared.network

data class TwitchTokenValidation(
    val client_id : String,
    val login : String,
    val scopes : Array<String>,
    val user_id : String,
    val expires_in : Int
)