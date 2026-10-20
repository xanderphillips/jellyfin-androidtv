package org.jellyfin.androidtv.ui.itemdetail

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

enum class DeleteTargetKind { SERIES, SEASON, EPISODE, MOVIE, OTHER }

/** What the delete confirmation dialog needs to say. [count] is the number of contained episodes when known. */
data class DeleteConfirmation(
	val kind: DeleteTargetKind,
	val label: String,
	val typeName: String,
	val count: Int?,
)

fun BaseItemDto.toDeleteConfirmation(): DeleteConfirmation {
	val itemName = name?.takeIf { it.isNotBlank() } ?: "?"
	return when (type) {
		BaseItemKind.SERIES -> DeleteConfirmation(DeleteTargetKind.SERIES, itemName, "Series", recursiveItemCount?.takeIf { it > 0 })
		BaseItemKind.SEASON -> DeleteConfirmation(
			DeleteTargetKind.SEASON,
			listOfNotNull(seriesName?.takeIf { it.isNotBlank() }, itemName).joinToString(" - "),
			"Season",
			(recursiveItemCount ?: childCount)?.takeIf { it > 0 },
		)

		BaseItemKind.EPISODE -> {
			val code = if (parentIndexNumber != null && indexNumber != null) "S%02dE%02d".format(parentIndexNumber, indexNumber) else null
			DeleteConfirmation(DeleteTargetKind.EPISODE, listOfNotNull(code, itemName).joinToString(" - "), "Episode", null)
		}

		BaseItemKind.MOVIE -> DeleteConfirmation(DeleteTargetKind.MOVIE, itemName, "Movie", null)
		else -> DeleteConfirmation(DeleteTargetKind.OTHER, itemName, type.serialName, null)
	}
}

private fun DeleteConfirmation.message(context: Context): String {
	val res = context.resources
	return when (kind) {
		DeleteTargetKind.SERIES ->
			if (count != null) res.getQuantityString(R.plurals.item_delete_confirm_series_count, count, label, count)
			else res.getString(R.string.item_delete_confirm_series, label)

		DeleteTargetKind.SEASON ->
			if (count != null) res.getQuantityString(R.plurals.item_delete_confirm_season_count, count, label, count)
			else res.getString(R.string.item_delete_confirm_season, label)

		DeleteTargetKind.EPISODE -> res.getString(R.string.item_delete_confirm_episode, label)
		DeleteTargetKind.MOVIE -> res.getString(R.string.item_delete_confirm_movie, label)
		DeleteTargetKind.OTHER -> res.getString(R.string.item_delete_confirm_other, label, typeName)
	}
}

/** Shows the delete confirmation for [item]; "No" has initial focus so a stray select press cancels. */
fun showDeleteConfirmation(context: Context, item: BaseItemDto, onConfirm: Runnable) {
	val confirmation = item.toDeleteConfirmation()
	Timber.i("Showing delete confirmation for ${confirmation.typeName} ${item.id}")
	AlertDialog.Builder(context)
		.setTitle(R.string.item_delete_confirm_title)
		.setMessage(confirmation.message(context))
		.setNegativeButton(R.string.lbl_no, null)
		.setPositiveButton(R.string.lbl_delete) { _, _ -> onConfirm.run() }
		.create()
		.apply {
			setOnShowListener { getButton(DialogInterface.BUTTON_NEGATIVE)?.requestFocus() }
		}
		.show()
}
