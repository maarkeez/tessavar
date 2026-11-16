package com.mkz.tessavar.shared.adapters.e2e

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.screen.adapters.presentation.battlefieldView
import com.mkz.tessavar.screen.adapters.presentation.callToActionView
import com.mkz.tessavar.screen.adapters.presentation.clickConfirmation
import com.mkz.tessavar.screen.adapters.presentation.clickMenuButton
import com.mkz.tessavar.screen.adapters.presentation.clickMenuOption
import com.mkz.tessavar.screen.adapters.presentation.clickQuit
import com.mkz.tessavar.screen.adapters.presentation.clickStart
import com.mkz.tessavar.screen.adapters.presentation.confirmDialogView
import com.mkz.tessavar.screen.adapters.presentation.hasBattleUnitAt
import com.mkz.tessavar.screen.adapters.presentation.isPadShown
import com.mkz.tessavar.screen.adapters.presentation.isSaveOffered
import com.mkz.tessavar.screen.adapters.presentation.pauseView
import com.mkz.tessavar.screen.adapters.presentation.virtualPadView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderHudView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderMenuView
import com.mkz.tessavar.shared.adapters.presentation.isShown
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.worldBuilder.adapters.storage.NativeStorageWorldBuilderRepository
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import korlibs.korge.service.storage.InmemoryStorage
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class PlaytestEndToEndTest : ViewsForTesting(windowSize = GAME_WINDOW_SIZE) {
    private suspend fun ViewsForTesting.playtestTheCampaign() {
        val storage = InmemoryStorage()
        storage[NativeStorageWorldBuilderRepository.KEY] = ResourcesBundle.root[ResourcesBundle.CAMPAIGN_WORLD_FILE].readString()
        startWorldBuilder(storage)
        waitUntil { stage.battlefieldView()?.hasBattleUnitAt(KNIGHT_START_ROW, KNIGHT_START_COLUMN) == true }
        val menuView = requireNotNull(stage.worldBuilderMenuView())
        clickMenuButton(menuView)
        clickMenuOption(menuView, "Playtest")
        waitUntil { stage.virtualPadView()?.isPadShown() == true && stage.battlefieldView()?.hasBattleUnitAt(KNIGHT_START_ROW, KNIGHT_START_COLUMN) == true }
        clickStart(requireNotNull(stage.virtualPadView()))
        waitUntil { stage.pauseView()?.isShown() == true }
    }

    @Test
    fun `A playtest should play the world being built, the campaign kept, without offering saving`() =
        sharedViewsTest {
            // When
            playtestTheCampaign()
            // Then
            pauseView()!!.isSaveOffered().shouldBeFalse()
            battlefieldView()!!.hasBattleUnitAt(KNIGHT_START_ROW, KNIGHT_START_COLUMN).shouldBeTrue()
        }

    @Test
    fun `A playtest should go back to the world builder, with the world kept, when the player quits`() =
        sharedViewsTest {
            // Given
            playtestTheCampaign()
            clickQuit(requireNotNull(pauseView()))
            waitUntil { confirmDialogView()?.isShown() == true }
            // When
            clickConfirmation(requireNotNull(callToActionView()))
            // Then
            waitUntil { worldBuilderHudView() != null && battlefieldView()?.hasBattleUnitAt(KNIGHT_START_ROW, KNIGHT_START_COLUMN) == true }
            worldBuilderHudView().shouldNotBeNull()
        }
}
