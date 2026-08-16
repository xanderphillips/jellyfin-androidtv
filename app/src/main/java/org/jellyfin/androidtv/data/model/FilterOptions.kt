package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.ItemFilter


class FilterOptions {
	var isFavoriteOnly = false
	var isUnwatchedOnly = false
	var isPlayedOnly = false
	var isResumableOnly = false

	val filters: Set<ItemFilter>
		get() = buildSet {
			if (isFavoriteOnly) add(ItemFilter.IS_FAVORITE)
			if (isUnwatchedOnly) add(ItemFilter.IS_UNPLAYED)
			if (isPlayedOnly) add(ItemFilter.IS_PLAYED)
			if (isResumableOnly) add(ItemFilter.IS_RESUMABLE)
		}
}
