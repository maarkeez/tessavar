package com.mkz.tessavar.screen.adapters.presentation

import korlibs.render.FileFilter
import korlibs.render.GameWindow
import korlibs.render.openFileDialog

interface WorldFileSaver {
    /** Saves the world file, and tells whether it was saved: false when the player cancels. */
    suspend fun saveWorld(
        fileName: String,
        text: String,
    ): Boolean
}

/** Downloads the file on the web, and asks where to save it with a dialog everywhere else. */
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

/** Downloads a text file named [fileName], and tells whether the platform can: only the web can. */
expect fun downloadFile(
    fileName: String,
    text: String,
): Boolean
