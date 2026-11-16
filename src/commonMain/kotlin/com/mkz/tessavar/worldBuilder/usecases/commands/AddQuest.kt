package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class AddQuest(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke(type: WorldBuilder.Dto.QuestTypeDto) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, addedWorldBuilder) = worldBuilder.addQuest(type).pullEvents()
        worldBuilderRepository.save(addedWorldBuilder)
        eventBus.publish(events)
    }
}
