package org.jellyfin.androidtv.update

/**
 * Coordinates the check-on-launch update check with startup navigation.
 *
 * StartupActivity finishes itself (via finishAfterTransition()) as soon as an existing session
 * is found. Before this class existed, that navigation ran completely independently of the
 * update check + "Update available" dialog: if the dialog attached to StartupActivity's window
 * right as navigation finished the activity, the dialog's window was torn down along with it
 * before the user could interact with it (observed on-device: the dialog flashed briefly, then
 * was immediately replaced by the app that had already finished launching underneath it).
 *
 * Callers must suspend on [awaitBeforeNavigating] before triggering navigation away from (or
 * finishing) the hosting activity. It resolves immediately when there's no update, and otherwise
 * only resolves once [showDialog] itself resolves - callers are expected to implement
 * [showDialog] so that it doesn't return until the user has responded to (or dismissed) the
 * update-available prompt, so navigation can never race ahead of a dialog that's currently
 * showing or about to show.
 */
class UpdateLaunchGate(
	private val updateChecker: UpdateChecker,
) {
	suspend fun awaitBeforeNavigating(
		currentVersionName: String,
		showDialog: suspend (UpdateCheckResult.UpdateAvailable) -> Unit,
	) {
		val result = updateChecker.checkForUpdate(currentVersionName)
		if (result is UpdateCheckResult.UpdateAvailable) showDialog(result)
	}
}
