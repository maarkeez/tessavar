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
    private const val OPEN = "open"

    private val default =
        StoryDto(id = "screen-world-builder-hud--default", name = "Default", description = "The pill closed: the grass as the brush, then Paint selected, Fill and Erase.", args = emptyMap())
    private val otherTerrain =
        StoryDto(
            id = "screen-world-builder-hud--other-terrain",
            name = "Other terrain",
            description = "Another terrain selected, shown as the brush.",
            args = mapOf(SELECTED_TERRAIN to ValueDto.text("water")),
        )

    private val fillTool =
        StoryDto(
            id = "screen-world-builder-hud--fill-tool",
            name = "Fill tool",
            description = "Fill selected, in blue: tapping a tile paints every tile of its terrain connected to it.",
            args = mapOf(TOOL to ValueDto.text(WorldBuilderHudView.Tool.FILL.name)),
        )

    private val buildingsTab =
        StoryDto(
            id = "screen-world-builder-hud--buildings-tab",
            name = "Buildings tab",
            description = "The card open on the buildings to place, the first framed: Paint shows the Place icon, a pin, and Fill is disabled and dimmed.",
            args = mapOf(TAB to ValueDto.text(WorldBuilderHudView.Tab.BUILDINGS.name), OPEN to ValueDto.boolean(true)),
        )

    private val unitsTab =
        StoryDto(
            id = "screen-world-builder-hud--units-tab",
            name = "Units tab",
            description = "The card open on a row of units for each player, titled with the player's name: a tap picks a battle unit of that player.",
            args = mapOf(TAB to ValueDto.text(WorldBuilderHudView.Tab.UNITS.name), OPEN to ValueDto.boolean(true)),
        )

    private val npcsTab =
        StoryDto(
            id = "screen-world-builder-hud--npcs-tab",
            name = "Npcs tab",
            description = "The card open on the npcs to place, and Stock in the pill instead of Fill, enabled as the npc selected stands in the world.",
            args = mapOf(TAB to ValueDto.text(WorldBuilderHudView.Tab.NPCS.name), CAN_RESTOCK to ValueDto.boolean(true), OPEN to ValueDto.boolean(true)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-hud",
            group = "Screen",
            title = "World builder HUD",
            description =
                "Floating over the world in the world builder: a pill with the brush, the option a tapped tile is painted or placed with, " +
                    "and the tools to paint, fill or erase with. Tapping the brush opens a card above it with the palette's tabs " +
                    "and the terrains, buildings, units or npcs of the tab in two rows. " +
                    "open: whether the card is open. canRestock: whether the npc selected stands in the world, so its stock can be chosen.",
            controls =
                listOf(
                    ControlDto.enum(TAB, WorldBuilderHudView.Tab.entries.map { tab -> tab.name }),
                    ControlDto.enum(TOOL, WorldBuilderHudView.Tool.entries.map { tool -> tool.name }),
                    ControlDto.pick(SELECTED_TERRAIN, context.terrainIds()),
                    ControlDto.boolean(CAN_RESTOCK),
                    ControlDto.boolean(OPEN),
                ),
            args =
                mapOf(
                    TAB to ValueDto.text(WorldBuilderHudView.Tab.TERRAIN.name),
                    CAN_RESTOCK to ValueDto.boolean(false),
                    TOOL to ValueDto.text(WorldBuilderHudView.Tool.PAINT.name),
                    SELECTED_TERRAIN to ValueDto.text(DEFAULT_TERRAIN),
                    OPEN to ValueDto.boolean(false),
                ),
            stories = listOf(default, otherTerrain, fillTool, buildingsTab, unitsTab, npcsTab),
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
            if (args.boolean(OPEN)) openCard()
        }
    }
}
