package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.isFaded
import com.mkz.tessavar.shared.adapters.presentation.sidesIn
import korlibs.image.bitmap.Bitmap
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Image

fun InventoryView.titleText(): String = titleView.nameText()

fun InventoryView.goldText(): String = goldView.nameText()

fun InventoryView.isDisplayingGoldButton(): Boolean = goldButton.visible && !goldView.visible

fun InventoryView.goldButtonText(): String = goldButton.amountText

fun InventoryView.isGoldButtonFaded(): Boolean = goldButton.isFaded

fun InventoryView.arrowsVisible(): Boolean = previousOwnerButton.visible && nextOwnerButton.visible

fun InventoryView.arrowsEnabled(): Pair<Boolean, Boolean> = previousOwnerButton.enabled to nextOwnerButton.enabled

fun InventoryView.arrowsFaded(): Pair<Boolean, Boolean> = previousOwnerButton.isFaded() to nextOwnerButton.isFaded()

fun InventoryView.isTitleLaidOut(): Boolean {
    val title = titleView.getGlobalBounds()
    val gold = goldView.getGlobalBounds()
    return previousOwnerButton.getGlobalBounds().right <= title.left &&
        nextOwnerButton.getGlobalBounds().left >= gold.right &&
        nextOwnerButton.getGlobalBounds().right < closeButton.getGlobalBounds().left
}

fun InventoryView.rowSides(): List<Pair<Double, Double>> =
    storageSpaceViews.chunked(InventoryView.STORAGE_SPACES_PER_ROW).map { row ->
        row.first().sidesIn(this).first to
            row.last().sidesIn(this).second
    }

fun InventoryView.storageSpaceWidths(): List<Double> = storageSpaceViews.map { storageSpaceView -> storageSpaceView.getBounds(this).width }

fun InventoryView.closeSides(): Pair<Double, Double> = closeButton.sidesIn(this)

fun InventoryView.storageSpaceCount(): Int = storageSpaceViews.size

fun InventoryView.storageSpaceBitmaps(): List<Bitmap?> = storageSpaceViews.map { storageSpaceView -> (storageSpaceView.findViewByName(StorageSpaceView.ITEM) as? Image)?.bitmap?.base }

fun InventoryView.equippedStorageSpaceIndexes(): List<Int> = storageSpaceViews.indices.filter { index -> storageSpaceViews[index].findViewByName(StorageSpaceView.EQUIPPED_FRAME) != null }

fun InventoryView.selectedStorageSpaceIndexes(): List<Int> = storageSpaceViews.indices.filter { index -> storageSpaceViews[index].findViewByName(StorageSpaceView.SELECTION_FRAME) != null }

suspend fun ViewsForTesting.clickStorageSpace(
    inventoryView: InventoryView,
    index: Int,
) = clickOn(inventoryView.storageSpaceViews[index])

suspend fun ViewsForTesting.clickClose(inventoryView: InventoryView) = clickOn(inventoryView.closeButton)

suspend fun ViewsForTesting.clickGoldButton(inventoryView: InventoryView) = clickOn(inventoryView.goldButton)

suspend fun ViewsForTesting.clickPreviousOwner(inventoryView: InventoryView) = clickOn(inventoryView.previousOwnerButton)

suspend fun ViewsForTesting.clickNextOwner(inventoryView: InventoryView) = clickOn(inventoryView.nextOwnerButton)

suspend fun ViewsForTesting.swipeInventory(
    inventoryView: InventoryView,
    dx: Double,
) {
    val start =
        inventoryView.storageSpaceViews
            .first()
            .getGlobalBounds()
            .center
    mouseMoveTo(start.x, start.y)
    mouseDown()
    val steps = 5
    for (step in 1..steps) mouseMoveTo(start.x + dx * step / steps, start.y)
    mouseUp()
}

suspend fun ViewsForTesting.swipeInventoryWhileItIsDisplayedAgain(
    inventoryView: InventoryView,
    dx: Double,
) {
    val start =
        inventoryView.storageSpaceViews
            .first()
            .getGlobalBounds()
            .center
    mouseMoveTo(start.x, start.y)
    mouseDown()
    inventoryView.display(emptyList(), gold = 0)
    val steps = 5
    for (step in 1..steps) mouseMoveTo(start.x + dx * step / steps, start.y)
    mouseUp()
}
