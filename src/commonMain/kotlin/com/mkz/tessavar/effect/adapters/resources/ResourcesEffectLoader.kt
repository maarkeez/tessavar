package com.mkz.tessavar.effect.adapters.resources

import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.OnPlayerTurnStartedDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.TimingTypeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.WhileOngoingDto
import com.mkz.tessavar.effect.usecases.services.EffectLoader
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.adapters.toml.enumOf
import com.mkz.tessavar.shared.adapters.toml.fromResource
import com.mkz.tessavar.shared.adapters.toml.readTomlFiles
import com.mkz.tessavar.shared.adapters.toml.required
import korlibs.io.file.VfsFile
import kotlinx.serialization.Serializable

class ResourcesEffectLoader(
    private val effectRoot: VfsFile = ResourcesBundle.root["effect"],
) : EffectLoader {
    private var effects = emptyList<Effect.Dto>()

    override suspend fun initResources() {
        effects =
            effectRoot.readTomlFiles<EffectDefinition>().map { (effectId, definition) ->
                fromResource("effect/$effectId.toml") { definition.toDto(effectId) }
            }
    }

    override fun loadEffects(): List<Effect.Dto> = effects

    @Serializable
    data class EffectDefinition(
        val outcome: OutcomeDefinition,
        val timing: TimingDefinition,
    ) {
        fun toDto(effectId: String): Effect.Dto =
            Effect.Dto(
                id = effectId,
                outcome = outcome.toDto(),
                timing = timing.toDto(),
            )
    }

    @Serializable
    data class OutcomeDefinition(
        val type: String,
        val damage: Int? = null,
        val healing: Int? = null,
        val effectId: String? = null,
        val unitId: String? = null,
        val mana: Int? = null,
        val maximumHealth: Int? = null,
        val maximumMana: Int? = null,
        val abilityId: String? = null,
        val ongoingEffectId: String? = null,
        val share: Int? = null,
    ) {
        fun toDto(): EffectOutcomeDto =
            when (enumOf<TypeDto>("outcome.type", type)) {
                TypeDto.DAMAGE -> EffectOutcomeDto.damage(damage = required("outcome.damage", damage))
                TypeDto.HEAL -> EffectOutcomeDto.heal(healing = required("outcome.healing", healing))
                TypeDto.NEGATE_ACTIONS -> EffectOutcomeDto.negateActions()
                TypeDto.APPLY_IF_ONGOING ->
                    EffectOutcomeDto.applyIfOngoing(effectId = required("outcome.effectId", effectId), ongoingEffectId = required("outcome.ongoingEffectId", ongoingEffectId))
                TypeDto.RESIST -> EffectOutcomeDto.resist(effectId = required("outcome.effectId", effectId), share = required("outcome.share", share))
                TypeDto.TELEPORT -> EffectOutcomeDto.teleport()
                TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES -> EffectOutcomeDto.applyEffectToAdjacentAllies(effectId = required("outcome.effectId", effectId))
                TypeDto.SUMMON -> EffectOutcomeDto.summon(unitId = required("outcome.unitId", unitId))
                TypeDto.RESTORE_MANA -> EffectOutcomeDto.restoreMana(mana = required("outcome.mana", mana))
                TypeDto.INCREASE_MAXIMUM_HEALTH -> EffectOutcomeDto.increaseMaximumHealth(maximumHealth = required("outcome.maximumHealth", maximumHealth))
                TypeDto.INCREASE_MAXIMUM_MANA -> EffectOutcomeDto.increaseMaximumMana(maximumMana = required("outcome.maximumMana", maximumMana))
                TypeDto.LEARN_ABILITY -> EffectOutcomeDto.learnAbility(abilityId = required("outcome.abilityId", abilityId))
            }
    }

    @Serializable
    data class TimingDefinition(
        val type: String,
        val duration: Int? = null,
    ) {
        fun toDto(): TimingDto {
            val timingType = enumOf<TimingTypeDto>("timing.type", type)
            return TimingDto(
                type = timingType,
                onPlayerTurnStarted =
                    if (timingType == TimingTypeDto.ON_PLAYER_TURN_STARTED) {
                        OnPlayerTurnStartedDto(required("timing.duration", duration))
                    } else {
                        null
                    },
                whileOngoing =
                    if (timingType == TimingTypeDto.WHILE_ONGOING) {
                        WhileOngoingDto(required("timing.duration", duration))
                    } else {
                        null
                    },
            )
        }
    }
}
