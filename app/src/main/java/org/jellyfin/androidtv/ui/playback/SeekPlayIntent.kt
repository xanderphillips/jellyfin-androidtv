package org.jellyfin.androidtv.ui.playback

/**
 * Remembers whether playback should carry on after a seek. The answer is taken when the first seek of a
 * series starts and kept while further seeks arrive before the first has finished, so a rapid second seek
 * cannot turn a playing video into a paused one (or the reverse).
 */
class SeekPlayIntent {
	private var intent: Boolean? = null

	val inFlight get() = intent != null

	/** True if playback should be playing once this seek completes. [playingNow] is only used for a new series. */
	fun begin(playingNow: Boolean): Boolean = intent ?: playingNow.also { intent = it }

	/** Call when the seek has finished, or when the user explicitly pauses or plays. */
	fun settle() {
		intent = null
	}

	companion object {
		/** Whether a player state counts as wanting to play: anything except an explicit pause, idle or an error. */
		fun wantsPlayback(isPaused: Boolean, isIdleOrError: Boolean) = !isPaused && !isIdleOrError
	}
}
