package org.jellyfin.androidtv.update

/**
 * Makes sure the app is allowed to install packages before an update is downloaded.
 *
 * When the permission is missing the user is sent to the system "Install unknown apps" settings. Google TV shows the
 * full list there instead of this app's page, and nothing resumes the update afterwards, so this waits for the user to
 * return and reports whether installing is now allowed, letting the caller continue the update in the same session.
 */
class InstallPermissionGate(
	private val canInstall: () -> Boolean,
	private val openSettings: () -> Boolean,
	private val awaitReturn: suspend () -> Unit,
) {
	suspend fun ensurePermission(): Boolean {
		if (canInstall()) return true
		if (!openSettings()) return false

		awaitReturn()
		return canInstall()
	}
}
