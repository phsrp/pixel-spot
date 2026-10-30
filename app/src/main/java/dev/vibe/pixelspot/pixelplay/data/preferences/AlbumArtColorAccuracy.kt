// Adapted from PixelPlayer (github.com/PixelPlayerHQ/PixelPlayer, commit d51105d, MIT licence).
// See THIRD_PARTY_NOTICES.md.
package dev.vibe.pixelspot.pixelplay.data.preferences

object AlbumArtColorAccuracy {
    const val MIN = 0
    const val MAX = 10
    const val DEFAULT = MIN
    const val STEPS = MAX - MIN - 1

    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)
}
