package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.CommunityWorldsView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object CommunityWorldsViewStories : StoryBinding {
    private const val WORLDS = "worlds"

    private val sampleWorlds =
        listOf(
            CommunityWorldsView.Entry(worldId = "ashen-pass", name = "Ashen Pass", author = "maarkeez"),
            CommunityWorldsView.Entry(worldId = "the-drowned-mill", name = "The Drowned Mill", author = "maarkeez"),
            CommunityWorldsView.Entry(worldId = "goat-hill", name = "Goat Hill", author = "a-player"),
            CommunityWorldsView.Entry(worldId = "lava-lake", name = "Lava Lake", author = "a-player"),
            CommunityWorldsView.Entry(worldId = "the-long-road-to-the-northern-fortress", name = "The Long Road to the Northern Fortress", author = "someone"),
            CommunityWorldsView.Entry(worldId = "sandy-shore", name = "Sandy Shore", author = "someone"),
        )

    private val twoWorlds =
        StoryDto(
            id = "screen-community-worlds--two-worlds",
            name = "Two worlds",
            description = "The 2 community worlds by maarkeez, each by its name and author; a tap starts a new game in it.",
            args = emptyMap(),
        )
    private val empty =
        StoryDto(
            id = "screen-community-worlds--empty",
            name = "Empty",
            description = "No community world is shipped yet: it says so, and how to build one.",
            args = mapOf(WORLDS to ValueDto.int(0)),
        )
    private val manyWorlds =
        StoryDto(
            id = "screen-community-worlds--many-worlds",
            name = "Many worlds",
            description = "More community worlds than the panel holds: the list is scrolled by dragging, and a long name wraps onto two lines.",
            args = mapOf(WORLDS to ValueDto.int(sampleWorlds.size)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-community-worlds",
            group = "Screen",
            title = "Community worlds",
            description =
                "Community worlds, from the menu: every world shipped other than the campaign, alphabetically, each by its name with its author. " +
                    "Its Back is in the row of the call to action. worlds: how many are listed.",
            controls = listOf(ControlDto.int(WORLDS, minimum = 0, maximum = sampleWorlds.size)),
            args = mapOf(WORLDS to ValueDto.int(2)),
            stories = listOf(twoWorlds, empty, manyWorlds),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        CommunityWorldsView(context.messages).apply {
            setDelegate(
                object : CommunityWorldsView.Delegate {
                    override fun communityWorldSelected(worldId: String) = context.action("communityWorldSelected", worldId)
                },
            )
            display(sampleWorlds.take(args.int(WORLDS)).sortedBy { entry -> entry.name.lowercase() })
        }
}
