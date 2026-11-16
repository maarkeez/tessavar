package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.battlefield.adapters.presentation.BattlefieldApi
import com.mkz.tessavar.shared.domain.EventBus
import com.mkz.tessavar.shared.domain.Subscription
import com.mkz.tessavar.shared.domain.subscribe
import com.mkz.tessavar.terrain.adapters.presentation.TerrainApi
import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.worldBuilder.adapters.presentation.WorldBuilderApi
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.BattleUnitDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.NpcDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.QuestDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.QuestTypeDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.TaskDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.TaskTypeDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.TileDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder.Dto.UnplayableReasonDto
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderEvent.WorldImportRejected.Reason
import korlibs.io.async.launchImmediately
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

class WorldBuilderPresenter(
    private val battlefieldView: BattlefieldView,
    private val infoView: WorldBuilderInfoView,
    private val hudView: WorldBuilderHudView,
    private val resizeView: WorldBuilderResizeView,
    private val detailsView: WorldBuilderDetailsView,
    private val stockView: WorldBuilderStockView,
    private val formView: WorldBuilderFormView,
    private val menuView: WorldBuilderMenuView,
    private val worldBuilderApi: WorldBuilderApi,
    private val battlefieldApi: BattlefieldApi,
    private val terrainApi: TerrainApi,
    private val messages: Messages,
    private val worldFileSaver: WorldFileSaver,
    private val worldFilePicker: WorldFilePicker,
    private val textPrompt: TextPrompt?,
    private val browser: Browser,
    private val coroutineScope: CoroutineScope,
    private val onLeft: () -> Unit,
    private val onPlaytest: (worldFile: String) -> Unit,
    eventBus: EventBus,
) : BattlefieldView.Delegate,
    WorldBuilderHudView.Delegate,
    WorldBuilderResizeView.Delegate,
    WorldBuilderDetailsView.Delegate,
    WorldBuilderStockView.Delegate,
    WorldBuilderFormView.Delegate {
    companion object {
        const val WORLD_FILE_NAME = "world"
        const val HOW_TO_SHARE_URL = "https://github.com/maarkeez/tessavar/blob/main/CONTRIBUTING.md#share-a-world"
        const val WORLD_FILE_EXTENSION = ".toml"
        const val QUESTS_FORM = "quests"
        const val GOLD_STEP = 5
        val PLAYTEST_BLOCKERS = setOf(UnplayableReasonDto.NO_HUMAN_BATTLE_UNIT, UnplayableReasonDto.NO_MAIN_QUEST, UnplayableReasonDto.QUEST_WITHOUT_TASKS)
    }

    private var tool = WorldBuilderHudView.Tool.PAINT
    private var tab = WorldBuilderHudView.Tab.TERRAIN
    private var selectedBuildingId: String? = null
    private var selectedUnit: Pair<String, String>? = null
    private var selectedNpcId: String? = null
    private var openedQuestId: String? = null
    private var pickingKey: String? = null

    private val subscriptions =
        listOf(
            eventBus.subscribe<WorldBuilderEvent.WorldOpened> {
                displayWorld()
                displayTerrains()
                displayOccupants()
            },
            eventBus.subscribe<WorldBuilderEvent.WorldResized> { displayWorld() },
            eventBus.subscribe<WorldBuilderEvent.TerrainSelected> { event -> hudView.selectTerrain(event.terrainId) },
            eventBus.subscribe<WorldBuilderEvent.TilePainted> { event -> displayPaintedTile(event.row, event.column) },
            eventBus.subscribe<WorldBuilderEvent.AreaFilled> { displayWorld() },
            eventBus.subscribe<WorldBuilderEvent.EditUndone> {
                displayWorld()
                displayForm()
            },
            eventBus.subscribe<WorldBuilderEvent.EditRedone> {
                displayWorld()
                displayForm()
            },
            eventBus.subscribe<WorldBuilderEvent.QuestAdded> { event -> openQuest(event.questId) },
            eventBus.subscribe<WorldBuilderEvent.QuestRemoved> { openQuests() },
            eventBus.subscribe<WorldBuilderEvent.QuestEdited> { displayForm() },
            eventBus.subscribe<WorldBuilderEvent.QuestRejected> { infoView.displayError(messages[UiMessage.WORLD_BUILDER_MAIN_QUEST_TAKEN]) },
            eventBus.subscribe<WorldBuilderEvent.TilePaintRejected> { event ->
                infoView.displayError(forbiddenAdjacentTerrainsMessage(event.terrainId, event.adjacentTerrainId))
            },
            eventBus.subscribe<WorldBuilderEvent.WorldImported> {
                displayWorld()
                infoView.displayInfo(messages[UiMessage.WORLD_BUILDER_WORLD_IMPORTED])
            },
            eventBus.subscribe<WorldBuilderEvent.WorldImportRejected> { event -> infoView.displayError(importRejectedMessage(event)) },
            eventBus.subscribe<WorldBuilderEvent.OccupantPlaced> { displayWorld() },
            eventBus.subscribe<WorldBuilderEvent.OccupantErased> { displayWorld() },
            eventBus.subscribe<WorldBuilderEvent.NpcRestocked> { displayWorld() },
            eventBus.subscribe<WorldBuilderEvent.OccupantPlaceRejected> { event -> infoView.displayError(placeRejectedMessage(event)) },
            eventBus.subscribe<WorldBuilderEvent.OccupiedTilePaintRejected> { event ->
                infoView.displayError(messages[UiMessage.WORLD_BUILDER_OCCUPIED_TILE, "terrain" to messages.terrainName(event.terrainId)])
            },
        )

    init {
        battlefieldView.setDelegate(this)
        hudView.setDelegate(this)
        resizeView.setDelegate(this)
        detailsView.setDelegate(this)
        stockView.setDelegate(this)
        formView.setDelegate(this)
        hudView.selectTool(tool)
        menuView.displayOptions(
            listOf(
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_RESIZE]) { resizeView.open() },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_DETAILS]) { openDetails() },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_QUESTS]) { openQuests() },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_PLAYTEST]) { playtest() },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_IMPORT]) { importWorld() },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_EXPORT]) { exportWorld() },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_HOW_TO_SHARE]) { coroutineScope.launchImmediately { browser.open(HOW_TO_SHARE_URL) } },
                WorldBuilderMenuView.Option(label = messages[UiMessage.WORLD_BUILDER_BACK]) { onLeft() },
            ),
        )
    }

    fun dispose() {
        subscriptions.forEach(Subscription::dispose)
    }

    private fun displayWorld() {
        val world = worldBuilderApi.searchWorld() ?: return
        battlefieldView.displayBattlefield(BattlefieldMapper.terrain(battlefieldApi.previewBattlefield(world.tiles)))
        infoView.displaySize(rows = world.rows, columns = world.columns)
        resizeView.displaySize(rows = world.rows, columns = world.columns, allowedSizes = WorldBuilder.MINIMUM_SIZE..WorldBuilder.MAXIMUM_SIZE)
        hudView.displayHistory(canUndo = world.canUndo, canRedo = world.canRedo)
        world.buildings.forEach { building -> battlefieldView.displayBuilding(row = building.row, column = building.column, buildingId = building.id) }
        world.battleUnits.forEach { battleUnit -> battlefieldView.displayBattleUnit(row = battleUnit.row, column = battleUnit.column, unitId = battleUnit.unitId) }
        val catalogNpcs = worldBuilderApi.searchCatalog().npcs
        world.npcs.forEach { npc ->
            battlefieldView.displayNpc(row = npc.row, column = npc.column, npcId = npc.id, unitId = catalogNpcs.firstOrNull { catalogNpc -> catalogNpc.id == npc.id }?.unitId)
        }
        displayStock()
        displayPickedTiles()
    }

    private fun displayOccupants() {
        val catalog = worldBuilderApi.searchCatalog()
        hudView.displayBuildings(
            catalog.buildings.mapNotNull { building -> paletteOption(building.id, "building/${building.id}.png") },
        )
        hudView.displayUnits(
            catalog.playerIds.map { playerId ->
                WorldBuilderHudView.UnitRow(
                    playerId = playerId,
                    title = messages.playerName(playerId),
                    units = catalog.unitIds.mapNotNull { unitId -> paletteOption(unitId, "unit/$unitId.png") },
                )
            },
        )
        hudView.displayNpcs(catalog.npcs.mapNotNull { npc -> paletteOption(npc.id, npcBitmapPath(npc.id, npc.unitId)) })
        selectedBuildingId = selectedBuildingId ?: catalog.buildings.firstOrNull()?.id
        selectedUnit = selectedUnit ?: catalog.playerIds.firstOrNull()?.let { playerId -> catalog.unitIds.firstOrNull()?.let { unitId -> playerId to unitId } }
        selectedNpcId = selectedNpcId ?: catalog.npcs.firstOrNull()?.id
        selectedBuildingId?.let(hudView::selectBuilding)
        selectedUnit?.let { (playerId, unitId) -> hudView.selectUnit(playerId, unitId) }
        selectedNpcId?.let(hudView::selectNpc)
        displayStock()
    }

    private fun paletteOption(
        id: String,
        path: String,
    ): WorldBuilderHudView.PaletteOption? = battlefieldView.occupantBitmap(path)?.let { bitmap -> WorldBuilderHudView.PaletteOption(id = id, bitmap = bitmap) }

    private fun npcBitmapPath(
        npcId: String,
        unitId: String?,
    ): String = unitId?.let { "unit/$it.png" } ?: "npc/$npcId.png"

    private fun displayStock() {
        val world = worldBuilderApi.searchWorld() ?: return
        hudView.displayStock(isEnabled = world.npcs.any { npc -> npc.id == selectedNpcId })
    }

    private fun playtest() {
        val world = worldBuilderApi.searchWorld() ?: return
        world.unplayableReasons.firstOrNull { reason -> reason in PLAYTEST_BLOCKERS }?.let { reason ->
            infoView.displayError(messages[UiMessage.WORLD_BUILDER_TO_PLAYTEST, "reason" to reasonText(reason)])
            return
        }
        onPlaytest(worldBuilderApi.exportWorld())
    }

    private fun reasonText(reason: UnplayableReasonDto): String =
        when (reason) {
            UnplayableReasonDto.NO_HUMAN_BATTLE_UNIT -> messages[UiMessage.WORLD_BUILDER_REASON_BATTLE_UNIT, "player" to messages.playerName(WorldBuilder.HUMAN_PLAYER_ID)]
            UnplayableReasonDto.NO_CPU_BATTLE_UNIT -> messages[UiMessage.WORLD_BUILDER_REASON_BATTLE_UNIT, "player" to messages.playerName(WorldBuilder.CPU_PLAYER_ID)]
            UnplayableReasonDto.NO_MAIN_QUEST -> messages[UiMessage.WORLD_BUILDER_REASON_MAIN_QUEST]
            UnplayableReasonDto.NO_START_TILES -> messages[UiMessage.WORLD_BUILDER_REASON_START_TILES]
            UnplayableReasonDto.QUEST_WITHOUT_TASKS -> messages[UiMessage.WORLD_BUILDER_REASON_TASKS]
        }

    private fun openQuests() {
        openedQuestId = null
        stopPicking()
        val world = worldBuilderApi.searchWorld() ?: return
        formView.open(formId = QUESTS_FORM, title = messages[UiMessage.WORLD_BUILDER_QUESTS_TITLE], rows = WorldBuilderQuestForms.questsRows(world.quests, messages))
    }

    private fun openQuest(questId: String) {
        openedQuestId = questId
        stopPicking()
        displayForm()
    }

    private fun displayForm() {
        if (!formView.isOpen) return
        val world = worldBuilderApi.searchWorld() ?: return
        val questId = openedQuestId ?: return openQuests()
        val quest = world.quests.firstOrNull { quest -> quest.id == questId } ?: return openQuests()
        val rows = WorldBuilderQuestForms.questRows(quest, worldBuilderApi.searchCatalog().lootableItemIds, pickingKey, messages)
        formView.open(formId = WorldBuilderQuestForms.QUEST + quest.id, title = WorldBuilderQuestForms.title(quest), rows = rows)
        displayPickedTiles()
    }

    private fun stopPicking() {
        pickingKey = null
        battlefieldView.resetTiles()
    }

    private fun openedQuest(): QuestDto? = worldBuilderApi.searchWorld()?.quests?.firstOrNull { quest -> quest.id == openedQuestId }

    private fun pickedTiles(): List<TileDto> {
        val key = pickingKey ?: return emptyList()
        val quest = openedQuest() ?: return emptyList()
        return if (key == WorldBuilderQuestForms.START_TILES) {
            quest.startTiles
        } else {
            quest.tasks
                .getOrNull(taskIndex(key))
                ?.tiles
                .orEmpty()
        }
    }

    private fun displayPickedTiles() {
        if (pickingKey == null) return
        battlefieldView.resetTiles()
        pickedTiles().forEach { tile -> battlefieldView.displayTileSelection(tile.row, tile.column) }
    }

    private fun pickTile(
        row: Int,
        column: Int,
    ) {
        val key = pickingKey ?: return
        val tile = TileDto(row = row, column = column)

        fun toggled(tiles: List<TileDto>) = if (tile in tiles) tiles - tile else tiles + tile
        editQuest { quest ->
            if (key == WorldBuilderQuestForms.START_TILES) {
                quest.copy(startTiles = toggled(quest.startTiles))
            } else {
                editTask(quest, taskIndex(key)) { task -> task.copy(tiles = toggled(task.tiles)) }
            }
        }
    }

    private fun taskIndex(key: String): Int = key.removePrefix(WorldBuilderQuestForms.TASK).substringBefore(".").toInt()

    private fun editTask(
        quest: QuestDto,
        index: Int,
        edit: (TaskDto) -> TaskDto,
    ): QuestDto = quest.copy(tasks = quest.tasks.mapIndexed { taskIndex, task -> if (taskIndex == index) edit(task) else task })

    private fun editQuest(edit: (QuestDto) -> QuestDto) {
        val quest = openedQuest() ?: return
        worldBuilderApi.editQuest(edit(quest))
    }

    private fun defeatCandidates(): List<String> {
        val world = worldBuilderApi.searchWorld() ?: return emptyList()
        val cpuUnitIds =
            world.battleUnits
                .filter { battleUnit -> battleUnit.playerId == WorldBuilder.CPU_PLAYER_ID }
                .map(BattleUnitDto::unitId)
                .distinct()
        return cpuUnitIds.ifEmpty { worldBuilderApi.searchCatalog().unitIds }
    }

    private fun npcCandidates(): List<String> =
        worldBuilderApi
            .searchWorld()
            ?.npcs
            ?.map(NpcDto::id)
            .orEmpty()

    private fun <T> List<T>.stepFrom(
        current: T,
        step: Int,
    ): T? {
        if (isEmpty()) return null
        val index = indexOf(current)
        return this[((index + step) % size + size) % size]
    }

    private fun newTask(type: TaskTypeDto): TaskDto =
        TaskDto(
            type = type,
            unitId = defeatCandidates().firstOrNull().takeIf { type == TaskTypeDto.DEFEAT },
            requiredDefeats = 1.takeIf { type == TaskTypeDto.DEFEAT },
            npcId = npcCandidates().firstOrNull().takeIf { type == TaskTypeDto.TALK },
            tiles = emptyList(),
            text = "",
            line = null,
        )

    override fun formClosed() {
        if (openedQuestId != null) openQuests() else formView.close()
    }

    override fun textTapped(key: String) {
        val prompt = textPrompt ?: return
        val label = formView.rows.filterIsInstance<WorldBuilderFormView.Row.Text>().firstOrNull { row -> row.key == key } ?: return
        coroutineScope.launchImmediately {
            prompt.prompt(label.label, label.value)?.let { text ->
                formView.displayText(key, text)
                textChanged(key, text)
            }
        }
    }

    override fun textChanged(
        key: String,
        text: String,
    ) = editQuest { quest ->
        val optional = text.takeIf(String::isNotBlank)
        when {
            key == WorldBuilderQuestForms.NAME -> quest.copy(name = text)
            key == WorldBuilderQuestForms.DESCRIPTION -> quest.copy(description = text)
            key == WorldBuilderQuestForms.LINE_START -> quest.copy(lines = quest.lines.copy(start = optional))
            key == WorldBuilderQuestForms.LINE_REMINDER -> quest.copy(lines = quest.lines.copy(reminder = optional))
            key == WorldBuilderQuestForms.LINE_COMPLETED -> quest.copy(lines = quest.lines.copy(completed = optional))
            key == WorldBuilderQuestForms.LINE_START_AGAIN -> quest.copy(lines = quest.lines.copy(startAgain = optional))
            key.endsWith(WorldBuilderQuestForms.TASK_TEXT) -> editTask(quest, taskIndex(key)) { task -> task.copy(text = text) }
            key.endsWith(WorldBuilderQuestForms.TASK_LINE) -> editTask(quest, taskIndex(key)) { task -> task.copy(line = optional) }
            else -> quest
        }
    }

    override fun choiceStepped(
        key: String,
        step: Int,
    ) = editQuest { quest ->
        when {
            key == WorldBuilderQuestForms.TYPE -> {
                val type = if (quest.type == QuestTypeDto.MAIN) QuestTypeDto.SIDE else QuestTypeDto.MAIN
                quest.copy(type = type, questGiverNpcId = quest.questGiverNpcId.takeIf { type == QuestTypeDto.SIDE }, repeatable = quest.repeatable && type == QuestTypeDto.SIDE)
            }
            key == WorldBuilderQuestForms.GIVER -> quest.copy(questGiverNpcId = (listOf(null) + npcCandidates()).stepFrom(quest.questGiverNpcId, step))
            key.endsWith(WorldBuilderQuestForms.TASK_TYPE) ->
                editTask(quest, taskIndex(key)) { task ->
                    val type = TaskTypeDto.entries.stepFrom(task.type, step) ?: task.type
                    newTask(type).copy(text = task.text, line = task.line)
                }
            key.endsWith(WorldBuilderQuestForms.TASK_UNIT) -> editTask(quest, taskIndex(key)) { task -> task.copy(unitId = defeatCandidates().stepFrom(task.unitId.orEmpty(), step) ?: task.unitId) }
            key.endsWith(WorldBuilderQuestForms.TASK_NPC) -> editTask(quest, taskIndex(key)) { task -> task.copy(npcId = npcCandidates().stepFrom(task.npcId.orEmpty(), step) ?: task.npcId) }
            else -> quest
        }
    }

    override fun toggled(key: String) {
        when {
            key == WorldBuilderQuestForms.START_TILES || key.endsWith(WorldBuilderQuestForms.TASK_TILES) -> {
                if (pickingKey == key) stopPicking() else pickingKey = key
                displayForm()
            }
            key == WorldBuilderQuestForms.REPEATABLE -> editQuest { quest -> quest.copy(repeatable = !quest.repeatable) }
            key.startsWith(WorldBuilderQuestForms.ITEM) -> {
                val lootableItemId = key.removePrefix(WorldBuilderQuestForms.ITEM)
                editQuest { quest ->
                    val items = quest.reward.lootableItemIds
                    quest.copy(reward = quest.reward.copy(lootableItemIds = if (lootableItemId in items) items - lootableItemId else items + lootableItemId))
                }
            }
        }
    }

    override fun stepped(
        key: String,
        step: Int,
    ) = editQuest { quest ->
        when {
            key == WorldBuilderQuestForms.GOLD -> quest.copy(reward = quest.reward.copy(gold = maxOf(0, quest.reward.gold + step * GOLD_STEP)))
            key.endsWith(WorldBuilderQuestForms.TASK_DEFEATS) -> editTask(quest, taskIndex(key)) { task -> task.copy(requiredDefeats = maxOf(1, (task.requiredDefeats ?: 1) + step)) }
            else -> quest
        }
    }

    override fun actionSelected(key: String) {
        when {
            key.startsWith(WorldBuilderQuestForms.QUEST) -> openQuest(key.removePrefix(WorldBuilderQuestForms.QUEST))
            key == WorldBuilderQuestForms.ADD_MAIN -> worldBuilderApi.addQuest(QuestTypeDto.MAIN)
            key == WorldBuilderQuestForms.ADD_SIDE -> worldBuilderApi.addQuest(QuestTypeDto.SIDE)
            key == WorldBuilderQuestForms.ADD_TASK -> editQuest { quest -> quest.copy(tasks = quest.tasks + newTask(TaskTypeDto.DEFEAT)) }
            key.endsWith(WorldBuilderQuestForms.TASK_REMOVE) -> {
                stopPicking()
                val index = taskIndex(key)
                editQuest { quest -> quest.copy(tasks = quest.tasks.filterIndexed { taskIndex, _ -> taskIndex != index }) }
            }
            key == WorldBuilderQuestForms.REMOVE -> openedQuestId?.let(worldBuilderApi.removeQuest::invoke)
        }
    }

    private fun placeRejectedMessage(event: WorldBuilderEvent.OccupantPlaceRejected): String =
        when (event.reason) {
            WorldBuilderEvent.OccupantPlaceRejected.Reason.OUT_OF_WORLD -> messages[UiMessage.WORLD_BUILDER_OUT_OF_WORLD]
            WorldBuilderEvent.OccupantPlaceRejected.Reason.NOT_OCCUPIABLE ->
                messages[UiMessage.WORLD_BUILDER_NOT_OCCUPIABLE, "terrain" to messages.terrainName(event.terrainId.orEmpty())]
            WorldBuilderEvent.OccupantPlaceRejected.Reason.CORNERED ->
                messages[UiMessage.WORLD_BUILDER_CORNERED, "terrain" to messages.terrainName(event.terrainId.orEmpty())]
            WorldBuilderEvent.OccupantPlaceRejected.Reason.NOT_VACANT -> messages[UiMessage.WORLD_BUILDER_NOT_VACANT]
        }

    private fun displayTerrains() {
        val world = worldBuilderApi.searchWorld() ?: return
        val terrains =
            terrainApi.searchTerrains().map { terrain ->
                WorldBuilderHudView.TerrainOption(terrainId = terrain.id, bitmap = battlefieldView.terrainTileBitmap(terrain.id, terrainTransition = null))
            }
        hudView.displayTerrains(terrains, selectedTerrainId = world.selectedTerrainId)
    }

    private fun displayPaintedTile(
        row: Int,
        column: Int,
    ) {
        val world = worldBuilderApi.searchWorld() ?: return
        val positions =
            listOf(row to column, row - 1 to column, row to column + 1, row + 1 to column, row to column - 1)
                .filter { (tileRow, tileColumn) -> tileRow in 0 until world.rows && tileColumn in 0 until world.columns }
                .map { (tileRow, tileColumn) -> TilePosition(tileRow, tileColumn) }
        battlefieldView.updateTerrain(BattlefieldMapper.terrain(battlefieldApi.previewBattlefield(world.tiles)), positions)
        hudView.displayHistory(canUndo = world.canUndo, canRedo = world.canRedo)
    }

    private fun exportWorld() {
        val text = worldBuilderApi.exportWorld()
        coroutineScope.launchImmediately {
            val saved =
                try {
                    worldFileSaver.saveWorld(worldFileName(), text)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    infoView.displayError(messages[UiMessage.WORLD_BUILDER_FILE_NOT_SAVED])
                    return@launchImmediately
                }
            if (!saved) return@launchImmediately
            val reason = worldBuilderApi.searchWorld()?.unplayableReasons?.firstOrNull()
            if (reason == null) {
                infoView.displayInfo(messages[UiMessage.WORLD_BUILDER_WORLD_EXPORTED])
            } else {
                infoView.displayInfo(messages[UiMessage.WORLD_BUILDER_EXPORTED_NOT_PLAYABLE, "reason" to reasonText(reason)])
            }
        }
    }

    private fun worldFileName(): String = World.idOf(worldBuilderApi.searchWorld()?.name.orEmpty()).ifEmpty { WORLD_FILE_NAME } + WORLD_FILE_EXTENSION

    private fun openDetails() {
        val world = worldBuilderApi.searchWorld() ?: return
        detailsView.open(name = world.name, author = world.author)
    }

    override fun fieldTapped(field: WorldBuilderDetailsView.Field) {
        val prompt = textPrompt ?: return
        val world = worldBuilderApi.searchWorld() ?: return
        val (label, current) =
            when (field) {
                WorldBuilderDetailsView.Field.NAME -> messages[UiMessage.WORLD_BUILDER_DETAILS_NAME] to world.name
                WorldBuilderDetailsView.Field.AUTHOR -> messages[UiMessage.WORLD_BUILDER_DETAILS_AUTHOR] to world.author
            }
        coroutineScope.launchImmediately {
            prompt.prompt(label, current)?.let { text -> detailsView.displayField(field, text) }
        }
    }

    override fun detailsChanged(
        name: String,
        author: String,
    ) = worldBuilderApi.renameWorld(name = name, author = author)

    private fun importWorld() {
        coroutineScope.launchImmediately {
            val text =
                try {
                    worldFilePicker.pickWorld()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    infoView.displayError(messages[UiMessage.WORLD_BUILDER_FILE_NOT_READ])
                    return@launchImmediately
                }
            if (text != null) worldBuilderApi.importWorld(text)
        }
    }

    private fun importRejectedMessage(event: WorldBuilderEvent.WorldImportRejected): String =
        when (event.reason) {
            Reason.NOT_A_WORLD -> messages[UiMessage.WORLD_BUILDER_NOT_A_WORLD]
            Reason.OTHER_WORLD_VERSION -> messages[UiMessage.WORLD_BUILDER_OTHER_WORLD_VERSION, "version" to World.CURRENT_VERSION]
            Reason.SIZE_OUT_OF_RANGE ->
                messages[UiMessage.WORLD_BUILDER_SIZE_OUT_OF_RANGE, "minimum" to WorldBuilder.MINIMUM_SIZE, "maximum" to WorldBuilder.MAXIMUM_SIZE]
            Reason.UNKNOWN_TERRAIN -> messages[UiMessage.WORLD_BUILDER_UNKNOWN_TERRAIN, "terrain" to event.terrainId.orEmpty()]
            Reason.FORBIDDEN_ADJACENT_TERRAINS -> forbiddenAdjacentTerrainsMessage(event.terrainId!!, event.adjacentTerrainId!!)
            Reason.UNKNOWN_OCCUPANT -> messages[UiMessage.WORLD_BUILDER_UNKNOWN_OCCUPANT, "occupant" to event.occupantId.orEmpty()]
            Reason.DUPLICATE_OCCUPANT -> messages[UiMessage.WORLD_BUILDER_DUPLICATE_OCCUPANT, "occupant" to event.occupantId.orEmpty()]
            Reason.MISPLACED_OCCUPANT -> messages[UiMessage.WORLD_BUILDER_MISPLACED_OCCUPANT, "occupant" to event.occupantId.orEmpty()]
        }

    private fun forbiddenAdjacentTerrainsMessage(
        terrainId: String,
        adjacentTerrainId: String,
    ): String = messages[UiMessage.WORLD_BUILDER_FORBIDDEN_ADJACENT_TERRAINS, "terrain" to messages.terrainName(terrainId), "adjacentTerrain" to messages.terrainName(adjacentTerrainId)]

    override fun tileSelected(
        row: Int,
        column: Int,
    ) {
        if (formView.isOpen) return pickTile(row, column)
        when (tool) {
            WorldBuilderHudView.Tool.ERASE -> worldBuilderApi.eraseOccupant(row = row, column = column)
            WorldBuilderHudView.Tool.FILL -> if (tab == WorldBuilderHudView.Tab.TERRAIN) worldBuilderApi.fillTile(row = row, column = column)
            WorldBuilderHudView.Tool.PAINT -> place(row, column)
        }
    }

    private fun place(
        row: Int,
        column: Int,
    ) {
        when (tab) {
            WorldBuilderHudView.Tab.TERRAIN -> worldBuilderApi.paintTile(row = row, column = column)
            WorldBuilderHudView.Tab.BUILDINGS -> selectedBuildingId?.let { buildingId -> worldBuilderApi.placeBuilding(buildingId = buildingId, row = row, column = column) }
            WorldBuilderHudView.Tab.UNITS ->
                selectedUnit?.let { (playerId, unitId) -> worldBuilderApi.placeBattleUnit(unitId = unitId, playerId = playerId, row = row, column = column) }
            WorldBuilderHudView.Tab.NPCS -> selectedNpcId?.let { npcId -> worldBuilderApi.placeNpc(npcId = npcId, row = row, column = column) }
        }
    }

    override fun toolSelected(tool: WorldBuilderHudView.Tool) {
        this.tool = tool
        hudView.selectTool(tool)
    }

    override fun tabSelected(tab: WorldBuilderHudView.Tab) {
        this.tab = tab
        hudView.displayTab(tab)
        if (tab != WorldBuilderHudView.Tab.TERRAIN && tool == WorldBuilderHudView.Tool.FILL) toolSelected(WorldBuilderHudView.Tool.PAINT)
    }

    override fun buildingSelected(buildingId: String) {
        selectedBuildingId = buildingId
        hudView.selectBuilding(buildingId)
    }

    override fun unitSelected(
        playerId: String,
        unitId: String,
    ) {
        selectedUnit = playerId to unitId
        hudView.selectUnit(playerId, unitId)
    }

    override fun npcSelected(npcId: String) {
        selectedNpcId = npcId
        hudView.selectNpc(npcId)
        displayStock()
    }

    override fun stockSelected() {
        val npcId = selectedNpcId ?: return
        val npc = worldBuilderApi.searchWorld()?.npcs?.firstOrNull { placedNpc -> placedNpc.id == npcId } ?: return
        val items =
            worldBuilderApi.searchCatalog().lootableItemIds.mapNotNull { lootableItemId ->
                battlefieldView.occupantBitmap("lootableItem/$lootableItemId.png")?.let { bitmap ->
                    WorldBuilderStockView.StockItem(lootableItemId = lootableItemId, name = messages.lootableItemName(lootableItemId), bitmap = bitmap)
                }
            }
        stockView.open(npcId = npcId, npcName = messages.npcName(npcId), items = items, stock = npc.stock)
    }

    override fun stockChanged(
        npcId: String,
        stock: List<String>,
    ) = worldBuilderApi.restockNpc(npcId = npcId, stock = stock)

    override fun sizeChanged(
        rows: Int,
        columns: Int,
    ) = worldBuilderApi.resizeWorld(rows = rows, columns = columns)

    override fun terrainSelected(terrainId: String) = worldBuilderApi.selectTerrain(terrainId)

    override fun undoSelected() = worldBuilderApi.undoEdit()

    override fun redoSelected() = worldBuilderApi.redoEdit()
}
