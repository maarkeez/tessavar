package com.mkz.tessavar.screen.adapters.presentation

import korlibs.event.Touch
import korlibs.event.TouchEvent
import korlibs.korge.view.View
import korlibs.korge.view.Views
import korlibs.korge.view.hasAncestor
import korlibs.math.geom.Point
import korlibs.time.DateTime
import korlibs.time.milliseconds

class FingerTap(
    private val view: View,
    private val onTapped: (fingers: Int) -> Unit = {},
) {
    companion object {
        val DURATION = 300.milliseconds
        const val DISTANCE = 10.0
        val HOLD_BACK_AFTER_LIFT = 100.milliseconds

        const val PASS_THROUGH = "FINGER_TAP_PASS_THROUGH"
    }

    private class Tap(
        val startTime: DateTime,
    ) {
        val startPositions = mutableMapOf<Int, Point>()
        var isOnView = true
        var mostFingers = 0
        var hasMoved = false
    }

    private var views: Views? = null
    private var tap: Tap? = null
    private var liftTime: DateTime? = null

    val isHoldingBack: Boolean
        get() {
            if ((tap?.mostFingers ?: 0) >= 2) return true
            val lifted = liftTime ?: return false
            val now = views?.timeProvider?.now() ?: return false
            return now - lifted < HOLD_BACK_AFTER_LIFT
        }

    init {
        view.onEvents(*TouchEvent.Type.ALL) { event ->
            val eventViews = event.target as? Views ?: return@onEvents
            views = eventViews
            handle(event, eventViews.timeProvider.now())
        }
    }

    private fun handle(
        event: TouchEvent,
        now: DateTime,
    ) {
        val fingers = event.touches.filter { touch -> touch.status != Touch.Status.REMOVE }
        val currentTap = tap ?: startTap(event, fingers, now) ?: return
        fingers.forEach { finger ->
            val start =
                currentTap.startPositions.getOrPut(finger.id) {
                    if (!isOnView(finger)) currentTap.isOnView = false
                    finger.p
                }
            if (Point.distance(start, finger.p) > DISTANCE) currentTap.hasMoved = true
        }
        currentTap.mostFingers = maxOf(currentTap.mostFingers, fingers.size)
        if (fingers.isNotEmpty()) return
        tap = null
        if (currentTap.mostFingers >= 2) liftTime = now
        val isQuick = now - currentTap.startTime <= DURATION
        if (currentTap.isOnView && isQuick && !currentTap.hasMoved && currentTap.mostFingers >= 2) onTapped(currentTap.mostFingers)
    }

    private fun startTap(
        event: TouchEvent,
        fingers: List<Touch>,
        now: DateTime,
    ): Tap? {
        if (event.type != TouchEvent.Type.START || fingers.isEmpty()) return null
        liftTime = null
        return Tap(startTime = now).also { started -> tap = started }
    }

    private fun isOnView(finger: Touch): Boolean {
        val hit = view.stage?.hitTest(finger.p) ?: return false
        return hit.hasAncestor(view) || hit.name == PASS_THROUGH
    }
}
