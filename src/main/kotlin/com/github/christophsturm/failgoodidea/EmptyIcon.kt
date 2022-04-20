package com.github.christophsturm.failgoodidea

import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class EmptyIcon @JvmOverloads constructor(private val iconWidth: Int = 0, private val iconHeight: Int = 0) : Icon {

    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {}
    override fun getIconWidth() = iconWidth

    override fun getIconHeight() = iconHeight
}
