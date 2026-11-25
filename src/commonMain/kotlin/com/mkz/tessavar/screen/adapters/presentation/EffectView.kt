package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import com.mkz.tessavar.shared.adapters.presentation.HudStyle.panel
import com.mkz.tessavar.shared.adapters.presentation.blink
import korlibs.korge.animate.Animator
import korlibs.korge.ui.UIContainer
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.align.centerOn
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.korge.view.roundRect
import korlibs.korge.view.xy
import korlibs.math.geom.RectCorners
import korlibs.math.geom.Size

class EffectView(
    size: Size,
    private val resourcesLoader: GameSceneResourcesLoader,
) : UIContainer(size) {
    companion object {
        const val EFFECT = "EFFECT"
        const val CHIP = "CHIP"
        const val REMAINING_DURATION = "REMAINING_DURATION"
        const val STACKS = "STACKS"

        private const val CHIP_RADIUS = 5.0
        private const val BADGE_TEXT_SIZE = 9.0
        private const val BADGE_INSET = 0.0
        private const val BADGE_PADDING = 1.5
        private const val BADGE_RADIUS = 3.0
    }

    internal var blinkAnimator: Animator? = null

    internal var effect: EffectsView.Effect? = null
        private set

    internal var isSelected = false
        private set

    fun displayEffect(effect: EffectsView.Effect) {
        val container = this
        this.effect = effect
        listOf(CHIP, EFFECT, REMAINING_DURATION, STACKS).forEach { name -> findViewByName(name)?.removeFromParent() }
        chip()
        val effectBitMap = resourcesLoader.bitmap("effect/${effect.effectId}.png")
        image(effectBitMap) {
            name = EFFECT
            smoothing = false
            scale = 1.0
            centerOn(container)
        }
        effect.remainingDuration?.let { remainingDuration ->
            val badge = badge(REMAINING_DURATION, "$remainingDuration")
            badge.xy(width - BADGE_INSET - badge.width, height - BADGE_INSET - badge.height)
        }
        if (effect.stacks > 1) {
            val badge = badge(STACKS, "x${effect.stacks}")
            badge.xy(width - BADGE_INSET - badge.width, BADGE_INSET)
        }
        stopBlinking()
        if (effect.isNew) blinkAnimator = blink(firstAlpha = 0.5, secondAlpha = 1.0)
        visible = true
    }

    fun select() {
        isSelected = true
        redrawChip()
    }

    fun unselect() {
        isSelected = false
        redrawChip()
    }

    fun hide() {
        stopBlinking()
        isSelected = false
        visible = false
    }

    private fun redrawChip() {
        val chip = findViewByName(CHIP) ?: return
        val index = chip.index
        chip.removeFromParent()
        chip().also { newChip -> newChip.removeFromParent() }.let { newChip -> addChildAt(newChip, index) }
    }

    private fun chip() =
        panel(
            size,
            fill = HudStyle.PANEL_RAISED,
            radius = CHIP_RADIUS,
            stroke = if (isSelected || effect?.isNew == true) HudStyle.SELECTION else HudStyle.STROKE,
        ).also { chip -> chip.name = CHIP }

    private fun badge(
        name: String,
        value: String,
    ): Container =
        container {
            this.name = name
            val label = Text(value, font = HudFonts.TITLE, textSize = BADGE_TEXT_SIZE, color = HudStyle.TEXT)
            roundRect(Size(label.width + 2 * BADGE_PADDING, BADGE_TEXT_SIZE + 1), RectCorners(BADGE_RADIUS), fill = HudStyle.PANEL)
            addChild(label.apply { xy(BADGE_PADDING, 0.0) })
        }

    private fun stopBlinking() {
        blinkAnimator?.cancel()
        blinkAnimator = null
        alpha = 1.0
    }
}
