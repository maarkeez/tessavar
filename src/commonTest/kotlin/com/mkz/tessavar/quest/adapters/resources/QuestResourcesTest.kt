package com.mkz.tessavar.quest.adapters.resources

import com.mkz.tessavar.lootableItem.adapters.resources.ResourcesLootableItemLoader
import com.mkz.tessavar.npc.adapters.resources.ResourcesNpcLoader
import com.mkz.tessavar.quest.domain.Quest
import com.mkz.tessavar.quest.domain.Quest.Dto.QuestTypeDto
import com.mkz.tessavar.quest.domain.Quest.Dto.TaskDto
import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import com.mkz.tessavar.unit.adapters.resources.ResourcesUnitLoader
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import korlibs.io.file.VfsFile
import korlibs.io.file.baseName
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class QuestResourcesTest {
    private suspend fun worldFiles(): List<VfsFile> =
        ResourcesBundle.root["world"]
            .listNames()
            .filter { fileName -> fileName.endsWith(".toml") }
            .map { fileName -> ResourcesBundle.root["world/$fileName"] }

    private suspend fun questsOf(worldFile: VfsFile): List<Quest.Dto> = ResourcesQuestLoader(worldFile).also { loader -> loader.initResources() }.loadQuests()

    private suspend fun quests(): List<Quest.Dto> = worldFiles().flatMap { worldFile -> questsOf(worldFile) }

    @Test
    fun `should find the unit of every defeat task in the unit resources`() =
        runTest {
            // Given
            val defeatTasks = quests().flatMap(Quest.Dto::tasks).mapNotNull(TaskDto::defeat)
            // When
            val unitIds =
                ResourcesUnitLoader()
                    .also { loader -> loader.initResources() }
                    .loadUnits()
                    .map { unit -> unit.id }
                    .toSet()
            // Then
            defeatTasks.shouldNotBeEmpty()
            defeatTasks.map(TaskDto.DefeatDto::unitId).filterNot(unitIds::contains).shouldBeEmpty()
        }

    @Test
    fun `should find the npc of every talk task and quest giver, and none recruitable, among the npcs of its world file`() =
        runTest {
            // When
            val missing =
                worldFiles().flatMap { worldFile ->
                    val quests = questsOf(worldFile)
                    val npcIds = quests.flatMap(Quest.Dto::tasks).mapNotNull(TaskDto::talk).map(TaskDto.TalkDto::npcId) + quests.mapNotNull(Quest.Dto::questGiverNpcId)
                    val npcs = ResourcesNpcLoader(worldFile).also { loader -> loader.initResources() }.loadNpcs()
                    val notRecruitableNpcIds = npcs.filter { npc -> npc.unitId == null }.map { npc -> npc.id }.toSet()
                    npcIds.filterNot(notRecruitableNpcIds::contains).map { npcId -> "${worldFile.baseName}: $npcId" }
                }
            // Then
            missing.shouldBeEmpty()
        }

    @Test
    fun `should find every lootable item of a reward in the lootable item resources`() =
        runTest {
            // Given
            val rewardItemIds = quests().flatMap { quest -> quest.reward.lootableItemIds }
            // When
            val lootableItemIds =
                ResourcesLootableItemLoader()
                    .also { loader -> loader.initResources() }
                    .loadLootableItems()
                    .map { item -> item.id }
                    .toSet()
            // Then
            rewardItemIds.filterNot(lootableItemIds::contains).shouldBeEmpty()
        }

    @Test
    fun `should hold exactly one main quest in every world file`() =
        runTest {
            // When
            val mainQuestCounts = worldFiles().associate { worldFile -> worldFile.baseName to questsOf(worldFile).count { quest -> quest.type == QuestTypeDto.MAIN } }
            // Then
            mainQuestCounts.filterValues { count -> count != 1 }.shouldBeEmpty()
            mainQuestCounts.shouldNotBeEmpty()
        }
}
