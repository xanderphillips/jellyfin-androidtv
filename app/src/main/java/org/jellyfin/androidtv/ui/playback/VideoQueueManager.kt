package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

class VideoQueueManager {
	private var _currentVideoQueue: List<BaseItemDto> = emptyList()
	private var _currentMediaPosition = -1
	private var _lastPlayedAudioLanguageIsoCode: String? = null
	private var _lastPlayedSubtitleLanguageIsoCode: String? = null
	private var _subtitleSelection: Pair<UUID, Int>? = null

	fun setCurrentVideoQueue(items: List<BaseItemDto>?) {
		if (items.isNullOrEmpty()) return clearVideoQueue()

		_currentVideoQueue = items.toMutableList()
		_currentMediaPosition = 0
	}

	fun getCurrentVideoQueue(): List<BaseItemDto> = _currentVideoQueue

	fun setCurrentMediaPosition(currentMediaPosition: Int) {
		if (currentMediaPosition !in 0.._currentVideoQueue.size) return

		_currentMediaPosition = currentMediaPosition
	}

	fun getCurrentMediaPosition() = _currentMediaPosition

	fun getLastPlayedAudioLanguageIsoCode(): String? {
		return _lastPlayedAudioLanguageIsoCode
	}

	fun setLastPlayedAudioLanguageIsoCode(isoCode: String) {
		_lastPlayedAudioLanguageIsoCode = isoCode
	}

	fun getLastPlayedSubtitleLanguageIsoCode(): String? {
		return _lastPlayedSubtitleLanguageIsoCode
	}

	fun setLastPlayedSubtitleLanguageIsoCode(isoCode: String?) {
		_lastPlayedSubtitleLanguageIsoCode = isoCode
	}

	/**
	 * Subtitle index the user picked for [itemId] (-1 when disabled), kept so playback restarts of the same item
	 * (retries, subtitle burn-in) keep that choice while the next item gets the default English selection again.
	 */
	fun getSubtitleSelection(itemId: UUID): Int? = _subtitleSelection?.takeIf { it.first == itemId }?.second

	fun setSubtitleSelection(itemId: UUID?, index: Int) {
		_subtitleSelection = itemId?.let { it to index }
	}

	fun clearVideoQueue() {
		_currentVideoQueue = emptyList()
		_currentMediaPosition = -1
		_lastPlayedAudioLanguageIsoCode = null
		_lastPlayedSubtitleLanguageIsoCode = null
		_subtitleSelection = null
	}
}
