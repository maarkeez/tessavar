package com.mkz.tessavar.shared.usecases.acceptance

import com.mkz.tessavar.unit.adapters.resources.ResourcesUnitLoader
import com.mkz.tessavar.unit.domain.Unit
import com.mkz.tessavar.unit.usecases.services.UnitLoader

class AcceptanceUnitLoader(
    private val resourcesUnitLoader: UnitLoader = ResourcesUnitLoader(),
) : UnitLoader {
    override suspend fun initResources() = resourcesUnitLoader.initResources()

    override fun loadUnits(): List<Unit.Dto> = resourcesUnitLoader.loadUnits() + unitsTheGameNoLongerHas

    private val unitsTheGameNoLongerHas =
        listOf(
            Unit.Dto(
                id = "knight",
                baseMaximumHealth = 100,
                baseMaximumMana = 30,
                startingAbilities = listOf("poisoned-sword", "mushroom", "skull", "teleport", "bee", "heal"),
                maximumSteps = 3,
                startingGold = 20,
            ),
            Unit.Dto(
                id = "goat",
                baseMaximumHealth = 30,
                baseMaximumMana = 30,
                startingAbilities = listOf("bat", "teleport", "heal"),
                maximumSteps = 1,
                startingGold = 0,
            ),
            Unit.Dto(
                id = "viking",
                baseMaximumHealth = 150,
                baseMaximumMana = 30,
                startingAbilities = listOf("sword"),
                maximumSteps = 3,
                startingGold = 100,
            ),
        )
}
