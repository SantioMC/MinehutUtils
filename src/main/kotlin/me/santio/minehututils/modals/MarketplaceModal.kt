package me.santio.minehututils.modals

import me.santio.coffee.jda.gui.modal.ModalType
import me.santio.coffee.jda.gui.modal.annotations.Item
import me.santio.coffee.jda.gui.modal.annotations.Modal

@Modal("Create a marketplace listing!")
class MarketplaceModal {

    @Item("Shortly describe your listing", ModalType.SHORT_TEXT, maxLength = 64)
    lateinit var title: String

    @Item("Talk about your listing!", ModalType.PARAGRAPH_TEXT, maxLength = 2048)
    lateinit var description: String

}