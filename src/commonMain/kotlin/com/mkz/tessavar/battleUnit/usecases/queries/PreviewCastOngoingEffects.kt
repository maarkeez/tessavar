package com.mkz.tessavar.battleUnit.usecases.queries

import com.mkz.tessavar.ability.usecases.queries.SearchAbilityById
import com.mkz.tessavar.battleUnit.domain.BattleUnitRepository
import com.mkz.tessavar.battleUnit.usecases.services.CastResolution
import com.mkz.tessavar.battlefield.domain.Battlefield
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto.APPLY_IF_ONGOING
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.TimingTypeDto
import com.mkz.tessavar.effect.usecases.queries.SearchEffectById

class PreviewCastOngoingEffects(
    private val searchAbilityById: SearchAbilityById,
    private val searchEffectById: SearchEffectById,
    private val castResolution: CastResolution,
    private val battleUnitRepository: BattleUnitRepository,
) {
    companion object {
        val ONGOING_TIMINGS = setOf(TimingTypeDto.ON_PLAYER_TURN_STARTED, TimingTypeDto.ON_DEFEATED, TimingTypeDto.WHILE_ONGOING)
        private const val MAXIMUM_NESTING = 5
    }

    operator fun invoke(
        battleUnitId: String,
        abilityId: String,
        castGroup: List<Battlefield.Dto.PositionDto>,
    ): Map<String, List<String>> {
        val ability = searchAbilityById(abilityId) ?: return emptyMap()
        val newEffectIds = linkedMapOf<String, MutableList<String>>()
        castGroup
            .flatMap { position -> castResolution(casterId = battleUnitId, ability = ability, targetedRow = position.row, targetedColumn = position.column) }
            .forEach { application ->
                val recipientId = application.recipient.battleUnitId ?: return@forEach
                preview(recipientId, application.effectId, newEffectIds, nesting = 0)
            }
        return newEffectIds
    }

    private fun preview(
        recipientId: String,
        effectId: String,
        newEffectIds: MutableMap<String, MutableList<String>>,
        nesting: Int,
    ) {
        if (nesting > MAXIMUM_NESTING) return
        val effect = searchEffectById(effectId) ?: return
        when {
            effect.timing.type in ONGOING_TIMINGS -> newEffectIds.getOrPut(recipientId) { mutableListOf() } += effectId
            effect.outcome.type == APPLY_IF_ONGOING -> {
                val applyIfOngoing = effect.outcome.applyIfOngoing!!
                val isOngoing =
                    battleUnitRepository.searchById(recipientId)?.hasOngoingEffect(applyIfOngoing.ongoingEffectId) == true ||
                        applyIfOngoing.ongoingEffectId in newEffectIds[recipientId].orEmpty()
                if (isOngoing) preview(recipientId, applyIfOngoing.effectId, newEffectIds, nesting + 1)
            }
        }
    }
}
