package com.mkz.tessavar.battlefield.adapters.resources

import com.mkz.tessavar.battlefield.usecases.services.BattlefieldEncoder

class TomlBattlefieldEncoder : BattlefieldEncoder {
    private companion object {
        const val HEADER = "[battlefield]"
        const val INDENTATION = "    "

        const val SPACES_BETWEEN_COLUMNS = 2
        const val QUOTES_AND_COMMA = "\"\","
    }

    override fun encode(tiles: List<List<String>>): String {
        val longestTerrainId = tiles.flatten().maxOfOrNull { terrainId -> terrainId.length } ?: 0
        val columnWidth = longestTerrainId + QUOTES_AND_COMMA.length + SPACES_BETWEEN_COLUMNS
        val rows =
            tiles.joinToString(separator = "") { row ->
                val terrainIds = row.dropLast(1).joinToString(separator = "") { terrainId -> "\"$terrainId\",".padEnd(columnWidth) } + "\"${row.last()}\""
                "$INDENTATION[$terrainIds],\n"
            }
        return "$HEADER\ntiles = [\n$rows]\n"
    }
}
