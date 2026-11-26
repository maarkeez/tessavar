package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import korlibs.image.color.RGBA
import korlibs.korge.tests.ViewsForTesting

fun PrimaryButton.buttonText(): String = primaryButton.text

fun PrimaryButton.buttonTextColor(): RGBA = primaryButton.textColor

suspend fun ViewsForTesting.clickPrimary(primaryButton: PrimaryButton) = clickOn(primaryButton.primaryButton)
