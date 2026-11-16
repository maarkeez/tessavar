package com.mkz.tessavar.worldBuilder.usecases.commands

import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository

class EditQuest(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val eventBus: EventBus,
) {
    operator fun invoke(quest: WorldBuilder.Dto.QuestDto) {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        val (events, editedWorldBuilder) = worldBuilder.editQuest(quest).pullEvents()
        worldBuilderRepository.save(editedWorldBuilder)
        eventBus.publish(events)
    }
}
