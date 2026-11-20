package com.mkz.tessavar.shared.adapters.clip

import korlibs.math.geom.Point
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

private const val SHOWN_FRAMES = 4
private const val FIRST_RADIUS = 10.0
private const val RADIUS_GROWTH_PER_FRAME = 4.0
private const val RING_WIDTH = 3f
private const val OUTLINE_WIDTH = 2f
private val RING_COLOR = Color.WHITE
private val OUTLINE_COLOR = Color(0x2b, 0x2b, 0x2b)

data class Tap(
    val position: Point,
    val firstFrame: Int,
) {
    fun isShownAt(frame: Int): Boolean = frame - firstFrame in 0 until SHOWN_FRAMES

    fun radiusAt(frame: Int): Double = FIRST_RADIUS + (frame - firstFrame) * RADIUS_GROWTH_PER_FRAME
}

fun BufferedImage.withTaps(
    taps: List<Tap>,
    frame: Int,
    scale: Int,
): BufferedImage {
    if (taps.isEmpty()) return this
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    graphics.drawImage(this, 0, 0, null)
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
    taps.forEach { tap ->
        val radius = tap.radiusAt(frame) * scale
        val x = (tap.position.x * scale - radius).toInt()
        val y = (tap.position.y * scale - radius).toInt()
        val diameter = (radius * 2).toInt()
        graphics.stroke = BasicStroke((RING_WIDTH + OUTLINE_WIDTH * 2) * scale)
        graphics.color = OUTLINE_COLOR
        graphics.drawOval(x, y, diameter, diameter)
        graphics.stroke = BasicStroke(RING_WIDTH * scale)
        graphics.color = RING_COLOR
        graphics.drawOval(x, y, diameter, diameter)
    }
    graphics.dispose()
    return image
}
