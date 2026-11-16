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
        mainQuestCount: Int = 0,
    ) = WorldBuilder.Dto.WorldFileDto(
        version = version,
        name = name,
        author = author,
        tiles = tiles,
        buildings = buildings,
        battleUnits = battleUnits,
        npcs = npcs,
        keptSections = keptSections,
        mainQuestCount = mainQuestCount,
    )

    fun catalog() =
        WorldBuilder.Dto.CatalogDto(
            buildings = listOf(WorldBuilder.Dto.CatalogBuildingDto(id = "fortress", rows = 2, columns = 2)),
            unitIds = listOf("knight", "rat"),
            npcs = listOf(WorldBuilder.Dto.CatalogNpcDto(id = "alchemist", unitId = null), WorldBuilder.Dto.CatalogNpcDto(id = "viking", unitId = "viking")),
            playerIds = listOf("player-one", "player-two"),
            lootableItemIds = listOf("health-potion", "mana-potion"),
        )
}
