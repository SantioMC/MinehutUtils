package me.santio.minehututils.utils

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.imageio.stream.MemoryCacheImageOutputStream

/**
 * Helpers for reading and writing images in memory, without ImageIO's temporary cache files
 */
object Images {

    /**
     * Decodes an image, refusing images larger than the given size before any pixels are allocated
     * @param bytes The encoded image
     * @param maxSize The largest width or height to accept
     * @return The decoded image, or null if it is invalid or too large
     */
    fun decode(bytes: ByteArray, maxSize: Int): BufferedImage? {
        val input = MemoryCacheImageInputStream(bytes.inputStream())
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null.also { input.close() }

        return try {
            reader.setInput(input, true, true)
            if (reader.getWidth(0) > maxSize || reader.getHeight(0) > maxSize) null else reader.read(0)
        } catch (_: Exception) {
            null
        } finally {
            reader.dispose()
            input.close()
        }
    }

    /**
     * Draws a square PNG image with sharp, pixelated scaling
     * @param size The width and height of the image
     * @param draw Draws onto the image
     * @return The PNG image data
     */
    fun square(size: Int, draw: Graphics2D.() -> Unit): ByteArray {
        val canvas = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        try {
            graphics.draw()
        } finally {
            graphics.dispose()
        }

        return encode(canvas)
    }

    /**
     * Draws an image centered in a square box, scaling large images down to fit and small ones up by whole
     * multiples
     * @param graphics Where to draw
     * @param image The image to draw
     * @param x The left of the box
     * @param y The top of the box
     * @param box The width and height of the box
     */
    fun drawFitted(graphics: Graphics2D, image: BufferedImage, x: Int, y: Int, box: Int) {
        val largest = maxOf(image.width, image.height)
        val size = if (largest <= box) largest * (box / largest) else box
        val width = image.width * size / largest
        val height = image.height * size / largest
        graphics.drawImage(image, x + (box - width) / 2, y + (box - height) / 2, width, height, null)
    }

    /**
     * Encodes an image as a PNG
     * @param image The image to encode
     * @return The PNG image data
     */
    fun encode(image: BufferedImage): ByteArray {
        val output = ByteArrayOutputStream()
        MemoryCacheImageOutputStream(output).use { ImageIO.write(image, "png", it) }
        return output.toByteArray()
    }

}
