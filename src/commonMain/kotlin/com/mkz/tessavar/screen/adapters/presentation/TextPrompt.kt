package com.mkz.tessavar.screen.adapters.presentation

import korlibs.render.GameWindow
import korlibs.render.prompt
import kotlinx.coroutines.CancellationException

interface TextPrompt {
    suspend fun prompt(
        message: String,
        current: String,
    ): String?
}

class GameWindowTextPrompt(
    private val gameWindow: GameWindow,
) : TextPrompt {
    override suspend fun prompt(
        message: String,
        current: String,
    ): String? =
        try {
            gameWindow.prompt(message, current)
        } catch (_: CancellationException) {
            null
        }
}

expect fun needsTextPrompt(): Boolean
