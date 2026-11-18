package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.GameButton
import com.mkz.tessavar.shared.adapters.presentation.gameButton
import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmaps
import korlibs.image.bitmap.slice
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.input.onClick
import korlibs.korge.style.styles
import korlibs.korge.style.textSize
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.UIContainer
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiSpacing
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.SolidRect
import korlibs.korge.view.View
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.xy
import korlibs.math.geom.Point
import korlibs.math.geom.Size

open class WorldBuilderHudView(
    private val messages: Messages,
) : UIContainer(Size(width = 390, height = 300)) {
    companion object {
        const val TAB_BUTTON_WIDTH = 90.0
        const val TAB_BUTTON_HEIGHT = 30.0
        const val TOOL_BUTTON_WIDTH = 64.0
        const val HISTORY_GAP = 20.0
        const val TOOL_BUTTON_HEIGHT = 36.0
        const val STOCK_BUTTON_WIDTH = 90.0
        const val ICON_SCALE = 2.0
        const val DISABLED_ICON_ALPHA = 0.35
        const val TOOL_ICON = "TOOL_ICON"

        fun toolIcons(resourcesLoader: GameSceneResourcesLoader): Map<ToolIcon, Bitmap> =
            ToolIcon.entries
                .mapNotNull { icon -> resourcesLoader.bitmapOrNull("ui/tool-${icon.name.lowercase()}.png")?.let { bitmap -> icon to bitmap } }
                .toMap()

        val SELECTED_TOOL_COLOR = RGBA(0x00, 0x6A, 0xCC)
        val SELECTED_TOOL_OVER_COLOR = RGBA(0x00, 0x84, 0xFF)

        const val TERRAIN_BUTTON_SIZE = 64.0
        const val OCCUPANT_BUTTON_SIZE = 34.0
        const val PALETTE_GAP = 10.0
        const val UNIT_GAP = 4.0
        const val PLAYER_TITLE_HEIGHT = 18.0
        const val TERRAIN_SCALE = 2.0
        const val SELECTION_BORDER = 3.0
        const val TERRAIN_IMAGE = "TERRAIN_IMAGE"
        const val OCCUPANT_IMAGE = "OCCUPANT_IMAGE"
    }

    enum class Tool { PAINT, FILL, ERASE }

    enum class Tab { TERRAIN, BUILDINGS, UNITS, NPCS }

    enum class ToolIcon { PAINT, PLACE, FILL, ERASE, UNDO, REDO }

    data class TerrainOption(
        val terrainId: String,
        val bitmap: Bitmap,
    )

    data class PaletteOption(
        val id: String,
        val bitmap: Bitmap,
    )

    data class UnitRow(
        val playerId: String,
        val title: String,
        val units: List<PaletteOption>,
    )

    interface Delegate {
        fun tabSelected(tab: Tab)

        fun toolSelected(tool: Tool)

        fun undoSelected()

        fun redoSelected()

        fun terrainSelected(terrainId: String)

        fun buildingSelected(buildingId: String)

        fun unitSelected(
            playerId: String,
            unitId: String,
        )

        fun npcSelected(npcId: String)

        fun stockSelected()
    }

    private var delegate: Delegate? = null

    internal val tabButtons = mutableMapOf<Tab, GameButton>()
    internal var displayedTab: Tab = Tab.TERRAIN
        private set
    internal val toolButtons = mutableMapOf<Tool, GameButton>()
    internal var selectedTool: Tool? = null
        private set
    internal lateinit var undoButton: GameButton
    internal lateinit var redoButton: GameButton
    internal val palettes = mutableMapOf<Tab, Container>()
    internal val paletteButtons = Tab.entries.associateWith { mutableMapOf<String, UIButton>() }
    internal val selectionBorders = mutableMapOf<Tab, SolidRect>()
    internal lateinit var stockButton: GameButton
    private var toolIcons: Map<ToolIcon, Bitmap> = emptyMap()
    internal val displayedIcons = mutableMapOf<GameButton, ToolIcon>()

    internal val terrainPalette: Container get() = palettes.getValue(Tab.TERRAIN)
    internal val terrainButtons: Map<String, UIButton> get() = paletteButtons.getValue(Tab.TERRAIN)
    internal val selectionBorder: SolidRect? get() = selectionBorders[Tab.TERRAIN]

    init {
        uiVerticalStack(padding = 10.0) {
            uiHorizontalStack(padding = 10.0) {
                Tab.entries.forEach { tab ->
                    tabButtons[tab] =
                        gameButton(label = tabLabel(tab), size = Size(TAB_BUTTON_WIDTH, TAB_BUTTON_HEIGHT)) {
                            onClick { delegate?.tabSelected(tab) }
                        }
                }
            }
            uiHorizontalStack(padding = 10.0) {
                Tool.entries.forEach { tool -> toolButtons[tool] = toolButton(tool) }
                uiSpacing(Size(HISTORY_GAP, 0))
                undoButton =
                    iconButton(ToolIcon.UNDO) {
                        enabled = false
                        onClick { if (enabled) delegate?.undoSelected() }
                    }
                redoButton =
                    iconButton(ToolIcon.REDO) {
                        enabled = false
                        onClick { if (enabled) delegate?.redoSelected() }
                    }
            }
            container {
                Tab.entries.forEach { tab -> palettes[tab] = container { visible = tab == Tab.TERRAIN } }
            }
        }
        showTab(Tab.TERRAIN)
    }

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun displayTab(tab: Tab) = showTab(tab)

    private fun showTab(tab: Tab) {
        displayedTab = tab
        tabButtons.forEach { (buttonTab, button) -> button.highlight(buttonTab == tab) }
        palettes.forEach { (paletteTab, palette) -> palette.visible = paletteTab == tab }
        toolButtons.getValue(Tool.PAINT).showIcon(if (tab == Tab.TERRAIN) ToolIcon.PAINT else ToolIcon.PLACE)
        toolButtons.getValue(Tool.FILL).enabled = tab == Tab.TERRAIN
        dimDisabledIcons()
    }

    open fun displayToolIcons(icons: Map<ToolIcon, Bitmap>) {
        toolIcons = icons
        displayedIcons.forEach { (button, icon) -> button.showIcon(icon) }
    }

    open fun displayHistory(
        canUndo: Boolean,
        canRedo: Boolean,
    ) {
        undoButton.enabled = canUndo
        redoButton.enabled = canRedo
        dimDisabledIcons()
    }

    open fun selectTool(tool: Tool) {
        selectedTool = tool
        toolButtons.forEach { (buttonTool, button) -> button.highlight(buttonTool == tool) }
    }

    open fun displayTerrains(
        terrains: List<TerrainOption>,
        selectedTerrainId: String,
    ) {
        val palette = resetPalette(Tab.TERRAIN)
        palette.uiHorizontalStack(padding = PALETTE_GAP) {
            terrains.forEach { terrain ->
                paletteButton(Tab.TERRAIN, terrain.terrainId, terrain.bitmap, TERRAIN_BUTTON_SIZE, TERRAIN_IMAGE) { delegate?.terrainSelected(terrain.terrainId) }
            }
        }
        selectTerrain(selectedTerrainId)
    }

    open fun selectTerrain(terrainId: String) = select(Tab.TERRAIN, terrainId)

    open fun displayBuildings(buildings: List<PaletteOption>) {
        val palette = resetPalette(Tab.BUILDINGS)
        palette.uiHorizontalStack(padding = PALETTE_GAP) {
            buildings.forEach { building ->
                paletteButton(Tab.BUILDINGS, building.id, building.bitmap, TERRAIN_BUTTON_SIZE, OCCUPANT_IMAGE) { delegate?.buildingSelected(building.id) }
            }
        }
    }

    open fun selectBuilding(buildingId: String) = select(Tab.BUILDINGS, buildingId)

    open fun displayUnits(rows: List<UnitRow>) {
        val palette = resetPalette(Tab.UNITS)
        palette.uiVerticalStack(padding = UNIT_GAP) {
            rows.forEach { row ->
                uiText(text = row.title, size = Size(width = 390, height = PLAYER_TITLE_HEIGHT)) { styles.textSize = 15.0 }
                uiHorizontalStack(padding = UNIT_GAP) {
                    row.units.forEach { unit ->
                        val key = unitKey(row.playerId, unit.id)
                        paletteButton(Tab.UNITS, key, unit.bitmap, OCCUPANT_BUTTON_SIZE, OCCUPANT_IMAGE) { delegate?.unitSelected(row.playerId, unit.id) }
                    }
                }
                uiSpacing(Size(0, UNIT_GAP))
            }
        }
    }

    open fun selectUnit(
        playerId: String,
        unitId: String,
    ) = select(Tab.UNITS, unitKey(playerId, unitId))

    open fun displayNpcs(npcs: List<PaletteOption>) {
        val palette = resetPalette(Tab.NPCS)
        palette.uiVerticalStack(padding = PALETTE_GAP) {
            uiHorizontalStack(padding = UNIT_GAP * 2) {
                npcs.forEach { npc ->
                    paletteButton(Tab.NPCS, npc.id, npc.bitmap, OCCUPANT_BUTTON_SIZE + UNIT_GAP, OCCUPANT_IMAGE) { delegate?.npcSelected(npc.id) }
                }
            }
            stockButton =
                gameButton(label = messages[UiMessage.WORLD_BUILDER_HUD_STOCK], size = Size(STOCK_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT)) {
                    enabled = false
                    onClick { if (enabled) delegate?.stockSelected() }
                }
        }
    }

    open fun selectNpc(npcId: String) = select(Tab.NPCS, npcId)

    open fun displayStock(isEnabled: Boolean) {
        if (::stockButton.isInitialized) stockButton.enabled = isEnabled
    }

    private fun tabLabel(tab: Tab): String =
        when (tab) {
            Tab.TERRAIN -> messages[UiMessage.WORLD_BUILDER_HUD_TERRAIN]
            Tab.BUILDINGS -> messages[UiMessage.WORLD_BUILDER_HUD_BUILDINGS]
            Tab.UNITS -> messages[UiMessage.WORLD_BUILDER_HUD_UNITS]
            Tab.NPCS -> messages[UiMessage.WORLD_BUILDER_HUD_NPCS]
        }

    private fun unitKey(
        playerId: String,
        unitId: String,
    ): String = "$playerId/$unitId"

    private fun resetPalette(tab: Tab): Container {
        val palette = palettes.getValue(tab)
        palette.removeChildren()
        paletteButtons.getValue(tab).clear()
        selectionBorders.remove(tab)
        return palette
    }

    private fun Container.paletteButton(
        tab: Tab,
        key: String,
        bitmap: Bitmap,
        size: Double,
        imageName: String,
        onSelected: () -> Unit,
    ) {
        paletteButtons.getValue(tab)[key] =
            uiButton(label = "", size = Size(size, size)) {
                bgColorOut = Colors.TRANSPARENT
                bgColorOver = Colors.TRANSPARENT
                background.borderColor = Colors.TRANSPARENT
                image(bitmap) {
                    name = imageName
                    scale = if (imageName == TERRAIN_IMAGE) TERRAIN_SCALE else size / maxOf(bitmap.width, bitmap.height)
                    smoothing = false
                }
                onClick { onSelected() }
            }
    }

    private fun select(
        tab: Tab,
        key: String,
    ) {
        selectionBorders.remove(tab)?.removeFromParent()
        val palette = palettes.getValue(tab)
        val button = paletteButtons.getValue(tab)[key] ?: return
        val buttonPosition = button.positionIn(palette)
        val borderWidth = button.width + 2 * SELECTION_BORDER
        val borderHeight = button.height + 2 * SELECTION_BORDER
        val border =
            palette.solidRect(Size(borderWidth, borderHeight), color = Colors.WHITE) {
                x = buttonPosition.x - SELECTION_BORDER
                y = buttonPosition.y - SELECTION_BORDER
            }
        palette.sendChildToBack(border)
        selectionBorders[tab] = border
    }

    private fun View.positionIn(ancestor: Container): Point {
        var position = pos
        var parentView = parent
        while (parentView != null && parentView != ancestor) {
            position += parentView.pos
            parentView = parentView.parent
        }
        return position
    }

    private fun GameButton.highlight(isSelected: Boolean) {
        bgColorOut = if (isSelected) SELECTED_TOOL_COLOR else GameButton.GREY
        bgColorOver = if (isSelected) SELECTED_TOOL_OVER_COLOR else GameButton.GREY_OVER
    }

    private fun Container.toolButton(tool: Tool): GameButton =
        iconButton(
            when (tool) {
                Tool.PAINT -> ToolIcon.PAINT
                Tool.FILL -> ToolIcon.FILL
                Tool.ERASE -> ToolIcon.ERASE
            },
        ) {
            onClick { if (enabled) delegate?.toolSelected(tool) }
        }

    private fun Container.iconButton(
        icon: ToolIcon,
        block: GameButton.() -> Unit,
    ): GameButton =
        gameButton(label = "", size = Size(TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT)) {
            image(Bitmaps.transparent) {
                name = TOOL_ICON
                scale = ICON_SCALE
                smoothing = false
            }
            showIcon(icon)
            block()
        }

    private fun GameButton.showIcon(icon: ToolIcon) {
        displayedIcons[this] = icon
        val image = findViewByName(TOOL_ICON) as? Image ?: return
        val bitmap = toolIcons[icon]?.slice() ?: Bitmaps.transparent
        image.bitmap = bitmap
        image.xy((width - bitmap.width * ICON_SCALE) / 2, (height - bitmap.height * ICON_SCALE) / 2)
    }

    private fun dimDisabledIcons() {
        displayedIcons.keys.forEach { button -> (button.findViewByName(TOOL_ICON) as? Image)?.alpha = if (button.enabled) 1.0 else DISABLED_ICON_ALPHA }
    }
}
