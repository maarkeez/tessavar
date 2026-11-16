package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.battlefield.domain.BattlefieldError.InvalidBattlefieldFormat
import com.mkz.tessavar.battlefield.usecases.queries.ExportBattlefield
import com.mkz.tessavar.battlefield.usecases.queries.ParseBattlefield
import com.mkz.tessavar.shared.adapters.toml.TomlService
import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.usecases.services.WorldFileCodec
import kotlinx.serialization.Serializable

class TomlWorldFileCodec(
    private val parseBattlefield: ParseBattlefield,
    private val exportBattlefield: ExportBattlefield,
) : WorldFileCodec {
    private companion object {
        const val BATTLEFIELD_TABLE = "[battlefield]"
        val TABLE_HEADER = Regex("""^\[\[?\s*[A-Za-z0-9_.\-]+\s*]]?\s*(#.*)?$""")
    }

    override fun decode(text: String): WorldBuilder.Dto.WorldFileDto? {
        val header =
            try {
                TomlService().deserializeSection<WorldHeader>(text)
            } catch (_: Exception) {
                return null
            }
        val version = header.version ?: return null
        val tiles =
            try {
                parseBattlefield(text)
            } catch (_: InvalidBattlefieldFormat) {
                return null
            }
        return WorldBuilder.Dto.WorldFileDto(version = version, name = header.name, author = header.author, tiles = tiles, keptSections = keptSections(text))
    }

    override fun encode(file: WorldBuilder.Dto.WorldFileDto): String {
        val header = "version = ${file.version}\nname = ${quoted(file.name)}\nauthor = ${quoted(file.author)}\n"
        val keptSections = if (file.keptSections.isBlank()) "" else "\n${file.keptSections}\n"
        return "$header\n${exportBattlefield(file.tiles)}$keptSections"
    }

    private fun keptSections(text: String): String {
        val tables = mutableListOf<MutableList<String>>()
        text.lines().forEach { line ->
            if (TABLE_HEADER.matches(line.trim()) && !line.startsWith(" ")) tables += mutableListOf(line) else tables.lastOrNull()?.add(line)
        }
        return tables
            .filterNot { table -> table.first().trim() == BATTLEFIELD_TABLE }
            .joinToString("\n") { table -> table.joinToString("\n").trimEnd() + "\n" }
            .trimEnd()
    }

    private fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    @Serializable
    data class WorldHeader(
        val version: Int? = null,
        val name: String = "",
        val author: String = "",
    )
}
