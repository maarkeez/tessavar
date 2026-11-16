package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderCallToActionView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import korlibs.korge.view.View

object WorldBuilderCallToActionViewStories : StoryBinding {
    private val leaveConfirmation =
        StoryDto(
            id = "screen-world-builder-call-to-action--leave-confirmation",
            name = "Leave confirmation",
            description = "Stay on the left and Leave on the right: leaving loses the world. Stay takes it away.",
            args = emptyMap(),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-call-to-action",
            group = "Screen",
            title = "World builder call to action",
            description = "Under the world builder controls, after Back in the menu: confirming leaving, as the world is lost.",
            controls = emptyList(),
            args = emptyMap(),
            stories = listOf(leaveConfirmation),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        WorldBuilderCallToActionView(context.messages).apply {
            displayLeaveConfirmation(onLeft = { context.action("onLeft") }, onStayed = { context.action("onStayed") })
        }
}
