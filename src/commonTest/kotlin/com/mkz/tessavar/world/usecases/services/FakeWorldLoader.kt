package com.mkz.tessavar.world.usecases.services

import com.mkz.tessavar.world.domain.World

class FakeWorldLoader(
    private val worlds: List<World.Dto>,
) : WorldLoader {
    override fun loadWorlds(): List<World.Dto> = worlds
}
