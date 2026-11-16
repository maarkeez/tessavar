package com.mkz.tessavar.worldBuilder.usecases.queries

import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderError.WorldNotCreated
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository
import com.mkz.tessavar.worldBuilder.usecases.services.WorldFileCodec

class ExportWorld(
    private val worldBuilderRepository: WorldBuilderRepository,
    private val worldFileCodec: WorldFileCodec,
) {
    operator fun invoke(): String {
        val worldBuilder = worldBuilderRepository.search() ?: throw WorldNotCreated()
        return worldFileCodec.encode(worldBuilder.toWorldFile(World.CURRENT_VERSION))
    }
}
