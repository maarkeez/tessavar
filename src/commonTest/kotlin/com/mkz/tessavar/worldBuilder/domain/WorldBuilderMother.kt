package com.mkz.tessavar.worldBuilder.domain

object WorldBuilderMother {
    fun file(
        tiles: List<List<String>>,
        name: String = "",
        author: String = "",
        keptSections: String = "",
        version: Int = 1,
    ) = WorldBuilder.Dto.WorldFileDto(version = version, name = name, author = author, tiles = tiles, keptSections = keptSections)
}
