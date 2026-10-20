package org.jellyfin.androidtv.preference

/** Defaults for transcoding requests. */
object TranscodeDefaults {
	/**
	 * Ask the server for H.264 before HEVC. The server currently encodes in software, where HEVC runs barely
	 * faster than realtime and H.264 about ten times faster. Switch to false once hardware transcoding is in place.
	 */
	const val PREFER_H264 = true
}
