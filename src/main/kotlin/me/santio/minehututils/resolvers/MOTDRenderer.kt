package me.santio.minehututils.resolvers

import me.santio.minehututils.utils.Images
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * Renders motds to an image the way Minehut's in-game server list shows them in an item tooltip, with full RGB colors
 */
object MOTDRenderer {

    private const val SCALE = 2
    private const val LINE_HEIGHT = 10 * SCALE
    private const val ASCENT = 7 * SCALE
    private const val PADDING = 4 * SCALE
    private const val MARGIN = 4 * SCALE
    private const val MAX_LINES = 20
    private const val MAX_WIDTH = 1000 * SCALE
    private const val DEFAULT_COLOR = 0xFFFFFF
    private const val BACKGROUND = 0xF0100010.toInt()
    private const val BORDER_TOP = 0x505000FF
    private const val BORDER_BOTTOM = 0x5028007F

    private const val SPACE_WIDTH = 4 * SCALE
    private const val NARROW = ",;@ilt{}~"

    // Meat on a bone, drawn with the Monocraft glyph at the scissors code point
    private const val MEAT = 0x1F356
    private const val MONOCRAFT_MEAT = "\u2702"

    private val context = FontRenderContext(null, false, false)
    private val pixel = load("Monocraft.ttf", 9f * SCALE)
    private val purejangles = load("Purejangles.ttf", 9f * SCALE)
    private val narrow = load("Minecraft.otf", 9f * SCALE)
    private val narrowWidths = load("Minecraft.otf", 8f * SCALE)
    private val unifont = load("Unifont.otf", 8f * SCALE)
    private val unifontUpper = load("UnifontUpper.otf", 8f * SCALE)
    private val fallback = Font(Font.DIALOG, Font.PLAIN, 8 * SCALE)
    private val italics = listOf(pixel, purejangles, narrow, unifont, unifontUpper, fallback)
        .associateWith { it.deriveFont(AffineTransform.getShearInstance(-0.2, 0.0)) }
    private val metrics = ConcurrentHashMap<Int, Metrics>()
    private val obfuscationPool by lazy {
        (0x21..0x7E).plus(0xA1..0x17F).plus(0x391..0x3C9).plus(0x410..0x44F)
            .filter { fontFor(it).let { font -> font === pixel || font === narrow } }
            .groupBy { metrics(it, fontFor(it), String(Character.toChars(it))).advance }
    }

    private fun load(name: String, size: Float): Font {
        return MOTDRenderer::class.java.getResourceAsStream("/fonts/$name")!!
            .use { Font.createFont(Font.TRUETYPE_FONT, it) }
            .deriveFont(size)
    }

    private class Metrics(val offset: Int, val advance: Int)

    private class Glyph(val text: String, val font: Font, style: Style, metrics: Metrics) {
        val color = style.color()?.value() ?: DEFAULT_COLOR
        val bold = style.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE
        val underlined = style.decoration(TextDecoration.UNDERLINED) == TextDecoration.State.TRUE
        val strikethrough = style.decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE

        // Underlined or struck through spaces still draw their line
        val visible = text.isNotBlank() || underlined || strikethrough
        val offset = metrics.offset
        val width = metrics.advance + if (bold) SCALE else 0
    }

    /**
     * Picks the font for a code point: Purejangles for symbols Minecraft draws itself, Minecraft-Font for narrow
     * glyphs, then Monocraft, Unifont and the system font
     */
    private fun fontFor(point: Int): Font = when {
        purejangles.canDisplay(point) -> purejangles
        NARROW.indexOf(point.toChar()) >= 0 && point < 0x10000 -> narrow
        pixel.canDisplay(point) -> pixel
        unifont.canDisplay(point) -> unifont
        unifontUpper.canDisplay(point) -> unifontUpper
        else -> fallback
    }

    /**
     * Measures a glyph the way Minecraft spaces its font: the drawn width plus one pixel, with a fixed width for
     * spaces. Narrow glyphs use Minecraft-Font's widths.
     */
    private fun metrics(point: Int, font: Font, text: String): Metrics {
        // Only the pixel fonts' metrics are cached
        if (font !== pixel && font !== narrow) return Metrics(0, font.getStringBounds(text, context).width.toInt())

        return metrics.getOrPut(point) {
            when {
                point == ' '.code -> Metrics(0, SPACE_WIDTH)
                font === narrow -> Metrics(0, narrowWidths.getStringBounds(text, context).width.roundToInt())
                else -> {
                    val ink = pixel.createGlyphVector(context, text).getGlyphOutline(0).bounds2D
                    if (ink.isEmpty) Metrics(0, pixel.getStringBounds(text, context).width.roundToInt())
                    else Metrics((ink.minX / SCALE).roundToInt() * SCALE, ((ink.width / SCALE).roundToInt() + 1) * SCALE)
                }
            }
        }
    }

    private fun flatten(component: Component, parent: Style, lines: MutableList<MutableList<Glyph>>, random: Random) {
        val style = parent.merge(component.style())

        if (component is TextComponent) {
            component.content().split("\n").forEachIndexed { index, part ->
                if (index > 0) lines += mutableListOf<Glyph>()
                if (lines.size > MAX_LINES) return

                var width = lines.last().sumOf { it.width }
                for (point in part.codePoints().iterator()) {
                    if (width >= MAX_WIDTH) break
                    if (point in 0xFE00..0xFE0F || Character.getType(point) == Character.FORMAT.toInt()) continue

                    val original = fontFor(point)
                    val scrambled = if (style.decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE && point != ' '.code) {
                        obfuscationPool[metrics(point, original, String(Character.toChars(point))).advance]?.random(random)
                    } else null

                    val shown = scrambled ?: point
                    val glyphFont = if (shown == MEAT) pixel else fontFor(shown)
                    val text = if (shown == MEAT) MONOCRAFT_MEAT else String(Character.toChars(shown))
                    val shaped = if (style.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE) {
                        italics.getValue(glyphFont)
                    } else glyphFont

                    val glyph = Glyph(text, shaped, style, metrics(shown, glyphFont, text))
                    lines.last() += glyph
                    width += glyph.width
                }
            }
        }

        for (child in component.children()) {
            if (lines.size > MAX_LINES) return
            flatten(child, style, lines, random)
        }
    }

    private fun blend(top: Int, bottom: Int, progress: Double): Int {
        fun channel(shift: Int): Int {
            val from = top ushr shift and 0xFF
            val to = bottom ushr shift and 0xFF
            return (from + (to - from) * progress).roundToInt() shl shift
        }
        return channel(24) or channel(16) or channel(8) or channel(0)
    }

    private fun Graphics2D.tooltip(width: Int, height: Int) {
        fun fill(x: Int, y: Int, w: Int, h: Int, argb: Int) {
            color = Color(argb, true)
            fillRect(x * SCALE, y * SCALE, w * SCALE, h * SCALE)
        }

        val right = width / SCALE - 1
        val bottom = height / SCALE - 1
        fill(1, 0, right - 1, 1, BACKGROUND)
        fill(1, bottom, right - 1, 1, BACKGROUND)
        fill(0, 1, 1, bottom - 1, BACKGROUND)
        fill(right, 1, 1, bottom - 1, BACKGROUND)
        fill(1, 1, right - 1, bottom - 1, BACKGROUND)

        fill(1, 1, right - 1, 1, BORDER_TOP)
        fill(1, bottom - 1, right - 1, 1, BORDER_BOTTOM)
        for (row in 2 until bottom - 1) {
            val border = blend(BORDER_TOP, BORDER_BOTTOM, (row - 2).toDouble() / (bottom - 4).coerceAtLeast(1))
            fill(1, row, 1, 1, border)
            fill(right - 1, row, 1, 1, border)
        }
    }

    /**
     * Draws a glyph, filling Purejangles' outlines directly instead of drawing them as text
     */
    private fun Graphics2D.drawGlyph(glyph: Glyph, x: Int, y: Int) {
        if (glyph.font.family == purejangles.family) {
            fill(glyph.font.createGlyphVector(context, glyph.text).getOutline(x.toFloat(), y.toFloat()))
        } else {
            drawString(glyph.text, x, y)
        }
    }

    private fun shadow(color: Int): Int = (color and 0xFCFCFC) shr 2

    /**
     * Renders a motd to a PNG image
     * @param motd The parsed motd, see [MOTDResolver.toComponent]
     * @return The PNG image data
     */
    fun render(motd: Component): ByteArray {
        val lines = mutableListOf(mutableListOf<Glyph>())
        flatten(motd, Style.empty(), lines, Random.Default)
        val visible = lines.dropLastWhile { line -> line.none { it.visible } }.take(MAX_LINES).ifEmpty { lines.take(1) }

        val textWidth = visible.maxOf { line -> line.sumOf { it.width } }
        val width = PADDING * 2 + (textWidth + SCALE - 1) / SCALE * SCALE
        val height = PADDING * 2 + visible.size * LINE_HEIGHT - 2 * SCALE
        val image = BufferedImage(width + MARGIN * 2, height + MARGIN * 2, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.translate(MARGIN, MARGIN)
        graphics.tooltip(width, height)

        for (pass in 0..1) {
            val offset = if (pass == 0) SCALE else 0

            visible.forEachIndexed { row, line ->
                var x = PADDING
                val y = PADDING + row * LINE_HEIGHT + ASCENT

                for (glyph in line) {
                    graphics.color = Color(if (pass == 0) shadow(glyph.color) else glyph.color)
                    graphics.font = glyph.font
                    graphics.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        if (glyph.font.family != fallback.family) RenderingHints.VALUE_TEXT_ANTIALIAS_OFF
                        else RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                    )

                    graphics.drawGlyph(glyph, x + offset - glyph.offset, y + offset)
                    if (glyph.bold) graphics.drawGlyph(glyph, x + offset - glyph.offset + SCALE, y + offset)
                    if (glyph.underlined) {
                        graphics.fillRect(x + offset - SCALE, y + offset + SCALE, glyph.width + SCALE, SCALE)
                    }
                    if (glyph.strikethrough) {
                        graphics.fillRect(x + offset - SCALE, y + offset - 7 * SCALE / 2, glyph.width + SCALE, SCALE)
                    }

                    x += glyph.width
                }
            }
        }

        graphics.dispose()
        return Images.encode(image)
    }

}
