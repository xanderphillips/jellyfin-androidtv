package org.jellyfin.androidtv.ui.playback

object SeekBusy {
	/**
	 * A seek counts as still running while the seeking flag is set, except for a paused player on a seekable
	 * stream: the flag is only cleared while playing, and overlapping seeks are safe there.
	 */
	fun isBusy(wasSeeking: Boolean, isPaused: Boolean, streamSeekable: Boolean): Boolean =
		wasSeeking && !(isPaused && streamSeekable)
}
