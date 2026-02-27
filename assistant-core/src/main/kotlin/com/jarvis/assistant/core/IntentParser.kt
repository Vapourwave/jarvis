package com.jarvis.assistant.core

class IntentParser {
    fun parse(command: String): ParsedIntent = ParsedIntent(rawCommand = command.trim())
}

data class ParsedIntent(val rawCommand: String)
