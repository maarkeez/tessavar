package com.mkz.tessavar.shared.usecases.acceptance

import com.mkz.tessavar.ability.adapters.presentation.AbilityApi
import com.mkz.tessavar.ability.adapters.resources.ResourcesAbilityLoader
import com.mkz.tessavar.battle.adapters.presentation.BattleApi
import com.mkz.tessavar.battleUnit.adapters.presentation.BattleUnitApi
import com.mkz.tessavar.battleUnit.adapters.resources.ResourcesBattleUnitLoader
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.effect.adapters.presentation.EffectApi
import com.mkz.tessavar.effect.adapters.resources.ResourcesEffectLoader
import com.mkz.tessavar.inventory.adapters.presentation.InventoryApi
import com.mkz.tessavar.lootDrop.adapters.presentation.LootDropApi
import com.mkz.tessavar.lootDrop.adapters.resources.ResourcesLootDropLoader
import com.mkz.tessavar.lootTable.adapters.presentation.LootTableApi
import com.mkz.tessavar.lootTable.adapters.resources.ResourcesLootTableLoader
import com.mkz.tessavar.lootableItem.adapters.presentation.LootableItemApi
import com.mkz.tessavar.lootableItem.adapters.resources.ResourcesLootableItemLoader
import com.mkz.tessavar.npc.adapters.presentation.NpcApi
import com.mkz.tessavar.npc.adapters.resources.ResourcesNpcLoader
import com.mkz.tessavar.party.adapters.presentation.PartyApi
import com.mkz.tessavar.player.adapters.presentation.PlayerApi
import com.mkz.tessavar.player.adapters.resources.ResourcesPlayerLoader
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.unit.adapters.presentation.UnitApi
import com.mkz.tessavar.unit.domain.UnitMother
import com.mkz.tessavar.world.adapters.presentation.WorldApi
import com.mkz.tessavar.world.adapters.resources.ResourcesWorldLoader
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

class PunchEssenceAcceptanceTest {
    private val eventBus = InMemoryEventBus()
    private val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
    private val unitApi = UnitApi(eventBus, AcceptanceUnitLoader())
    private val playerApi = PlayerApi(eventBus, ResourcesPlayerLoader())
    private val battlefieldApi = BattlefieldApi(terrainApi, eventBus, ResourcesBattlefieldLoader(), TomlBattlefieldEncoder(), TomlBattlefieldDecoder())
    private val effectApi = EffectApi(eventBus, ResourcesEffectLoader())
    private val abilityApi = AbilityApi(effectApi, eventBus, ResourcesAbilityLoader())
    private val lootableItemApi = LootableItemApi(effectApi, eventBus, ResourcesLootableItemLoader())
    private val lootTableApi = LootTableApi(lootableItemApi, eventBus, ResourcesLootTableLoader())
    private val battleUnitApi = BattleUnitApi(effectApi, abilityApi, unitApi, playerApi, battlefieldApi, eventBus, ResourcesBattleUnitLoader())
    private val lootDropApi = LootDropApi(unitApi, lootTableApi, eventBus, ResourcesLootDropLoader(), Random.Default)
    private val battleApi = BattleApi(eventBus)
    private val partyApi = PartyApi(playerApi, battleUnitApi, battlefieldApi, eventBus)
    private val npcApi = NpcApi(battlefieldApi, lootableItemApi, partyApi, eventBus, ResourcesNpcLoader())
    private val inventoryApi = InventoryApi(effectApi, lootableItemApi, unitApi, battleApi, partyApi, npcApi, eventBus)
    private val worldApi = WorldApi(eventBus, ResourcesWorldLoader())

    private val vikingBattleUnitId = "viking-1"
    private val vikingRow = 2
    private val vikingColumn = 2
    private val enemyBattleUnitId = "dummy-1"
    private val enemyHealth = 100

    private suspend fun setUp() {
        terrainApi.init()
        effectApi.init()
        lootableItemApi.init()
        lootTableApi.init()
        abilityApi.init()
        unitApi.init()
        lootDropApi.init()
        playerApi.init()
        battlefieldApi.init()
        battleUnitApi.init()
        worldApi.setUpGame()
        eventBus.dispatch()
        unitApi.requestUnitCreation(UnitMother.unit(id = "dummy", baseMaximumHealth = enemyHealth, baseMaximumMana = 0, startingAbilities = emptyList(), maximumSteps = 1).toDto())
        battleUnitApi.deployBattleUnit(
            battleUnitId = vikingBattleUnitId,
            unitId = "viking",
            playerId = "player-one",
            deployAtRow = vikingRow,
            deployAtColumn = vikingColumn,
        )
        eventBus.dispatch()
        partyApi.joinParty(vikingBattleUnitId)
        eventBus.dispatch()
    }

    private fun consumeEssence(lootableItemId: String) {
        lootDropApi.dropItemWithoutRolling(unitId = "rat", lootableItemId = lootableItemId, row = vikingRow, column = vikingColumn)
        inventoryApi.storeLootedItem(battleUnitId = vikingBattleUnitId, lootableItemId = lootableItemId, row = vikingRow, column = vikingColumn)
        inventoryApi.consumeItem(battleUnitId = vikingBattleUnitId, lootableItemId = lootableItemId)
        eventBus.dispatch()
    }

    private fun castOnTheAdjacentEnemy(abilityId: String) {
        consumeEssence("$abilityId-essence")
        battleUnitApi.deployBattleUnit(enemyBattleUnitId, "dummy", "player-two", deployAtRow = vikingRow, deployAtColumn = vikingColumn + 1)
        eventBus.dispatch()
        battleApi.startBattle(
            engagingPlayerId = "player-one",
            engagedPlayerId = "player-two",
            engagingBattleUnitIds = listOf(vikingBattleUnitId),
            engagedBattleUnitIds = listOf(enemyBattleUnitId),
        )
        eventBus.dispatch()
        val castGroup =
            battleUnitApi.whereCanCast(vikingBattleUnitId, abilityId).single { group ->
                group.positions.any { position -> position.row == vikingRow && position.column == vikingColumn + 1 }
            }
        battleUnitApi.castAbility(battleUnitId = vikingBattleUnitId, abilityId = abilityId, castGroup = castGroup)
        eventBus.dispatch()
    }

    @Test
    fun `The viking should learn each punch for good when the viking consumes its essence`() =
        scenarioTest(::setUp) {
            // Given
            require(
                battleUnitApi
                    .searchBattleUnitById(vikingBattleUnitId)!!
                    .knownAbilities.keys
                    .toList() == listOf("sword"),
            )
            // When
            consumeEssence("fire-punch-essence")
            consumeEssence("thunder-punch-essence")
            consumeEssence("water-punch-essence")
            // Then
            val viking = battleUnitApi.searchBattleUnitById(vikingBattleUnitId)!!
            viking.knownAbilities.toList().shouldContainExactly("sword" to 0, "fire-punch" to 0, "thunder-punch" to 0, "water-punch" to 0)
            inventoryApi.searchInventoryByBattleUnitId(vikingBattleUnitId)!!.storedItems.shouldBeEmpty()
        }

    @Test
    fun `Fire Punch should deal 5 damage to the adjacent enemy and burn it when the viking casts it`() =
        scenarioTest(::setUp) {
            // When
            castOnTheAdjacentEnemy("fire-punch")
            // Then
            val enemy = battleUnitApi.searchBattleUnitById(enemyBattleUnitId)!!
            enemy.remainingHealth shouldBe enemyHealth - 5
            enemy.ongoingEffects.onPlayerTurnStarted shouldContain "burn"
        }

    @Test
    fun `Thunder Punch should deal 5 damage to the adjacent enemy and stun it when the viking casts it`() =
        scenarioTest(::setUp) {
            // When
            castOnTheAdjacentEnemy("thunder-punch")
            // Then
            val enemy = battleUnitApi.searchBattleUnitById(enemyBattleUnitId)!!
            enemy.remainingHealth shouldBe enemyHealth - 5
            enemy.ongoingEffects.onPlayerTurnStarted.shouldContainExactly("stun")
        }

    @Test
    fun `Water Punch should deal 5 damage to the adjacent enemy and soak it when the viking casts it`() =
        scenarioTest(::setUp) {
            // When
            castOnTheAdjacentEnemy("water-punch")
            // Then
            val enemy = battleUnitApi.searchBattleUnitById(enemyBattleUnitId)!!
            enemy.remainingHealth shouldBe enemyHealth - 5
            enemy.ongoingEffects.whileOngoing shouldContain "soak"
        }
}
