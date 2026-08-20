package org.jellyfin.androidtv.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads an update APK into app-private storage and hands it to the system installer.
 * Requires the `REQUEST_INSTALL_PACKAGES` permission and the `${applicationId}.fileprovider`
 * FileProvider declared in AndroidManifest.xml. Always surfaces the system "install this app?"
 * confirmation - a non-system, non-device-owner app cannot install silently.
 */
class UpdateInstaller(
	private val context: Context,
) {
	/**
	 * Downloads [apkDownloadUrl] to the app's update directory, returning the resulting file,
	 * or null on failure.
	 */
	suspend fun downloadApk(apkDownloadUrl: String, apkAssetName: String): File? = withContext(Dispatchers.IO) {
		runCatching {
			val updatesDir = File(context.getExternalFilesDir(null), UPDATES_SUBDIRECTORY).apply { mkdirs() }
			val destination = File(updatesDir, apkAssetName)

			val connection = URL(apkDownloadUrl).openConnection() as HttpURLConnection
			try {
				connection.instanceFollowRedirects = true
				connection.connectTimeout = CONNECT_TIMEOUT_MS
				connection.readTimeout = READ_TIMEOUT_MS

				if (connection.responseCode != HttpURLConnection.HTTP_OK) {
					Timber.w("Update download failed with HTTP ${connection.responseCode}")
					return@runCatching null
				}

				connection.inputStream.use { input ->
					destination.outputStream().use { output -> input.copyTo(output) }
				}
			} finally {
				connection.disconnect()
			}

			destination
		}.onFailure { error ->
			Timber.w(error, "Failed to download update APK")
		}.getOrNull()
	}

	/**
	 * Builds and starts the system package-installer intent for [apkFile]. This always shows
	 * a user-facing confirmation dialog; there is no silent-install path available to a
	 * normal (non-system, non-device-owner) application.
	 */
	fun startInstall(apkFile: File) {
		val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)

		val intent = Intent(Intent.ACTION_VIEW).apply {
			setDataAndType(uri, "application/vnd.android.package-archive")
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}

		context.startActivity(intent)
	}

	companion object {
		private const val UPDATES_SUBDIRECTORY = "updates"
		private const val CONNECT_TIMEOUT_MS = 15_000
		private const val READ_TIMEOUT_MS = 30_000
	}
}
