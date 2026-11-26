package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.sidesIn
import korlibs.korge.tests.ViewsForTesting

fun CancelAndConfirmView.buttonSides(): Pair<Double, Double> = cancelButton.cancelButton.sidesIn(this).first to confirmButton.confirmButton.sidesIn(this).second

fun CancelAndConfirmView.buttonWidths(): List<Double> = listOf(cancelButton.cancelButton.width, confirmButton.confirmButton.width)

suspend fun ViewsForTesting.clickCancel(cancelAndConfirmView: CancelAndConfirmView) = clickCancel(cancelAndConfirmView.cancelButton)

suspend fun ViewsForTesting.clickConfirm(cancelAndConfirmView: CancelAndConfirmView) = clickConfirm(cancelAndConfirmView.confirmButton)
