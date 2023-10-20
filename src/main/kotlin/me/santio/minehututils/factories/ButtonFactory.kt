package me.santio.minehututils.factories

/**
 * A super simple factory for embedding buttons into text (using markdown links)
 */
object ButtonFactory {

    /**
     * Create a text button
     * @param label The label of the button
     * @param url The URL the button should link to
     * @return The embeddable piece of text
     */
    fun textButton(label: String, url: String) = "[`[$label]`](<$url>)"

}