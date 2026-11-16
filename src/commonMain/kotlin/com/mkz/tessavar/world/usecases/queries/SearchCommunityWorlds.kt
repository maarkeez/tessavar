package com.mkz.tessavar.world.usecases.queries

import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.world.usecases.services.WorldLoader

class SearchCommunityWorlds(
    private val worldLoader: WorldLoader,
) {
    operator fun invoke(): List<World.Dto> =
        worldLoader
            .loadWorlds()
            .filter { world -> world.id != World.CAMPAIGN_ID }
            .map { world -> World.create(world).toDto() }
            .sortedWith(compareBy({ world -> world.name.lowercase() }, { world -> world.id }))
}
