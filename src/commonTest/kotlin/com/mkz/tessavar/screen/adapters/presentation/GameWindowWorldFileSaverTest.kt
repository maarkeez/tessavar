package com.mkz.tessavar.screen.adapters.presentation

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.io.file.VfsFile
import korlibs.io.file.std.MemoryVfsMix
import korlibs.render.DialogInterface
import korlibs.render.FileFilter
import korlibs.render.GameWindow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GameWindowWorldFileSaverTest {
    private class FakeFileDialogGameWindow(
        private val pickedFiles: List<VfsFile>,
    ) : GameWindow() {
        val dialogs = mutableListOf<Pair<FileFilter?, Boolean>>()

        override val dialogInterface: DialogInterface =
            object : DialogInterface {
                override suspend fun openFileDialog(
                    filter: FileFilter?,
                    write: Boolean,
                    multi: Boolean,
                    currentDir: VfsFile?,
                ): List<VfsFile> {
                    dialogs += filter to write
                    return pickedFiles
                }
            }
    }

    private val files = MemoryVfsMix()
    private val downloads = mutableListOf<Pair<String, String>>()
    private val noDownload = { _: String, _: String -> false }
    private val download = { fileName: String, text: String -> downloads.add(fileName to text) }

    @Test
    fun `saveWorld should download the file and open no dialog when the platform downloads files`() =
        runTest {
            // Given
            val gameWindow = FakeFileDialogGameWindow(listOf(files["picked.toml"]))
            // When
            val saved = GameWindowWorldFileSaver(gameWindow, MessagesMother.messages(), download).saveWorld("world.toml", "version = 1")
            // Then
            saved shouldBe true
            downloads.shouldContainExactly("world.toml" to "version = 1")
            gameWindow.dialogs.shouldBeEmpty()
        }

    @Test
    fun `saveWorld should write the file picked in a save dialog when the platform does not download files`() =
        runTest {
            // Given
            val gameWindow = FakeFileDialogGameWindow(listOf(files["picked.toml"]))
            // When
            val saver = GameWindowWorldFileSaver(gameWindow, MessagesMother.messages(), noDownload)
            val saved = saver.saveWorld("world.toml", "version = 1")
            // Then
            saved shouldBe true
            files["picked.toml"].readString() shouldBe "version = 1"
            gameWindow.dialogs.shouldContainExactly(saver.worldFiles to true)
        }

    @Test
    fun `saveWorld should save nothing when no file is picked`() =
        runTest {
            // Given
            val gameWindow = FakeFileDialogGameWindow(emptyList())
            // When
            val saved = GameWindowWorldFileSaver(gameWindow, MessagesMother.messages(), noDownload).saveWorld("world.toml", "version = 1")
            // Then
            saved shouldBe false
        }
}
