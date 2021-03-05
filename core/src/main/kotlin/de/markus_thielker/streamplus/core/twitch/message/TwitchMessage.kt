package de.markus_thielker.streamplus.core.twitch.message

data class TwitchMessage(

    val channel : TwitchMessageChannel,
    val messageId : String,
    val color : String,

    val author : String,
    val message : String,

    val isBroadcaster : Boolean = false,
    val isModerator : Boolean = false,
    val isSubscriber : Boolean = false,

    )