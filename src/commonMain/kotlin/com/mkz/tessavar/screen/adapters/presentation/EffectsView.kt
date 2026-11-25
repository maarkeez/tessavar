package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.HudFonts
import com.mkz.tessavar.shared.adapters.presentation.HudStyle
import korlibs.korge.input.onClick
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.view.Container
import korlibs.korge.view.Text
import korlibs.korge.view.text
import korlibs.math.geom.Size

class EffectsView(
    resourcesLoader: GameSceneResourcesLoader,
) : Container() {
    data class Effect(
        val effectId: String,
        val stacks: Int = 1,
        val remainingDuration: Int? = null,
        val isNew: Boolean = false,
    )

    internal lateinit var effectViews: Array<EffectView>

    internal lateinit var overflowLabel: Text

    private var onEffectTapped: ((String) -> Unit)? = null

    private val effectViewsLayout =
        uiHorizontalStack(padding = 2.0) {
            val effectViewSize = Size(width = 20, height = 20)
            effectViews = Array(10) { EffectView(effectViewSize, resourcesLoader) }
            effectViews.forEach(::addChild)
            overflowLabel = text("+", font = HudFonts.TITLE, textSize = HudStyle.BODY, color = HudStyle.TEXT)
        }

    init {
        visible = false
        addChild(effectViewsLayout)
        effectViews.forEach { effectView ->
            effectView.onClick {
                val effect = effectView.effect
                if (effectView.visible && effect != null) onEffectTapped?.invoke(effect.effectId)
            }
        }
    }

    fun setOnEffectTapped(onEffectTapped: (String) -> Unit) {
        this.onEffectTapped = onEffectTapped
    }

    fun select(effectId: String?) {
        effectViews.forEach { effectView -> if (effectView.effect?.effectId == effectId && effectView.visible) effectView.select() else effectView.unselect() }
    }

    fun display(effects: List<Effect>) {
        effectViews.forEach { effectView -> effectView.hide() }
        effects.take(effectViews.size).forEachIndexed { index, effect -> effectViews[index].displayEffect(effect) }
        overflowLabel.visible = effects.size > effectViews.size
        visible = true
    }

    fun hide() {
        visible = false
    }
}
