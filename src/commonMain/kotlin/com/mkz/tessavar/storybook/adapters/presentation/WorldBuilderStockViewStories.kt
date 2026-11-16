package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.WorldBuilderStockView
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ComponentDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ControlDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.StoryDto
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto
import korlibs.korge.view.View

object WorldBuilderStockViewStories : StoryBinding {
    private const val NPC = "npc"
    private const val SELLS_ANYTHING = "sellsAnything"
    private val ALCHEMIST_STOCK = listOf("health-potion", "mana-potion", "health-elixir", "mana-elixir", "teleport-essence")

    private val alchemist =
        StoryDto(
            id = "screen-world-builder-stock--alchemist",
            name = "Alchemist",
            description = "What the Alchemist sells in the campaign, in blue: a tap on an item adds it to the stock or takes it away.",
            args = emptyMap(),
        )

    private val nothingSold =
        StoryDto(
            id = "screen-world-builder-stock--nothing-sold",
            name = "Nothing sold",
            description = "An npc who sells nothing, so it is no merchant.",
            args = mapOf(NPC to ValueDto.text("elder"), SELLS_ANYTHING to ValueDto.boolean(false)),
        )

    override fun component(context: StoryContext) =
        ComponentDto(
            id = "screen-world-builder-stock",
            group = "Screen",
            title = "World builder stock",
            description =
                "Opened from Stock in the world builder's Npcs tab, over its HUD: every lootable item, in blue the ones the npc sells, and Done to keep them. " +
                    "sellsAnything: whether the npc sells the Alchemist's items.",
            controls = listOf(ControlDto.pick(NPC, context.npcIds()), ControlDto.boolean(SELLS_ANYTHING)),
            args = mapOf(NPC to ValueDto.text("alchemist"), SELLS_ANYTHING to ValueDto.boolean(true)),
            stories = listOf(alchemist, nothingSold),
        )

    override fun render(
        args: Args,
        context: StoryContext,
    ): View =
        WorldBuilderStockView(context.messages).apply {
            setDelegate(
                object : WorldBuilderStockView.Delegate {
                    override fun stockChanged(
                        npcId: String,
                        stock: List<String>,
                    ) = context.action("stockChanged", npcId, stock.joinToString(", "))
                },
            )
            val items =
                context.lootableItemIds().mapNotNull { lootableItemId ->
                    context.resourcesLoader.bitmapOrNull("lootableItem/$lootableItemId.png")?.let { bitmap ->
                        WorldBuilderStockView.StockItem(lootableItemId = lootableItemId, name = context.messages.lootableItemName(lootableItemId), bitmap = bitmap)
                    }
                }
            val npcId = args.text(NPC)
            open(npcId = npcId, npcName = context.messages.npcName(npcId), items = items, stock = if (args.boolean(SELLS_ANYTHING)) ALCHEMIST_STOCK else emptyList())
        }
}
