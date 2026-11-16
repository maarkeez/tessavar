package com.mkz.tessavar.screen.adapters.presentation

class FakeTextPrompt(
    private val answer: String?,
) : TextPrompt {
    val prompts = mutableListOf<Pair<String, String>>()

    override suspend fun prompt(
        message: String,
        current: String,
    ): String? {
        prompts += message to current
        return answer
    }
}
