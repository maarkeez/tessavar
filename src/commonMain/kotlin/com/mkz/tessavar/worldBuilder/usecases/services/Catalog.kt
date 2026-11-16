package com.mkz.tessavar.worldBuilder.usecases.services

import com.mkz.tessavar.worldBuilder.domain.WorldBuilder

interface Catalog {
    suspend fun initResources()

    fun loadCatalog(): WorldBuilder.Dto.CatalogDto
}
