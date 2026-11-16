package com.mkz.tessavar.world.adapters.resources

import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.shared.adapters.events.InMemoryEventBus
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.shared.domain.subscribe
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.terrain.adapters.resources.ResourcesTerrainLoader
import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.worldBuilder.adapters.presentation.WorldBuilderApi
import com.mkz.tessavar.worldBuilder.adapters.resources.ResourcesCatalog
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.UnplayableReasonDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.maps.shouldBeEmpty
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
}
