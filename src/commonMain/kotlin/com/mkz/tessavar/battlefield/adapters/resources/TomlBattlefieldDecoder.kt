package com.mkz.tessavar.battlefield.adapters.resources

import com.mkz.tessavar.battlefield.adapters.resources.ResourcesBattlefieldLoader.BattlefieldSection
import com.mkz.tessavar.battlefield.domain.BattlefieldError.InvalidBattlefieldFormat
import com.mkz.tessavar.battlefield.usecases.services.BattlefieldDecoder
import com.mkz.tessavar.shared.adapters.toml.TomlService

class TomlBattlefieldDecoder : BattlefieldDecoder {
    override fun decode(text: String): List<List<String>> =
        try {
            TomlService().deserializeSection<BattlefieldSection>(text).battlefield.toTiles()
        } catch (exception: Exception) {
            throw InvalidBattlefieldFormat(exception.message)
        }
}
