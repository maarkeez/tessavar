package com.mkz.tessavar.world.usecases.queries

import com.mkz.tessavar.world.domain.World
import com.mkz.tessavar.world.usecases.services.FakeWorldLoader
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class SearchCommunityWorldsTest {
    private val campaign = world(id = World.CAMPAIGN_ID, name = "Tessavar")
    private val sunkenKeep = world(id = "the-sunken-keep", name = "The Sunken Keep")
    private val ashenPass = world(id = "ashen-pass", name = "Ashen Pass")

    @Test
    fun `SearchCommunityWorlds should return every world other than the campaign, sorted by name, when worlds are shipped`() {
        // Given
        val searchCommunityWorlds = SearchCommunityWorlds(FakeWorldLoader(listOf(sunkenKeep, campaign, ashenPass)))
        // When
        val worlds = searchCommunityWorlds()
        // Then
        worlds.shouldContainExactly(ashenPass, sunkenKeep)
    }

    @Test
    fun `SearchCommunityWorlds should return nothing when only the campaign is shipped`() {
        // Given
        val searchCommunityWorlds = SearchCommunityWorlds(FakeWorldLoader(listOf(campaign)))
        // When
        val worlds = searchCommunityWorlds()
        // Then
        worlds.shouldBeEmpty()
    }

    private fun world(
        id: String,
        name: String,
    ) = World.Dto(id = id, name = name, author = "maarkeez", version = World.CURRENT_VERSION)
}
