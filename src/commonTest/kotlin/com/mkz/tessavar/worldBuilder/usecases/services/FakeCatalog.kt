package com.mkz.tessavar.worldBuilder.usecases.services

import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.domain.WorldBuilderMother

class FakeCatalog(
    private val catalog: WorldBuilder.Dto.CatalogDto = WorldBuilderMother.catalog(),
) : Catalog {
    override suspend fun initResources() = Unit

    override fun loadCatalog(): WorldBuilder.Dto.CatalogDto = catalog
}
