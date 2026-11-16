package com.mkz.tessavar.worldBuilder.adapters.storage

import com.mkz.tessavar.battlefield.domain.BattlefieldError.InvalidBattlefieldFormat
import com.mkz.tessavar.battlefield.usecases.queries.ExportBattlefield
import com.mkz.tessavar.battlefield.usecases.queries.ParseBattlefield
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository
import korlibs.korge.service.storage.IStorage

/**
 * Keeps the world being built in the storage, written as its world file on every save, so it survives leaving the world
 * builder and a reload. What can't be written in a world file, the selected terrain and the edits to undo, only lasts
 * while the world builder is open.
 */
class NativeStorageWorldBuilderRepository(
    private val storage: IStorage,
    private val exportBattlefield: ExportBattlefield,
    private val parseBattlefield: ParseBattlefield,
) : WorldBuilderRepository {
    companion object {
        const val KEY = "worldBuilder.world"
    }

    private var worldBuilder: WorldBuilder? = null

    override fun save(worldBuilder: WorldBuilder) {
        this.worldBuilder = worldBuilder
        storage[KEY] = exportBattlefield(worldBuilder.toDto().tiles)
    }

    override fun search(): WorldBuilder? = worldBuilder ?: read()?.also { read -> worldBuilder = read }

    private fun read(): WorldBuilder? {
        val worldFile = storage.getOrNull(KEY) ?: return null
        return try {
            WorldBuilder.restore(parseBattlefield(worldFile))
        } catch (_: InvalidBattlefieldFormat) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
