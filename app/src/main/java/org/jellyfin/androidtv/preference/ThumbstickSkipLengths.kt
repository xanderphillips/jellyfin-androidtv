package org.jellyfin.androidtv.preference

import kotlin.math.abs

object ThumbstickSkipLengths {
	val ALLOWED_MS = listOf(5_000, 15_000, 30_000, 60_000, 300_000)
	const val DEFAULT_BACK_MS = 15_000
	const val DEFAULT_FORWARD_MS = 30_000

	/** Snaps any stored value to the closest allowed one so unexpected values keep working. */
	fun coerce(valueMs: Int): Int = ALLOWED_MS.minByOrNull { abs(it - valueMs) } ?: DEFAULT_FORWARD_MS

	/** Next allowed value after [valueMs], wrapping around. */
	fun next(valueMs: Int): Int {
		val index = ALLOWED_MS.indexOf(coerce(valueMs))
		return ALLOWED_MS[(index + 1) % ALLOWED_MS.size]
	}
}
