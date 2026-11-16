package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class RedoEdit(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke() {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, updatedWorldBuilder) = worldBuilder.redo().pullEvents()
        worldBuilderRepository.save(updatedWorldBuilder)
        eventBus.publish(events)
    }
}
