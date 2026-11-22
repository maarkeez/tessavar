package com.mkz.tessavar.cpuBattleBrain.usecases.queries

import com.mkz.tessavar.ability.domain.Ability
import com.mkz.tessavar.ability.domain.Ability.Dto.EffectTargetDto
import com.mkz.tessavar.ability.usecases.queries.SearchAbilityById
import com.mkz.tessavar.battleUnit.domain.BattleUnit
import com.mkz.tessavar.battleUnit.usecases.queries.CanCastAbility
import com.mkz.tessavar.battleUnit.usecases.queries.SearchBattleUnitById
import com.mkz.tessavar.battleUnit.usecases.queries.WhereCanCast
import com.mkz.tessavar.battlefield.usecases.queries.SearchOccupant
import com.mkz.tessavar.battlefield.usecases.queries.SearchPosition
import com.mkz.tessavar.cpuBattleBrain.usecases.services.MINIMUM_UTILITY_GAIN
import com.mkz.tessavar.effect.domain.Effect
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectOutcomeDto.TypeDto
import com.mkz.tessavar.effect.domain.Effect.Dto.TimingDto.TimingTypeDto
import com.mkz.tessavar.effect.usecases.queries.SearchEffectById
import com.mkz.tessavar.unit.usecases.queries.SearchUnitById
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class WhatShouldCast(
    private val searchBattleUnitById: SearchBattleUnitById,
    private val searchUnitById: SearchUnitById,
    private val canCastAbility: CanCastAbility,
    private val whereCanCast: WhereCanCast,
    private val searchAbilityById: SearchAbilityById,
    private val searchEffectById: SearchEffectById,
    private val searchOccupant: SearchOccupant,
    private val searchPosition: SearchPosition,
    private val searchEnemiesWithinReach: SearchEnemiesWithinReach,
    private val healingNeed: HealingNeed,
    private val random: Random = Random.Default,
) {
    operator fun invoke(battleUnitId: String): CastDecision? {
        val caster = searchBattleUnitById(id = battleUnitId) ?: return null
        if (caster.remainingActions.remainingCasts <= 0) return null
        val casterMaxHealth = caster.maximumHealth
        val enemiesWithinReach = searchEnemiesWithinReach(battleUnitId = caster.id)
        val options =
            caster.knownAbilities.keys
                .filter { abilityId -> canCastAbility(battleUnitId = caster.id, abilityId = abilityId) }
                .mapNotNull { abilityId -> searchAbilityById(abilityId) }
                .flatMap { ability ->
                    whereCanCast(battleUnitId = caster.id, abilityId = ability.id).map { castGroup ->
                        evaluate(ability, castGroup, caster, casterMaxHealth, enemiesWithinReach)
                    }
                }.filter { option -> enemiesWithinReach.isNotEmpty() || option.kind == CastKind.HEAL }
        val bestOption = options.filter { it.kind != CastKind.OTHER }.maxByOrNull { it.utility }
        if (bestOption != null && bestOption.utility > MINIMUM_UTILITY_GAIN) {
            return CastDecision(abilityId = bestOption.abilityId, castGroup = bestOption.castGroup)
        }
        return options
            .filter { it.kind == CastKind.OTHER }
            .randomOrNull(random)
            ?.let { option -> CastDecision(abilityId = option.abilityId, castGroup = option.castGroup) }
    }

    data class CastDecision(
        val abilityId: String,
        val castGroup: WhereCanCast.CastGroup,
    )

    private enum class CastKind {
        DAMAGE,
        HEAL,
        SUMMON,
        OTHER,
    }

    private data class UtilityEvaluationData(
        val abilityId: String,
        val castGroup: WhereCanCast.CastGroup,
        val kind: CastKind,
        val utility: Double,
    )

    private fun evaluate(
        ability: Ability.Dto,
        castGroup: WhereCanCast.CastGroup,
        caster: BattleUnit.Dto,
        casterMaxHealth: Int,
        enemiesWithinReach: List<BattleUnit.Dto>,
    ): UtilityEvaluationData {
        val effects = effectsOf(ability)
        val targetEffects = effects.filter { (target, _) -> target == EffectTargetDto.Type.TARGETED_BATTLE_UNIT }.map { (_, effect) -> effect }
        val healing = expectedAmount(effects, target = EffectTargetDto.Type.CASTER, outcome = TypeDto.HEAL)
        val deployedUnitId = effects.firstNotNullOfOrNull { (_, effect) -> effect.outcome.summon?.unitId }
        return when {
            targetEffects.any { effect -> effect.outcome.type in HARMFUL_OUTCOMES } ->
                UtilityEvaluationData(ability.id, castGroup, CastKind.DAMAGE, damageUtility(targetEffects, castGroup, caster, casterMaxHealth, enemiesWithinReach))
            healing > 0 -> UtilityEvaluationData(ability.id, castGroup, CastKind.HEAL, healUtility(healing, caster, casterMaxHealth))
            deployedUnitId != null -> UtilityEvaluationData(ability.id, castGroup, CastKind.SUMMON, summonUtility(deployedUnitId, castGroup, caster, casterMaxHealth, enemiesWithinReach))
            targetEffects.any { effect -> effect.outcome.type == TypeDto.RESIST } ->
                UtilityEvaluationData(ability.id, castGroup, CastKind.DAMAGE, setupUtility(ability, targetEffects, castGroup, caster, casterMaxHealth, enemiesWithinReach))
            else -> UtilityEvaluationData(ability.id, castGroup, CastKind.OTHER, utility = 0.0)
        }
    }

    private fun effectsOf(ability: Ability.Dto): List<Pair<EffectTargetDto.Type, Effect.Dto>> =
        ability.abilityEffects.mapNotNull { abilityEffect -> searchEffectById(abilityEffect.effectId)?.let { effect -> abilityEffect.target.type to effect } }

    private fun damageUtility(
        targetEffects: List<Effect.Dto>,
        castGroup: WhereCanCast.CastGroup,
        caster: BattleUnit.Dto,
        casterMaxHealth: Int,
        enemiesWithinReach: List<BattleUnit.Dto>,
    ): Double {
        val targetsUtility =
            targets(castGroup, enemiesWithinReach)
                .sumOf { target -> harmUtility(harm(targetEffects, target, ongoingEffectIds(target)), target) }
                .coerceAtMost(1.0)
        val casterHealthRatio = caster.remainingHealth.toDouble() / casterMaxHealth
        return casterHealthRatio * targetsUtility
    }

    private fun setupUtility(
        ability: Ability.Dto,
        targetEffects: List<Effect.Dto>,
        castGroup: WhereCanCast.CastGroup,
        caster: BattleUnit.Dto,
        casterMaxHealth: Int,
        enemiesWithinReach: List<BattleUnit.Dto>,
    ): Double {
        val setUpEffectIds = targetEffects.filter { effect -> effect.outcome.type == TypeDto.RESIST }.map(Effect.Dto::id).toSet()
        val followUps =
            caster.knownAbilities.keys
                .filter { abilityId -> abilityId != ability.id }
                .mapNotNull { abilityId -> searchAbilityById(abilityId) }
                .map { followUp -> effectsOf(followUp).filter { (target, _) -> target == EffectTargetDto.Type.TARGETED_BATTLE_UNIT }.map { (_, effect) -> effect } }
                .filter { followUpEffects -> followUpEffects.any { effect -> effect.outcome.applyIfOngoing?.ongoingEffectId in setUpEffectIds } }
        val targetsUtility =
            targets(castGroup, enemiesWithinReach)
                .sumOf { target ->
                    val ongoing = ongoingEffectIds(target)
                    followUps.maxOfOrNull { followUpEffects ->
                        harmUtility(harm(followUpEffects, target, ongoing + setUpEffectIds), target) - harmUtility(harm(followUpEffects, target, ongoing), target)
                    } ?: 0.0
                }.coerceAtMost(1.0)
        val casterHealthRatio = caster.remainingHealth.toDouble() / casterMaxHealth
        return casterHealthRatio * PLAYER_TURN_DISCOUNT * targetsUtility
    }

    private fun targets(
        castGroup: WhereCanCast.CastGroup,
        enemiesWithinReach: List<BattleUnit.Dto>,
    ): List<BattleUnit.Dto> {
        val enemyIdsWithinReach = enemiesWithinReach.map(BattleUnit.Dto::id).toSet()
        return castGroup.positions
            .mapNotNull { position -> searchOccupant(row = position.row, column = position.column) }
            .filter { targetId -> targetId in enemyIdsWithinReach }
            .mapNotNull { targetId -> searchBattleUnitById(id = targetId) }
    }

    private fun ongoingEffectIds(battleUnit: BattleUnit.Dto): Set<String> =
        (battleUnit.ongoingEffects.onPlayerTurnStarted + battleUnit.ongoingEffects.whileOngoing + battleUnit.ongoingEffects.onDefeatedEffects).toSet()

    private data class Harm(
        val damage: Double,
        val playerTurnsNegated: Int,
        val damageToOthers: Map<String, Double>,
    )

    private fun harm(
        effects: List<Effect.Dto>,
        target: BattleUnit.Dto,
        ongoingEffectIds: Set<String>,
        nesting: Int = 0,
    ): Harm {
        var applied = ongoingEffectIds
        var damage = 0.0
        var playerTurnsNegated = 0
        val damageToOthers = mutableMapOf<String, Double>()
        effects.forEach { effect ->
            when (effect.outcome.type) {
                TypeDto.DAMAGE -> damage += discountedDamage(effect)
                TypeDto.NEGATE_ACTIONS -> playerTurnsNegated = maxOf(playerTurnsNegated, effect.timing.onPlayerTurnStarted?.duration ?: 0)
                TypeDto.APPLY_IF_ONGOING -> {
                    val applyIfOngoing = effect.outcome.applyIfOngoing!!
                    val appliedEffect = searchEffectById(applyIfOngoing.effectId)
                    if (applyIfOngoing.ongoingEffectId in applied && appliedEffect != null && nesting < MAXIMUM_NESTING) {
                        val nested = harm(listOf(appliedEffect), target, applied, nesting + 1)
                        damage += nested.damage
                        playerTurnsNegated = maxOf(playerTurnsNegated, nested.playerTurnsNegated)
                        nested.damageToOthers.forEach { (battleUnitId, amount) -> damageToOthers[battleUnitId] = (damageToOthers[battleUnitId] ?: 0.0) + amount }
                        applied = applied + appliedEffect.id
                    }
                }
                TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES ->
                    if (effect.timing.type == TimingTypeDto.IMMEDIATELY) {
                        val spreadDamage = searchEffectById(effect.outcome.applyEffectToAdjacentAllies!!.effectId)?.let(::discountedDamage) ?: 0.0
                        adjacentAllies(target).forEach { ally -> damageToOthers[ally.id] = (damageToOthers[ally.id] ?: 0.0) + spreadDamage }
                    }
                else -> Unit
            }
            applied = applied + effect.id
        }
        return Harm(damage = damage, playerTurnsNegated = playerTurnsNegated, damageToOthers = damageToOthers)
    }

    private fun harmUtility(
        harm: Harm,
        target: BattleUnit.Dto,
    ): Double {
        val preventedDamage = bestDamage(target) * (1..harm.playerTurnsNegated).sumOf { turn -> PLAYER_TURN_DISCOUNT.pow(turn) }
        val targetUtility = min(harm.damage + preventedDamage, target.remainingHealth.toDouble()) / target.remainingHealth
        val othersUtility =
            harm.damageToOthers.entries.sumOf { (battleUnitId, damage) ->
                val other = searchBattleUnitById(id = battleUnitId) ?: return@sumOf 0.0
                min(damage, other.remainingHealth.toDouble()) / other.remainingHealth
            }
        return targetUtility + othersUtility
    }

    private fun bestDamage(battleUnit: BattleUnit.Dto): Double =
        battleUnit.knownAbilities.keys
            .mapNotNull { abilityId -> searchAbilityById(abilityId) }
            .maxOfOrNull { ability -> expectedAmount(effectsOf(ability), target = EffectTargetDto.Type.TARGETED_BATTLE_UNIT, outcome = TypeDto.DAMAGE) }
            ?: 0.0

    private fun adjacentAllies(battleUnit: BattleUnit.Dto): List<BattleUnit.Dto> {
        val position = searchPosition(occupantId = battleUnit.id) ?: return emptyList()
        return listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
            .mapNotNull { (rowOffset, columnOffset) -> searchOccupant(row = position.row + rowOffset, column = position.column + columnOffset) }
            .mapNotNull { occupantId -> searchBattleUnitById(id = occupantId) }
            .filter { occupant -> occupant.playerId == battleUnit.playerId && occupant.remainingHealth > 0 }
    }

    private fun discountedDamage(effect: Effect.Dto): Double {
        val amount = effect.outcome.damage?.damage ?: return 0.0
        return when (effect.timing.type) {
            TimingTypeDto.IMMEDIATELY -> amount.toDouble()
            TimingTypeDto.ON_PLAYER_TURN_STARTED -> {
                val turns = effect.timing.onPlayerTurnStarted?.duration ?: 0
                (1..turns).sumOf { turn -> amount * PLAYER_TURN_DISCOUNT.pow(turn) }
            }
            else -> 0.0
        }
    }

    private fun healUtility(
        healing: Double,
        caster: BattleUnit.Dto,
        casterMaxHealth: Int,
    ): Double {
        val missingHealth = (casterMaxHealth - caster.remainingHealth).toDouble()
        val usefulHealing = min(healing, missingHealth)
        return healingNeed(battleUnitId = caster.id) * usefulHealing / healing
    }

    private fun summonUtility(
        deployedUnitId: String,
        castGroup: WhereCanCast.CastGroup,
        caster: BattleUnit.Dto,
        casterMaxHealth: Int,
        enemiesWithinReach: List<BattleUnit.Dto>,
    ): Double {
        val deployedUnit = searchUnitById(id = deployedUnitId) ?: return 0.0
        val damage =
            deployedUnit.startingAbilities
                .mapNotNull { abilityId -> searchAbilityById(abilityId) }
                .maxOfOrNull { ability -> expectedAmount(effectsOf(ability), target = EffectTargetDto.Type.TARGETED_BATTLE_UNIT, outcome = TypeDto.DAMAGE) }
                ?: 0.0
        val tile = castGroup.positions.firstOrNull() ?: return 0.0
        val nearestEnemy =
            enemiesWithinReach
                .mapNotNull { enemy ->
                    searchPosition(occupantId = enemy.id)?.let { position -> enemy to abs(position.row - tile.row) + abs(position.column - tile.column) }
                }.minByOrNull { (_, distance) -> distance }
        if (damage <= 0 || nearestEnemy == null) return 0.0
        val (enemy, distance) = nearestEnemy
        val tilesToWalk = distance - 1
        if (tilesToWalk > 0 && deployedUnit.maximumSteps <= 0) return 0.0
        val playerTurnsToDamage = if (tilesToWalk <= 0) 1 else (tilesToWalk + deployedUnit.maximumSteps - 1) / deployedUnit.maximumSteps
        val casterHealthRatio = caster.remainingHealth.toDouble() / casterMaxHealth
        return casterHealthRatio * PLAYER_TURN_DISCOUNT.pow(playerTurnsToDamage) * min(damage, enemy.remainingHealth.toDouble()) / enemy.remainingHealth
    }

    private fun expectedAmount(
        effects: List<Pair<EffectTargetDto.Type, Effect.Dto>>,
        target: EffectTargetDto.Type,
        outcome: TypeDto,
    ): Double =
        effects
            .filter { (effectTarget, effect) -> effectTarget == target && effect.outcome.type == outcome }
            .sumOf { (_, effect) ->
                val amount =
                    when (outcome) {
                        TypeDto.DAMAGE -> effect.outcome.damage?.damage ?: 0
                        TypeDto.HEAL -> effect.outcome.heal?.healing ?: 0
                        else -> 0
                    }
                when (effect.timing.type) {
                    TimingTypeDto.IMMEDIATELY -> amount.toDouble()
                    TimingTypeDto.ON_PLAYER_TURN_STARTED -> {
                        val turns = effect.timing.onPlayerTurnStarted?.duration ?: 0
                        (1..turns).sumOf { turn -> amount * PLAYER_TURN_DISCOUNT.pow(turn) }
                    }
                    else -> 0.0
                }
            }

    private companion object {
        const val PLAYER_TURN_DISCOUNT = 0.9
        const val MAXIMUM_NESTING = 5
        val HARMFUL_OUTCOMES = setOf(TypeDto.DAMAGE, TypeDto.NEGATE_ACTIONS, TypeDto.APPLY_IF_ONGOING, TypeDto.APPLY_EFFECT_TO_ADJACENT_ALLIES)
    }
}
