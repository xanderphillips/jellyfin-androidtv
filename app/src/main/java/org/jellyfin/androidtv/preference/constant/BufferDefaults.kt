package org.jellyfin.androidtv.preference.constant

/** Choices for the default buffer length and for how much memory the player buffer may use. */
object BufferDefaults {
	private const val MIB = 1024L * 1024L
	private const val MIN_HEAP_FOR_LARGE_BUFFER = 192 * MIB
	private const val MIN_BYTE_CAP = 32 * MIB
	private const val MAX_BYTE_CAP = 128 * MIB
	private const val HEAP_DIVISOR = 3

	/**
	 * Meta headsets and any device with a heap of at least 192 MB (a 3 GB Onn 4K Pro has 256 MB) default to
	 * Large; small-heap devices keep Auto.
	 */
	fun defaultBufferLength(isMetaHeadset: Boolean, maxHeapBytes: Long): BufferLength =
		if (isMetaHeadset || maxHeapBytes >= MIN_HEAP_FOR_LARGE_BUFFER) BufferLength.LARGE else BufferLength.AUTO

	/** Upper limit for buffered bytes: a third of the app heap, kept between 32 and 128 MB. */
	fun byteCap(maxHeapBytes: Long): Int = (maxHeapBytes / HEAP_DIVISOR).coerceIn(MIN_BYTE_CAP, MAX_BYTE_CAP).toInt()
}
