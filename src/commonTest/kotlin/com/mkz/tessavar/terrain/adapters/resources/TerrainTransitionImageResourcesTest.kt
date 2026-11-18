package com.mkz.tessavar.terrain.adapters.resources

import com.mkz.tessavar.shared.adapters.resources.ResourcesBundle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import korlibs.image.format.readBitmap
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TerrainTransitionImageResourcesTest {
    @Test
    fun `should find every terrain transition strip in the resources as 16 tiles of 32x32 pixels`() =
        runTest {
            // Given
            val stripNames =
                ResourcesBundle.root[TRANSITIONS_DIRECTORY]
                    .listNames()
                    .filter { fileName -> fileName.endsWith(".png") }
            // When
            val mismatches =
                stripNames.mapNotNull { stripName ->
                    val strip = ResourcesBundle.root["$TRANSITIONS_DIRECTORY/$stripName"].readBitmap()
                    if (strip.width == STRIP_WIDTH && strip.height == TILE_PIXEL_SIZE) return@mapNotNull null
                    "$stripName: ${strip.width}x${strip.height} instead of ${STRIP_WIDTH}x$TILE_PIXEL_SIZE"
                }
            // Then
            stripNames.shouldNotBeEmpty()
            mismatches.shouldBeEmpty()
        }

    private companion object {
        const val TRANSITIONS_DIRECTORY = "terrain/transitions"
        const val TILE_PIXEL_SIZE = 32
        const val TILES_PER_STRIP = 16
        const val STRIP_WIDTH = TILE_PIXEL_SIZE * TILES_PER_STRIP
    }
}
