package com.mkz.tessavar.shared.usecases.acceptance

import com.mkz.tessavar.ability.adapters.presentation.AbilityApi
import com.mkz.tessavar.ability.adapters.resources.ResourcesAbilityLoader
import com.mkz.tessavar.battle.adapters.presentation.BattleApi
import com.mkz.tessavar.battle.adapters.presentation.startBattleWithEveryBattleUnit
import com.mkz.tessavar.battle.domain.BattleEvent
import com.mkz.tessavar.battleUnit.adapters.presentation.BattleUnitApi
import com.mkz.tessavar.battleUnit.adapters.resources.ResourcesBattleUnitLoader
import com.mkz.tessavar.battleUnit.domain.BattleUnit.Dto.OngoingEffectDto
import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.cpuBattleBrain.adapters.presentation.CpuBattleBrainApi
import com.mkz.tessavar.cpuWanderBrain.adapters.presentation.CpuWanderBrainApi
import com.mkz.tessavar.effect.adapters.presentation.EffectApi
import com.mkz.tessavar.effect.adapters.resources.ResourcesEffectLoader
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto.SourceDto
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto
import com.mkz.tessavar.party.adapters.presentation.PartyApi
import com.mkz.tessavar.player.adapters.presentation.PlayerApi
import com.mkz.tessavar.player.adapters.resources.ResourcesPlayerLoader
import com.mkz.tessavar.player.usecases.commands.RequestPlayerCreation.PlayerType.CPU
import com.mkz.tessavar.player.usecases.commands.RequestPlayerCreation.PlayerType.HUMAN
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.shared.domain.FixedDoublesRandom
import com.mkz.tessavar.shared.domain.subscribe
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.unit.adapters.presentation.UnitApi
import com.mkz.tessavar.unit.domain.UnitMother
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EffectInteractionsAcceptanceTest {
    private val eventBus = InMemoryEventBus()
    private val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
    private val unitApi = UnitApi(eventBus, AcceptanceUnitLoader())
    private val playerApi = PlayerApi(eventBus, ResourcesPlayerLoader())
    private val battlefieldApi = BattlefieldApi(terrainApi, eventBus, ResourcesBattlefieldLoader(), TomlBattlefieldEncoder(), TomlBattlefieldDecoder())
    private val effectApi = EffectApi(eventBus, ResourcesEffectLoader())
    private val abilityApi = AbilityApi(effectApi, eventBus, ResourcesAbilityLoader())
    private val battleUnitApi = BattleUnitApi(effectApi, abilityApi, unitApi, playerApi, battlefieldApi, eventBus, ResourcesBattleUnitLoader())
    private val battleApi = BattleApi(eventBus)
    private val cpuBattleBrainApi = CpuBattleBrainApi(unitApi, playerApi, battleUnitApi, battlefieldApi, terrainApi, effectApi, abilityApi, eventBus, FixedDoublesRandom.cpuBattleUnitsAlwaysResting())
    private val partyApi = PartyApi(playerApi, battleUnitApi, battlefieldApi, eventBus)
    private val cpuWanderBrainApi = CpuWanderBrainApi(playerApi, battleUnitApi, battlefieldApi, cpuBattleBrainApi, partyApi, eventBus)

    private val humanPlayerId = "human"
    private val cpuPlayerId = "cpu"

    private suspend fun initialise() {
        terrainApi.init()
        effectApi.init()
        abilityApi.init()
        unitApi.init()
        terrainApi.initialiseTerrains()
        effectApi.initialiseEffects()
        abilityApi.initialiseAbilities()
        unitApi.initialiseUnits()
        unitApi.requestUnitCreation(UnitMother.unit(id = "dummy", baseMaximumHealth = 100, baseMaximumMana = 0, startingAbilities = emptyList(), maximumSteps = 1).toDto())
        playerApi.requestPlayerCreation(humanPlayerId, HUMAN)
        playerApi.requestPlayerCreation(cpuPlayerId, CPU)
    }

    private suspend fun setUp() {
        initialise()
        battlefieldApi.requestBattlefieldCreation(8, 8, List(8) { List(8) { "sand" } })
        battleUnitApi.deployBattleUnit("water-bat-1", "water-bat", humanPlayerId, deployAtRow = 2, deployAtColumn = 1)
        battleUnitApi.deployBattleUnit("storm-bat-1", "storm-bat", humanPlayerId, deployAtRow = 2, deployAtColumn = 3)
        battleUnitApi.deployBattleUnit("fire-bat-1", "fire-bat", humanPlayerId, deployAtRow = 1, deployAtColumn = 2)
        battleUnitApi.deployBattleUnit("dummy-1", "dummy", cpuPlayerId, deployAtRow = 2, deployAtColumn = 2)
        battleUnitApi.deployBattleUnit("dummy-2", "dummy", cpuPlayerId, deployAtRow = 1, deployAtColumn = 3)
        battleUnitApi.deployBattleUnit("dummy-4", "dummy", cpuPlayerId, deployAtRow = 3, deployAtColumn = 2)
        battleApi.startBattleWithEveryBattleUnit(battleUnitApi, humanPlayerId, cpuPlayerId)
        eventBus.dispatch()
    }

    private suspend fun setUpOnLava() {
        initialise()
        battlefieldApi.requestBattlefieldCreation(8, 8, List(8) { row -> List(8) { column -> if (row == 0 && column < 2) "lava" else "sand" } })
        battleUnitApi.deployBattleUnit("soaked-1", "dummy", humanPlayerId, deployAtRow = 0, deployAtColumn = 0)
        battleUnitApi.deployBattleUnit("dry-1", "dummy", humanPlayerId, deployAtRow = 0, deployAtColumn = 1)
        battleUnitApi.deployBattleUnit("dummy-1", "dummy", cpuPlayerId, deployAtRow = 2, deployAtColumn = 2)
        battleApi.startBattleWithEveryBattleUnit(battleUnitApi, humanPlayerId, cpuPlayerId)
        eventBus.dispatch()
    }

    private fun cast(
        casterId: String,
        abilityId: String,
        targetId: String,
    ) {
        val target = battlefieldApi.searchPosition(targetId)!!
        val castGroup = battleUnitApi.whereCanCast(casterId, abilityId).single { group -> group.positions.any { position -> position.row == target.row && position.column == target.column } }
        battleUnitApi.castAbility(battleUnitId = casterId, abilityId = abilityId, castGroup = castGroup)
        eventBus.dispatch()
    }

    private fun apply(
        effectId: String,
        recipientId: String,
    ) {
        eventBus.publish(
            BattleUnitEvent.RequestApplyEffect(
                EffectApplicationDto(source = SourceDto.battleUnit("nobody"), recipient = RecipientDto.battleUnit(recipientId), effectId = effectId, destination = null),
            ),
        )
        eventBus.dispatch()
    }

    private fun playARound() {
        battleApi.finishPlayerTurn()
        eventBus.dispatch()
    }

    private fun castsLeftAfterTheCpuTurn(battleUnitId: String): Int = battleUnitApi.searchBattleUnitById(battleUnitId)!!.remainingActions.remainingCasts

    private fun effectsAtEachCpuPlayerTurnStart(battleUnitId: String): List<List<OngoingEffectDto>> =
        mutableListOf<List<OngoingEffectDto>>().also { effects ->
            eventBus.subscribe<BattleEvent.PlayerTurnStarted> { event ->
                if (event.playerId == cpuPlayerId) effects += battleUnitApi.searchBattleUnitById(battleUnitId)!!.ongoingEffects.effects
            }
        }

    @Test
    fun `The soaked battle unit should show stun-long and soak at 0 through its second stunned player turn when Thunder is cast after splash`() =
        scenarioTest(::setUp) {
            // Given
            val effectsAtEachTurnStart = effectsAtEachCpuPlayerTurnStart("dummy-1")
            cast(casterId = "water-bat-1", abilityId = "splash", targetId = "dummy-1")
            cast(casterId = "storm-bat-1", abilityId = "thunder", targetId = "dummy-1")
            // When
            playARound()
            playARound()
            // Then
            effectsAtEachTurnStart.shouldContainExactly(
                listOf(OngoingEffectDto("soak", 1), OngoingEffectDto("stun", 0), OngoingEffectDto("stun-long", 1)),
                listOf(OngoingEffectDto("soak", 0), OngoingEffectDto("stun-long", 0)),
            )
            battleUnitApi
                .searchBattleUnitById("dummy-1")!!
                .ongoingEffects.effects
                .shouldContainExactly()
        }

    @Test
    fun `Thunder should stun the soaked enemy for 2 player turns when it is cast after splash`() =
        scenarioTest(::setUp) {
            // Given
            cast(casterId = "water-bat-1", abilityId = "splash", targetId = "dummy-1")
            // When
            cast(casterId = "storm-bat-1", abilityId = "thunder", targetId = "dummy-1")
            // Then
            battleUnitApi
                .searchBattleUnitById("dummy-1")!!
                .ongoingEffects.onPlayerTurnStarted
                .shouldContainExactly("stun", "stun-long")
            playARound()
            castsLeftAfterTheCpuTurn("dummy-1") shouldBe 0
            playARound()
            castsLeftAfterTheCpuTurn("dummy-1") shouldBe 0
            playARound()
            castsLeftAfterTheCpuTurn("dummy-1") shouldBe 1
        }

    @Test
    fun `Thunder should stun the dry enemy for 1 player turn`() =
        scenarioTest(::setUp) {
            // When
            cast(casterId = "storm-bat-1", abilityId = "thunder", targetId = "dummy-2")
            // Then
            battleUnitApi
                .searchBattleUnitById("dummy-2")!!
                .ongoingEffects.onPlayerTurnStarted
                .shouldContainExactly("stun")
            playARound()
            castsLeftAfterTheCpuTurn("dummy-2") shouldBe 0
            playARound()
            castsLeftAfterTheCpuTurn("dummy-2") shouldBe 1
        }

    @Test
    fun `Fireball should spread the venom to the adjacent allies when the enemy has an ongoing venom`() =
        scenarioTest(::setUp) {
            // Given
            apply(effectId = "venom-damage", recipientId = "dummy-1")
            // When
            cast(casterId = "fire-bat-1", abilityId = "fireball", targetId = "dummy-1")
            // Then
            battleUnitApi.searchBattleUnitById("dummy-1")!!.ongoingEffects.onPlayerTurnStarted shouldContain "burn"
            battleUnitApi.searchBattleUnitById("dummy-4")!!.ongoingEffects.onPlayerTurnStarted shouldContain "venom-damage"
            battleUnitApi.searchBattleUnitById("dummy-2")!!.ongoingEffects.onPlayerTurnStarted shouldNotContain "venom-damage"
        }

    @Test
    fun `Fireball should spread no venom when the enemy has none`() =
        scenarioTest(::setUp) {
            // When
            cast(casterId = "fire-bat-1", abilityId = "fireball", targetId = "dummy-1")
            // Then
            battleUnitApi
                .searchBattleUnitById("dummy-1")!!
                .ongoingEffects.onPlayerTurnStarted
                .shouldContainExactly("burn")
            battleUnitApi.searchBattleUnitById("dummy-4")!!.ongoingEffects.onPlayerTurnStarted shouldNotContain "venom-damage"
        }

    @Test
    fun `Lava should burn the battle unit standing on it for half when it is soaked`() =
        scenarioTest(::setUpOnLava) {
            // Given
            playARound()
            val lavaDamage = 100 - battleUnitApi.searchBattleUnitById("dry-1")!!.remainingHealth
            apply(effectId = "soak", recipientId = "soaked-1")
            // When
            playARound()
            // Then
            val dryDamage = 100 - lavaDamage - battleUnitApi.searchBattleUnitById("dry-1")!!.remainingHealth
            val soakedDamage = 100 - lavaDamage - battleUnitApi.searchBattleUnitById("soaked-1")!!.remainingHealth
            dryDamage shouldBe 5 + 4
            soakedDamage shouldBe 5 + 2
        }
}
