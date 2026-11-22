package com.mkz.tessavar.storybook.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.EffectInfoView
import com.mkz.tessavar.screen.adapters.presentation.EffectsView
import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoader
import com.mkz.tessavar.screen.adapters.presentation.Messages
import com.mkz.tessavar.storybook.domain.Storybook.Dto.ValueDto

class StoryContext(
    val resourcesLoader: GameSceneResourcesLoader,
    val messages: Messages,
) {
    interface Delegate {
        fun actionCalled(
            name: String,
            arguments: List<String>,
            setArg: Pair<String, ValueDto>?,
        )
    }

    private companion object {
        const val ITEMS_DIRECTORY = "lootableItem"
        const val UNITS_DIRECTORY = "unit"
        const val NPCS_DIRECTORY = "npc"
        const val EFFECTS_DIRECTORY = "effect"
        const val ABILITIES_DIRECTORY = "ability"
        const val TERRAIN_TRANSITIONS_DIRECTORY = "terrain/transitions"
        const val TRANSITION_NAME_SEPARATOR = "_to_"
        const val PORTRAIT_SUFFIX = "_portrait"
        const val IMAGE_EXTENSION = ".png"
        const val ON_DEFEATED_EFFECT_ID = "venom-on-defeat"
        const val EQUIPPED_EFFECT_PREFIX = "maximum-"
        const val LONGEST_SAMPLE_DURATION = 5
    }

    private var delegate: Delegate? = null

    fun setDelegate(delegate: Delegate) {
        this.delegate = delegate
    }

    fun lootableItemIds(): List<String> = imageIds(ITEMS_DIRECTORY)

    fun unitIds(): List<String> = imageIds(UNITS_DIRECTORY).filter { id -> id.endsWith(PORTRAIT_SUFFIX) }.map { id -> id.removeSuffix(PORTRAIT_SUFFIX) }

    fun npcIds(): List<String> = imageIds(NPCS_DIRECTORY).filter { id -> id.endsWith(PORTRAIT_SUFFIX) }.map { id -> id.removeSuffix(PORTRAIT_SUFFIX) }

    fun effectIds(): List<String> = imageIds(EFFECTS_DIRECTORY)

    private fun ongoingEffectIds(): List<String> = effectIds().filterNot { effectId -> effectId.startsWith(EQUIPPED_EFFECT_PREFIX) }

    fun effects(
        count: Int,
        newCount: Int = 0,
    ): List<EffectsView.Effect> {
        val effectIds = ongoingEffectIds()
        return List(count + newCount) { index ->
            val effectId = effectIds[index % effectIds.size]
            EffectsView.Effect(
                effectId = effectId,
                remainingDuration = if (effectId == ON_DEFEATED_EFFECT_ID) null else LONGEST_SAMPLE_DURATION - index % LONGEST_SAMPLE_DURATION,
                isNew = index >= count,
            )
        }
    }

    fun effectInfo(
        effectId: String,
        stacks: Int = 1,
        isExploring: Boolean = false,
        longestRemainingDuration: Int? = LONGEST_SAMPLE_DURATION,
    ): EffectInfoView.EffectInfo =
        EffectInfoView.EffectInfo(
            effectId = effectId,
            stacks = stacks,
            sentence = sampleSentence(effectId),
            remainingDurations =
                if (effectId == ON_DEFEATED_EFFECT_ID || longestRemainingDuration == null) {
                    emptyList()
                } else {
                    List(stacks) { index -> maxOf(longestRemainingDuration - 2 * index, 1) }
                },
            isExploring = isExploring,
        )

    private fun sampleSentence(effectId: String): EffectInfoView.EffectInfo.Sentence =
        when (effectId) {
            "burn" -> EffectInfoView.EffectInfo.Sentence.DamageOnPlayerTurnStarted(damage = 4)
            "soak" -> EffectInfoView.EffectInfo.Sentence.Resist(resistedEffectId = "burn", share = 50)
            "stun", "stun-long" -> EffectInfoView.EffectInfo.Sentence.NegateActions
            ON_DEFEATED_EFFECT_ID -> EffectInfoView.EffectInfo.Sentence.OnDefeatedDamageAdjacentAllies(damage = 3, turns = 5)
            else -> EffectInfoView.EffectInfo.Sentence.DamageOnPlayerTurnStarted(damage = 3)
        }

    fun abilityIds(): List<String> = imageIds(ABILITIES_DIRECTORY)

    fun terrainIds(): List<String> =
        terrainTransitionIds()
            .flatMap { transition -> transition.split(TRANSITION_NAME_SEPARATOR) }
            .distinct()
            .sorted()

    fun terrainTransitionIds(): List<String> = imageIds(TERRAIN_TRANSITIONS_DIRECTORY)

    private fun imageIds(directory: String): List<String> =
        resourcesLoader
            .bitmaps(directory)
            .keys
            .filter { name -> name.endsWith(IMAGE_EXTENSION) }
            .map { name -> name.removeSuffix(IMAGE_EXTENSION) }
            .sorted()

    fun action(
        name: String,
        vararg arguments: Any?,
        setArg: Pair<String, ValueDto>? = null,
    ) {
        delegate?.actionCalled(name, arguments.map { argument -> argument.toString() }, setArg)
    }
}
