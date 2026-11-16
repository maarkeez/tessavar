package com.mkz.tessavar.world.usecases.services

import com.mkz.tessavar.world.domain.World

interface WorldLoader {
    suspend fun initResources() {}

    fun loadWorlds(): List<World.Dto>
}
