package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import korlibs.image.bitmap.Bitmap
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Image

fun WorldBuilderHudView.paletteTerrainIds(): List<String> = terrainButtons.keys.toList()

fun WorldBuilderHudView.paletteBitmap(terrainId: String): Bitmap? = (terrainButtons[terrainId]?.findViewByName(WorldBuilderHudView.TERRAIN_IMAGE) as? Image)?.bitmap?.base

fun WorldBuilderHudView.highlightedTerrainId(): String? {
    val border = selectionBorder ?: return null
    return terrainButtons.entries
        .firstOrNull { (_, button) ->
            val buttonPosition = button.parent!!.pos + button.pos
            buttonPosition.x == border.x + WorldBuilderHudView.SELECTION_BORDER && buttonPosition.y == border.y + WorldBuilderHudView.SELECTION_BORDER
        }?.key
}

suspend fun ViewsForTesting.clickPaletteTerrain(
    hudView: WorldBuilderHudView,
    terrainId: String,
) = clickOn(hudView.terrainButtons.getValue(terrainId))

fun WorldBuilderHudView.toolIcons(): List<WorldBuilderHudView.ToolIcon> = toolButtons.values.map { button -> displayedIcons.getValue(button) }

fun WorldBuilderHudView.toolIconImage(tool: WorldBuilderHudView.Tool): Image = toolButtons.getValue(tool).findViewByName(WorldBuilderHudView.TOOL_ICON) as Image

fun WorldBuilderHudView.highlightedTools(): List<WorldBuilderHudView.Tool> = toolButtons.filterValues { button -> button.bgColorOut == WorldBuilderHudView.SELECTED_TOOL_COLOR }.keys.toList()

suspend fun ViewsForTesting.clickTool(
    hudView: WorldBuilderHudView,
    tool: WorldBuilderHudView.Tool,
) = clickOn(hudView.toolButtons.getValue(tool))

fun WorldBuilderHudView.paletteKeys(tab: WorldBuilderHudView.Tab): List<String> = paletteButtons.getValue(tab).keys.toList()

fun WorldBuilderHudView.highlightedTab(): WorldBuilderHudView.Tab? = tabButtons.entries.firstOrNull { (_, button) -> button.bgColorOut == WorldBuilderHudView.SELECTED_TOOL_COLOR }?.key

suspend fun ViewsForTesting.clickTab(
    hudView: WorldBuilderHudView,
    tab: WorldBuilderHudView.Tab,
) = clickOn(hudView.tabButtons.getValue(tab))

suspend fun ViewsForTesting.clickPaletteUnit(
    hudView: WorldBuilderHudView,
    playerId: String,
    unitId: String,
) = clickOn(hudView.paletteButtons.getValue(WorldBuilderHudView.Tab.UNITS).getValue("$playerId/$unitId"))

suspend fun ViewsForTesting.clickStock(hudView: WorldBuilderHudView) = clickOn(hudView.stockButton)

fun Image.isDimmed(): Boolean = alpha < 1.0
