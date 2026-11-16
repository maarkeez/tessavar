package com.mkz.tessavar.world.adapters.resources

import com.mkz.tessavar.ability.adapters.presentation.AbilityApi
import com.mkz.tessavar.ability.adapters.resources.ResourcesAbilityLoader
import com.mkz.tessavar.battle.adapters.presentation.BattleApi
import com.mkz.tessavar.battleUnit.adapters.presentation.BattleUnitApi
import com.mkz.tessavar.battleUnit.adapters.resources.ResourcesBattleUnitLoader
import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.building.adapters.presentation.BuildingApi
import com.mkz.tessavar.building.adapters.resources.ResourcesBuildingLoader
import com.mkz.tessavar.cpuBattleBrain.adapters.presentation.CpuBattleBrainApi
import com.mkz.tessavar.cpuWanderBrain.adapters.presentation.CpuWanderBrainApi
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
import com.mkz.tessavar.quest.adapters.presentation.QuestApi
import com.mkz.tessavar.quest.adapters.resources.ResourcesQuestLoader
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.domain.FixedDoublesRandom
import com.mkz.tessavar.shared.domain.subscribe
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.unit.adapters.presentation.UnitApi
import com.mkz.tessavar.unit.adapters.resources.ResourcesUnitLoader
import com.mkz.tessavar.world.adapters.presentation.WorldApi
import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.worldBuilder.adapters.presentation.WorldBuilderApi
import com.mkz.tessavar.worldBuilder.adapters.resources.ResourcesCatalog
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.UnplayableReasonDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import korlibs.io.file.VfsFile
import korlibs.korge.service.storage.InmemoryStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class WorldResourcesTest {
    private suspend fun worldFiles(): Map<String, String> =
        ResourcesBundle.root[ResourcesWorldLoader.WORLD_FOLDER]
            .listNames()
            .filter { fileName -> fileName.endsWith(".toml") }
            .sorted()
            .associate { fileName -> fileName.removeSuffix(".toml") to ResourcesBundle.root["${ResourcesWorldLoader.WORLD_FOLDER}/$fileName"].readString() }

    private suspend fun worlds(): List<World.Dto> = ResourcesWorldLoader().also { loader -> loader.initResources() }.loadWorlds()

    private data class Imported(
        val rejections: List<WorldImportRejected>,
        val unplayableReasons: List<UnplayableReasonDto>,
    )

    private suspend fun import(worldFile: String): Imported {
        val eventBus = InMemoryEventBus()
        val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
        val battlefieldApi = BattlefieldApi(terrainApi, eventBus, ResourcesBattlefieldLoader(), TomlBattlefieldEncoder(), TomlBattlefieldDecoder())
        val worldBuilderApi = WorldBuilderApi(terrainApi, battlefieldApi, eventBus, InmemoryStorage(), ResourcesCatalog())
        terrainApi.init()
        terrainApi.initialiseTerrains()
        worldBuilderApi.init()
        val rejections = mutableListOf<WorldImportRejected>()
        eventBus.subscribe<WorldImportRejected> { event -> rejections += event }
        worldBuilderApi.openWorld()
        worldBuilderApi.importWorld(worldFile)
        eventBus.dispatch()
        return Imported(rejections, requireNotNull(worldBuilderApi.searchWorld()).unplayableReasons)
    }

    @Test
    fun `Every world file should be of world version 1`() =
        runTest {
            // When
            val otherVersions = worlds().filter { world -> world.version != World.CURRENT_VERSION }.map { world -> "${world.id}: ${world.version}" }
            // Then
            otherVersions.shouldBeEmpty()
        }

    @Test
    fun `Every world file should be named lowercase words joined by hyphens, other than playtest`() =
        runTest {
            // When
            val worldIds = worldFiles().keys
            // Then
            worldIds.filterNot(World::isValidId).shouldBeEmpty()
            worldIds.filter { worldId -> worldId == World.PLAYTEST_ID }.shouldBeEmpty()
            worldIds.shouldContain(World.CAMPAIGN_ID)
        }

    @Test
    fun `Every world file should be named after its world's name, as Export names it`() =
        runTest {
            // When
            val misnamed = worlds().filter { world -> World.idOf(world.name) != world.id }.map { world -> "${world.id}: ${world.name}" }
            // Then
            misnamed.shouldBeEmpty()
        }

    @Test
    fun `Every world file should have a name and an author`() =
        runTest {
            // When
            val unnamed = worlds().filter { world -> world.name.isBlank() || world.author.isBlank() }.map(World.Dto::id)
            // Then
            unnamed.shouldBeEmpty()
        }

    @Test
    fun `Every world file's terrain, occupants and quests should be imported by the world builder, each occupant on a vacant and occupiable tile`() =
        runTest {
            // When
            val rejected = worldFiles().mapValues { (_, worldFile) -> import(worldFile).rejections }.filterValues { rejections -> rejections.isNotEmpty() }
            // Then
            withClue("World files the world builder rejects: $rejected") { rejected.shouldBeEmpty() }
        }

    @Test
    fun `Every world file should be playable`() =
        runTest {
            // When
            val unplayable = worldFiles().mapValues { (_, worldFile) -> import(worldFile).unplayableReasons }.filterValues { reasons -> reasons.isNotEmpty() }
            // Then
            withClue("World files that are not playable: $unplayable") { unplayable.shouldBeEmpty() }
        }

    @Test
    fun `Every world file should set up a game, every battle unit, npc and building standing where the world file says`() =
        runTest {
            // When
            val failures =
                ResourcesBundle.root[ResourcesWorldLoader.WORLD_FOLDER]
                    .listNames()
                    .filter { fileName -> fileName.endsWith(".toml") }
                    .mapNotNull { fileName ->
                        val worldId = fileName.removeSuffix(".toml")
                        runCatching { setUpGame(worldId, ResourcesBundle.root["${ResourcesWorldLoader.WORLD_FOLDER}/$fileName"]) }
                            .exceptionOrNull()
                            ?.let { error -> "$worldId: $error" }
                    }
            // Then
            failures shouldBe emptyList()
        }

    private suspend fun setUpGame(
        worldId: String,
        worldFile: VfsFile,
    ) {
        val eventBus = InMemoryEventBus()
        val terrainApi = TerrainApi(eventBus, ResourcesTerrainLoader())
        val unitApi = UnitApi(eventBus, ResourcesUnitLoader())
        val playerApi = PlayerApi(eventBus, ResourcesPlayerLoader())
        val battlefieldApi = BattlefieldApi(terrainApi, eventBus, ResourcesBattlefieldLoader(worldFile), TomlBattlefieldEncoder(), TomlBattlefieldDecoder())
        val buildingApi = BuildingApi(battlefieldApi, eventBus, ResourcesBuildingLoader(worldFile))
        val effectApi = EffectApi(eventBus, ResourcesEffectLoader())
        val abilityApi = AbilityApi(effectApi, eventBus, ResourcesAbilityLoader())
        val lootableItemApi = LootableItemApi(effectApi, eventBus, ResourcesLootableItemLoader())
        val lootTableApi = LootTableApi(lootableItemApi, eventBus, ResourcesLootTableLoader())
        val battleUnitApi = BattleUnitApi(effectApi, abilityApi, unitApi, playerApi, battlefieldApi, eventBus, ResourcesBattleUnitLoader(worldFile))
        val lootDropApi = LootDropApi(unitApi, lootTableApi, eventBus, ResourcesLootDropLoader(), FixedDoublesRandom.cpuBattleUnitsAlwaysResting())
        val battleApi = BattleApi(eventBus)
        val cpuBattleBrainApi = CpuBattleBrainApi(unitApi, playerApi, battleUnitApi, battlefieldApi, terrainApi, effectApi, abilityApi, eventBus, FixedDoublesRandom.cpuBattleUnitsAlwaysResting())
        val partyApi = PartyApi(playerApi, battleUnitApi, battlefieldApi, eventBus)
        CpuWanderBrainApi(playerApi, battleUnitApi, battlefieldApi, cpuBattleBrainApi, partyApi, eventBus)
        val npcApi = NpcApi(battlefieldApi, lootableItemApi, partyApi, eventBus, ResourcesNpcLoader(worldFile))
        InventoryApi(effectApi, lootableItemApi, unitApi, battleApi, partyApi, npcApi, eventBus)
        val questApi = QuestApi(battlefieldApi, partyApi, eventBus, ResourcesQuestLoader(worldFile))
        val worldApi = WorldApi(eventBus, ResourcesWorldLoader())
        terrainApi.init()
        effectApi.init()
        lootableItemApi.init()
        lootTableApi.init()
        abilityApi.init()
        unitApi.init()
        lootDropApi.init()
        playerApi.init()
        battlefieldApi.init()
        buildingApi.init()
        battleUnitApi.init()
        npcApi.init()
        questApi.init()
        worldApi.init()
        worldApi.setUpGame(worldId)
        eventBus.dispatch()
    }
}
