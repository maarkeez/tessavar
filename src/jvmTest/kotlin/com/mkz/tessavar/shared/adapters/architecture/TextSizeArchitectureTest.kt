package com.mkz.tessavar.shared.adapters.architecture

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlin.test.Test

class TextSizeArchitectureTest {
    private val viewPackages = listOf("com.mkz.tessavar.screen.adapters.presentation", "com.mkz.tessavar.shared.adapters.presentation")

    private val viewFiles =
        Konsist
            .scopeFromProduction()
            .files
            .filter { file -> file.packagee?.fullyQualifiedName in viewPackages }

    @Test
    fun `Views should not draw text under 12 px when they set a text size constant`() {
        // Given
        val textSizeNames = Regex("""textSize = ([A-Za-z_][A-Za-z0-9_.]*|[0-9.]+)""")
        val constants = Regex("""const val ([A-Z][A-Z0-9_]*) = ([0-9.]+)\b""")
        // When
        val constantsByFile = viewFiles.associate { file -> file.name to constants.findAll(file.text).associate { match -> match.groupValues[1] to match.groupValues[2].toDouble() } }
        val tooSmall =
            viewFiles.flatMap { file ->
                textSizeNames
                    .findAll(file.text)
                    .map { match -> match.groupValues[1] }
                    .mapNotNull { size ->
                        val owner = if ("." in size) size.substringBeforeLast(".") else file.name
                        (size.toDoubleOrNull() ?: constantsByFile[owner]?.get(size.substringAfterLast(".")))?.let { value -> size to value }
                    }.filter { (_, value) -> value < MINIMUM_TEXT_SIZE }
                    .map { (size, value) -> "${file.name}: $size = $value" }
                    .toList()
            }
        // Then
        viewFiles.shouldNotBeEmpty()
        withClue("Texts under $MINIMUM_TEXT_SIZE px: $tooSmall. Use a size of the type scale in HudStyle.") {
            tooSmall.distinct().shouldBeEmpty()
        }
    }

    private companion object {
        const val MINIMUM_TEXT_SIZE = 12.0
    }
}
