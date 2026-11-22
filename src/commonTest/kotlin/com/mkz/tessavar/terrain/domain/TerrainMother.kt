package com.mkz.tessavar.terrain.domain

object TerrainMother {
    fun terrainThatCanBeOccupied(
        id: String = terrainId(),
        transitionsTo: Set<String> = emptySet(),
        effectIds: List<String> = emptyList(),
    ) = Terrain
        .create(
            Terrain.Dto(
                id = id,
                canBeOccupied = true,
                transitionsTo = transitionsTo,
                effectIds = effectIds,
            ),
        )

    fun terrainThatCanNotBeOccupied(
        id: String = terrainId(),
        transitionsTo: Set<String> = emptySet(),
        effectIds: List<String> = emptyList(),
    ) = Terrain
        .create(
            Terrain.Dto(
                id = id,
                canBeOccupied = false,
                transitionsTo = transitionsTo,
                effectIds = effectIds,
            ),
        )

    fun terrainId() = listOf("sand", "void").random()
}
