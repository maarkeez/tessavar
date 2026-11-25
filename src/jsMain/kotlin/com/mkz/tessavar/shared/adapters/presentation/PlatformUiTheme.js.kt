package com.mkz.tessavar.shared.adapters.presentation

import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams

actual fun requestedUiTheme(): String? = URLSearchParams(window.location.search).get("theme")
