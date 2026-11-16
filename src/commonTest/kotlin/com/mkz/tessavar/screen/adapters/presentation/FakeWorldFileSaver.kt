package com.mkz.tessavar.screen.adapters.presentation

class FakeWorldFileSaver : WorldFileSaver {
    val savedFiles = mutableListOf<Pair<String, String>>()

    var isCancelled = false

    var failure: Exception? = null

    override suspend fun saveWorld(
        fileName: String,
        text: String,
    ): Boolean {
        failure?.let { throw it }
        if (isCancelled) return false
        savedFiles += fileName to text
        return true
    }
}
