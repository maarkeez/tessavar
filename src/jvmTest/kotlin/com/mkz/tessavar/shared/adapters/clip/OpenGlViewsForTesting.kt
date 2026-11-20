package com.mkz.tessavar.shared.adapters.clip

import com.mkz.tessavar.GAME_WINDOW_SIZE
import korlibs.graphics.AG
import korlibs.kgl.KmlGlContext
import korlibs.korge.testing.createKmlGlContext
import korlibs.korge.tests.ViewsForTesting
import korlibs.render.awt.AGOpenglAWT
import kotlin.test.AfterTest

abstract class OpenGlViewsForTesting : ViewsForTesting(windowSize = GAME_WINDOW_SIZE) {
    private lateinit var glContext: KmlGlContext

    override fun createAg(): AG {
        glContext = createKmlGlContext(GAME_WINDOW_SIZE.width.toInt(), GAME_WINDOW_SIZE.height.toInt())
        glContext.set()
        return AGOpenglAWT(context = glContext)
    }

    @AfterTest
    fun releaseGlContext() {
        if (!::glContext.isInitialized) return
        glContext.unset()
        glContext.close()
    }
}
