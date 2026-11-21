package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.GAME_WINDOW_SIZE
import com.mkz.tessavar.screen.adapters.presentation.HudStyle.panel
import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmaps
import korlibs.image.bitmap.slice
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.input.onClick
import korlibs.korge.ui.UIButton
import korlibs.korge.ui.UIContainer
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.RoundRect
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.View
import korlibs.korge.view.addTo
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.korge.view.xy
import korlibs.math.geom.Point
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size
import korlibs.math.geom.collider.HitTestDirection
import kotlin.math.floor
import kotlin.math.max

open class WorldBuilderHudView(
    private val messages: Messages,
) : UIContainer(GAME_WINDOW_SIZE) {
    companion object {
        const val PILL_WIDTH = 270.0
        const val PILL_HEIGHT = 76.0
        const val PILL_BOTTOM = 22.0
        const val PILL_PADDING = 10.0
        const val PILL_BUTTON_SIZE = 56.0
        const val PILL_GAP = 6.0
        const val DIVIDER_WIDTH = 1.0
        const val DIVIDER_HEIGHT = 44.0

        const val BOTTOM_CLEARANCE = PILL_BOTTOM + PILL_HEIGHT + PILL_GAP

        const val CARD_WIDTH = 366.0
        const val CARD_HEIGHT = 270.0
        const val CARD_GAP = 8.0
        const val CARD_PADDING = 12.0
        const val CARD_RADIUS = 16.0
        const val TAB_HEIGHT = 56.0
        const val TAB_GAP = 6.0
        const val TAB_ICON_SIZE = 32.0
        const val TAB_ICON_CENTER = 20.0
        const val TAB_LABEL_SIZE = 12.0
        const val TAB_LABEL_BOTTOM = 18.0
        const val UNDERLINE_HEIGHT = 3.0
        const val UNDERLINE_INSET = 10.0

        const val SLOT_SIZE = 68.0
        const val SLOT_GAP = 6.0
        const val SLOT_SPRITE_INSET = 4.0
        const val SELECTION_BORDER = 3.0
        const val ROW_SLOTS = 4
        const val PLAYER_TITLE_HEIGHT = 18.0
        const val PLAYER_TITLE_SIZE = 12.0
        const val STOCK_LABEL_SIZE = 13.0
        const val DISABLED_ALPHA = 0.5

        const val ICON_SCALE = 2.0
        const val DISABLED_ICON_ALPHA = 0.35
        const val TOOL_ICON = "TOOL_ICON"
        const val TERRAIN_IMAGE = "TERRAIN_IMAGE"
        const val OCCUPANT_IMAGE = "OCCUPANT_IMAGE"
        const val BRUSH_IMAGE = "BRUSH_IMAGE"
        const val TAB_ICON = "TAB_ICON"
        const val TAB_UNDERLINE = "TAB_UNDERLINE"

        val PILL_X = (GAME_WINDOW_SIZE.width - PILL_WIDTH) / 2
        val PILL_Y = GAME_WINDOW_SIZE.height - PILL_BOTTOM - PILL_HEIGHT
        val CARD_X = (GAME_WINDOW_SIZE.width - CARD_WIDTH) / 2
        val CARD_Y = PILL_Y - CARD_GAP - CARD_HEIGHT
        const val ROW_WIDTH = CARD_WIDTH - 2 * CARD_PADDING
        const val PALETTE_TOP = CARD_PADDING + TAB_HEIGHT + CARD_PADDING

        fun toolIcons(resourcesLoader: GameSceneResourcesLoader): Map<ToolIcon, Bitmap> =
            ToolIcon.entries
                .mapNotNull { icon -> resourcesLoader.bitmapOrNull("ui/tool-${icon.name.lowercase()}.png")?.let { bitmap -> icon to bitmap } }
                .toMap()

        val SELECTED_TOOL_COLOR = RGBA(0x00, 0x6A, 0xCC)
        val SELECTED_TOOL_OVER_COLOR = RGBA(0x00, 0x84, 0xFF)

        internal fun Container.fittedImage(
            bitmap: Bitmap,
            boxSize: Double,
            inner: Double,
            imageName: String,
        ): Image {
            val longest = max(bitmap.width, bitmap.height).toDouble()
            val wholeScale = floor(inner / longest)
            val imageScale = if (wholeScale >= 1.0) wholeScale else inner / longest
            return image(bitmap) {
                name = imageName
                smoothing = false
                scale = imageScale
                xy(floor((boxSize - bitmap.width * imageScale) / 2), floor((boxSize - bitmap.height * imageScale) / 2))
            }
        }
    }

    enum class Tool { PAINT, FILL, ERASE }

    enum class Tab { TERRAIN, BUILDINGS, UNITS, NPCS }

    enum class ToolIcon { PAINT, PLACE, FILL, ERASE }

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

        fun terrainSelected(terrainId: String)

        fun buildingSelected(buildingId: String)

        fun unitSelected(
            playerId: String,
            unitId: String,
        )

        fun npcSelected(npcId: String)

        fun stockSelected()
    }

    class SlotButton(
        size: Size,
    ) : UIButton(size) {
        private var face: RoundRect = panel(size, fill = HudStyle.PANEL_RAISED, radius = HudStyle.SLOT_RADIUS)

        var isHighlighted: Boolean = false
            set(value) {
                field = value
                repaint()
            }

        init {
            bgColorOut = Colors.TRANSPARENT
            bgColorOver = Colors.TRANSPARENT
            bgColorDisabled = Colors.TRANSPARENT
            background.borderColor = Colors.TRANSPARENT
            background.bgColor = Colors.TRANSPARENT
            background.shadowColor = Colors.TRANSPARENT
        }

        private fun repaint() {
            val index = face.index
            face.removeFromParent()
            val color = if (isHighlighted) SELECTED_TOOL_COLOR else HudStyle.PANEL_RAISED
            face = RoundRect(size, RectCorners(HudStyle.SLOT_RADIUS), fill = color, stroke = if (isHighlighted) color else HudStyle.STROKE, strokeThickness = 1.0)
            addChildAt(face, index)
        }
    }

    private var delegate: Delegate? = null
    private val fingerTap = FingerTap(this)

    internal var displayedTab: Tab = Tab.TERRAIN
        private set
    internal var selectedTool: Tool? = null
        private set
    internal var isCardOpen: Boolean = false
        private set

    private var toolIcons: Map<ToolIcon, Bitmap> = emptyMap()
    internal val displayedIcons = mutableMapOf<UIButton, ToolIcon>()
    private val options = Tab.entries.associateWith { linkedMapOf<String, Bitmap>() }
    private val selectedKeys = mutableMapOf<Tab, String>()

    internal val outsideTapCatcher: SolidRect
    internal val card: Container
    internal val tabButtons = mutableMapOf<Tab, UIButton>()
    internal val palettes = mutableMapOf<Tab, Container>()
    internal val paletteButtons = Tab.entries.associateWith { mutableMapOf<String, SlotButton>() }
    internal val paletteRows = Tab.entries.associateWith { mutableListOf<PaletteRowView>() }
    internal val selectionBorders = mutableMapOf<Tab, View>()
    internal val brushButton: SlotButton
    internal val toolButtons = mutableMapOf<Tool, SlotButton>()
    internal val stockButton: SlotButton
    private val stockLabel: Text

    internal val terrainButtons: Map<String, SlotButton> get() = paletteButtons.getValue(Tab.TERRAIN)
    internal val selectionBorder: View? get() = selectionBorders[Tab.TERRAIN]

    init {
        outsideTapCatcher =
            solidRect(GAME_WINDOW_SIZE, Colors.TRANSPARENT) {
                name = FingerTap.PASS_THROUGH
                visible = false
                onClick { if (!fingerTap.isHoldingBack) closeCard() }
            }
        card =
            container {
                xy(CARD_X, CARD_Y)
                visible = false
                panel(Size(CARD_WIDTH, CARD_HEIGHT), radius = CARD_RADIUS)
                val tabWidth = (ROW_WIDTH - (Tab.entries.size - 1) * TAB_GAP) / Tab.entries.size
                Tab.entries.forEachIndexed { index, tab ->
                    tabButtons[tab] = tabButton(tab, tabWidth).xy(CARD_PADDING + index * (tabWidth + TAB_GAP), CARD_PADDING)
                }
                Tab.entries.forEach { tab -> palettes[tab] = container { xy(CARD_PADDING, PALETTE_TOP) } }
            }
        lateinit var brush: SlotButton
        lateinit var stock: SlotButton
        lateinit var stockText: Text
        container {
            xy(PILL_X, PILL_Y)
            panel(Size(PILL_WIDTH, PILL_HEIGHT), radius = PILL_HEIGHT / 2)
            brush =
                SlotButton(Size(PILL_BUTTON_SIZE, PILL_BUTTON_SIZE)).addTo(this).apply {
                    xy(PILL_PADDING, PILL_PADDING)
                    onClick { if (isCardOpen) closeCard() else openCard() }
                }
            val dividerX = PILL_PADDING + PILL_BUTTON_SIZE + PILL_PADDING / 2 + 2
            solidRect(Size(DIVIDER_WIDTH, DIVIDER_HEIGHT), HudStyle.STROKE).xy(dividerX, (PILL_HEIGHT - DIVIDER_HEIGHT) / 2)
            val toolsX = PILL_PADDING + PILL_BUTTON_SIZE + PILL_PADDING + 4
            Tool.entries.forEachIndexed { index, tool ->
                toolButtons[tool] = toolButton(tool).xy(toolsX + index * (PILL_BUTTON_SIZE + PILL_GAP), PILL_PADDING)
            }
            stock =
                SlotButton(Size(PILL_BUTTON_SIZE, PILL_BUTTON_SIZE)).addTo(this).apply {
                    xy(toolsX + Tool.FILL.ordinal * (PILL_BUTTON_SIZE + PILL_GAP), PILL_PADDING)
                    visible = false
                    enabled = false
                    stockText =
                        text(messages[UiMessage.WORLD_BUILDER_HUD_STOCK], textSize = STOCK_LABEL_SIZE, color = Colors.WHITE).also { label ->
                            label.xy(floor((PILL_BUTTON_SIZE - label.width) / 2), floor((PILL_BUTTON_SIZE - label.height) / 2))
                        }
                    onClick { if (enabled) delegate?.stockSelected() }
                }
        }
        brushButton = brush
        stockButton = stock
        stockLabel = stockText
        showTab(Tab.TERRAIN)
        displayStock(isEnabled = false)
    }

    override fun hitTestInternal(
        p: Point,
        direction: HitTestDirection,
    ): View? = null

    open fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    open fun displayTab(tab: Tab) = showTab(tab)

    private fun showTab(tab: Tab) {
        displayedTab = tab
        tabButtons.forEach { (buttonTab, button) -> button.findViewByName(TAB_UNDERLINE)?.visible = buttonTab == tab }
        palettes.forEach { (paletteTab, palette) -> palette.visible = paletteTab == tab }
        toolButtons.getValue(Tool.PAINT).showIcon(if (tab == Tab.TERRAIN) ToolIcon.PAINT else ToolIcon.PLACE)
        toolButtons.getValue(Tool.FILL).enabled = tab == Tab.TERRAIN
        toolButtons.getValue(Tool.FILL).visible = tab != Tab.NPCS
        stockButton.visible = tab == Tab.NPCS
        dimDisabledIcons()
        refreshBrush()
    }

    open fun displayToolIcons(icons: Map<ToolIcon, Bitmap>) {
        toolIcons = icons
        displayedIcons.forEach { (button, icon) -> button.showIcon(icon) }
        dimDisabledIcons()
    }

    open fun selectTool(tool: Tool) {
        selectedTool = tool
        toolButtons.forEach { (buttonTool, button) -> button.isHighlighted = buttonTool == tool }
    }

    open fun openCard() {
        isCardOpen = true
        card.visible = true
        outsideTapCatcher.visible = true
        brushButton.isHighlighted = true
    }

    open fun closeCard() {
        isCardOpen = false
        card.visible = false
        outsideTapCatcher.visible = false
        brushButton.isHighlighted = false
    }

    open fun displayTerrains(
        terrains: List<TerrainOption>,
        selectedTerrainId: String,
    ) {
        displayPalette(Tab.TERRAIN, terrains.map { terrain -> terrain.terrainId to terrain.bitmap }, TERRAIN_IMAGE) { terrainId -> delegate?.terrainSelected(terrainId) }
        selectTerrain(selectedTerrainId)
    }

    open fun selectTerrain(terrainId: String) = select(Tab.TERRAIN, terrainId)

    open fun displayBuildings(buildings: List<PaletteOption>) =
        displayPalette(Tab.BUILDINGS, buildings.map { building -> building.id to building.bitmap }, OCCUPANT_IMAGE) { buildingId -> delegate?.buildingSelected(buildingId) }

    open fun selectBuilding(buildingId: String) = select(Tab.BUILDINGS, buildingId)

    open fun displayUnits(rows: List<UnitRow>) {
        val palette = resetPalette(Tab.UNITS)
        rows.forEachIndexed { index, row ->
            val top = index * (PLAYER_TITLE_HEIGHT + SLOT_SIZE + SLOT_GAP)
            palette.text(row.title, textSize = PLAYER_TITLE_SIZE, color = HudStyle.MUTED).xy(0.0, top)
            val slots = row.units.map { unit -> unitKey(row.playerId, unit.id) to unit.bitmap }
            slots.forEach { (key, bitmap) -> options.getValue(Tab.UNITS)[key] = bitmap }
            palette.paletteRow(Tab.UNITS, slots, OCCUPANT_IMAGE, top + PLAYER_TITLE_HEIGHT) { key ->
                delegate?.unitSelected(key.substringBefore(KEY_SEPARATOR), key.substringAfter(KEY_SEPARATOR))
            }
        }
        refreshTabIcon(Tab.UNITS)
    }

    open fun selectUnit(
        playerId: String,
        unitId: String,
    ) = select(Tab.UNITS, unitKey(playerId, unitId))

    open fun displayNpcs(npcs: List<PaletteOption>) = displayPalette(Tab.NPCS, npcs.map { npc -> npc.id to npc.bitmap }, OCCUPANT_IMAGE) { npcId -> delegate?.npcSelected(npcId) }

    open fun selectNpc(npcId: String) = select(Tab.NPCS, npcId)

    open fun displayStock(isEnabled: Boolean) {
        stockButton.enabled = isEnabled
        stockButton.alpha = if (isEnabled) 1.0 else DISABLED_ALPHA
        stockLabel.color = if (isEnabled) Colors.WHITE else HudStyle.MUTED
    }

    private fun displayPalette(
        tab: Tab,
        slots: List<Pair<String, Bitmap>>,
        imageName: String,
        onSelected: (String) -> Unit,
    ) {
        val palette = resetPalette(tab)
        slots.forEach { (key, bitmap) -> options.getValue(tab)[key] = bitmap }
        palette.paletteRow(tab, slots.take(ROW_SLOTS), imageName, top = 0.0, onSelected)
        palette.paletteRow(tab, slots.drop(ROW_SLOTS), imageName, top = SLOT_SIZE + SLOT_GAP, onSelected)
        refreshTabIcon(tab)
    }

    private fun Container.paletteRow(
        tab: Tab,
        slots: List<Pair<String, Bitmap>>,
        imageName: String,
        top: Double,
        onSelected: (String) -> Unit,
    ) {
        val row = PaletteRowView(ROW_WIDTH, SLOT_SIZE, SLOT_GAP).addTo(this).xy(-PaletteRowView.BORDER, top - PaletteRowView.BORDER)
        row.display(slots.map { (key, bitmap) -> PaletteRowView.Slot(key, bitmap, imageName) }) { key ->
            onSelected(key)
            closeCard()
        }
        paletteRows.getValue(tab) += row
        paletteButtons.getValue(tab).putAll(row.slotButtons)
    }

    private fun resetPalette(tab: Tab): Container {
        val palette = palettes.getValue(tab)
        palette.removeChildren()
        paletteButtons.getValue(tab).clear()
        paletteRows.getValue(tab).clear()
        options.getValue(tab).clear()
        selectionBorders.remove(tab)
        return palette
    }

    private fun select(
        tab: Tab,
        key: String,
    ) {
        selectionBorders.remove(tab)?.removeFromParent()
        val button = paletteButtons.getValue(tab)[key] ?: return
        selectedKeys[tab] = key
        val border =
            RoundRect(
                Size(button.width + 2 * SELECTION_BORDER, button.height + 2 * SELECTION_BORDER),
                RectCorners(HudStyle.SLOT_RADIUS + SELECTION_BORDER),
                fill = HudStyle.SELECTION,
            ).xy(button.x - SELECTION_BORDER, button.y - SELECTION_BORDER)
        val row = button.parent!!
        row.addChildAt(border, 0)
        selectionBorders[tab] = border
        paletteRows.getValue(tab).firstOrNull { paletteRow -> key in paletteRow.slotButtons }?.scrollTo(key)
        refreshTabIcon(tab)
        if (tab == displayedTab) refreshBrush()
    }

    private fun selectedBitmap(tab: Tab): Bitmap? {
        val tabOptions = options.getValue(tab)
        return selectedKeys[tab]?.let(tabOptions::get) ?: tabOptions.values.firstOrNull()
    }

    private fun refreshBrush() {
        brushButton.findViewByName(BRUSH_IMAGE)?.removeFromParent()
        val bitmap = selectedBitmap(displayedTab) ?: return
        brushButton.fittedImage(bitmap, PILL_BUTTON_SIZE, PILL_BUTTON_SIZE - SLOT_SPRITE_INSET, BRUSH_IMAGE)
    }

    private fun refreshTabIcon(tab: Tab) {
        val button = tabButtons[tab] ?: return
        button.findViewByName(TAB_ICON)?.removeFromParent()
        val bitmap = selectedBitmap(tab) ?: return
        val icon = Container().addTo(button)
        icon.name = TAB_ICON
        icon.fittedImage(bitmap, TAB_ICON_SIZE, TAB_ICON_SIZE, TAB_ICON)
        icon.xy(floor((button.width - TAB_ICON_SIZE) / 2), TAB_ICON_CENTER - TAB_ICON_SIZE / 2)
        if (tab == displayedTab) refreshBrush()
    }

    private fun Container.tabButton(
        tab: Tab,
        width: Double,
    ): UIButton =
        SlotButton(Size(width, TAB_HEIGHT)).addTo(this).apply {
            text(tabLabel(tab), textSize = TAB_LABEL_SIZE, color = Colors.WHITE).also { label ->
                label.xy(floor((width - label.width) / 2), TAB_HEIGHT - TAB_LABEL_BOTTOM)
            }
            solidRect(Size(width - 2 * UNDERLINE_INSET, UNDERLINE_HEIGHT), HudStyle.SELECTION) {
                name = TAB_UNDERLINE
                xy(UNDERLINE_INSET, TAB_HEIGHT - UNDERLINE_HEIGHT)
                visible = false
            }
            onClick { delegate?.tabSelected(tab) }
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
    ): String = "$playerId$KEY_SEPARATOR$unitId"

    private fun Container.toolButton(tool: Tool): SlotButton =
        SlotButton(Size(PILL_BUTTON_SIZE, PILL_BUTTON_SIZE)).addTo(this).apply {
            image(Bitmaps.transparent) {
                name = TOOL_ICON
                scale = ICON_SCALE
                smoothing = false
            }
            showIcon(
                when (tool) {
                    Tool.PAINT -> ToolIcon.PAINT
                    Tool.FILL -> ToolIcon.FILL
                    Tool.ERASE -> ToolIcon.ERASE
                },
            )
            onClick { if (enabled) delegate?.toolSelected(tool) }
        }

    private fun UIButton.showIcon(icon: ToolIcon) {
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

private const val KEY_SEPARATOR = "/"
