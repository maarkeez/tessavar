package com.mkz.tessavar.screen.adapters.presentation

import korlibs.render.FileFilter
import korlibs.render.GameWindow
import korlibs.render.openFileDialog

interface WorldFileSaver {
    suspend fun saveWorld(
        fileName: String,
        text: String,
    ): Boolean
}

class GameWindowWorldFileSaver(
    private val gameWindow: GameWindow,
    messages: Messages,
    private val download: (fileName: String, text: String) -> Boolean = ::downloadFile,
) : WorldFileSaver {
    internal val worldFiles = FileFilter(messages[UiMessage.WORLD_FILE_PICKER_WORLDS] to listOf("*.toml"))

    override suspend fun saveWorld(
        fileName: String,
        text: String,
    ): Boolean {
        if (download(fileName, text)) return true
        val file = gameWindow.openFileDialog(worldFiles, write = true).firstOrNull() ?: return false
        file.writeString(text)
        return true
    }
}

expect fun downloadFile(
    fileName: String,
    text: String,
): Boolean
