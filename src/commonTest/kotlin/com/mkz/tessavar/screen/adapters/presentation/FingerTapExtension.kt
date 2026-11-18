package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.nextFrame
import korlibs.event.Touch
import korlibs.event.TouchEvent
import korlibs.image.color.Colors
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.Container
import korlibs.korge.view.View
import korlibs.korge.view.solidRect
import korlibs.math.geom.Point
import korlibs.math.geom.Size

const val FINGER_TAP_AREA_SIZE = 200

fun Container.fingerTapOnArea(onTapped: (fingers: Int) -> Unit = {}): FingerTap = FingerTap(solidRect(Size(FINGER_TAP_AREA_SIZE, FINGER_TAP_AREA_SIZE), Colors.DARKGRAY), onTapped)

fun Container.coverWithPassThrough() {
    solidRect(Size(2 * FINGER_TAP_AREA_SIZE, 2 * FINGER_TAP_AREA_SIZE), Colors.TRANSPARENT) { name = FingerTap.PASS_THROUGH }
}

suspend fun ViewsForTesting.tapWithFingers(
    vararg fingers: Point,
    moveBy: Point = Point.ZERO,
    holdFrames: Int = 0,
) {
    fingers.indices.forEach { touching ->
        gameWindow.dispatch(fingerEvent(TouchEvent.Type.START, fingers.take(touching + 1), changing = touching, Touch.Status.ADD))
        nextFrame()
    }
    val lifted = fingers.map { finger -> finger + moveBy }
    if (moveBy != Point.ZERO) {
        gameWindow.dispatch(fingerEvent(TouchEvent.Type.MOVE, lifted, changing = null, Touch.Status.KEEP))
        nextFrame()
    }
    repeat(holdFrames) { nextFrame() }
    fingers.indices.reversed().forEach { lifting ->
        gameWindow.dispatch(fingerEvent(TouchEvent.Type.END, lifted.take(lifting + 1), changing = lifting, Touch.Status.REMOVE))
        nextFrame()
    }
}

suspend fun ViewsForTesting.tapWithFingersOn(
    view: View,
    fingers: Int,
) {
    val bounds = view.getGlobalBounds()
    tapWithFingers(*Array(fingers) { finger -> Point(bounds.left + bounds.width * (finger + 1) / (fingers + 1), bounds.center.y) })
}

private fun fingerEvent(
    type: TouchEvent.Type,
    fingers: List<Point>,
    changing: Int?,
    status: Touch.Status,
) = TouchEvent(type).apply {
    startFrame(type)
    fingers.forEachIndexed { id, finger -> touch(id, finger, if (id == changing) status else Touch.Status.KEEP) }
    endFrame()
}
