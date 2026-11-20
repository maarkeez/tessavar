package com.mkz.tessavar.shared.adapters.clip

import org.jcodec.api.awt.AWTSequenceEncoder
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.Closeable
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriter
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageOutputStream
import kotlin.time.Duration

interface ClipWriter : Closeable {
    fun write(frame: BufferedImage)
}

class GifClipWriter(
    file: File,
    frameDuration: Duration,
) : ClipWriter {
    private val stream: ImageOutputStream = ImageIO.createImageOutputStream(file.apply { delete() })
    private val writer: ImageWriter = ImageIO.getImageWritersByFormatName("gif").next().apply { output = stream }
    private val frameCentiseconds = (frameDuration.inWholeMilliseconds / 10).toInt()
    private var pendingFrame: BufferedImage? = null
    private var pendingPixels: IntArray? = null
    private var pendingCentiseconds = 0
    private var framesWritten = 0

    init {
        writer.prepareWriteSequence(null)
    }

    override fun write(frame: BufferedImage) {
        val pixels = frame.getRGB(0, 0, frame.width, frame.height, null, 0, frame.width)
        if (pixels.contentEquals(pendingPixels)) {
            pendingCentiseconds += frameCentiseconds
            return
        }
        writePending()
        pendingFrame = frame
        pendingPixels = pixels
        pendingCentiseconds = frameCentiseconds
    }

    override fun close() {
        writePending()
        writer.endWriteSequence()
        stream.close()
        writer.dispose()
    }

    private fun writePending() {
        val frame = pendingFrame ?: return
        val indexed = frame.indexed(requireNotNull(pendingPixels))
        writer.writeToSequence(IIOImage(indexed, null, metadataOf(indexed, pendingCentiseconds, loops = framesWritten == 0)), null)
        framesWritten++
    }

    private fun metadataOf(
        image: BufferedImage,
        centiseconds: Int,
        loops: Boolean,
    ): IIOMetadata {
        val metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), writer.defaultWriteParam)
        val format = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(format) as IIOMetadataNode
        root.child("GraphicControlExtension").apply {
            setAttribute("disposalMethod", "none")
            setAttribute("userInputFlag", "FALSE")
            setAttribute("transparentColorFlag", "FALSE")
            setAttribute("delayTime", centiseconds.toString())
            setAttribute("transparentColorIndex", "0")
        }
        root.replaceChild("LocalColorTable", image.localColorTable())
        if (loops) {
            val loopForever = IIOMetadataNode("ApplicationExtension")
            loopForever.setAttribute("applicationID", "NETSCAPE")
            loopForever.setAttribute("authenticationCode", "2.0")
            loopForever.userObject = byteArrayOf(1, 0, 0)
            root.child("ApplicationExtensions").appendChild(loopForever)
        }
        metadata.setFromTree(format, root)
        return metadata
    }

    private fun IIOMetadataNode.child(name: String): IIOMetadataNode =
        children().firstOrNull { node -> node.nodeName == name }
            ?: IIOMetadataNode(name).also { node -> appendChild(node) }

    private fun IIOMetadataNode.replaceChild(
        name: String,
        node: IIOMetadataNode,
    ) {
        children().filter { child -> child.nodeName == name }.forEach { child -> removeChild(child) }
        val imageDescriptor = children().indexOfFirst { child -> child.nodeName == "ImageDescriptor" }
        val next = children().getOrNull(imageDescriptor + 1)
        if (next == null) appendChild(node) else insertBefore(node, next)
    }

    private fun IIOMetadataNode.children(): List<IIOMetadataNode> = (0 until length).map { index -> item(index) as IIOMetadataNode }

    private fun BufferedImage.localColorTable(): IIOMetadataNode {
        val model = colorModel as IndexColorModel
        val table = IIOMetadataNode("LocalColorTable")
        table.setAttribute("sizeOfLocalColorTable", model.mapSize.toString())
        table.setAttribute("sortFlag", "FALSE")
        repeat(model.mapSize) { index ->
            val entry = IIOMetadataNode("ColorTableEntry")
            entry.setAttribute("index", index.toString())
            entry.setAttribute("red", model.getRed(index).toString())
            entry.setAttribute("green", model.getGreen(index).toString())
            entry.setAttribute("blue", model.getBlue(index).toString())
            table.appendChild(entry)
        }
        return table
    }
}

private const val GIF_COLORS = 256

private fun BufferedImage.indexed(pixels: IntArray): BufferedImage {
    val counts = HashMap<Int, Int>()
    pixels.forEach { pixel -> counts.merge(pixel and 0xFFFFFF, 1, Int::plus) }
    val palette =
        counts.entries
            .sortedByDescending { entry -> entry.value }
            .take(GIF_COLORS)
            .map { entry -> entry.key }
            .toIntArray()
    val indexOf = HashMap<Int, Int>()
    palette.forEachIndexed { index, color -> indexOf[color] = index }
    val model =
        IndexColorModel(
            8,
            GIF_COLORS,
            ByteArray(GIF_COLORS) { index -> (palette.getOrElse(index) { 0 } shr 16).toByte() },
            ByteArray(GIF_COLORS) { index -> (palette.getOrElse(index) { 0 } shr 8).toByte() },
            ByteArray(GIF_COLORS) { index -> palette.getOrElse(index) { 0 }.toByte() },
        )
    val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, model)
    val raster = image.raster
    pixels.forEachIndexed { offset, pixel ->
        val color = pixel and 0xFFFFFF
        val index = indexOf.getOrPut(color) { palette.nearestIndexTo(color) }
        raster.setSample(offset % width, offset / width, 0, index)
    }
    return image
}

private fun IntArray.nearestIndexTo(color: Int): Int = indices.minBy { index -> distanceSquared(this[index], color) }

private fun distanceSquared(
    first: Int,
    second: Int,
): Int {
    val red = (first shr 16 and 0xFF) - (second shr 16 and 0xFF)
    val green = (first shr 8 and 0xFF) - (second shr 8 and 0xFF)
    val blue = (first and 0xFF) - (second and 0xFF)
    return red * red + green * green + blue * blue
}

class Mp4ClipWriter(
    file: File,
    framesPerSecond: Int,
) : ClipWriter {
    private val encoder = AWTSequenceEncoder.createSequenceEncoder(file.apply { delete() }, framesPerSecond)

    override fun write(frame: BufferedImage) = encoder.encodeImage(frame)

    override fun close() = encoder.finish()
}
