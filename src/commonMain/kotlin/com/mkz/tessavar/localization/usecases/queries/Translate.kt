package com.mkz.tessavar.localization.usecases.queries

import com.mkz.tessavar.localization.domain.LocalizationError.LocalizationNotFound
import com.mkz.tessavar.localization.domain.LocalizationRepository

class Translate(
    private val localizationRepository: LocalizationRepository,
) {
    operator fun invoke(
        key: String,
        arguments: Map<String, String> = emptyMap(),
        fallback: String? = null,
    ): String = (localizationRepository.search() ?: throw LocalizationNotFound()).translate(key, arguments, fallback)
}
