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
    private const val TOOL = "tool"

    private val default = StoryDto(id = "screen-world-builder-hud--default", name = "Default", description = "A new world, with Paint and the grass selected.", args = emptyMap())
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

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-hud",
            group = "Screen",
            title = "World builder HUD",
            description = "Under the world in the world builder: the tools a tapped tile is painted with, Paint or Fill, and the terrains it is painted with.",
            controls =
                listOf(
                    ControlDto.enum(TOOL, WorldBuilderHudView.Tool.entries.map { tool -> tool.name }),
                    ControlDto.pick(SELECTED_TERRAIN, context.terrainIds()),
                ),
            args = mapOf(TOOL to ValueDto.text(WorldBuilderHudView.Tool.PAINT.name), SELECTED_TERRAIN to ValueDto.text(DEFAULT_TERRAIN)),
            stories = listOf(default, otherTerrain, fillTool),
        )

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
            setDelegate(
                object : WorldBuilderHudView.Delegate {
                    override fun toolSelected(tool: WorldBuilderHudView.Tool) = context.action("toolSelected", tool.name, setArg = TOOL to ValueDto.text(tool.name))

                    override fun terrainSelected(terrainId: String) = context.action("terrainSelected", terrainId, setArg = SELECTED_TERRAIN to ValueDto.text(terrainId))
                },
            )
            selectTool(WorldBuilderHudView.Tool.valueOf(args.text(TOOL)))
            displayTerrains(terrains, selectedTerrainId = args.text(SELECTED_TERRAIN))
        }
    }
}
