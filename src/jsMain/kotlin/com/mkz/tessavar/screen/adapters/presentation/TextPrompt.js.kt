package com.mkz.tessavar.screen.adapters.presentation

import kotlinx.browser.window

actual fun needsTextPrompt(): Boolean {
    val navigator = window.navigator
    val isIPad = navigator.platform == "MacIntel" && (navigator.asDynamic().maxTouchPoints as? Int ?: 0) > 1
    return Regex("iPad|iPhone|iPod").containsMatchIn(navigator.userAgent) || isIPad
}
