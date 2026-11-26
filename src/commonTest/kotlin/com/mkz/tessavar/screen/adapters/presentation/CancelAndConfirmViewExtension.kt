package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sidesIn
import korlibs.korge.tests.ViewsForTesting

fun CancelAndConfirmView.buttonSides(): Pair<Double, Double> = cancelButton.cancelButton.sidesIn(this).first to confirmButton.primaryButton.sidesIn(this).second

fun CancelAndConfirmView.buttonWidths(): List<Double> = listOf(cancelButton.cancelButton.width, confirmButton.primaryButton.width)

suspend fun ViewsForTesting.clickCancel(cancelAndConfirmView: CancelAndConfirmView) = clickCancel(cancelAndConfirmView.cancelButton)

suspend fun ViewsForTesting.clickConfirm(cancelAndConfirmView: CancelAndConfirmView) = clickPrimary(cancelAndConfirmView.confirmButton)
