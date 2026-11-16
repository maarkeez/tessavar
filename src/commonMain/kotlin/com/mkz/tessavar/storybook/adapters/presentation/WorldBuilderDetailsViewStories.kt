package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderDetailsView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object WorldBuilderDetailsViewStories : StoryBinding {
    private const val NAME = "name"
    private const val AUTHOR = "author"

    private val default = StoryDto(id = "screen-world-builder-details--default", name = "Default", description = "The name of a world and its author, typed.", args = emptyMap())
    private val newWorld =
        StoryDto(
            id = "screen-world-builder-details--new-world",
            name = "New world",
            description = "A world not named yet: both fields are empty.",
            args = mapOf(NAME to ValueDto.text(""), AUTHOR to ValueDto.text("")),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-details",
            group = "Screen",
            title = "World builder details",
            description = "Opened from Details in the world builder's menu, over its HUD: the name of the world and its author, typed, and Done to keep them.",
            controls = listOf(ControlDto.text(NAME), ControlDto.text(AUTHOR)),
            args = mapOf(NAME to ValueDto.text("The Sunken Keep"), AUTHOR to ValueDto.text("maarkeez")),
            stories = listOf(default, newWorld),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        WorldBuilderDetailsView(context.messages).apply {
            setDelegate(
                object : WorldBuilderDetailsView.Delegate {
                    override fun fieldTapped(field: WorldBuilderDetailsView.Field) = context.action("fieldTapped", field.name)

                    override fun detailsChanged(
                        name: String,
                        author: String,
                    ) = context.action("detailsChanged", name, author)
                },
            )
            open(name = args.text(NAME), author = args.text(AUTHOR))
        }
}
