package io.github.shivamingale3.mytube.skin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * YouTube-style seeking with the remote, shared by every skin's player. A tap on left or right
 * moves 10 s; holding it moves faster the longer it's held (10 s, 30 s, 1 min, 3 min, 5 min,
 * then 10 min steps). While held, only the target moves ([pendingMs], shown on the progress
 * bar); the player jumps once, on release, so it doesn't rebuffer at every step.
 */
class Scrubber {
    /** Where the user is seeking to, or null when not seeking. */
    var pendingMs by mutableStateOf<Long?>(null)
        private set

    /** The progress bar has focus, so left and right seek instead of moving focus. */
    var barFocused by mutableStateOf(false)

    private var holdStartedAt = 0L
    private var lastStepAt = 0L
    private var direction = 0

    /** One key-down of left ([forward] false) or right; [repeat] is the key's repeat count. */
    fun step(forward: Boolean, repeat: Int, positionMs: Long, durationMs: Long) {
        val now = System.currentTimeMillis()
        val dir = if (forward) 1 else -1
        if (repeat == 0 || dir != direction) {
            holdStartedAt = now
            direction = dir
        } else if (now - lastStepAt < REPEAT_EVERY_MS) {
            return
        }
        lastStepAt = now
        val from = pendingMs ?: positionMs
        val max = if (durationMs > 0) durationMs - END_MARGIN_MS else Long.MAX_VALUE
        pendingMs = (from + dir * stepFor(now - holdStartedAt)).coerceIn(0, max.coerceAtLeast(0))
    }

    /** Seeks to the target, if there is one. */
    fun commit(seekTo: (Long) -> Unit) {
        pendingMs?.let(seekTo)
        pendingMs = null
        direction = 0
    }

    private fun stepFor(heldMs: Long): Long = STEPS.lastOrNull { heldMs >= it.first }?.second ?: STEPS.first().second

    private companion object {
        const val REPEAT_EVERY_MS = 120L
        const val END_MARGIN_MS = 1_000L

        /** Held for at least first ms → step of second ms. */
        val STEPS = listOf(
            0L to 10_000L,
            1_000L to 30_000L,
            2_000L to 60_000L,
            3_000L to 180_000L,
            4_000L to 300_000L,
            5_000L to 600_000L,
        )
    }
}
