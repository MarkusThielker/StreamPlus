package de.markus_thielker.streamplus.core

import de.markus_thielker.streamplus.core.components.InputDialogObject
import de.markus_thielker.streamplus.core.components.MessageDialogObject

interface UIComponent {

    fun updateChatbotState(status : ChatbotStatus)

    suspend fun requestInputDialog(dialog : InputDialogObject) : String

    fun requestMessageDialog(dialog : MessageDialogObject)
}