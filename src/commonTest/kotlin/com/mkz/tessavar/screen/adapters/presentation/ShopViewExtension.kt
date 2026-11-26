package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.isFaded
import com.mkz.tessavar.shared.adapters.presentation.sidesIn
import korlibs.image.bitmap.Bitmap
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Image

fun ShopView.rowSides(): List<Pair<Double, Double>> = storageSpaceViews.chunked(ShopView.STORAGE_SPACES_PER_ROW).map { row -> row.first().sidesIn(this).first to row.last().sidesIn(this).second }

fun ShopView.storageSpaceWidths(): List<Double> = storageSpaceViews.map { storageSpaceView -> storageSpaceView.getBounds(this).width }

fun ShopView.arrowSides(): Pair<Double, Double> = previousBuyerButton.sidesIn(this).first to nextBuyerButton.sidesIn(this).second

fun ShopView.storageSpaceCount(): Int = storageSpaceViews.size

fun ShopView.storageSpaceBitmaps(): List<Bitmap?> = storageSpaceViews.map { storageSpaceView -> (storageSpaceView.findViewByName(StorageSpaceView.ITEM) as? Image)?.bitmap?.base }

fun ShopView.equippedStorageSpaceIndexes(): List<Int> = storageSpaceViews.indices.filter { index -> storageSpaceViews[index].findViewByName(StorageSpaceView.EQUIPPED_FRAME) != null }

fun ShopView.dimmedStorageSpaceIndexes(): List<Int> = storageSpaceViews.indices.filter { index -> storageSpaceViews[index].isDimmed }

fun ShopView.itemAlphas(): List<Double?> = storageSpaceViews.map { storageSpaceView -> (storageSpaceView.findViewByName(StorageSpaceView.ITEM) as? Image)?.alpha }

fun ShopView.selectedStorageSpaceIndexes(): List<Int> = storageSpaceViews.indices.filter { index -> storageSpaceViews[index].findViewByName(StorageSpaceView.SELECTION_FRAME) != null }

fun ShopView.buyerText(): String = buyerText.text

fun ShopView.arrowsEnabled(): Pair<Boolean, Boolean> = previousBuyerButton.enabled to nextBuyerButton.enabled

fun ShopView.arrowsFaded(): Pair<Boolean, Boolean> = previousBuyerButton.isFaded() to nextBuyerButton.isFaded()

fun ShopView.footerText(): String = itemNameText.text

fun ShopView.footerPrice(): String = itemPriceText.text

fun ShopView.footerBottom(): Double = itemNameText.y + itemNameText.height

fun ShopView.isLaidOut(): Boolean {
    val header = buyerText.getGlobalBounds()
    val storageSpaces = storageSpaceViews.first().getGlobalBounds()
    val footer = itemNameText.getGlobalBounds()
    return header.bottom <= storageSpaces.top &&
        storageSpaceViews.last().getGlobalBounds().bottom <= footer.top &&
        previousBuyerButton.getGlobalBounds().right <= header.left &&
        nextBuyerButton.getGlobalBounds().left >= header.right
}

suspend fun ViewsForTesting.clickStorageSpace(
    shopView: ShopView,
    index: Int,
) = clickOn(shopView.storageSpaceViews[index])

suspend fun ViewsForTesting.clickPreviousBuyer(shopView: ShopView) = clickOn(shopView.previousBuyerButton)

suspend fun ViewsForTesting.clickNextBuyer(shopView: ShopView) = clickOn(shopView.nextBuyerButton)

suspend fun ViewsForTesting.swipeShop(
    shopView: ShopView,
    dx: Double,
) {
    val start =
        shopView.storageSpaceViews
            .first()
            .getGlobalBounds()
            .center
    mouseMoveTo(start.x, start.y)
    mouseDown()
    val steps = 5
    for (step in 1..steps) mouseMoveTo(start.x + dx * step / steps, start.y)
    mouseUp()
}
