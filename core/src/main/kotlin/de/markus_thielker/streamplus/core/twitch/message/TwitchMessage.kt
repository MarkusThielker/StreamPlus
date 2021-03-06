package de.markus_thielker.streamplus.core.twitch.message

class TwitchMessage(

    val channel : TwitchMessageChannel,
    val messageId : String,
    val color : String,

    val author : String,
    val message : String,

    val isBroadcaster : Boolean = false,
    val isModerator : Boolean = false,
    val isSubscriber : Boolean = false,

    ) {

    override fun toString() : String {
        return "$channel <$messageId> <$color> -> $author [ broadcaster = $isBroadcaster | moderator = $isModerator | subscriber = $isSubscriber ] -> $message"
    }
}