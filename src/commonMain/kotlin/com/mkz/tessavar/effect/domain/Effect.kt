package com.mkz.tessavar.effect.domain

import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.APPLY_IF_ONGOING
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.DAMAGE
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.HEAL
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.INCREASE_MAXIMUM_HEALTH
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.INCREASE_MAXIMUM_MANA
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.LEARN_ABILITY
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.NEGATE_ACTIONS
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.RESIST
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.RESTORE_MANA
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.SUMMON
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.TELEPORT
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.OnPlayerTurnStartedDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.TimingTypeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.WhileOngoingDto
import com.mkz.tessavar.effect.domain.EffectError.AmountAboveLimit
import com.mkz.tessavar.effect.domain.EffectError.DurationAboveLimit
import com.mkz.tessavar.effect.domain.EffectError.EmptyEffectId
import com.mkz.tessavar.effect.domain.EffectError.InvalidEffectTiming
import com.mkz.tessavar.effect.domain.EffectError.MissingEffectTimingDetails
import com.mkz.tessavar.effect.domain.EffectError.NegativeAmount
import com.mkz.tessavar.effect.domain.EffectError.NegativeDuration
import kotlin.jvm.JvmInline

@ConsistentCopyVisibility
data class Effect private constructor(
    private val id: Id,
    private val outcome: Outcome,
    private val timing: Timing,
    private val events: Set<EffectEvent>,
) {
    companion object {
        fun create(dto: Dto): Effect {
            val outcome = Outcome(dto.outcome)
            val timing = Timing(dto.timing)
            if (outcome.lastsWhileEquipped() != timing.isWhileEquipped()) throw InvalidEffectTiming()
            if (outcome.learnsAbility() && !timing.isImmediately()) throw InvalidEffectTiming()
            if (outcome.negatesActions() && !timing.isOnPlayerTurnStarted()) throw InvalidEffectTiming()
            if (outcome.appliesIfOngoing() && !timing.isImmediately()) throw InvalidEffectTiming()
            if (outcome.resists() != timing.isWhileOngoing()) throw InvalidEffectTiming()
            if (outcome.appliesToAdjacentAllies() && !timing.isImmediately() && !timing.isOnDefeated()) throw InvalidEffectTiming()
            return Effect(
                id = Id(dto.id),
                outcome = outcome,
                timing = timing,
                events = setOf(EffectEvent.EffectCreated(dto.id)),
            )
        }
    }

    fun pullEvents(): Pair<Set<EffectEvent>, Effect> = events to copy(events = emptySet())

    fun toDto() =
        Dto(
            id = id.value,
            outcome = outcome.toDto(),
            timing = timing.toDto(),
        )

    @JvmInline private value class Id(
        val value: String,
    ) {
        init {
            if (value.isBlank()) throw EmptyEffectId()
        }
    }

    private sealed interface Outcome {
        companion object {
            operator fun invoke(dto: Dto.EffectOutcomeDto): Outcome =
                when (dto.type) {
                    DAMAGE -> Damage(dto.damage!!.damage)
                    HEAL -> Heal(dto.heal!!.healing)
                    NEGATE_ACTIONS -> NegateActions
                    APPLY_IF_ONGOING -> ApplyIfOngoing(effectId = dto.applyIfOngoing!!.effectId, ongoingEffectId = dto.applyIfOngoing.ongoingEffectId)
                    RESIST -> Resist(effectId = dto.resist!!.effectId, share = dto.resist.share)
                    TELEPORT -> Teleport
                    APPLY_EFFECT_TO_ADJACENT_ALLIES -> ApplyEffectToAdjacentAllies(dto.applyEffectToAdjacentAllies!!.effectId)
                    SUMMON -> Summon(dto.summon!!.unitId)
                    RESTORE_MANA -> RestoreMana(dto.restoreMana!!.mana)
                    INCREASE_MAXIMUM_HEALTH -> IncreaseMaximumHealth(dto.increaseMaximumHealth!!.maximumHealth)
                    INCREASE_MAXIMUM_MANA -> IncreaseMaximumMana(dto.increaseMaximumMana!!.maximumMana)
                    LEARN_ABILITY -> LearnAbility(dto.learnAbility!!.abilityId)
                }
        }

        fun lastsWhileEquipped() = this is IncreaseMaximumHealth || this is IncreaseMaximumMana

        fun learnsAbility() = this is LearnAbility

        fun negatesActions() = this is NegateActions

        fun appliesIfOngoing() = this is ApplyIfOngoing

        fun resists() = this is Resist

        fun appliesToAdjacentAllies() = this is ApplyEffectToAdjacentAllies

        fun toDto() =
            when (this) {
                is Damage -> Dto.EffectOutcomeDto.damage(damage = damage)
                is Heal -> Dto.EffectOutcomeDto.heal(healing = healing)
                NegateActions -> Dto.EffectOutcomeDto.negateActions()
                is ApplyIfOngoing -> Dto.EffectOutcomeDto.applyIfOngoing(effectId = effectId, ongoingEffectId = ongoingEffectId)
                is Resist -> Dto.EffectOutcomeDto.resist(effectId = effectId, share = share)
                Teleport -> Dto.EffectOutcomeDto.teleport()
                is ApplyEffectToAdjacentAllies -> Dto.EffectOutcomeDto.applyEffectToAdjacentAllies(effectId = effectId)
                is Summon -> Dto.EffectOutcomeDto.summon(unitId = unitId)
                is RestoreMana -> Dto.EffectOutcomeDto.restoreMana(mana = mana)
                is IncreaseMaximumHealth -> Dto.EffectOutcomeDto.increaseMaximumHealth(maximumHealth = maximumHealth)
                is IncreaseMaximumMana -> Dto.EffectOutcomeDto.increaseMaximumMana(maximumMana = maximumMana)
                is LearnAbility -> Dto.EffectOutcomeDto.learnAbility(abilityId = abilityId)
            }

        @JvmInline private value class Damage(
            val damage: Int,
        ) : Outcome {
            init {
                if (damage < 0) throw NegativeAmount()
                if (damage > 999) throw AmountAboveLimit()
            }
        }

        @JvmInline private value class Heal(
            val healing: Int,
        ) : Outcome {
            init {
                if (healing < 0) throw NegativeAmount()
                if (healing > 999) throw AmountAboveLimit()
            }
        }

        @JvmInline private value class RestoreMana(
            val mana: Int,
        ) : Outcome {
            init {
                if (mana < 0) throw NegativeAmount()
                if (mana > 999) throw AmountAboveLimit()
            }
        }

        @JvmInline private value class IncreaseMaximumHealth(
            val maximumHealth: Int,
        ) : Outcome {
            init {
                if (maximumHealth < 0) throw NegativeAmount()
                if (maximumHealth > 999) throw AmountAboveLimit()
            }
        }

        @JvmInline private value class IncreaseMaximumMana(
            val maximumMana: Int,
        ) : Outcome {
            init {
                if (maximumMana < 0) throw NegativeAmount()
                if (maximumMana > 999) throw AmountAboveLimit()
            }
        }

        @JvmInline private value class ApplyEffectToAdjacentAllies(
            val effectId: String,
        ) : Outcome

        @JvmInline private value class Summon(
            val unitId: String,
        ) : Outcome

        @JvmInline private value class LearnAbility(
            val abilityId: String,
        ) : Outcome

        private data class ApplyIfOngoing(
            val effectId: String,
            val ongoingEffectId: String,
        ) : Outcome

        private data class Resist(
            val effectId: String,
            val share: Int,
        ) : Outcome {
            init {
                if (share < 1) throw NegativeAmount()
                if (share > 100) throw AmountAboveLimit()
            }
        }

        private object NegateActions : Outcome

        private object Teleport : Outcome
    }

    private sealed interface Timing {
        companion object {
            operator fun invoke(dto: TimingDto): Timing =
                when (dto.type) {
                    TimingTypeDto.IMMEDIATELY -> Immediately
                    TimingTypeDto.ON_PLAYER_TURN_STARTED -> OnPlayerTurnStarted(dto.onPlayerTurnStarted ?: throw MissingEffectTimingDetails())
                    TimingTypeDto.WHILE_ONGOING -> WhileOngoing(dto.whileOngoing ?: throw MissingEffectTimingDetails())
                    TimingTypeDto.ON_DEFEATED -> OnDefeated
                    TimingTypeDto.WHILE_EQUIPPED -> WhileEquipped
                }
        }

        fun isWhileEquipped() = this is WhileEquipped

        fun isImmediately() = this is Immediately

        fun isOnPlayerTurnStarted() = this is OnPlayerTurnStarted

        fun isOnDefeated() = this is OnDefeated

        fun isWhileOngoing() = this is WhileOngoing

        fun toDto() =
            TimingDto(
                type =
                    when (this) {
                        is Immediately -> TimingTypeDto.IMMEDIATELY
                        is OnPlayerTurnStarted -> TimingTypeDto.ON_PLAYER_TURN_STARTED
                        is WhileOngoing -> TimingTypeDto.WHILE_ONGOING
                        is OnDefeated -> TimingTypeDto.ON_DEFEATED
                        is WhileEquipped -> TimingTypeDto.WHILE_EQUIPPED
                    },
                onPlayerTurnStarted = if (this is OnPlayerTurnStarted) toPlayerTurnStartedDto() else null,
                whileOngoing = if (this is WhileOngoing) toWhileOngoingDto() else null,
            )

        private object Immediately : Timing

        private object OnDefeated : Timing

        private object WhileEquipped : Timing

        private data class OnPlayerTurnStarted(
            val duration: Int,
        ) : Timing {
            constructor(dto: OnPlayerTurnStartedDto) : this(duration = dto.duration)

            init {
                if (duration < 0) throw NegativeDuration()
                if (duration > 99) throw DurationAboveLimit()
            }

            fun toPlayerTurnStartedDto() = OnPlayerTurnStartedDto(duration = duration)
        }

        private data class WhileOngoing(
            val duration: Int,
        ) : Timing {
            constructor(dto: WhileOngoingDto) : this(duration = dto.duration)

            init {
                if (duration < 0) throw NegativeDuration()
                if (duration > 99) throw DurationAboveLimit()
            }

            fun toWhileOngoingDto() = WhileOngoingDto(duration = duration)
        }
    }

    data class Dto(
        val id: String,
        val outcome: EffectOutcomeDto,
        val timing: TimingDto,
    ) {
        data class EffectApplicationDto(
            val source: SourceDto,
            val recipient: RecipientDto,
            val effectId: String,
            val destination: RecipientDto?,
        ) {
            data class SourceDto(
                val type: SourceTypeDto,
                val battleUnitId: String?,
                val terrainId: String?,
                val lootableItemId: String?,
                val abilityId: String?,
            ) {
                enum class SourceTypeDto {
                    BATTLE_UNIT,
                    TERRAIN,
                    ITEM,
                }

                companion object {
                    fun battleUnit(
                        battleUnitId: String,
                        abilityId: String? = null,
                    ) = SourceDto(
                        type = SourceTypeDto.BATTLE_UNIT,
                        battleUnitId = battleUnitId,
                        terrainId = null,
                        lootableItemId = null,
                        abilityId = abilityId,
                    )

                    fun terrain(terrainId: String) =
                        SourceDto(
                            type = SourceTypeDto.TERRAIN,
                            battleUnitId = null,
                            terrainId = terrainId,
                            lootableItemId = null,
                            abilityId = null,
                        )

                    fun item(lootableItemId: String) =
                        SourceDto(
                            type = SourceTypeDto.ITEM,
                            battleUnitId = null,
                            terrainId = null,
                            lootableItemId = lootableItemId,
                            abilityId = null,
                        )
                }
            }
        }

        data class RecipientDto(
            val type: TypeDto,
            val battleUnitId: String?,
            val position: PositionDto?,
        ) {
            companion object {
                fun battleUnit(battleUnitId: String) = RecipientDto(type = TypeDto.BATTLE_UNIT, battleUnitId = battleUnitId, position = null)

                fun position(
                    row: Int,
                    column: Int,
                ) = RecipientDto(type = TypeDto.POSITION, battleUnitId = null, position = PositionDto(row = row, column = column))
            }

            enum class TypeDto {
                BATTLE_UNIT,
                POSITION,
            }

            data class PositionDto(
                val row: Int,
                val column: Int,
            )
        }

        data class EffectOutcomeDto(
            val type: TypeDto,
            val damage: DamageDto?,
            val heal: HealDto?,
            val applyEffectToAdjacentAllies: ApplyEffectToAdjacentAlliesDto?,
            val summon: SummonDto?,
            val restoreMana: RestoreManaDto?,
            val increaseMaximumHealth: IncreaseMaximumHealthDto?,
            val increaseMaximumMana: IncreaseMaximumManaDto?,
            val learnAbility: LearnAbilityDto?,
            val applyIfOngoing: ApplyIfOngoingDto?,
            val resist: ResistDto?,
        ) {
            companion object {
                fun damage(damage: Int) = withoutDetails(TypeDto.DAMAGE).copy(damage = DamageDto(damage = damage))

                fun heal(healing: Int) = withoutDetails(TypeDto.HEAL).copy(heal = HealDto(healing = healing))

                fun negateActions() = withoutDetails(TypeDto.NEGATE_ACTIONS)

                fun applyIfOngoing(
                    effectId: String,
                    ongoingEffectId: String,
                ) = withoutDetails(TypeDto.APPLY_IF_ONGOING).copy(applyIfOngoing = ApplyIfOngoingDto(effectId = effectId, ongoingEffectId = ongoingEffectId))

                fun resist(
                    effectId: String,
                    share: Int,
                ) = withoutDetails(TypeDto.RESIST).copy(resist = ResistDto(effectId = effectId, share = share))

                fun teleport() = withoutDetails(TypeDto.TELEPORT)

                fun applyEffectToAdjacentAllies(effectId: String) =
                    withoutDetails(TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES).copy(applyEffectToAdjacentAllies = ApplyEffectToAdjacentAlliesDto(effectId = effectId))

                fun summon(unitId: String) = withoutDetails(TypeDto.SUMMON).copy(summon = SummonDto(unitId = unitId))

                fun restoreMana(mana: Int) = withoutDetails(TypeDto.RESTORE_MANA).copy(restoreMana = RestoreManaDto(mana = mana))

                fun increaseMaximumHealth(maximumHealth: Int) = withoutDetails(TypeDto.INCREASE_MAXIMUM_HEALTH).copy(increaseMaximumHealth = IncreaseMaximumHealthDto(maximumHealth = maximumHealth))

                fun increaseMaximumMana(maximumMana: Int) = withoutDetails(TypeDto.INCREASE_MAXIMUM_MANA).copy(increaseMaximumMana = IncreaseMaximumManaDto(maximumMana = maximumMana))

                fun learnAbility(abilityId: String) = withoutDetails(TypeDto.LEARN_ABILITY).copy(learnAbility = LearnAbilityDto(abilityId = abilityId))

                private fun withoutDetails(type: TypeDto) =
                    EffectOutcomeDto(
                        type = type,
                        damage = null,
                        heal = null,
                        applyEffectToAdjacentAllies = null,
                        summon = null,
                        restoreMana = null,
                        increaseMaximumHealth = null,
                        increaseMaximumMana = null,
                        learnAbility = null,
                        applyIfOngoing = null,
                        resist = null,
                    )
            }

            enum class TypeDto {
                DAMAGE,
                HEAL,
                NEGATE_ACTIONS,
                TELEPORT,
                APPLY_EFFECT_TO_ADJACENT_ALLIES,
                SUMMON,
                RESTORE_MANA,
                INCREASE_MAXIMUM_HEALTH,
                INCREASE_MAXIMUM_MANA,
                LEARN_ABILITY,
                APPLY_IF_ONGOING,
                RESIST,
            }

            data class DamageDto(
                val damage: Int,
            )

            data class HealDto(
                val healing: Int,
            )

            data class ApplyEffectToAdjacentAlliesDto(
                val effectId: String,
            )

            data class SummonDto(
                val unitId: String,
            )

            data class RestoreManaDto(
                val mana: Int,
            )

            data class IncreaseMaximumHealthDto(
                val maximumHealth: Int,
            )

            data class IncreaseMaximumManaDto(
                val maximumMana: Int,
            )

            data class LearnAbilityDto(
                val abilityId: String,
            )

            data class ApplyIfOngoingDto(
                val effectId: String,
                val ongoingEffectId: String,
            )

            data class ResistDto(
                val effectId: String,
                val share: Int,
            )
        }

        data class TimingDto(
            val type: TimingTypeDto,
            val onPlayerTurnStarted: OnPlayerTurnStartedDto?,
            val whileOngoing: WhileOngoingDto?,
        ) {
            enum class TimingTypeDto {
                IMMEDIATELY,
                ON_PLAYER_TURN_STARTED,
                ON_DEFEATED,
                WHILE_EQUIPPED,
                WHILE_ONGOING,
            }

            data class OnPlayerTurnStartedDto(
                val duration: Int,
            )

            data class WhileOngoingDto(
                val duration: Int,
            )
        }
    }
}
