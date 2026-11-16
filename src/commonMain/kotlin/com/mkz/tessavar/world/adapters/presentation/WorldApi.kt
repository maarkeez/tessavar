package com.mkz.tessavar.world.adapters.presentation

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.world.usecases.commands.SetUpGame
import com.mkz.tessavar.world.usecases.queries.SearchWorld
import com.mkz.tessavar.world.usecases.services.WorldLoader

class WorldApi(
    eventBus: EventBus,
    private val worldLoader: WorldLoader,
) {
    val setUpGame = SetUpGame(eventBus)

    val searchWorld = SearchWorld(worldLoader)

    suspend fun init() {
        worldLoader.initResources()
    }
}
