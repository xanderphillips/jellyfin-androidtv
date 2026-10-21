package org.jellyfin.androidtv.preference

/** The skip lengths offered in settings for the skip buttons and the thumbstick. */
object SkipLengths {
	const val MS_PER_SECOND = 1_000
	const val MS_PER_MINUTE = 60_000

	private const val SECONDS_5_MS = 5_000
	private const val SECONDS_15_MS = 15_000
	private const val SECONDS_30_MS = 30_000
	private const val MINUTES_1_MS = 60_000
	private const val MINUTES_5_MS = 300_000

	val ALLOWED_MS = listOf(SECONDS_5_MS, SECONDS_15_MS, SECONDS_30_MS, MINUTES_1_MS, MINUTES_5_MS)

	/**
	 * The next offered length above [valueMs], wrapping to the shortest. A stored value that is not on the
	 * list (for example 10 seconds from the old slider) keeps working and simply moves to the next step up.
	 */
	fun next(valueMs: Int): Int = ALLOWED_MS.firstOrNull { it > valueMs } ?: ALLOWED_MS.first()
}
