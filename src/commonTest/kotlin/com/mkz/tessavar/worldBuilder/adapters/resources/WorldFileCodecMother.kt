package com.mkz.tessavar.worldBuilder.adapters.resources

import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldDecoder
import com.mkz.tessavar.battlefield.adapters.resources.TomlBattlefieldEncoder
import com.mkz.tessavar.battlefield.usecases.queries.ExportBattlefield
import com.mkz.tessavar.battlefield.usecases.queries.ParseBattlefield

object WorldFileCodecMother {
    fun codec() = TomlWorldFileCodec(ParseBattlefield(TomlBattlefieldDecoder()), ExportBattlefield(TomlBattlefieldEncoder()))
}
