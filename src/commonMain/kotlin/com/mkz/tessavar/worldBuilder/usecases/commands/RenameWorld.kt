package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class RenameWorld(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        name: String,
        author: String,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, renamedWorldBuilder) = worldBuilder.rename(name = name, author = author).pullEvents()
        worldBuilderRepository.save(renamedWorldBuilder)
        eventBus.publish(events)
    }
}
