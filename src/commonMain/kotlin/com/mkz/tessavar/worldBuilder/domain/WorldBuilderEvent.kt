package com.mkz.tessavar.worldBuilder.domain

import com.mkz.tessavar.shared.domain.DomainEvent

sealed interface WorldBuilderEvent : DomainEvent {
    data class WorldOpened(
        val rows: Int,
        val columns: Int,
    ) : WorldBuilderEvent

    data class WorldResized(
        val rows: Int,
        val columns: Int,
    ) : WorldBuilderEvent

    data class TerrainSelected(
        val terrainId: String,
    ) : WorldBuilderEvent

    data class TilePainted(
        val row: Int,
        val column: Int,
        val terrainId: String,
    ) : WorldBuilderEvent

    data class AreaFilled(
        val row: Int,
        val column: Int,
        val terrainId: String,
        val tileCount: Int,
    ) : WorldBuilderEvent

    data class EditUndone(
        val rows: Int,
        val columns: Int,
    ) : WorldBuilderEvent

    data class EditRedone(
        val rows: Int,
        val columns: Int,
    ) : WorldBuilderEvent

    data class WorldRenamed(
        val name: String,
        val author: String,
    ) : WorldBuilderEvent

    data class WorldImported(
        val rows: Int,
        val columns: Int,
    ) : WorldBuilderEvent

    data class WorldImportRejected(
        val reason: Reason,
        val terrainId: String? = null,
        val adjacentTerrainId: String? = null,
        val occupantId: String? = null,
    ) : WorldBuilderEvent {
        enum class Reason {
            NOT_A_WORLD,
            OTHER_WORLD_VERSION,
            SIZE_OUT_OF_RANGE,
            UNKNOWN_TERRAIN,
            FORBIDDEN_ADJACENT_TERRAINS,
            UNKNOWN_OCCUPANT,
            DUPLICATE_OCCUPANT,
            MISPLACED_OCCUPANT,
        }
    }

    data class OccupantPlaced(
        val occupantId: String,
        val row: Int,
        val column: Int,
    ) : WorldBuilderEvent

    data class OccupantPlaceRejected(
        val row: Int,
        val column: Int,
        val reason: Reason,
        val terrainId: String? = null,
    ) : WorldBuilderEvent {
        enum class Reason {
            OUT_OF_WORLD,
            NOT_OCCUPIABLE,
            CORNERED,
            NOT_VACANT,
        }
    }

    data class OccupantErased(
        val occupantId: String,
        val row: Int,
        val column: Int,
    ) : WorldBuilderEvent

    data class NpcRestocked(
        val npcId: String,
        val stock: List<String>,
    ) : WorldBuilderEvent

    data class OccupiedTilePaintRejected(
        val row: Int,
        val column: Int,
        val terrainId: String,
    ) : WorldBuilderEvent

    data class TilePaintRejected(
        val row: Int,
        val column: Int,
        val terrainId: String,
        val adjacentTerrainId: String,
    ) : WorldBuilderEvent

    data class QuestAdded(
        val questId: String,
    ) : WorldBuilderEvent

    data class QuestRemoved(
        val questId: String,
    ) : WorldBuilderEvent

    data class QuestEdited(
        val questId: String,
    ) : WorldBuilderEvent

    data class QuestRejected(
        val reason: Reason,
    ) : WorldBuilderEvent {
        enum class Reason {
            MAIN_QUEST_TAKEN,
        }
    }
}
