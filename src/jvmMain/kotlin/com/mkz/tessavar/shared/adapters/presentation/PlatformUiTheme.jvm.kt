package com.mkz.tessavar.shared.adapters.presentation

actual fun requestedUiTheme(): String? = System.getProperty("tessavar.theme")
