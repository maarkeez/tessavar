package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class RestockNpc(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        npcId: String,
        stock: List<String>,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, restockedWorldBuilder) = worldBuilder.restock(npcId = npcId, stock = stock).pullEvents()
        worldBuilderRepository.save(restockedWorldBuilder)
        eventBus.publish(events)
    }
}
