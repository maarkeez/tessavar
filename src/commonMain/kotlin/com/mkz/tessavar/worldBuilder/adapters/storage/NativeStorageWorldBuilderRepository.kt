package com.mkz.tessavar.worldBuilder.adapters.storage

import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository
import com.mkz.tessavar.worldBuilder.usecases.services.Catalog
import com.mkz.tessavar.worldBuilder.usecases.services.WorldFileCodec
import korlibs.korge.service.storage.IStorage

class NativeStorageWorldBuilderRepository(
    private val storage: IStorage,
    private val worldFileCodec: WorldFileCodec,
    private val catalog: Catalog,
) : WorldBuilderRepository {
    companion object {
        const val KEY = "worldBuilder.world"
    }

    private var worldBuilder: WorldBuilder? = null

    override fun save(worldBuilder: WorldBuilder) {
        this.worldBuilder = worldBuilder
        storage[KEY] = worldFileCodec.encode(worldBuilder.toWorldFile(World.CURRENT_VERSION))
    }

    override fun search(): WorldBuilder? = worldBuilder ?: read()?.also { read -> worldBuilder = read }

    private fun read(): WorldBuilder? {
        val worldFile = storage.getOrNull(KEY) ?: return null
        return try {
            worldFileCodec.decode(worldFile)?.let { file -> WorldBuilder.restore(file, catalog.loadCatalog()) }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
