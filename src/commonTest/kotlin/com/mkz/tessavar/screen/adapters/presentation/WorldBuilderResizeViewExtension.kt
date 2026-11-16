package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.descendants
import com.mkz.tessavar.shared.adapters.presentation.isFaded
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Stage

fun WorldBuilderResizeView.rowsText(): String = rowsValue.text

fun WorldBuilderResizeView.columnsText(): String = columnsValue.text

fun WorldBuilderResizeView.enabledRowsSteps(): List<Int> = rowsStepButtons.filterValues { button -> button.enabled }.keys.sorted()

fun WorldBuilderResizeView.enabledColumnsSteps(): List<Int> = columnsStepButtons.filterValues { button -> button.enabled }.keys.sorted()

fun WorldBuilderResizeView.fadedRowsSteps(): List<Int> = rowsStepButtons.filterValues { button -> button.isFaded() }.keys.sorted()

fun WorldBuilderResizeView.fadedColumnsSteps(): List<Int> = columnsStepButtons.filterValues { button -> button.isFaded() }.keys.sorted()

suspend fun ViewsForTesting.clickRowsStep(
    resizeView: WorldBuilderResizeView,
    step: Int,
) = clickOn(resizeView.rowsStepButtons.getValue(step))

suspend fun ViewsForTesting.clickColumnsStep(
    resizeView: WorldBuilderResizeView,
    step: Int,
) = clickOn(resizeView.columnsStepButtons.getValue(step))

suspend fun ViewsForTesting.clickDone(resizeView: WorldBuilderResizeView) = clickOn(resizeView.doneButton)

fun Stage.worldBuilderResizeView(): WorldBuilderResizeView? = descendants().filterIsInstance<WorldBuilderResizeView>().firstOrNull()
