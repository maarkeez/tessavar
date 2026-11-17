package com.mkz.tessavar.screen.adapters.presentation

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.container
import korlibs.korge.view.graphics
import korlibs.math.geom.Point
import korlibs.math.geom.degrees
import korlibs.math.geom.vector.LineCap
import korlibs.math.geom.vector.LineJoin
import korlibs.math.geom.vector.StrokeInfo
import kotlin.math.cos
import kotlin.math.sin

/** PROTOTYPE: the world builder's tool icons drawn as flat shapes in code, in a box of 32 by 32. */
object ToolGlyphs {
    private val COLOR: RGBA = Colors.WHITE
    private const val BOX = 32.0

    fun Container.toolGlyph(icon: WorldBuilderHudView.ToolIcon) {
        when (icon) {
            WorldBuilderHudView.ToolIcon.PAINT -> brush()
            WorldBuilderHudView.ToolIcon.FILL -> bucket()
            WorldBuilderHudView.ToolIcon.ERASE -> eraser()
            WorldBuilderHudView.ToolIcon.UNDO -> container { curvedArrow() }
            WorldBuilderHudView.ToolIcon.REDO ->
                container {
                    scaleX = -1.0
                    x = BOX
                    curvedArrow()
                }
        }
    }

    private fun round(thickness: Double) = StrokeInfo(thickness = thickness, startCap = LineCap.ROUND, endCap = LineCap.ROUND, join = LineJoin.ROUND)

    private fun Container.brush() {
        graphics {
            stroke(COLOR, round(4.0)) {
                moveTo(Point(27.0, 5.0))
                lineTo(Point(20.5, 11.5))
            }
            stroke(COLOR, StrokeInfo(thickness = 7.0, startCap = LineCap.BUTT, endCap = LineCap.BUTT)) {
                moveTo(Point(19.0, 13.0))
                lineTo(Point(16.4, 15.6))
            }
            fill(COLOR) {
                moveTo(Point(13.2, 16.0))
                lineTo(Point(16.0, 18.8))
                quadTo(Point(15.0, 25.0), Point(5.0, 27.5))
                quadTo(Point(7.5, 18.5), Point(13.2, 16.0))
                close()
            }
        }
    }

    private fun Container.bucket() {
        graphics {
            stroke(COLOR, round(2.5)) {
                arc(Point(15.0, 12.0), 7.0, 180.degrees, 360.degrees)
            }
            fill(COLOR) {
                moveTo(Point(6.0, 13.0))
                lineTo(Point(24.0, 13.0))
                lineTo(Point(21.5, 28.0))
                lineTo(Point(8.5, 28.0))
                close()
            }
            fill(COLOR) {
                circle(Point(27.5, 25.0), 2.6)
                moveTo(Point(25.1, 24.0))
                lineTo(Point(27.5, 18.0))
                lineTo(Point(29.9, 24.0))
                close()
            }
        }
    }

    private fun rotated(
        x: Double,
        y: Double,
    ): Point {
        val angle = (-45.0).degrees
        val dx = x - BOX / 2
        val dy = y - BOX / 2
        return Point(BOX / 2 + dx * cos(angle.radians) - dy * sin(angle.radians), BOX / 2 + dx * sin(angle.radians) + dy * cos(angle.radians))
    }

    private fun Container.eraser() {
        graphics {
            fill(COLOR) {
                moveTo(rotated(3.0, 10.5))
                lineTo(rotated(13.0, 10.5))
                lineTo(rotated(13.0, 21.5))
                lineTo(rotated(3.0, 21.5))
                close()
            }
            stroke(COLOR, round(2.5)) {
                moveTo(rotated(3.0, 10.5))
                lineTo(rotated(29.0, 10.5))
                lineTo(rotated(29.0, 21.5))
                lineTo(rotated(3.0, 21.5))
                close()
            }
        }
    }

    private fun Container.curvedArrow() {
        graphics {
            stroke(COLOR, round(3.5)) {
                moveTo(Point(10.0, 12.0))
                lineTo(Point(19.0, 12.0))
                arc(Point(19.0, 18.5), 6.5, 270.degrees, 90.degrees)
                lineTo(Point(12.0, 25.0))
            }
            fill(COLOR) {
                moveTo(Point(3.0, 12.0))
                lineTo(Point(11.5, 4.5))
                lineTo(Point(11.5, 19.5))
                close()
            }
        }
    }
}
