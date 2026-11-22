package com.mkz.tessavar.screen.adapters.presentation

import korlibs.image.paint.Paint
import korlibs.korge.view.Container
import korlibs.korge.view.RoundRect
import korlibs.korge.view.Text

fun EffectView.effectIconCount(): Int = children.count { child -> child.name == EffectView.EFFECT }

fun EffectView.isEffectIconDisplayed(): Boolean = visible && effectIconCount() > 0

fun EffectView.isBlinking(): Boolean = blinkAnimator?.isActive == true

fun EffectView.iconAlpha(): Double = alpha

fun EffectView.remainingDurationText(): String? = badgeText(EffectView.REMAINING_DURATION)

fun EffectView.stacksText(): String? = badgeText(EffectView.STACKS)

fun EffectView.chipStroke(): Paint? = (findViewByName(EffectView.CHIP) as? RoundRect)?.stroke

private fun EffectView.badgeText(name: String): String? =
    (findViewByName(name) as? Container)
        ?.children
        ?.filterIsInstance<Text>()
        ?.firstOrNull()
        ?.text
