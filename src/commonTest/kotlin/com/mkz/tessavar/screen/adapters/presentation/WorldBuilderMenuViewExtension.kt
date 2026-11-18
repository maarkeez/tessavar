package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.descendants
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Image
import korlibs.korge.view.Stage

fun WorldBuilderMenuView.optionLabels(): List<String> = optionButtons.map { button -> button.text }

fun WorldBuilderMenuView.historyIconImage(icon: WorldBuilderMenuView.HistoryIcon): Image =
    when (icon) {
        WorldBuilderMenuView.HistoryIcon.UNDO -> undoButton
        WorldBuilderMenuView.HistoryIcon.REDO -> redoButton
    }!!.findViewByName(WorldBuilderMenuView.HISTORY_ICON) as Image

fun WorldBuilderMenuView.isHistoryAboveOptions(): Boolean {
    val undo = undoButton?.getGlobalBounds() ?: return false
    val redo = redoButton?.getGlobalBounds() ?: return false
    val firstOption = optionButtons.firstOrNull()?.getGlobalBounds() ?: return false
    return undo.top == redo.top && undo.right <= redo.left && undo.bottom <= firstOption.top
}

suspend fun ViewsForTesting.clickMenuButton(menuView: WorldBuilderMenuView) = clickOn(menuView.menuButton)

suspend fun ViewsForTesting.clickMenuOption(
    menuView: WorldBuilderMenuView,
    label: String,
) = clickOn(menuView.optionButtons.single { button -> button.text == label })

suspend fun ViewsForTesting.clickUndo(menuView: WorldBuilderMenuView) = clickOn(menuView.undoButton!!)

suspend fun ViewsForTesting.clickRedo(menuView: WorldBuilderMenuView) = clickOn(menuView.redoButton!!)

suspend fun ViewsForTesting.clickOutOfMenu(
    x: Double,
    y: Double,
) = mouseMoveAndClickTo(x, y)

fun Stage.worldBuilderMenuView(): WorldBuilderMenuView? = descendants().filterIsInstance<WorldBuilderMenuView>().firstOrNull()
