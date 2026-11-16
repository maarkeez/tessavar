package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class EraseOccupant(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke(
        row: Int,
        column: Int,
    ) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, erasedWorldBuilder) = worldBuilder.erase(row = row, column = column).pullEvents()
        worldBuilderRepository.save(erasedWorldBuilder)
        eventBus.publish(events)
    }
}
