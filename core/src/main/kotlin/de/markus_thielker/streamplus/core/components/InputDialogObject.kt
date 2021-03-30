package de.markus_thielker.streamplus.core.components

data class InputDialogObject(
    val title : String,
    val text : String = "",
    val hint : String = "",
    val button : String,
    var confirmed : Boolean = false
)