package com.mkz.tessavar.screen.adapters.presentation

import korlibs.image.bitmap.Bitmap
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.style.styles
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.math.geom.Size

open class WorldBuilderHudView(
    messages: Messages,
) : UIContainer(Size(width = 390, height = 300)) {
    companion object {
        const val LABEL_WIDTH = 80.0

        const val TERRAIN_BUTTON_SIZE = 48.0
        const val TERRAIN_SCALE = 3.0
        const val SELECTION_BORDER = 3.0
        const val TERRAIN_IMAGE = "TERRAIN_IMAGE"
    }

    data class TerrainOption(
        val terrainId: String,
        val bitmap: Bitmap,
    )

    interface Delegate {
        fun terrainSelected(terrainId: String)
    }

    private var delegate: Delegate? = null

    internal lateinit var terrainPalette: Container
    internal val terrainButtons = mutableMapOf<String, UIButton>()
    internal var selectionBorder: SolidRect? = null

    init {
        uiVerticalStack(padding = 10.0) {
            uiText(text = messages[UiMessage.WORLD_BUILDER_HUD_TERRAIN], size = Size(width = LABEL_WIDTH, height = 20)) { styles.textSize = 15.0 }
            terrainPalette = container()
        }
    }

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun displayTerrains(
        terrains: List<TerrainOption>,
        selectedTerrainId: String,
    ) {
        terrainPalette.removeChildren()
        terrainButtons.clear()
        terrainPalette.uiHorizontalStack(padding = 10.0) {
            terrains.forEach { terrain ->
                terrainButtons[terrain.terrainId] =
                    uiButton(label = "", size = Size(TERRAIN_BUTTON_SIZE, TERRAIN_BUTTON_SIZE)) {
                        bgColorOut = Colors.TRANSPARENT
                        bgColorOver = Colors.TRANSPARENT
                        background.borderColor = Colors.TRANSPARENT
                        image(terrain.bitmap) {
                            name = TERRAIN_IMAGE
                            scale = TERRAIN_SCALE
                            smoothing = false
                        }
                        onClick { delegate?.terrainSelected(terrain.terrainId) }
                    }
            }
        }
        selectTerrain(selectedTerrainId)
    }

    open fun selectTerrain(terrainId: String) {
        selectionBorder?.removeFromParent()
        selectionBorder = null
        val terrainButton = terrainButtons[terrainId] ?: return
        val buttonPosition = terrainButton.parent!!.pos + terrainButton.pos
        val borderSize = TERRAIN_BUTTON_SIZE + 2 * SELECTION_BORDER
        selectionBorder =
            terrainPalette.solidRect(Size(borderSize, borderSize), color = Colors.WHITE) {
                x = buttonPosition.x - SELECTION_BORDER
                y = buttonPosition.y - SELECTION_BORDER
            }
        terrainPalette.sendChildToBack(selectionBorder!!)
    }
}
