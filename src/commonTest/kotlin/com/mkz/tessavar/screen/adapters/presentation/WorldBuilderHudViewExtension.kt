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

fun WorldBuilderHudView.toolLabels(): List<String> = toolButtons.values.map { button -> button.text }

fun WorldBuilderHudView.highlightedTools(): List<WorldBuilderHudView.Tool> = toolButtons.filterValues { button -> button.bgColorOut == WorldBuilderHudView.SELECTED_TOOL_COLOR }.keys.toList()

suspend fun ViewsForTesting.clickTool(
    hudView: WorldBuilderHudView,
    tool: WorldBuilderHudView.Tool,
) = clickOn(hudView.toolButtons.getValue(tool))

suspend fun ViewsForTesting.clickUndo(hudView: WorldBuilderHudView) = clickOn(hudView.undoButton)

suspend fun ViewsForTesting.clickRedo(hudView: WorldBuilderHudView) = clickOn(hudView.redoButton)
