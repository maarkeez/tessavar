package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.BattleUnitInfoMother.battleUnitInfo
import com.mkz.tessavar.screen.adapters.presentation.BattleUnitInfoMother.turnActions
import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoaderMother.loadedResourcesLoader
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import com.mkz.tessavar.unit.adapters.resources.ResourcesUnitLoader
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class BattleUnitInfoResourcesTest : ViewsForTesting() {
    private val sword =
        AbilityInfoView.AbilityInfo(
            abilityId = "sword",
            manaCost = 0,
            cooldown = 0,
            targeting = AbilityInfoView.AbilityInfo.Targeting.ADJACENT_ENEMY,
            effects = listOf(AbilityInfoView.AbilityInfo.Effect.Damage(10)),
        )

    @Test
    fun `BattleUnitInfoView should draw the steps of every unit inside the strip when it reads the game resources`() =
        sharedViewsTest {
            // Given
            val units = ResourcesUnitLoader().also { loader -> loader.initResources() }.loadUnits()
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            addChild(battleUnitInfoView)
            // When
            val overflowing =
                units.filterNot { unit ->
                    battleUnitInfoView.display(
                        battleUnit = battleUnitInfo(unitId = unit.id),
                        turnActions = turnActions(remainingSteps = unit.maximumSteps, maximumSteps = unit.maximumSteps),
                        remainingTurns = null,
                        abilities = emptyList(),
                    )
                    battleUnitInfoView.displayAbilityInfo(sword)
                    battleUnitInfoView.areStripStepsInsideItsPanel()
                }
            // Then
            units.shouldNotBeEmpty()
            withClue("Units whose steps end past the strip of the battle unit info: give them fewer steps or lay the strip out again") {
                overflowing.map { unit -> "${unit.id} (${unit.maximumSteps} steps)" }.shouldBeEmpty()
            }
        }
}
