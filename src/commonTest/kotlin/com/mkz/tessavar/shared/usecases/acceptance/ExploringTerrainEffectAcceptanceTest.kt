package com.mkz.tessavar.shared.usecases.acceptance

import com.mkz.tessavar.ability.adapters.presentation.AbilityApi
import com.mkz.tessavar.ability.adapters.resources.ResourcesAbilityLoader
import com.mkz.tessavar.battle.adapters.presentation.BattleApi
import com.mkz.tessavar.battleUnit.adapters.presentation.BattleUnitApi
import com.mkz.tessavar.battleUnit.adapters.resources.ResourcesBattleUnitLoader
import com.mkz.tessavar.battleUnit.domain.BattleUnitEvent
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.cpuBattleBrain.adapters.presentation.CpuBattleBrainApi
import com.mkz.tessavar.effect.adapters.presentation.EffectApi
import com.mkz.tessavar.effect.adapters.resources.ResourcesEffectLoader
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto
import com.mkz.tessavar.effect.domain.Effect.Dto.EffectApplicationDto.SourceDto
import com.mkz.tessavar.effect.domain.Effect.Dto.RecipientDto
import com.mkz.tessavar.party.adapters.presentation.PartyApi
import com.mkz.tessavar.party.domain.Party.Dto.DirectionDto
import com.mkz.tessavar.player.adapters.presentation.PlayerApi
import com.mkz.tessavar.player.adapters.resources.ResourcesPlayerLoader
import com.mkz.tessavar.player.usecases.commands.RequestPlayerCreation.PlayerType.CPU
import com.mkz.tessavar.player.usecases.commands.RequestPlayerCreation.PlayerType.HUMAN
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.unit.adapters.presentation.UnitApi
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

class ExploringTerrainEffectAcceptanceTest {
    private val eventBus = InMemoryEventBus()
    private val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
    private val unitApi = UnitApi(eventBus, AcceptanceUnitLoader())
    private val playerApi = PlayerApi(eventBus, ResourcesPlayerLoader())
    private val battlefieldApi = BattlefieldApi(terrainApi, eventBus, ResourcesBattlefieldLoader(), TomlBattlefieldEncoder(), TomlBattlefieldDecoder())
    private val effectApi = EffectApi(eventBus, ResourcesEffectLoader())
    private val abilityApi = AbilityApi(effectApi, eventBus, ResourcesAbilityLoader())
    private val battleUnitApi = BattleUnitApi(effectApi, abilityApi, unitApi, playerApi, battlefieldApi, eventBus, ResourcesBattleUnitLoader())
    private val battleApi = BattleApi(eventBus)
    private val cpuBattleBrainApi = CpuBattleBrainApi(unitApi, playerApi, battleUnitApi, battlefieldApi, terrainApi, effectApi, abilityApi, eventBus, Random.Default)
    private val partyApi = PartyApi(playerApi, battleUnitApi, battlefieldApi, eventBus)

    private val humanPlayerId = "human"
    private val cpuPlayerId = "cpu"

    private suspend fun setUp() {
        terrainApi.init()
        effectApi.init()
        abilityApi.init()
        unitApi.init()
        terrainApi.initialiseTerrains()
        effectApi.initialiseEffects()
        abilityApi.initialiseAbilities()
        unitApi.initialiseUnits()
        playerApi.requestPlayerCreation(humanPlayerId, HUMAN)
        playerApi.requestPlayerCreation(cpuPlayerId, CPU)
        val lavaTiles = setOf(6 to 1, 0 to 6)
        battlefieldApi.requestBattlefieldCreation(
            8,
            8,
            List(8) { row ->
                List(8) { column ->
                    when (row to column) {
                        in lavaTiles -> "lava"
                        0 to 0 -> "water"
                        else -> "sand"
                    }
                }
            },
        )
        battleUnitApi.deployBattleUnit("knight-1", "knight", humanPlayerId, deployAtRow = 6, deployAtColumn = 1)
        battleUnitApi.deployBattleUnit("knight-2", "knight", humanPlayerId, deployAtRow = 7, deployAtColumn = 1)
        battleUnitApi.deployBattleUnit("rat-1", "rat", cpuPlayerId, deployAtRow = 0, deployAtColumn = 6)
        partyApi.startExploration()
        eventBus.dispatch()
    }

    private fun waitASecond() {
        partyApi.applyStandingTerrainEffects()
        eventBus.dispatch()
    }

    private fun healthOf(battleUnitId: String) = battleUnitApi.searchBattleUnitById(battleUnitId)!!.remainingHealth

    @Test
    fun `should burn the battle units standing on lava, the cpu ones included, every second while exploring`() =
        scenarioTest(::setUp) {
            // When
            waitASecond()
            waitASecond()
            waitASecond()
            // Then
            healthOf("knight-1") shouldBe 100 - 5 - 9 - 13
            healthOf("knight-2") shouldBe 100
            battleUnitApi
                .searchBattleUnitById("knight-1")!!
                .ongoingEffects.onPlayerTurnStarted
                .shouldContainExactly("burn", "burn")
        }

    @Test
    fun `should soak the battle units standing on water while exploring`() =
        scenarioTest(::setUp) {
            // Given
            battleUnitApi.deployBattleUnit("knight-3", "knight", humanPlayerId, deployAtRow = 0, deployAtColumn = 0)
            eventBus.dispatch()
            // When
            waitASecond()
            // Then
            battleUnitApi
                .searchBattleUnitById("knight-3")!!
                .ongoingEffects.whileOngoing
                .shouldContainExactly("soak")
        }

    @Test
    fun `should not hurt the battle units that walked during the second, until they stand on the lava for a second`() =
        scenarioTest(::setUp) {
            // Given
            partyApi.stepParty(DirectionDto.NORTH)
            eventBus.dispatch()
            // When
            waitASecond()
            // Then
            healthOf("knight-1") shouldBe 100
            healthOf("knight-2") shouldBe 100
            // When
            waitASecond()
            // Then
            healthOf("knight-1") shouldBe 100
            healthOf("knight-2") shouldBe 95
        }

    @Test
    fun `should keep exploring when the last cpu battle unit is defeated by the lava while exploring`() =
        scenarioTest(::setUp) {
            // When
            repeat(4) { waitASecond() }
            // Then
            battlefieldApi.searchPosition("rat-1").shouldBeNull()
            battleApi.searchBattle().shouldBeNull()
            partyApi.searchParty()!!.isExploring.shouldBeTrue()
        }

    @Test
    fun `should finish the venom left from a battle when its duration runs out while exploring`() =
        scenarioTest(::setUp) {
            // Given
            eventBus.publish(
                BattleUnitEvent.RequestApplyEffect(
                    EffectApplicationDto(source = SourceDto.battleUnit("rat-1"), recipient = RecipientDto.battleUnit("knight-2"), effectId = "venom-damage", destination = null),
                ),
            )
            eventBus.dispatch()
            // When
            repeat(6) { waitASecond() }
            // Then
            healthOf("knight-2") shouldBe 85
            battleUnitApi
                .searchBattleUnitById("knight-2")!!
                .ongoingEffects.onPlayerTurnStarted
                .shouldBeEmpty()
        }
}
