package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.BattlefieldView
import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderHudView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object WorldBuilderHudViewStories : StoryBinding {
    private const val DEFAULT_TERRAIN = "grass"

    private const val SELECTED_TERRAIN = "selectedTerrain"
    private const val TAB = "tab"
    private const val CAN_RESTOCK = "canRestock"
    private val PLAYER_IDS = listOf("player-one", "player-two")
    private const val TOOL = "tool"
    private const val CAN_UNDO = "canUndo"
    private const val CAN_REDO = "canRedo"

    private val default = StoryDto(id = "screen-world-builder-hud--default", name = "Default", description = "Paint and the grass selected, with edits to undo and to redo.", args = emptyMap())
    private val otherTerrain =
        StoryDto(
            id = "screen-world-builder-hud--other-terrain",
            name = "Other terrain",
            description = "Another terrain selected, framed in white.",
            args = mapOf(SELECTED_TERRAIN to ValueDto.text("water")),
        )

    private val fillTool =
        StoryDto(
            id = "screen-world-builder-hud--fill-tool",
            name = "Fill tool",
            description = "Fill selected, in blue: tapping a tile paints every tile of its terrain connected to it.",
            args = mapOf(TOOL to ValueDto.text(WorldBuilderHudView.Tool.FILL.name)),
        )

    private val nothingToUndo =
        StoryDto(
            id = "screen-world-builder-hud--nothing-to-undo",
            name = "Nothing to undo",
            description = "A new world: Undo and Redo are disabled, their icons dimmed, until the world is edited.",
            args = mapOf(CAN_UNDO to ValueDto.boolean(false), CAN_REDO to ValueDto.boolean(false)),
        )

    private val buildingsTab =
        StoryDto(
            id = "screen-world-builder-hud--buildings-tab",
            name = "Buildings tab",
            description = "The buildings to place, the first selected: Paint shows the Place icon, a pin, and Fill is disabled and dimmed.",
            args = mapOf(TAB to ValueDto.text(WorldBuilderHudView.Tab.BUILDINGS.name)),
        )

    private val unitsTab =
        StoryDto(
            id = "screen-world-builder-hud--units-tab",
            name = "Units tab",
            description = "Every unit in a row for each player, titled with the player's name: a tap places a battle unit of that player.",
            args = mapOf(TAB to ValueDto.text(WorldBuilderHudView.Tab.UNITS.name)),
        )

    private val npcsTab =
        StoryDto(
            id = "screen-world-builder-hud--npcs-tab",
            name = "Npcs tab",
            description = "The npcs to place, with Stock enabled as the npc selected stands in the world.",
            args = mapOf(TAB to ValueDto.text(WorldBuilderHudView.Tab.NPCS.name), CAN_RESTOCK to ValueDto.boolean(true)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-hud",
            group = "Screen",
            title = "World builder HUD",
            description =
                "Under the world in the world builder: the palette's tabs, the tools a tapped tile is painted, placed on or erased with, Undo and Redo, " +
                    "and the terrains, buildings, units or npcs of the tab. canUndo and canRedo: whether there is an edit to undo or to redo. " +
                    "canRestock: whether the npc selected stands in the world, so its stock can be chosen.",
            controls =
                listOf(
                    ControlDto.enum(TAB, WorldBuilderHudView.Tab.entries.map { tab -> tab.name }),
                    ControlDto.enum(TOOL, WorldBuilderHudView.Tool.entries.map { tool -> tool.name }),
                    ControlDto.pick(SELECTED_TERRAIN, context.terrainIds()),
                    ControlDto.boolean(CAN_UNDO),
                    ControlDto.boolean(CAN_REDO),
                    ControlDto.boolean(CAN_RESTOCK),
                ),
            args =
                mapOf(
                    TAB to ValueDto.text(WorldBuilderHudView.Tab.TERRAIN.name),
                    CAN_RESTOCK to ValueDto.boolean(false),
                    TOOL to ValueDto.text(WorldBuilderHudView.Tool.PAINT.name),
                    SELECTED_TERRAIN to ValueDto.text(DEFAULT_TERRAIN),
                    CAN_UNDO to ValueDto.boolean(true),
                    CAN_REDO to ValueDto.boolean(true),
                ),
            stories = listOf(default, otherTerrain, fillTool, nothingToUndo, buildingsTab, unitsTab, npcsTab),
        )

    private const val FORTRESS = "fortress"

    private fun paletteOptions(
        context: StoryContext,
        ids: List<String>,
        path: (String) -> String,
    ): List<WorldBuilderHudView.PaletteOption> = ids.mapNotNull { id -> context.resourcesLoader.bitmapOrNull(path(id))?.let { bitmap -> WorldBuilderHudView.PaletteOption(id = id, bitmap = bitmap) } }

    override fun render(
        args: Args,
        context: StoryContext,
    ): View {
        val battlefieldView = BattlefieldView(context.resourcesLoader)
        val terrains =
            context.terrainIds().map { terrainId ->
                WorldBuilderHudView.TerrainOption(terrainId = terrainId, bitmap = battlefieldView.terrainTileBitmap(terrainId, terrainTransition = null))
            }
        return WorldBuilderHudView(context.messages).apply {
            displayToolIcons(WorldBuilderHudView.toolIcons(context.resourcesLoader))
            setDelegate(
                object : WorldBuilderHudView.Delegate {
                    override fun toolSelected(tool: WorldBuilderHudView.Tool) = context.action("toolSelected", tool.name, setArg = TOOL to ValueDto.text(tool.name))

                    override fun undoSelected() = context.action("undoSelected")

                    override fun redoSelected() = context.action("redoSelected")

                    override fun terrainSelected(terrainId: String) = context.action("terrainSelected", terrainId, setArg = SELECTED_TERRAIN to ValueDto.text(terrainId))

                    override fun tabSelected(tab: WorldBuilderHudView.Tab) = context.action("tabSelected", tab.name, setArg = TAB to ValueDto.text(tab.name))

                    override fun buildingSelected(buildingId: String) = context.action("buildingSelected", buildingId)

                    override fun unitSelected(
                        playerId: String,
                        unitId: String,
                    ) = context.action("unitSelected", "$playerId $unitId")

                    override fun npcSelected(npcId: String) = context.action("npcSelected", npcId)

                    override fun stockSelected() = context.action("stockSelected")
                },
            )
            displayTab(WorldBuilderHudView.Tab.valueOf(args.text(TAB)))
            selectTool(WorldBuilderHudView.Tool.valueOf(args.text(TOOL)))
            displayHistory(canUndo = args.boolean(CAN_UNDO), canRedo = args.boolean(CAN_REDO))
            displayTerrains(terrains, selectedTerrainId = args.text(SELECTED_TERRAIN))
            val buildings = paletteOptions(context, listOf(FORTRESS)) { buildingId -> "building/$buildingId.png" }
            displayBuildings(buildings)
            buildings.firstOrNull()?.let { building -> selectBuilding(building.id) }
            val units = paletteOptions(context, context.unitIds()) { unitId -> "unit/$unitId.png" }
            displayUnits(PLAYER_IDS.map { playerId -> WorldBuilderHudView.UnitRow(playerId = playerId, title = context.messages.playerName(playerId), units = units) })
            units.firstOrNull()?.let { unit -> selectUnit(PLAYER_IDS.first(), unit.id) }
            val npcs = paletteOptions(context, context.npcIds()) { npcId -> "npc/$npcId.png" }
            displayNpcs(npcs)
            npcs.firstOrNull()?.let { npc -> selectNpc(npc.id) }
            displayStock(isEnabled = args.boolean(CAN_RESTOCK))
        }
    }
}
