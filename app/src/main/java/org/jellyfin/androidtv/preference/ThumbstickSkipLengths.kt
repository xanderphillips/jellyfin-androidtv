package org.jellyfin.androidtv.preference

import kotlin.math.abs

object ThumbstickSkipLengths {
	const val MS_PER_SECOND = 1_000
	const val MS_PER_MINUTE = 60_000

	private const val SECONDS_5_MS = 5_000
	private const val SECONDS_15_MS = 15_000
	private const val SECONDS_30_MS = 30_000
	private const val MINUTES_1_MS = 60_000
	private const val MINUTES_5_MS = 300_000

	val ALLOWED_MS = listOf(SECONDS_5_MS, SECONDS_15_MS, SECONDS_30_MS, MINUTES_1_MS, MINUTES_5_MS)
	const val DEFAULT_BACK_MS = SECONDS_15_MS
	const val DEFAULT_FORWARD_MS = SECONDS_30_MS

	/** Snaps any stored value to the closest allowed one so unexpected values keep working. */
	fun coerce(valueMs: Int): Int = ALLOWED_MS.minByOrNull { abs(it - valueMs) } ?: DEFAULT_FORWARD_MS

	/** Next allowed value after [valueMs], wrapping around. */
	fun next(valueMs: Int): Int {
		val index = ALLOWED_MS.indexOf(coerce(valueMs))
		return ALLOWED_MS[(index + 1) % ALLOWED_MS.size]
	}
}
