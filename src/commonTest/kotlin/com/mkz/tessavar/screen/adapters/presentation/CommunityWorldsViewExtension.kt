package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.descendants
import com.mkz.tessavar.shared.adapters.presentation.isShown
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Stage

fun Stage.communityWorldsView(): CommunityWorldsView? = descendants().filterIsInstance<CommunityWorldsView>().firstOrNull()

fun CommunityWorldsView.title(): String = titleText.text

fun CommunityWorldsView.worldLabels(): List<String> =
    list.children
        .filterIsInstance<GameButton>()
        .sortedBy { button -> button.y }
        .map { button -> button.text }

fun CommunityWorldsView.isEmptyMessageShown(): Boolean = emptyText.isShown()

fun CommunityWorldsView.emptyMessage(): String = emptyText.plainText

fun CommunityWorldsView.listTop(): Double = list.y

fun CommunityWorldsView.areWorldsInsideThePanel(): Boolean = worldButtons.values.all { button -> button.x >= 0.0 && button.x + button.width <= width }

suspend fun ViewsForTesting.clickWorld(
    communityWorldsView: CommunityWorldsView,
    worldId: String,
) = clickOn(requireNotNull(communityWorldsView.worldButtons[worldId]) { "No world $worldId is listed" })
