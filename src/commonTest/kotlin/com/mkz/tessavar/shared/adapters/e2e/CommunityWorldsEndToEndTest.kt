package com.mkz.tessavar.shared.adapters.e2e

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.screen.adapters.presentation.VirtualPadView.Direction
import com.mkz.tessavar.screen.adapters.presentation.battlefieldView
import com.mkz.tessavar.screen.adapters.presentation.clickArrow
import com.mkz.tessavar.screen.adapters.presentation.clickCommunityWorlds
import com.mkz.tessavar.screen.adapters.presentation.clickWorld
import com.mkz.tessavar.screen.adapters.presentation.communityWorldsView
import com.mkz.tessavar.screen.adapters.presentation.displayedName
import com.mkz.tessavar.screen.adapters.presentation.displayedTask
import com.mkz.tessavar.screen.adapters.presentation.hasBattleUnitAt
import com.mkz.tessavar.screen.adapters.presentation.isAnnouncementShown
import com.mkz.tessavar.screen.adapters.presentation.isPadShown
import com.mkz.tessavar.screen.adapters.presentation.questAnnouncementView
import com.mkz.tessavar.screen.adapters.presentation.virtualPadView
import com.mkz.tessavar.screen.adapters.presentation.worldLabels
import com.mkz.tessavar.shared.adapters.presentation.isShown
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import com.mkz.tessavar.shared.domain.FixedDoublesRandom
import com.mkz.tessavar.startApp
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.service.storage.InmemoryStorage
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class CommunityWorldsEndToEndTest : ViewsForTesting(windowSize = GAME_WINDOW_SIZE) {
    private companion object {
        const val HERO_ROW = 1
        const val HERO_COLUMN = 2
        const val KNIGHT_COLUMN = 3
    }

    @Test
    fun `Community worlds should list the worlds by maarkeez, and start a new game in the one tapped`() =
        sharedViewsTest {
            // Given
            stage.startApp(random = FixedDoublesRandom.cpuBattleUnitsAlwaysResting(), storage = InmemoryStorage())
            clickCommunityWorlds(openMenu())
            waitUntil { stage.communityWorldsView()?.isShown() == true }
            val communityWorldsView = requireNotNull(stage.communityWorldsView())
            communityWorldsView.worldLabels().shouldContainExactly("Ashen Pass, by maarkeez", "The Drowned Mill, by maarkeez")
            // When
            clickWorld(communityWorldsView, "the-drowned-mill")
            // Then
            waitUntil {
                stage.virtualPadView()?.isPadShown() == true &&
                    stage.battlefieldView()?.hasBattleUnitAt(HERO_ROW, HERO_COLUMN) == true &&
                    stage.battlefieldView()?.hasBattleUnitAt(HERO_ROW, KNIGHT_COLUMN) == true
            }
        }

    @Test
    fun `A community world should start its main quest when the leader walks onto one of its start tiles`() =
        sharedViewsTest {
            // Given
            stage.startApp(random = FixedDoublesRandom.cpuBattleUnitsAlwaysResting(), storage = InmemoryStorage())
            clickCommunityWorlds(openMenu())
            waitUntil { stage.communityWorldsView()?.isShown() == true }
            clickWorld(requireNotNull(stage.communityWorldsView()), "the-drowned-mill")
            waitUntil { stage.virtualPadView()?.isPadShown() == true && stage.battlefieldView()?.hasBattleUnitAt(HERO_ROW, HERO_COLUMN) == true }
            // When
            clickArrow(requireNotNull(stage.virtualPadView()), Direction.SOUTH)
            val announcementView = requireNotNull(stage.questAnnouncementView())
            waitUntil { announcementView.isAnnouncementShown() }
            // Then
            announcementView.displayedName() shouldBe "The Drowned Mill"
            announcementView.displayedTask() shouldBe "Talk to the Princess"
        }
}
