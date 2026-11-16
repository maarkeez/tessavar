package com.mkz.tessavar.worldBuilder.domain

object WorldBuilderMother {
    fun file(
        tiles: List<List<String>>,
        name: String = "",
        author: String = "",
        keptSections: String = "",
        version: Int = 1,
        buildings: List<WorldBuilder.Dto.BuildingDto> = emptyList(),
        battleUnits: List<WorldBuilder.Dto.BattleUnitDto> = emptyList(),
        npcs: List<WorldBuilder.Dto.NpcDto> = emptyList(),
        quests: List<WorldBuilder.Dto.QuestDto> = emptyList(),
    ) = WorldBuilder.Dto.WorldFileDto(
        version = version,
        name = name,
        author = author,
        tiles = tiles,
        buildings = buildings,
        battleUnits = battleUnits,
        npcs = npcs,
        quests = quests,
        keptSections = keptSections,
    )

    fun catalog() =
        WorldBuilder.Dto.CatalogDto(
            buildings = listOf(WorldBuilder.Dto.CatalogBuildingDto(id = "fortress", rows = 2, columns = 2)),
            unitIds = listOf("knight", "rat"),
            npcs = listOf(WorldBuilder.Dto.CatalogNpcDto(id = "alchemist", unitId = null), WorldBuilder.Dto.CatalogNpcDto(id = "viking", unitId = "viking")),
            playerIds = listOf("player-one", "player-two"),
            lootableItemIds = listOf("health-potion", "mana-potion"),
        )

    fun quest(
        id: String = "the-keep",
        type: WorldBuilder.Dto.QuestTypeDto = WorldBuilder.Dto.QuestTypeDto.MAIN,
        questGiverNpcId: String? = null,
        startTiles: List<WorldBuilder.Dto.TileDto> = listOf(WorldBuilder.Dto.TileDto(row = 0, column = 0)),
        tasks: List<WorldBuilder.Dto.TaskDto> = listOf(defeatTask()),
    ) = WorldBuilder.Dto.QuestDto(
        id = id,
        type = type,
        questGiverNpcId = questGiverNpcId,
        repeatable = false,
        startTiles = startTiles,
        name = "The Keep",
        description = "",
        lines = WorldBuilder.Dto.LinesDto(start = null, reminder = null, completed = null, startAgain = null),
        tasks = tasks,
        reward = WorldBuilder.Dto.RewardDto(gold = 10, lootableItemIds = emptyList()),
    )

    fun defeatTask(unitId: String = "rat") =
        WorldBuilder.Dto.TaskDto(type = WorldBuilder.Dto.TaskTypeDto.DEFEAT, unitId = unitId, requiredDefeats = 1, npcId = null, tiles = emptyList(), text = "Defeat a rat", line = null)

    fun talkTask(npcId: String = "alchemist") =
        WorldBuilder.Dto.TaskDto(type = WorldBuilder.Dto.TaskTypeDto.TALK, unitId = null, requiredDefeats = null, npcId = npcId, tiles = emptyList(), text = "Talk", line = null)
}
