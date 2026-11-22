package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.screen.adapters.presentation.BattleUnitInfoMother.battleUnitInfo
import com.mkz.tessavar.screen.adapters.presentation.BattleUnitInfoMother.turnActions
import com.mkz.tessavar.screen.adapters.presentation.GameSceneResourcesLoaderMother.loadedResourcesLoader
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test

class BattleUnitInfoViewTest : ViewsForTesting() {
    private companion object {
        const val FOCUS_ANIMATION_FRAMES = 20
    }

    private val sword =
        AbilityInfoView.AbilityInfo(
            abilityId = "sword",
            manaCost = 0,
            cooldown = 0,
            targeting = AbilityInfoView.AbilityInfo.Targeting.ADJACENT_ENEMY,
            effects = listOf(AbilityInfoView.AbilityInfo.Effect.Damage(10)),
        )

    private val venom =
        EffectInfoView.EffectInfo(
            "venom-damage",
            stacks = 1,
            sentence = EffectInfoView.EffectInfo.Sentence.DamageOnPlayerTurnStarted(3),
            remainingDurations = listOf(4),
            isExploring = false,
        )
    private val soak = EffectInfoView.EffectInfo("soak", stacks = 1, sentence = EffectInfoView.EffectInfo.Sentence.Resist("burn", 50), remainingDurations = listOf(2), isExploring = false)

    private fun poisonedAndSoaked(
        battleUnitId: String = "knight-1",
        venomInfo: EffectInfoView.EffectInfo = venom,
        withVenom: Boolean = true,
    ) = battleUnitInfo(
        effects =
            listOfNotNull(
                EffectsView.Effect("venom-damage", remainingDuration = venomInfo.remainingDurations.max()).takeIf { withVenom },
                EffectsView.Effect("soak", remainingDuration = 2),
            ),
    ).copy(battleUnitId = battleUnitId, effectInfos = listOfNotNull(("venom-damage" to venomInfo).takeIf { withVenom }, "soak" to soak).toMap())

    private fun BattleUnitInfoView.displayBattleUnit(battleUnit: BattleUnitInfo) = display(battleUnit = battleUnit, turnActions = turnActions(), remainingTurns = null, abilities = emptyList())

    private fun BattleUnitInfoView.display(
        turnActions: BattleUnitInfoView.TurnActions = turnActions(),
        remainingTurns: Int? = null,
        abilities: List<BattleUnitInfoView.Ability> = emptyList(),
    ) = display(battleUnit = battleUnitInfo(), turnActions = turnActions, remainingTurns = remainingTurns, abilities = abilities)

    @Test
    fun `Init should not be visible when is initialized`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            // Then
            battleUnitInfoView.isVisibleToUser().shouldBeFalse()
        }

    @Test
    fun `Display should be visible when is displayed`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            battleUnitInfoView.display()
            // Then
            battleUnitInfoView.isVisibleToUser().shouldBeTrue()
        }

    @Test
    fun `BattleUnitInfoView should ring the portrait green when the battle unit is an ally`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            battleUnitInfoView.display(battleUnit = battleUnitInfo(isHumanPlayers = true), turnActions = turnActions(), remainingTurns = null, abilities = emptyList())
            // Then
            battleUnitInfoView.portraitRingColor() shouldBe HudStyle.ALLY
        }

    @Test
    fun `Display should display the remaining steps and casts out of their maximum when is displayed`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            battleUnitInfoView.display(turnActions = turnActions(remainingSteps = 2, maximumSteps = 3, remainingCasts = 1, maximumCasts = 1))
            // Then
            battleUnitInfoView.remainingSteps() shouldBe 2
            battleUnitInfoView.maximumSteps() shouldBe 3
            battleUnitInfoView.remainingCasts() shouldBe 1
            battleUnitInfoView.maximumCasts() shouldBe 1
        }

    @Test
    fun `Display should display the steps in white and the casts in gold when is displayed`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            battleUnitInfoView.display()
            // Then
            battleUnitInfoView.movementPointColors().shouldContainExactly(BattleUnitInfoView.MOVEMENT_POINTS_COLOR)
            battleUnitInfoView.castPointColors().shouldContainExactly(BattleUnitInfoView.CAST_POINTS_COLOR)
        }

    @Test
    fun `Display should display no remaining casts when the battle unit has cast this turn`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            battleUnitInfoView.display(turnActions = turnActions(remainingCasts = 0, maximumCasts = 1))
            // Then
            battleUnitInfoView.remainingCasts() shouldBe 0
            battleUnitInfoView.maximumCasts() shouldBe 1
        }

    @Test
    fun `Display should only darken the abilities in cooldown when the battle unit has no casts left`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            val abilities =
                listOf(
                    BattleUnitInfoView.Ability(abilityId = "sword", remainingCooldown = 0, hasEnoughMana = true, manaCost = 10),
                    BattleUnitInfoView.Ability(abilityId = "mushroom", remainingCooldown = 2, hasEnoughMana = true, manaCost = 10),
                    BattleUnitInfoView.Ability(abilityId = "heal", remainingCooldown = 0, hasEnoughMana = false, manaCost = 10),
                )
            // When
            battleUnitInfoView.display(turnActions = turnActions(remainingCasts = 0, maximumCasts = 1), abilities = abilities)
            // Then
            val (sword, mushroom, heal) = battleUnitInfoView.abilityButtons()
            sword.isAbilityIconDarkened().shouldBeFalse()
            sword.isAbilityIconTintedBlue().shouldBeFalse()
            mushroom.isAbilityIconDarkened().shouldBeTrue()
            mushroom.cooldownBadgeText() shouldBe "2"
            heal.isAbilityIconTintedBlue().shouldBeTrue()
        }

    @Test
    fun `Display should tell how many turns a summon has left, after its name`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            // When
            battleUnitInfoView.display(remainingTurns = 3)
            // Then
            battleUnitInfoView.remainingTurnsText() shouldBe "Vanishes in 3 turns"
            battleUnitInfoView.isRemainingTurnsAfterName().shouldBeTrue()
        }

    @Test
    fun `Display should not tell any turns left when the battle unit never vanishes`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display(remainingTurns = 3)
            // When
            battleUnitInfoView.display(remainingTurns = null)
            // Then
            battleUnitInfoView.remainingTurnsText() shouldBe null
        }

    @Test
    fun `DisplayAbilitySelected should select ability button when ability is selected`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            val selectedAbilityIndex = 0
            // When
            battleUnitInfoView.displayAbilitySelected(selectedAbilityIndex)
            // Then
            val abilityButtons = battleUnitInfoView.abilityButtons()
            val selectedAbilityButton = abilityButtons[selectedAbilityIndex]
            selectedAbilityButton.isSelected().shouldBeTrue()
        }

    @Test
    fun `DisplayAbilitySelected should unselect other ability buttons when ability is selected`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            val abilityButtons = battleUnitInfoView.abilityButtons()
            abilityButtons.forEach { abilityButton -> abilityButton.select() }
            val selectedAbilityIndex = 0
            // When
            battleUnitInfoView.displayAbilitySelected(selectedAbilityIndex)
            // Then
            abilityButtons.forEachIndexed { index, abilityButton ->
                val isSelected = index == selectedAbilityIndex
                abilityButton.isSelected() shouldBe isSelected
            }
        }

    @Test
    fun `BattleUnitInfoView should display the ability info under the ability buttons when an ability is selected`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display()
            // When
            battleUnitInfoView.displayAbilitySelected(0)
            battleUnitInfoView.displayAbilityInfo(sword)
            // Then
            battleUnitInfoView.isAbilityInfoDisplayed().shouldBeTrue()
            battleUnitInfoView.displayedAbilityName() shouldBe "Sword"
            battleUnitInfoView.isAbilityInfoUnderAbilityButtons().shouldBeTrue()
        }

    @Test
    fun `BattleUnitInfoView should hide the ability info when it displays a battle unit`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display()
            battleUnitInfoView.displayAbilityInfo(sword)
            // When
            battleUnitInfoView.display()
            // Then
            battleUnitInfoView.isAbilityInfoDisplayed().shouldBeFalse()
        }

    @Test
    fun `BattleUnitInfoView should hide the ability info when it hides the ability info`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display()
            battleUnitInfoView.displayAbilityInfo(sword)
            // When
            battleUnitInfoView.hideAbilityInfo()
            // Then
            battleUnitInfoView.isAbilityInfoDisplayed().shouldBeFalse()
        }

    @Test
    fun `BattleUnitInfoView should collapse the header when it displays the ability info`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            addChild(battleUnitInfoView)
            battleUnitInfoView.display()
            // When
            battleUnitInfoView.displayAbilityInfo(sword)
            repeat(FOCUS_ANIMATION_FRAMES) { delayFrame() }
            // Then
            battleUnitInfoView.isHeaderCollapsed().shouldBeTrue()
            battleUnitInfoView.areAbilityButtonsUnderHeader().shouldBeTrue()
            battleUnitInfoView.isAbilityInfoUnderAbilityButtons().shouldBeTrue()
        }

    @Test
    fun `BattleUnitInfoView should hide the name, the effects and the inventory button when it displays the ability info`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display(
                battleUnit = battleUnitInfo(effects = listOf(EffectsView.Effect("venom-damage", remainingDuration = 5))),
                turnActions = turnActions(),
                remainingTurns = null,
                abilities = emptyList(),
            )
            battleUnitInfoView.displayInventoryButton(onInventoryButtonSelected = {})
            // When
            battleUnitInfoView.displayAbilityInfo(sword)
            // Then
            battleUnitInfoView.isNameDisplayed().shouldBeFalse()
            battleUnitInfoView.areEffectsDisplayed().shouldBeFalse()
            battleUnitInfoView.isInventoryButtonDisplayed().shouldBeFalse()
        }

    @Test
    fun `BattleUnitInfoView should expand the header and show the inventory button again when it hides the ability info`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            addChild(battleUnitInfoView)
            battleUnitInfoView.display()
            battleUnitInfoView.displayInventoryButton(onInventoryButtonSelected = {})
            battleUnitInfoView.displayAbilityInfo(sword)
            // When
            battleUnitInfoView.hideAbilityInfo()
            repeat(FOCUS_ANIMATION_FRAMES) { delayFrame() }
            // Then
            battleUnitInfoView.isHeaderCollapsed().shouldBeFalse()
            battleUnitInfoView.isNameDisplayed().shouldBeTrue()
            battleUnitInfoView.isInventoryButtonDisplayed().shouldBeTrue()
            battleUnitInfoView.areAbilityButtonsUnderHeader().shouldBeTrue()
        }

    @Test
    fun `BattleUnitInfoView should expand the header when it displays a battle unit while focused`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display()
            battleUnitInfoView.displayAbilityInfo(sword)
            // When
            battleUnitInfoView.display()
            // Then
            battleUnitInfoView.isHeaderCollapsed().shouldBeFalse()
            battleUnitInfoView.isNameDisplayed().shouldBeTrue()
        }

    @Test
    fun `Hide should not be visible when is hidden`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display()
            // When
            battleUnitInfoView.hide()
            // Then
            battleUnitInfoView.isVisibleToUser().shouldBeFalse()
        }

    @Test
    fun `DisplayInventoryButton should display the inventory button and open the inventory when the inventory button is clicked`() =
        sharedViewsTest {
            // Given
            var inventoryButtonSelected = false
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.display()
            addChild(battleUnitInfoView)
            battleUnitInfoView.displayInventoryButton(onInventoryButtonSelected = { inventoryButtonSelected = true })
            // When
            clickInventoryButton(battleUnitInfoView)
            // Then
            battleUnitInfoView.isInventoryButtonDisplayed().shouldBeTrue()
            inventoryButtonSelected.shouldBeTrue()
        }

    @Test
    fun `HideInventoryButton should not display the inventory button when the inventory button is hidden`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayInventoryButton(onInventoryButtonSelected = {})
            // When
            battleUnitInfoView.hideInventoryButton()
            // Then
            battleUnitInfoView.isInventoryButtonDisplayed().shouldBeFalse()
        }

    @Test
    fun `TapEffect should display the effect info in place of the abilities when an effect icon is tapped`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            // When
            clickEffect(battleUnitInfoView, 0)
            // Then
            battleUnitInfoView.displayedEffectInfo() shouldBe venom
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeFalse()
            battleUnitInfoView.selectedEffectIds().shouldContainExactly("venom-damage")
        }

    @Test
    fun `TapEffect should display the abilities again when the same effect icon is tapped again`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            // When
            clickEffect(battleUnitInfoView, 0)
            // Then
            battleUnitInfoView.isEffectInfoDisplayed().shouldBeFalse()
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeTrue()
            battleUnitInfoView.selectedEffectIds().shouldBeEmpty()
        }

    @Test
    fun `TapEffect should switch the effect info when another effect icon is tapped`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            // When
            clickEffect(battleUnitInfoView, 1)
            // Then
            battleUnitInfoView.displayedEffectInfo() shouldBe soak
            battleUnitInfoView.selectedEffectIds().shouldContainExactly("soak")
        }

    @Test
    fun `TapEffectInfo should display the abilities again when the effect info is tapped`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            // When
            clickEffectInfo(battleUnitInfoView)
            // Then
            battleUnitInfoView.isEffectInfoDisplayed().shouldBeFalse()
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeTrue()
        }

    @Test
    fun `Display should keep the effect info open when the same battle unit is displayed again`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            val venomLater = venom.copy(remainingDurations = listOf(3))
            // When
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked(venomInfo = venomLater))
            // Then
            battleUnitInfoView.displayedEffectInfo() shouldBe venomLater
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeFalse()
            battleUnitInfoView.selectedEffectIds().shouldContainExactly("venom-damage")
        }

    @Test
    fun `Display should close the effect info when another battle unit is displayed`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            // When
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked(battleUnitId = "knight-2"))
            // Then
            battleUnitInfoView.isEffectInfoDisplayed().shouldBeFalse()
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeTrue()
        }

    @Test
    fun `Display should close the effect info when its effect is no longer ongoing`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            // When
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked(withVenom = false))
            // Then
            battleUnitInfoView.isEffectInfoDisplayed().shouldBeFalse()
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeTrue()
        }

    @Test
    fun `Refresh should display the effect info with its new remaining durations when it is open`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            val venomLater = venom.copy(remainingDurations = listOf(3))
            // When
            battleUnitInfoView.refresh(poisonedAndSoaked(venomInfo = venomLater))
            // Then
            battleUnitInfoView.displayedEffectInfo() shouldBe venomLater
        }

    @Test
    fun `Hide should close the effect info when is hidden`() =
        sharedViewsTest {
            // Given
            val battleUnitInfoView = BattleUnitInfoView(loadedResourcesLoader(), MessagesMother.messages())
            battleUnitInfoView.displayBattleUnit(poisonedAndSoaked())
            addChild(battleUnitInfoView)
            clickEffect(battleUnitInfoView, 0)
            // When
            battleUnitInfoView.hide()
            // Then
            battleUnitInfoView.isEffectInfoDisplayed().shouldBeFalse()
            battleUnitInfoView.areAbilityButtonsDisplayed().shouldBeTrue()
        }
}
