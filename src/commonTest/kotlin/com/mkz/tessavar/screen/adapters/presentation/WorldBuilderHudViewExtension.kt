package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.clickOn
import com.mkz.tessavar.shared.adapters.presentation.descendants
import com.mkz.tessavar.shared.adapters.presentation.isShown
import korlibs.image.bitmap.Bitmap
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.Text

fun WorldBuilderHudView.paletteTerrainIds(): List<String> = terrainButtons.keys.toList()

fun WorldBuilderHudView.paletteBitmap(terrainId: String): Bitmap? = (terrainButtons[terrainId]?.findViewByName(WorldBuilderHudView.TERRAIN_IMAGE) as? Image)?.bitmap?.base

fun WorldBuilderHudView.highlightedTerrainId(): String? = highlightedKey(WorldBuilderHudView.Tab.TERRAIN)

fun WorldBuilderHudView.highlightedKey(tab: WorldBuilderHudView.Tab): String? {
    val border = selectionBorders[tab] ?: return null
    return paletteButtons
        .getValue(tab)
        .entries
        .firstOrNull { (_, button) ->
            button.parent == border.parent &&
                button.x == border.x + WorldBuilderHudView.SELECTION_BORDER &&
                button.y == border.y + WorldBuilderHudView.SELECTION_BORDER
        }?.key
}

fun WorldBuilderHudView.brushBitmap(): Bitmap? = (brushButton.findViewByName(WorldBuilderHudView.BRUSH_IMAGE) as? Image)?.bitmap?.base

fun WorldBuilderHudView.tabIconBitmap(tab: WorldBuilderHudView.Tab): Bitmap? =
    (tabButtons.getValue(tab).findViewByName(WorldBuilderHudView.TAB_ICON)?.let { icon -> (icon as? Container)?.firstChild } as? Image)?.bitmap?.base

suspend fun ViewsForTesting.openPaletteCard(hudView: WorldBuilderHudView) {
    if (!hudView.isCardOpen) clickOn(hudView.brushButton)
}

suspend fun ViewsForTesting.clickBrush(hudView: WorldBuilderHudView) = clickOn(hudView.brushButton)

suspend fun ViewsForTesting.clickPaletteTerrain(
    hudView: WorldBuilderHudView,
    terrainId: String,
) {
    openPaletteCard(hudView)
    clickOn(hudView.terrainButtons.getValue(terrainId))
}

fun WorldBuilderHudView.toolIcons(): List<WorldBuilderHudView.ToolIcon> = toolButtons.values.map { button -> displayedIcons.getValue(button) }

fun WorldBuilderHudView.toolIconImage(tool: WorldBuilderHudView.Tool): Image = toolButtons.getValue(tool).findViewByName(WorldBuilderHudView.TOOL_ICON) as Image

fun WorldBuilderHudView.highlightedTools(): List<WorldBuilderHudView.Tool> = toolButtons.filterValues { button -> button.isHighlighted }.keys.toList()

fun WorldBuilderHudView.pillTools(): List<String> =
    buildList {
        toolButtons.forEach { (tool, button) ->
            if (tool == WorldBuilderHudView.Tool.FILL && stockButton.isShown()) add("STOCK")
            if (button.isShown()) add(tool.name)
        }
    }

suspend fun ViewsForTesting.clickTool(
    hudView: WorldBuilderHudView,
    tool: WorldBuilderHudView.Tool,
) = clickOn(hudView.toolButtons.getValue(tool))

fun WorldBuilderHudView.playerTitles(): List<String> =
    palettes
        .getValue(WorldBuilderHudView.Tab.UNITS)
        .descendants()
        .filterIsInstance<Text>()
        .map(Text::text)

fun WorldBuilderHudView.paletteKeys(tab: WorldBuilderHudView.Tab): List<String> = paletteButtons.getValue(tab).keys.toList()

fun WorldBuilderHudView.highlightedTab(): WorldBuilderHudView.Tab? = tabButtons.entries.firstOrNull { (_, button) -> button.findViewByName(WorldBuilderHudView.TAB_UNDERLINE)?.visible == true }?.key

suspend fun ViewsForTesting.clickTab(
    hudView: WorldBuilderHudView,
    tab: WorldBuilderHudView.Tab,
) {
    openPaletteCard(hudView)
    clickOn(hudView.tabButtons.getValue(tab))
}

suspend fun ViewsForTesting.clickPaletteUnit(
    hudView: WorldBuilderHudView,
    playerId: String,
    unitId: String,
) {
    openPaletteCard(hudView)
    clickOn(hudView.paletteButtons.getValue(WorldBuilderHudView.Tab.UNITS).getValue("$playerId/$unitId"))
}

suspend fun ViewsForTesting.clickStock(hudView: WorldBuilderHudView) = clickOn(hudView.stockButton)

fun Image.isDimmed(): Boolean = alpha < 1.0

suspend fun ViewsForTesting.clickPaletteNpc(
    hudView: WorldBuilderHudView,
    npcId: String,
) {
    openPaletteCard(hudView)
    clickOn(hudView.paletteButtons.getValue(WorldBuilderHudView.Tab.NPCS).getValue(npcId))
}
