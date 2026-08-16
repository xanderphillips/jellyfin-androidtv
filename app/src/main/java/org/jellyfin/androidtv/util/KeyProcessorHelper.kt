package org.jellyfin.androidtv.util

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.ui.itemdetail.deleteLibraryItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID

/**
 * Deletes [item] from the library and lets the surrounding row/grid react to the removal:
 * the shared delete helper records the id in [DataRefreshService.lastDeletedItemId] (consumed
 * by the browse/home surfaces) and a refresh message is pushed for the current item.
 */
fun FragmentActivity.deleteItemAndRefresh(item: BaseItemDto) {
	val api by inject<ApiClient>()
	val dataRefreshService by inject<DataRefreshService>()
	val customMessageRepository by inject<CustomMessageRepository>()

	lifecycleScope.launch {
		val success = deleteLibraryItem(api, dataRefreshService, item.id, item.name)

		if (success) {
			Toast.makeText(
				this@deleteItemAndRefresh,
				getString(R.string.item_deleted, item.name),
				Toast.LENGTH_LONG
			).show()
			customMessageRepository.pushMessage(CustomMessage.RefreshCurrentItem)
		} else {
			Toast.makeText(
				this@deleteItemAndRefresh,
				getString(R.string.item_deletion_failed, item.name),
				Toast.LENGTH_LONG
			).show()
		}
	}
}

fun FragmentActivity.playFirstUnwatchedItem(parentId: UUID) {
	val api by inject<ApiClient>()
	val playbackHelper by inject<PlaybackHelper>()

	lifecycleScope.launch(Dispatchers.IO) {
		runCatching {
			api.itemsApi.getItems(
				parentId = parentId,
				recursive = true,
				isMissing = false,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				sortOrder = setOf(SortOrder.ASCENDING),
				limit = 1,
				excludeItemTypes = setOf(),
				filters = setOf(ItemFilter.IS_UNPLAYED),
			)
		}.fold(
			onSuccess = { response ->
				val item = response.content.items?.firstOrNull()
				withContext(Dispatchers.Main) {
					if (item == null) {
						Toast.makeText(
							this@playFirstUnwatchedItem,
							R.string.msg_no_items,
							Toast.LENGTH_LONG
						).show()
					} else {
						playbackHelper.retrieveAndPlay(item.id, false, this@playFirstUnwatchedItem)
					}
				}
			},
			onFailure = { error ->
				Timber.e(error, "Error trying to play first unwatched")
				Toast.makeText(
					this@playFirstUnwatchedItem,
					R.string.msg_video_playback_error,
					Toast.LENGTH_LONG
				).show()
			},
		)
	}
}
