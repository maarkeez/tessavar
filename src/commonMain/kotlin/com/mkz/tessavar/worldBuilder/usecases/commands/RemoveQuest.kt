package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class RemoveQuest(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke(questId: String) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, removedWorldBuilder) = worldBuilder.removeQuest(questId).pullEvents()
        worldBuilderRepository.save(removedWorldBuilder)
        eventBus.publish(events)
    }
}
