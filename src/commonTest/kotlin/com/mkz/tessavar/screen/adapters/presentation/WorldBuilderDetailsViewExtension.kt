package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import korlibs.korge.tests.ViewsForTesting

fun WorldBuilderDetailsView.nameText(): String = nameInput.text

fun WorldBuilderDetailsView.authorText(): String = authorInput.text

fun WorldBuilderDetailsView.typeName(name: String) {
    nameInput.text = name
}

fun WorldBuilderDetailsView.typeAuthor(author: String) {
    authorInput.text = author
}

suspend fun ViewsForTesting.clickDone(detailsView: WorldBuilderDetailsView) = clickOn(detailsView.doneButton)

suspend fun ViewsForTesting.clickName(detailsView: WorldBuilderDetailsView) = clickOn(detailsView.nameInput)
