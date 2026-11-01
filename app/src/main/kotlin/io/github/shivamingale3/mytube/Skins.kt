package io.github.shivamingale3.mytube

import io.github.shivamingale3.mytube.skin.Skin
import io.github.shivamingale3.mytube.skin.dot.DotSkin
import io.github.shivamingale3.mytube.skin.tonal.TonalSkin

/** Every installed skin; the first is the default. */
object Skins {
    val all: List<Skin> = listOf(DotSkin, TonalSkin)

    fun byId(id: String): Skin = all.firstOrNull { it.id == id } ?: all.first()
}
