package com.mkz.tessavar.worldBuilder.usecases.queries

import com.mkz.tessavar.worldBuilder.domain.WorldBuilder
import com.mkz.tessavar.worldBuilder.usecases.services.Catalog

class SearchCatalog(
    private val catalog: Catalog,
) {
    operator fun invoke(): WorldBuilder.Dto.CatalogDto = catalog.loadCatalog()
}
