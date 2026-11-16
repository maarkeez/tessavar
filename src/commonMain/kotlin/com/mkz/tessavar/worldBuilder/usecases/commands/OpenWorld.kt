package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class OpenWorld(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke() {
        val (events, worldBuilder) = (worldBuilderRepository.search()?.reopen() ?: WorldBuilder.create()).pullEvents()
        worldBuilderRepository.save(worldBuilder)
        eventBus.publish(events)
    }
}
