package com.mkz.tessavar.shared.adapters.toml

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.decodeFromString

class TomlService {
    companion object {
        val sections = Toml(inputConfig = TomlInputConfig(ignoreUnknownNames = true))
    }

    inline fun <reified T> deserialize(toml: String): T = Toml.Default.decodeFromString(toml)

    inline fun <reified T> deserializeSection(toml: String): T = sections.decodeFromString(toml)
}
