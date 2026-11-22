package com.mkz.tessavar.battleUnit.usecases.commands

import com.mkz.tessavar.ability.usecases.queries.SearchAbilityById
import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.domain.BattleUnitError.FailedToReceiveAbilityEffects
import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent.RequestApplyEffect
import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battleUnit.usecases.services.AdjacentAllies
import com.mkz.tessavar.battleUnit.usecases.services.BattleUnitDeployment
import com.mkz.tessavar.battlefield.usecases.queries.SearchPosition
import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto.SourceDto.SourceTypeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.APPLY_IF_ONGOING
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.SUMMON
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.TELEPORT
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto.TypeDto.BATTLE_UNIT
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto.TypeDto.POSITION
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.TimingTypeDto
import com.mkz.tessavar.effect.usecases.queries.SearchEffectById
import com.mkz.tessavar.shared.domain.DomainEvent
import com.mkz.tessavar.shared.domain.EventBus
import kotlin.random.Random

class ApplyEffect(
    private val battleUnitRepository: BattleUnitRepository,
    private val eventBus: EventBus,
    private val searchEffectById: SearchEffectById,
    private val searchPosition: SearchPosition,
    private val battleUnitDeployment: BattleUnitDeployment,
    private val searchAbilityById: SearchAbilityById,
    private val adjacentAllies: AdjacentAllies,
) {
    operator fun invoke(application: EffectApplicationDto) {
        val effect = searchEffectById(application.effectId) ?: throw FailedToReceiveAbilityEffects()
        when (application.recipient.type) {
            POSITION -> applyToTile(application = application, effect = effect)
            BATTLE_UNIT -> applyToUnit(application = application, effect = effect)
        }
    }

    private fun applyToTile(
        application: EffectApplicationDto,
        effect: Effect.Dto,
    ) {
        val tile = application.recipient.position ?: throw FailedToReceiveAbilityEffects()
        if (effect.outcome.type != SUMMON) throw FailedToReceiveAbilityEffects()
        val casterId = application.source.battleUnitId ?: throw FailedToReceiveAbilityEffects()
        val caster = battleUnitRepository.searchById(casterId) ?: throw FailedToReceiveAbilityEffects()
        val abilityId = application.source.abilityId ?: throw FailedToReceiveAbilityEffects()
        val ability = searchAbilityById(abilityId) ?: throw FailedToReceiveAbilityEffects()
        val events =
            battleUnitDeployment.deploy(
                battleUnitId = "summon-${Random.nextLong()}",
                unitId = effect.outcome.summon!!.unitId,
                playerId = caster.toDto().playerId,
                deployAtRow = tile.row,
                deployAtColumn = tile.column,
                remainingSummonDuration = ability.summonDuration,
            )
        eventBus.publish(events)
    }

    private fun applyToUnit(
        application: EffectApplicationDto,
        effect: Effect.Dto,
    ) {
        val battleUnitId = application.recipient.battleUnitId ?: throw FailedToReceiveAbilityEffects()
        val battleUnit = battleUnitRepository.searchById(battleUnitId) ?: throw FailedToReceiveAbilityEffects()
        val position = searchPosition(battleUnit.toDto().id) ?: throw FailedToReceiveAbilityEffects()
        if (effect.timing.type == TimingTypeDto.IMMEDIATELY && effect.outcome.type in setOf(APPLY_IF_ONGOING, APPLY_EFFECT_TO_ADJACENT_ALLIES)) {
            eventBus.publish(furtherApplications(application, effect, battleUnit, position.row, position.column))
            return
        }
        val (events, appliedBattleUnit) =
            when (effect.timing.type) {
                TimingTypeDto.IMMEDIATELY ->
                    battleUnit
                        .receiveImmediateEffect(effectId = effect.id)
                        .triggerImmediateEffect(
                            effect = effect,
                            currentRow = position.row,
                            currentColumn = position.column,
                            ongoingEffectDefinitions = whileOngoingEffects(battleUnit),
                        ).pullEvents()
                TimingTypeDto.ON_PLAYER_TURN_STARTED ->
                    battleUnit
                        .receiveOnPlayerTurnStartedEffect(
                            effectId = effect.id,
                            remainingDuration = effect.timing.onPlayerTurnStarted!!.duration,
                        ).pullEvents()
                TimingTypeDto.ON_DEFEATED -> {
                    battleUnit.receiveOnDefeatedEffect(effectId = effect.id).pullEvents()
                }
                TimingTypeDto.WHILE_ONGOING ->
                    battleUnit
                        .receiveWhileOngoingEffect(
                            effectId = effect.id,
                            remainingDuration = effect.timing.whileOngoing!!.duration,
                        ).pullEvents()
                TimingTypeDto.WHILE_EQUIPPED -> {
                    val lootableItemId = application.source.lootableItemId
                    if (application.source.type != SourceTypeDto.ITEM || lootableItemId == null) throw FailedToReceiveAbilityEffects()
                    battleUnit.applyEquippedEffect(lootableItemId = lootableItemId, effect = effect).pullEvents()
                }
            }
        var pendingEvents: Set<DomainEvent> = events
        var updatedBattleUnit: BattleUnit = appliedBattleUnit
        val destination = application.destination?.takeIf { it.type == POSITION }?.position
        if (effect.outcome.type == TELEPORT && destination != null) {
            val currentPosition = searchPosition(battleUnitId) ?: throw FailedToReceiveAbilityEffects()
            val (teleportEvents, teleportedBattleUnit) =
                updatedBattleUnit
                    .teleport(
                        fromRow = currentPosition.row,
                        fromColumn = currentPosition.column,
                        toRow = destination.row,
                        toColumn = destination.column,
                    ).pullEvents()
            pendingEvents = pendingEvents + teleportEvents
            updatedBattleUnit = teleportedBattleUnit
        }
        battleUnitRepository.update(updatedBattleUnit)
        eventBus.publish(pendingEvents)
    }

    private fun furtherApplications(
        application: EffectApplicationDto,
        effect: Effect.Dto,
        battleUnit: BattleUnit,
        row: Int,
        column: Int,
    ): Set<DomainEvent> =
        when (effect.outcome.type) {
            APPLY_IF_ONGOING -> {
                val applyIfOngoing = effect.outcome.applyIfOngoing!!
                if (battleUnit.hasOngoingEffect(applyIfOngoing.ongoingEffectId)) {
                    setOf(RequestApplyEffect(application.copy(effectId = applyIfOngoing.effectId, destination = null)))
                } else {
                    emptySet()
                }
            }
            APPLY_EFFECT_TO_ADJACENT_ALLIES ->
                adjacentAllies(battleUnit = battleUnit, row = row, column = column)
                    .map { adjacentAlly ->
                        RequestApplyEffect(
                            application.copy(
                                recipient = RecipientDto.battleUnit(adjacentAlly.toDto().id),
                                effectId = effect.outcome.applyEffectToAdjacentAllies!!.effectId,
                                destination = null,
                            ),
                        )
                    }.toSet()
            else -> emptySet()
        }

    private fun whileOngoingEffects(battleUnit: BattleUnit): List<Effect.Dto> =
        battleUnit
            .toDto()
            .ongoingEffects.whileOngoing
            .distinct()
            .mapNotNull { effectId -> searchEffectById(effectId) }
}
