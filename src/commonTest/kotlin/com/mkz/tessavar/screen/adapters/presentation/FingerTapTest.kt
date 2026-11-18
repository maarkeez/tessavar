package com.mkz.tessavar.screen.adapters.presentation

import com.mkz.tessavar.shared.adapters.presentation.nextFrame
import com.mkz.tessavar.shared.adapters.presentation.sharedViewsTest
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import korlibs.korge.tests.ViewsForTesting
import korlibs.math.geom.Point
import kotlin.test.Test

class FingerTapTest : ViewsForTesting() {
    private val taps = mutableListOf<Int>()

    @Test
    fun `FingerTap should tell two fingers when two fingers tap inside the view`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            // When
            tapWithFingers(Point(50, 100), Point(150, 100))
            // Then
            taps.shouldContainExactly(2)
        }

    @Test
    fun `FingerTap should tell three fingers when a third finger touches before the first two lift`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            // When
            tapWithFingers(Point(40, 100), Point(100, 100), Point(160, 100))
            // Then
            taps.shouldContainExactly(3)
        }

    @Test
    fun `FingerTap should tell nothing when one finger taps the view`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            // When
            tapWithFingers(Point(100, 100))
            // Then
            taps.shouldBeEmpty()
        }

    @Test
    fun `FingerTap should tell nothing when the fingers lift after 300 ms`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            // When
            tapWithFingers(Point(50, 100), Point(150, 100), holdFrames = 30)
            // Then
            taps.shouldBeEmpty()
        }

    @Test
    fun `FingerTap should tell nothing when a finger moves more than 10 px`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            // When
            tapWithFingers(Point(50, 100), Point(150, 100), moveBy = Point(0, 12))
            // Then
            taps.shouldBeEmpty()
        }

    @Test
    fun `FingerTap should tell nothing when a finger touches out of the view`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            // When
            tapWithFingers(Point(150, 100), Point(250, 100))
            // Then
            taps.shouldBeEmpty()
        }

    @Test
    fun `FingerTap should tell the fingers when they touch a view that lets them through over the view`() =
        sharedViewsTest {
            // Given
            fingerTapOnArea { fingers -> taps += fingers }
            coverWithPassThrough()
            // When
            tapWithFingers(Point(50, 100), Point(150, 100))
            // Then
            taps.shouldContainExactly(2)
        }

    @Test
    fun `FingerTap should hold back the mouse from the second finger touching until 100 ms after the last one lifts`() =
        sharedViewsTest {
            // Given
            val fingerTap = fingerTapOnArea()
            // When
            tapWithFingers(Point(50, 100), Point(150, 100))
            // Then
            fingerTap.isHoldingBack.shouldBeTrue()
            repeat(10) { nextFrame() }
            fingerTap.isHoldingBack.shouldBeFalse()
        }

    @Test
    fun `FingerTap should not hold back the mouse when one finger taps`() =
        sharedViewsTest {
            // Given
            val fingerTap = fingerTapOnArea()
            // When
            tapWithFingers(Point(100, 100))
            // Then
            fingerTap.isHoldingBack.shouldBeFalse()
        }
}
