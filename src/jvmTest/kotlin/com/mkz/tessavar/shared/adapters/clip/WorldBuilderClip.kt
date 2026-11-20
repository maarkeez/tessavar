package com.mkz.tessavar.shared.adapters.clip

import com.mkz.tessavar.screen.adapters.presentation.VirtualPadView.Direction
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderHudView.Tab
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderHudView.Tool
import com.mkz.tessavar.screen.adapters.presentation.battlefieldView
import com.mkz.tessavar.screen.adapters.presentation.clickArrow
import com.mkz.tessavar.screen.adapters.presentation.clickMenuButton
import com.mkz.tessavar.screen.adapters.presentation.clickMenuOption
import com.mkz.tessavar.screen.adapters.presentation.clickPaletteNpc
import com.mkz.tessavar.screen.adapters.presentation.clickPaletteTerrain
import com.mkz.tessavar.screen.adapters.presentation.clickPaletteUnit
import com.mkz.tessavar.screen.adapters.presentation.clickTab
import com.mkz.tessavar.screen.adapters.presentation.clickTile
import com.mkz.tessavar.screen.adapters.presentation.clickTool
import com.mkz.tessavar.screen.adapters.presentation.dragBattlefield
import com.mkz.tessavar.screen.adapters.presentation.hasBattleUnitAt
import com.mkz.tessavar.screen.adapters.presentation.isPadShown
import com.mkz.tessavar.screen.adapters.presentation.virtualPadView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderHudView
import com.mkz.tessavar.screen.adapters.presentation.worldBuilderMenuView
import com.mkz.tessavar.shared.adapters.e2e.startWorldBuilder
import com.mkz.tessavar.shared.adapters.e2e.waitFor
import com.mkz.tessavar.shared.adapters.e2e.waitUntil
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import com.mkz.tessavar.worldBuilder.adapters.storage.NativeStorageWorldBuilderRepository
import korlibs.image.color.Colors
import korlibs.korge.service.storage.InmemoryStorage
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorldBuilderClip : OpenGlViewsForTesting() {
    private val rows = 6
    private val columns = 12

    private val emptyWorld =
        """
        |version = 1
        |name = "The Island"
        |author = "maarkeez"
        |
        |[battlefield]
        |tiles = [
        |${List(rows) { "    [" + List(columns) { "\"sand\"" }.joinToString(", ") + "]," }.joinToString("\n")}
        |]
        |
        |[[quests]]
        |id = "explore-the-island"
        |type = "MAIN"
        |startTiles = [[2, 2], [2, 3]]
        |name = "Explore the island"
        |description = "Walk the island from shore to shore."
        |
        |[[quests.tasks]]
        |type = "VISIT"
        |tiles = [[4, 1]]
        |text = "Reach the south shore"
        |
        |[quests.reward]
        |gold = 10
        """.trimMargin()

    private val sandTiles = (1..4).flatMap { row -> (1..4).map { column -> row to column } }
    private val grassTiles = listOf(2 to 2, 2 to 3, 3 to 2, 3 to 3)
    private val isletTiles = listOf(2 to 9, 2 to 10, 3 to 9, 3 to 10)
    private val walk = listOf(Direction.NORTH to (2 to 2), Direction.EAST to (2 to 3), Direction.EAST to (2 to 4))
    private val scrollToTheEast = listOf(-96.0 to 0.0, -192.0 to 0.0, -288.0 to 0.0, -384.0 to 0.0)
    private val scrollToTheWest = listOf(96.0 to 0.0, 192.0 to 0.0, 288.0 to 0.0, 384.0 to 0.0)

    private val hold = 400.milliseconds
    private val paintHold = 50.milliseconds

    @Test
    fun `should record building an island and playtesting it`() =
        sharedViewsTest {
            val storage = InmemoryStorage()
            storage[NativeStorageWorldBuilderRepository.KEY] = emptyWorld
            views.clearColor = Colors["#2b2b2b"]
            startWorldBuilder(storage)
            views.recordClip("world-builder", frameTime) {
                waitFor(hold)
                val hudView = worldBuilderHudView()!!
                val battlefieldView = battlefieldView()!!
                clickTool(hudView, Tool.FILL)
                clickPaletteTerrain(hudView, "water")
                clickTile(battlefieldView, row = 0, column = 0)
                waitFor(hold)
                clickTool(hudView, Tool.PAINT)
                clickPaletteTerrain(hudView, "sand")
                sandTiles.forEach { (row, column) ->
                    clickTile(battlefieldView, row, column)
                    waitFor(paintHold)
                }
                clickPaletteTerrain(hudView, "grass")
                grassTiles.forEach { (row, column) ->
                    clickTile(battlefieldView, row, column)
                    waitFor(paintHold)
                }
                waitFor(hold)
                clickTab(hudView, Tab.UNITS)
                clickPaletteUnit(hudView, "player-one", "hero")
                clickTile(battlefieldView, row = 3, column = 2)
                waitFor(hold)
                clickPaletteUnit(hudView, "player-one", "viking-warrior")
                clickTile(battlefieldView, row = 3, column = 3)
                waitFor(hold)
                clickTab(hudView, Tab.NPCS)
                clickPaletteNpc(hudView, "blacksmith")
                clickTile(battlefieldView, row = 1, column = 3)
                waitFor(hold)
                dragBattlefield(battlefieldView, *scrollToTheEast.toTypedArray())
                clickTab(hudView, Tab.TERRAIN)
                clickPaletteTerrain(hudView, "sand")
                isletTiles.forEach { (row, column) ->
                    clickTile(battlefieldView, row, column)
                    waitFor(paintHold)
                }
                clickTab(hudView, Tab.UNITS)
                clickPaletteUnit(hudView, "player-two", "rat")
                clickTile(battlefieldView, row = 2, column = 10)
                waitFor(hold)
                dragBattlefield(battlefieldView, *scrollToTheWest.toTypedArray())
                waitFor(hold)
                val menuView = worldBuilderMenuView()!!
                clickMenuButton(menuView)
                waitFor(hold)
                views.saveStill("world-builder-menu")
                clickMenuOption(menuView, "Playtest")
                waitUntil { virtualPadView()?.isPadShown() == true && battlefieldView()?.hasBattleUnitAt(3, 2) == true }
                waitFor(hold)
                walk.forEach { (direction, tile) ->
                    clickArrow(virtualPadView()!!, direction)
                    waitUntil { battlefieldView()!!.hasBattleUnitAt(tile.first, tile.second) }
                    waitFor(hold)
                }
                waitFor(1.seconds)
            }
        }
}
