package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.isFaded
import com.mkz.tessavar.shared.adapters.presentation.sidesIn
import korlibs.korge.ui.UIButton

fun GiveGoldActionsView.buttonSides(): Pair<Double, Double> = cancelButton.sidesIn(this).first to giveButton.sidesIn(this).second

fun GiveGoldActionsView.giveLabel(): String = giveButton.text

fun GiveGoldActionsView.isAllEnabled(): Boolean = allButton.enabled

fun GiveGoldActionsView.isAllFaded(): Boolean = allButton.isFaded()

fun GiveGoldActionsView.isAsTallAsFinishPlayerTurn(): Boolean = listOf(cancelButton, allButton, giveButton).all { button -> button.height == UIButton.DEFAULT_SIZE.height }
