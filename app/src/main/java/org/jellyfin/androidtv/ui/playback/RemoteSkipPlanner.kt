package org.jellyfin.androidtv.ui.playback

/**
 * Decides when skip requests from the remote, media keys and the on-screen buttons become seeks.
 *
 * The first press goes out immediately. Presses that arrive while a seek is still running, or sooner than
 * [minSeekGapMs] after the last one, add up and go out as one seek. Auto-repeat events from a held key are
 * thinned to one per [repeatIntervalMs] so holding a key gives a steady pace instead of a flood.
 */
class RemoteSkipPlanner(
	private val minSeekGapMs: Long = 500L,
	private val repeatIntervalMs: Long = 300L,
) {
	private var pendingMs = 0
	private var lastAcceptedAt = Long.MIN_VALUE
	private var lastSentAt = Long.MIN_VALUE

	val hasPending get() = pendingMs != 0

	/** Records a press; false when it is a key repeat that came too soon after the last accepted press. */
	fun onPress(deltaMs: Int, isRepeat: Boolean, nowMs: Long): Boolean {
		if (isRepeat && lastAcceptedAt != Long.MIN_VALUE && nowMs - lastAcceptedAt < repeatIntervalMs) return false
		pendingMs += deltaMs
		lastAcceptedAt = nowMs
		return true
	}

	/** The total to seek by now, or null while [busy] or inside the minimum gap. Clears what it returns. */
	fun next(nowMs: Long, busy: Boolean): Int? {
		if (pendingMs == 0 || busy) return null
		if (lastSentAt != Long.MIN_VALUE && nowMs - lastSentAt < minSeekGapMs) return null
		lastSentAt = nowMs
		return pendingMs.also { pendingMs = 0 }
	}

	/** Puts a skip back after the player refused it, so the next poll can retry at once. */
	fun restore(deltaMs: Int) {
		pendingMs += deltaMs
		lastSentAt = Long.MIN_VALUE
	}

	fun clear() {
		pendingMs = 0
	}
}

/** Keeps only the latest requested seek target while a seek is running, instead of dropping it. */
class LatestSeekQueue {
	private var target: Long? = null

	val hasPending get() = target != null

	fun set(positionMs: Long) {
		target = positionMs
	}

	/** The target to seek to now, or null while [busy] or when nothing is waiting. */
	fun take(busy: Boolean): Long? {
		if (busy) return null
		return target.also { target = null }
	}

	fun clear() {
		target = null
	}
}
