package com.mkz.tessavar.world.domain

import com.mkz.tessavar.world.domain.WorldError.InvalidWorldId
import com.mkz.tessavar.world.domain.WorldError.OtherWorldVersion
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class WorldTest {
    private fun dto(
        id: String = "the-sunken-keep",
        version: Int = 1,
    ) = World.Dto(id = id, name = "The Sunken Keep", author = "maarkeez", version = version)

    @Test
    fun `World should hold its id, name and author when it is of world version 1`() {
        // When
        val world = World.create(dto())
        // Then
        world.toDto() shouldBe dto()
    }

    @Test
    fun `World should fail when it is of another world version`() {
        // When
        val creation = { World.create(dto(version = 2)) }
        // Then
        shouldThrow<OtherWorldVersion> { creation() }
    }

    @Test
    fun `World should fail when its id is not lowercase words joined by hyphens`() {
        // When
        val creation = { World.create(dto(id = "The Sunken Keep")) }
        // Then
        shouldThrow<InvalidWorldId> { creation() }
    }

    @Test
    fun `IdOf should join the lowercase words of the name with hyphens when a world is named`() {
        // When
        val id = World.idOf("The Sunken Keep: Part 2!")
        // Then
        id shouldBe "the-sunken-keep-part-2"
    }
}
