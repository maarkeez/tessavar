package com.mkz.tessavar.world.usecases.queries

import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.world.usecases.services.WorldLoader

class SearchWorld(
    private val worldLoader: WorldLoader,
) {
    operator fun invoke(id: String): World.Dto? =
        worldLoader
            .loadWorlds()
            .firstOrNull { world -> world.id == id }
            ?.let(World::create)
            ?.toDto()
}
