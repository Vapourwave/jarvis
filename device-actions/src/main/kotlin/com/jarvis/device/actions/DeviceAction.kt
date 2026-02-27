package com.jarvis.device.actions

interface DeviceAction {
    val requiresExplicitConfirmation: Boolean
    fun describe(): String
}

data class OpenAppAction(
    val packageName: String
) : DeviceAction {
    override val requiresExplicitConfirmation: Boolean = false
    override fun describe(): String = "Open app: $packageName"
}

data class SendMessageAction(
    val contact: String,
    val message: String,
    val channel: String
) : DeviceAction {
    override val requiresExplicitConfirmation: Boolean = true
    override fun describe(): String = "Send $channel message to $contact"
}

data class WebSearchAction(
    val query: String
) : DeviceAction {
    override val requiresExplicitConfirmation: Boolean = false
    override fun describe(): String = "Web search: $query"
}

data class CallContactAction(
    val contact: String
) : DeviceAction {
    override val requiresExplicitConfirmation: Boolean = true
    override fun describe(): String = "Call contact: $contact"
}
