package com.mkz.tessavar.shared.adapters.clip

import korlibs.event.MouseEvent
import korlibs.image.bitmap.Bitmap32
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.view.Views
import korlibs.korge.view.renderToBitmap
import korlibs.korge.view.unsafeRenderToBitmapSync
import korlibs.math.geom.Point
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val CLIPS_DIRECTORY = File("build/clips")

sealed interface ClipFormat {
    val scale: Int

    data class Gif(
        override val scale: Int = 1,
    ) : ClipFormat

    data class Mp4(
        override val scale: Int = 2,
    ) : ClipFormat
}

@OptIn(KorgeExperimental::class)
suspend fun Views.recordClip(
    name: String,
    frameTime: Duration,
    formats: List<ClipFormat> = listOf(ClipFormat.Gif(), ClipFormat.Mp4()),
    framesPerSecond: Int = 10,
    play: suspend () -> Unit,
) {
    CLIPS_DIRECTORY.mkdirs()
    val frameDuration = 1.seconds / framesPerSecond
    val writers = formats.associateWith { format -> format.writer(name, frameDuration, framesPerSecond) }
    val renderedFramesPerCapture = (frameDuration / frameTime).roundToInt().coerceAtLeast(1)
    val taps = mutableListOf<Tap>()
    var renderedFrames = 0
    var capturedFrames = 0
    val tapListener =
        gameWindow.onEvent(MouseEvent.Type.DOWN) { event ->
            taps += Tap(windowToGlobalMatrix.transform(Point(event.x, event.y)), firstFrame = capturedFrames)
        }
    val renderListener =
        onBeforeRender.add { context ->
            if (renderedFrames++ % renderedFramesPerCapture != 0) return@add
            val frame = stage.unsafeRenderToBitmapSync(context, bgcolor = clearColor).toBufferedImage()
            val shownTaps = taps.filter { tap -> tap.isShownAt(capturedFrames) }
            writers.forEach { (format, writer) -> writer.write(frame.scaled(format.scale).withTaps(shownTaps, capturedFrames, format.scale)) }
            capturedFrames++
        }
    try {
        play()
    } finally {
        renderListener.close()
        tapListener.close()
        writers.values.forEach(ClipWriter::close)
    }
}

suspend fun Views.saveStill(
    name: String,
    scale: Int = 2,
) {
    CLIPS_DIRECTORY.mkdirs()
    val still = stage.renderToBitmap(this, includeBackground = true).toBufferedImage()
    ImageIO.write(still.scaled(scale), "png", File(CLIPS_DIRECTORY, "$name.png"))
}

private fun ClipFormat.writer(
    name: String,
    frameDuration: Duration,
    framesPerSecond: Int,
): ClipWriter =
    when (this) {
        is ClipFormat.Gif -> GifClipWriter(File(CLIPS_DIRECTORY, "$name.gif"), frameDuration)
        is ClipFormat.Mp4 -> Mp4ClipWriter(File(CLIPS_DIRECTORY, "$name.mp4"), framesPerSecond)
    }

private fun Bitmap32.toBufferedImage(): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = getRgbaRaw(x, y)
            image.setRGB(x, y, (color.r shl 16) or (color.g shl 8) or color.b)
        }
    }
    return image
}

private fun BufferedImage.scaled(scale: Int): BufferedImage {
    if (scale == 1) return this
    val image = BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    graphics.drawImage(this, 0, 0, width * scale, height * scale, null)
    graphics.dispose()
    return image
}
