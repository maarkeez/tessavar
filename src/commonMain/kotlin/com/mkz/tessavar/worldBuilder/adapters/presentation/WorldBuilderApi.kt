package com.mkz.tessavar.worldBuilder.adapters.presentation

import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.worldBuilder.adapters.resources.TomlWorldFileCodec
import com.mkz.tessavar.worldBuilder.adapters.storage.NativeStorageWorldBuilderRepository
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderRepository
import com.mkz.tessavar.worldBuilder.usecases.commands.FillTile
import com.mkz.tessavar.worldBuilder.usecases.commands.ImportWorld
import com.mkz.tessavar.worldBuilder.usecases.commands.OpenWorld
import com.mkz.tessavar.worldBuilder.usecases.commands.PaintTile
import com.mkz.tessavar.worldBuilder.usecases.commands.RedoEdit
import com.mkz.tessavar.worldBuilder.usecases.commands.RenameWorld
import com.mkz.tessavar.worldBuilder.usecases.commands.ResizeWorld
import com.mkz.tessavar.worldBuilder.usecases.commands.SelectTerrain
import com.mkz.tessavar.worldBuilder.usecases.commands.UndoEdit
import com.mkz.tessavar.worldBuilder.usecases.queries.ExportWorld
import com.mkz.tessavar.worldBuilder.usecases.queries.SearchWorld
import com.mkz.tessavar.worldBuilder.usecases.services.WorldFileCodec
import korlibs.korge.service.storage.IStorage

class WorldBuilderApi(
    terrainApi: TerrainApi,
    battlefieldApi: BattlefieldApi,
    eventBus: EventBus,
    storage: IStorage,
) {
    private val worldFileCodec: WorldFileCodec = TomlWorldFileCodec(battlefieldApi.parseBattlefield, battlefieldApi.exportBattlefield)
    private val worldBuilderRepository: WorldBuilderRepository =
        NativeStorageWorldBuilderRepository(storage, worldFileCodec)

    val openWorld = OpenWorld(worldBuilderRepository, eventBus)
    val resizeWorld = ResizeWorld(worldBuilderRepository, eventBus)
    val selectTerrain = SelectTerrain(worldBuilderRepository, eventBus)
    val paintTile = PaintTile(worldBuilderRepository, terrainApi.hasTerrainTransition, eventBus)
    val fillTile = FillTile(worldBuilderRepository, terrainApi.hasTerrainTransition, eventBus)
    val importWorld = ImportWorld(worldBuilderRepository, worldFileCodec, terrainApi.searchTerrainById, terrainApi.hasTerrainTransition, eventBus)
    val renameWorld = RenameWorld(worldBuilderRepository, eventBus)
    val undoEdit = UndoEdit(worldBuilderRepository, eventBus)
    val redoEdit = RedoEdit(worldBuilderRepository, eventBus)

    val searchWorld = SearchWorld(worldBuilderRepository)
    val exportWorld = ExportWorld(worldBuilderRepository, worldFileCodec)
}
