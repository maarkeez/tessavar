package com.mkz.tessavar.shared.adapters.clip

import com.mkz.tessavar.screen.adapters.presentation.VirtualPadView.Direction
import com.mkz.tessavar.screen.adapters.presentation.battleUnitInfoView
import com.mkz.tessavar.screen.adapters.presentation.battlefieldView
import com.mkz.tessavar.screen.adapters.presentation.callToActionView
import com.mkz.tessavar.screen.adapters.presentation.clickA
import com.mkz.tessavar.screen.adapters.presentation.clickAction
import com.mkz.tessavar.screen.adapters.presentation.clickArrow
import com.mkz.tessavar.screen.adapters.presentation.clickCapsule
import com.mkz.tessavar.screen.adapters.presentation.clickConfirmation
import com.mkz.tessavar.screen.adapters.presentation.clickEffect
import com.mkz.tessavar.screen.adapters.presentation.clickLoadGame
import com.mkz.tessavar.screen.adapters.presentation.clickPin
import com.mkz.tessavar.screen.adapters.presentation.clickQuit
import com.mkz.tessavar.screen.adapters.presentation.clickRow
import com.mkz.tessavar.screen.adapters.presentation.clickSave
import com.mkz.tessavar.screen.adapters.presentation.clickSlot
import com.mkz.tessavar.screen.adapters.presentation.clickStart
import com.mkz.tessavar.screen.adapters.presentation.confirmDialogView
import com.mkz.tessavar.screen.adapters.presentation.displayedEffectIds
import com.mkz.tessavar.screen.adapters.presentation.displayedTask
import com.mkz.tessavar.screen.adapters.presentation.hasBattleUnitAt
import com.mkz.tessavar.screen.adapters.presentation.isAnnouncementShown
import com.mkz.tessavar.screen.adapters.presentation.isDetailShown
import com.mkz.tessavar.screen.adapters.presentation.isDialogShown
import com.mkz.tessavar.screen.adapters.presentation.isEffectInfoDisplayed
import com.mkz.tessavar.screen.adapters.presentation.isPadShown
import com.mkz.tessavar.screen.adapters.presentation.isSavedShown
import com.mkz.tessavar.screen.adapters.presentation.menuView
import com.mkz.tessavar.screen.adapters.presentation.pauseView
import com.mkz.tessavar.screen.adapters.presentation.questAnnouncementView
import com.mkz.tessavar.screen.adapters.presentation.questDetailView
import com.mkz.tessavar.screen.adapters.presentation.questJournalView
import com.mkz.tessavar.screen.adapters.presentation.questTrackerView
import com.mkz.tessavar.screen.adapters.presentation.saveSlotsView
import com.mkz.tessavar.screen.adapters.presentation.talkView
import com.mkz.tessavar.screen.adapters.presentation.virtualPadView
import com.mkz.tessavar.shared.adapters.e2e.KNIGHT_START_COLUMN
import com.mkz.tessavar.shared.adapters.e2e.KNIGHT_START_ROW
import com.mkz.tessavar.shared.adapters.e2e.startNewGame
import com.mkz.tessavar.shared.adapters.e2e.waitFor
import com.mkz.tessavar.shared.adapters.e2e.waitUntil
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.isShown
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import korlibs.korge.service.storage.InmemoryStorage
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StormBatClip : OpenGlViewsForTesting() {
    private val storage = InmemoryStorage()
    private val saveSlotKey = "save.slot1"
    private val questId = "storm-in-a-bottle"

    private val northOfTheAlchemistRow = 20
    private val northOfTheAlchemistColumn = 18
    private val pondColumn = 17
    private val northOfThePondRow = 18
    private val southOfThePondRow = 19
    private val soakHold = 1500.milliseconds
    private val battleTimeout = 30.seconds

    private val hold = 400.milliseconds

    @Test
    fun `should record the Alchemist's quest and the storm bat stunning a soaked hero by the pond`() =
        sharedViewsTest {
            views.clearColor = HudStyle.BACKGROUND
            startNewGame(storage = storage)
            saveAndQuit()
            moveTheHeroNextToTheAlchemistInTheSave()
            loadSlot()
            views.recordClip("storm-bat", frameTime) {
                waitFor(1.seconds)
                clickA(virtualPadView()!!)
                val talkView = talkView()!!
                waitUntil { talkView.isDialogShown() }
                waitFor(3.seconds)
                views.saveStill("storm-bat-talk")
                clickAction(callToActionView()!!, "Leave")
                val announcementView = questAnnouncementView()!!
                waitUntil { announcementView.isAnnouncementShown() }
                waitFor(1500.milliseconds)
                val trackerView = questTrackerView()!!
                clickCapsule(trackerView)
                val journalView = questJournalView()!!
                waitUntil { journalView.isShown() }
                waitFor(hold)
                clickRow(journalView, questId)
                waitUntil { questDetailView()?.isDetailShown() == true }
                waitFor(1.seconds)
                views.saveStill("storm-bat-quest-detail")
                clickAction(callToActionView()!!, "Back")
                waitUntil { journalView.isShown() }
                clickPin(journalView, questId)
                waitUntil { trackerView.displayedTask() == "Defeat 1 Storm Bat" }
                waitFor(hold)
                clickAction(callToActionView()!!, "Back")
                waitUntil { virtualPadView()?.isPadShown() == true }
                waitFor(hold)
                views.saveStill("storm-bat-tracker")
                clickArrow(virtualPadView()!!, Direction.WEST)
                waitUntil { battlefieldView()!!.hasBattleUnitAt(northOfTheAlchemistRow, pondColumn) }
                clickArrow(virtualPadView()!!, Direction.NORTH)
                waitUntil { battlefieldView()!!.hasBattleUnitAt(southOfThePondRow, pondColumn) }
                waitFor(soakHold)
                clickArrow(virtualPadView()!!, Direction.NORTH)
                waitUntil { battlefieldView()!!.hasBattleUnitAt(northOfThePondRow, pondColumn) }
                waitUntil(timeout = battleTimeout) { battleUnitInfoView()?.displayedEffectIds().orEmpty().containsAll(listOf("soak", "stun-long")) }
                val battleUnitInfoView = battleUnitInfoView()!!
                waitFor(1500.milliseconds)
                views.saveStill("storm-bat-stunned")
                clickEffect(battleUnitInfoView, battleUnitInfoView.displayedEffectIds().indexOf("stun-long"))
                waitUntil { battleUnitInfoView.isEffectInfoDisplayed() }
                waitFor(2500.milliseconds)
                views.saveStill("storm-bat-effect-info")
                waitFor(1500.milliseconds)
            }
        }

    private suspend fun ViewsForTesting.saveAndQuit() {
        clickStart(requireNotNull(stage.virtualPadView()))
        waitUntil { stage.pauseView()?.isShown() == true }
        clickSave(requireNotNull(stage.pauseView()))
        waitUntil { stage.saveSlotsView()?.isShown() == true }
        clickSlot(requireNotNull(stage.saveSlotsView()), 1)
        waitUntil { stage.pauseView()?.isSavedShown() == true }
        clickQuit(requireNotNull(stage.pauseView()))
        waitUntil { stage.confirmDialogView()?.isShown() == true }
        clickConfirmation(requireNotNull(stage.callToActionView()))
        waitUntil { stage.menuView()?.isShown() == true }
    }

    private fun moveTheHeroNextToTheAlchemistInTheSave() {
        val heroAtTheStart = """"occupantId":"hero","row":$KNIGHT_START_ROW,"column":$KNIGHT_START_COLUMN"""
        val save = requireNotNull(storage.getOrNull(saveSlotKey)) { "Nothing is saved in the slot" }
        check(heroAtTheStart in save) { "The hero is not at the start in the save" }
        storage[saveSlotKey] = save.replace(heroAtTheStart, """"occupantId":"hero","row":$northOfTheAlchemistRow,"column":$northOfTheAlchemistColumn""")
    }

    private suspend fun ViewsForTesting.loadSlot() {
        clickLoadGame(requireNotNull(stage.menuView()))
        waitUntil { stage.saveSlotsView()?.isShown() == true }
        clickSlot(requireNotNull(stage.saveSlotsView()), 1)
        waitUntil { stage.virtualPadView()?.isPadShown() == true && stage.battlefieldView()?.hasBattleUnitAt(northOfTheAlchemistRow, northOfTheAlchemistColumn) == true }
    }
}
