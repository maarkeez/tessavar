package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.HudStyle.panel
import korlibs.image.color.Colors
import korlibs.korge.input.onClick
import korlibs.korge.view.Container
import korlibs.korge.view.RoundRect
import korlibs.korge.view.Text
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.text
import korlibs.korge.view.xy
import korlibs.math.geom.Size

open class EffectInfoView(
    private val panelWidth: Double,
    private val resourcesLoader: GameSceneResourcesLoader,
    private val messages: Messages,
) : Container() {
    companion object {
        private const val PADDING = 10.0
        private const val TEXT_SIZE = 12.0
        private const val NAME_TEXT_SIZE = 16.0
        private const val LINE_SPACING = 3.0
        private const val ICON_SCALE = 2.0
        private const val ICON_SLOT_SIZE = 40.0
        private const val TEXT_X = PADDING + ICON_SLOT_SIZE + 8
        private const val SENTENCE_Y = PADDING + 24
        private const val STACK_SEPARATOR = " · "
    }

    data class EffectInfo(
        val effectId: String,
        val stacks: Int,
        val sentence: Sentence,
        val remainingDurations: List<Int>,
        val isExploring: Boolean,
    ) {
        sealed interface Sentence {
            data class DamageOnPlayerTurnStarted(
                val damage: Int,
            ) : Sentence

            data object NegateActions : Sentence

            data class Resist(
                val resistedEffectId: String,
                val share: Int,
            ) : Sentence

            data class OnDefeatedDamageAdjacentAllies(
                val damage: Int,
                val turns: Int,
            ) : Sentence

            data class IncreaseMaximumHealth(
                val amount: Int,
            ) : Sentence

            data class IncreaseMaximumMana(
                val amount: Int,
            ) : Sentence
        }
    }

    private val background: RoundRect = panel(Size(panelWidth, ICON_SLOT_SIZE + 2 * PADDING))
    private val content: Container = container()

    internal lateinit var nameText: Text
    internal lateinit var sentenceWords: List<Text>
    internal var remainingDurationsText: Text? = null

    internal var effectInfo: EffectInfo? = null
        private set

    private var onTapped: (() -> Unit)? = null

    init {
        visible = false
        onClick { onTapped?.invoke() }
    }

    fun setOnTapped(onTapped: () -> Unit) {
        this.onTapped = onTapped
    }

    open fun display(effectInfo: EffectInfo) {
        this.effectInfo = effectInfo
        content.removeChildren()
        content.panel(Size(ICON_SLOT_SIZE, ICON_SLOT_SIZE), fill = HudStyle.PANEL_RAISED, radius = HudStyle.SLOT_RADIUS).xy(PADDING, PADDING)
        resourcesLoader.bitmapOrNull("effect/${effectInfo.effectId}.png")?.let { bitmap ->
            content.image(bitmap) {
                smoothing = false
                scale = ICON_SCALE
                xy(PADDING + (ICON_SLOT_SIZE - bitmap.width * ICON_SCALE) / 2, PADDING + (ICON_SLOT_SIZE - bitmap.height * ICON_SCALE) / 2)
            }
        }
        nameText = content.text(title(effectInfo), textSize = NAME_TEXT_SIZE, color = Colors.WHITE) { xy(TEXT_X, PADDING + 2) }
        val (words, sentenceHeight) = wrappedWords(sentence(effectInfo), top = SENTENCE_Y)
        sentenceWords = words
        var bottom = SENTENCE_Y + sentenceHeight
        remainingDurationsText =
            remainingDurations(effectInfo)?.let { line ->
                content.text(line, textSize = TEXT_SIZE, color = HudStyle.MUTED) { xy(TEXT_X, bottom + LINE_SPACING) }.also { bottom += LINE_SPACING + TEXT_SIZE }
            }
        background.size = Size(panelWidth, maxOf(bottom, PADDING + ICON_SLOT_SIZE) + PADDING)
        visible = true
    }

    open fun hide() {
        effectInfo = null
        visible = false
    }

    private fun title(effectInfo: EffectInfo): String {
        val name = messages.effectName(effectInfo.effectId).replaceFirstChar(Char::uppercaseChar)
        return if (effectInfo.stacks > 1) messages[UiMessage.EFFECT_INFO_TITLE, "effect" to name, "stacks" to effectInfo.stacks] else name
    }

    private fun sentence(effectInfo: EffectInfo): String =
        when (val sentence = effectInfo.sentence) {
            is EffectInfo.Sentence.DamageOnPlayerTurnStarted ->
                messages[if (effectInfo.isExploring) UiMessage.EFFECT_INFO_DAMAGE_EACH_SECOND else UiMessage.EFFECT_INFO_DAMAGE_ON_PLAYER_TURN_STARTED, "damage" to sentence.damage]
            is EffectInfo.Sentence.NegateActions -> messages[UiMessage.EFFECT_INFO_NEGATE_ACTIONS]
            is EffectInfo.Sentence.Resist -> messages[UiMessage.EFFECT_INFO_RESIST, "share" to sentence.share, "effect" to messages.effectName(sentence.resistedEffectId)]
            is EffectInfo.Sentence.OnDefeatedDamageAdjacentAllies ->
                messages.plural(UiMessage.ABILITY_INFO_ON_DEFEATED_DAMAGE_ADJACENT_ALLIES, sentence.turns, "damage" to sentence.damage, "turns" to sentence.turns)
            is EffectInfo.Sentence.IncreaseMaximumHealth -> messages[UiMessage.EFFECT_INFO_INCREASE_MAXIMUM_HEALTH, "amount" to sentence.amount]
            is EffectInfo.Sentence.IncreaseMaximumMana -> messages[UiMessage.EFFECT_INFO_INCREASE_MAXIMUM_MANA, "amount" to sentence.amount]
        }

    private fun remainingDurations(effectInfo: EffectInfo): String? =
        effectInfo.remainingDurations
            .map { remainingDuration ->
                if (remainingDuration == 0) {
                    messages[UiMessage.EFFECT_INFO_ENDS_THIS_TURN]
                } else if (effectInfo.isExploring) {
                    messages.plural(UiMessage.EFFECT_INFO_SECONDS_LEFT, remainingDuration, "seconds" to remainingDuration)
                } else {
                    messages.plural(UiMessage.EFFECT_INFO_TURNS_LEFT, remainingDuration, "turns" to remainingDuration)
                }
            }.takeIf { lines -> lines.isNotEmpty() }
            ?.joinToString(STACK_SEPARATOR)

    private fun wrappedWords(
        sentence: String,
        top: Double,
    ): Pair<List<Text>, Double> {
        val right = panelWidth - PADDING
        val space = Text(" ", textSize = TEXT_SIZE).width
        var x = TEXT_X
        var y = top
        val words =
            sentence.split(" ").map { word ->
                val isNumber = word.any(Char::isDigit)
                content.text(word, textSize = TEXT_SIZE, color = if (isNumber) HudStyle.SELECTION else Colors.WHITE).also { text ->
                    if (x > TEXT_X && x + text.width > right) {
                        x = TEXT_X
                        y += TEXT_SIZE + LINE_SPACING
                    }
                    text.xy(x, y)
                    x += text.width + space
                }
            }
        return words to (y + TEXT_SIZE - top)
    }
}
