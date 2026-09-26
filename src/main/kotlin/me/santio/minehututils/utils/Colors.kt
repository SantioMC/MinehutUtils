package me.santio.minehututils.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Picks the most prominent vivid color out of images and text
 */
object Colors {

    private const val BUCKETS = 24
    private const val MIN_SATURATION = 0.35f
    private const val MIN_BRIGHTNESS = 0.35f

    private fun hsb(rgb: Int, into: FloatArray? = null): FloatArray =
        Color.RGBtoHSB(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF, into)

    private fun isVivid(hsb: FloatArray) = hsb[1] >= MIN_SATURATION && hsb[2] >= MIN_BRIGHTNESS

    private fun bucket(hsb: FloatArray) = (hsb[0] * BUCKETS).toInt() % BUCKETS

    /**
     * Finds the dominant vivid color of an image, ignoring transparent, gray, dark and washed out pixels
     * @param image The image to look at
     * @return The average color of the most common hue, or null if the image has no vivid pixels
     */
    fun dominant(image: BufferedImage): Int? {
        val weight = DoubleArray(BUCKETS)
        val red = DoubleArray(BUCKETS)
        val green = DoubleArray(BUCKETS)
        val blue = DoubleArray(BUCKETS)

        val hsb = FloatArray(3)

        for (pixel in image.getRGB(0, 0, image.width, image.height, null, 0, image.width)) {
            if (pixel ushr 24 < 128) continue

            hsb(pixel, hsb)
            if (!isVivid(hsb)) continue

            val bucket = bucket(hsb)
            val amount = (hsb[1] * hsb[2]).toDouble()
            weight[bucket] += amount
            red[bucket] += amount * (pixel shr 16 and 0xFF)
            green[bucket] += amount * (pixel shr 8 and 0xFF)
            blue[bucket] += amount * (pixel and 0xFF)
        }

        val best = weight.indices.maxBy { weight[it] }
        if (weight[best] == 0.0) return null

        return ((red[best] / weight[best]).toInt() shl 16) or
            ((green[best] / weight[best]).toInt() shl 8) or
            (blue[best] / weight[best]).toInt()
    }

    /**
     * Finds the vivid text color used on the most characters
     * @param component The text to look at
     * @return The most used vivid color, or null if the text has no vivid colors
     */
    fun dominant(component: Component): Int? {
        val counts = HashMap<Int, Int>()

        fun walk(component: Component, parent: Style) {
            val style = parent.merge(component.style())
            val color = style.color()?.value()
            if (component is TextComponent && color != null && isVivid(hsb(color))) {
                counts.merge(color, component.content().count { !it.isWhitespace() }, Int::plus)
            }
            component.children().forEach { walk(it, style) }
        }

        walk(component, Style.empty())
        if (counts.isEmpty()) return null

        val buckets = counts.entries.groupBy { bucket(hsb(it.key)) }
        val best = buckets.values.maxBy { entries -> entries.sumOf { it.value } }
        return best.maxBy { it.value }.key
    }

}
