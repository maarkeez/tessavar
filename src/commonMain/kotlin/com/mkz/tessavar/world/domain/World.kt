package com.mkz.tessavar.world.domain

import com.mkz.tessavar.world.domain.WorldError.InvalidWorldId
import com.mkz.tessavar.world.domain.WorldError.OtherWorldVersion

@ConsistentCopyVisibility
data class World private constructor(
    private val id: String,
    private val name: String,
    private val author: String,
    private val events: Set<WorldEvent>,
) {
    companion object {
        const val CAMPAIGN_ID = "tessavar"
        const val PLAYTEST_ID = "playtest"
        const val CURRENT_VERSION = 1

        private val ID = Regex("[a-z0-9]+(-[a-z0-9]+)*")

        fun create(dto: Dto): World {
            if (!isValidId(dto.id)) throw InvalidWorldId(dto.id)
            if (dto.version != CURRENT_VERSION) throw OtherWorldVersion(dto.id, dto.version)
            return World(id = dto.id, name = dto.name, author = dto.author, events = emptySet())
        }

        fun isValidId(id: String): Boolean = ID.matches(id)

        fun idOf(name: String): String =
            name
                .lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter(String::isNotEmpty)
                .joinToString("-")
    }

    fun pullEvents(): Pair<Set<WorldEvent>, World> = events to copy(events = emptySet())

    fun toDto() = Dto(id = id, name = name, author = author, version = CURRENT_VERSION)

    data class Dto(
        val id: String,
        val name: String,
        val author: String,
        val version: Int,
    )
}
