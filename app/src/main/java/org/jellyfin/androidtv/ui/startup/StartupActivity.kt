package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.eventhandling.SocketHandler
import org.jellyfin.androidtv.databinding.ActivityStartupBinding
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.startup.fragment.SelectServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.ServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.SplashFragment
import org.jellyfin.androidtv.ui.startup.fragment.StartupToolbarFragment
import org.jellyfin.androidtv.update.UpdateCheckResult
import org.jellyfin.androidtv.update.UpdateInstaller
import org.jellyfin.androidtv.update.UpdateLaunchGate
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.androidtv.util.createBundle
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.UUID

class StartupActivity : FragmentActivity() {
	companion object {
		const val EXTRA_ITEM_ID = "ItemId"
		const val EXTRA_ITEM_IS_USER_VIEW = "ItemIsUserView"
		const val EXTRA_HIDE_SPLASH = "HideSplash"
	}

	private val startupViewModel: StartupViewModel by viewModel()
	private val api: ApiClient by inject()
	private val mediaManager: MediaManager by inject()
	private val sessionRepository: SessionRepository by inject()
	private val userRepository: UserRepository by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val itemLauncher: ItemLauncher by inject()
	private val workManager: WorkManager by inject()
	private val socketListener: SocketHandler by inject()
	private val updateLaunchGate: UpdateLaunchGate by inject()

	private lateinit var binding: ActivityStartupBinding

	/**
	 * Resolves once the check-on-launch update check (and, if applicable, the "Update available"
	 * dialog it shows) has fully resolved. Must be awaited before finishing this activity - see
	 * [UpdateLaunchGate] for why.
	 */
	private lateinit var updateCheckJob: Deferred<Unit>

	private val networkPermissionsRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { grants ->
		val anyRejected = grants.any { !it.value }

		if (anyRejected) {
			// Permission denied, exit the app.
			Toast.makeText(this, R.string.no_network_permissions, Toast.LENGTH_LONG).show()
			finish()
		} else {
			onPermissionsGranted()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		binding = ActivityStartupBinding.inflate(layoutInflater)
		binding.background.setContent { AppBackground() }
		binding.screensaver.isVisible = false
		setContentView(binding.root)

		if (!intent.getBooleanExtra(EXTRA_HIDE_SPLASH, false)) showSplash()

		// Ensure basic permissions
		networkPermissionsRequester.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))

		// MVP check-on-launch update check. Does not block reaching the login/server-select UI,
		// but IS awaited (see updateCheckJob.await() below) before this activity finishes itself
		// to navigate to MainActivity - otherwise finishing the activity can destroy the update
		// dialog's window mid-flight if the check/dialog resolve concurrently with session
		// readiness (this raced and lost in practice: the dialog would flash briefly then get
		// torn down as the already-in-flight navigation replaced it).
		updateCheckJob = lifecycleScope.async {
			runCatching {
				updateLaunchGate.awaitBeforeNavigating(BuildConfig.VERSION_NAME) { update ->
					awaitUpdateAvailableDialog(update)
				}
			}.onFailure { error ->
				Timber.w(error, "Update check/dialog gate failed")
			}
		}
	}

	private suspend fun awaitUpdateAvailableDialog(update: UpdateCheckResult.UpdateAvailable) {
		if (isFinishing || isDestroyed) return

		val dismissed = CompletableDeferred<Unit>()

		AlertDialog.Builder(this)
			.setTitle(getString(R.string.update_available_title))
			.setMessage(getString(R.string.update_available_message, update.tagName))
			.setPositiveButton(R.string.update_available_install) { _, _ -> installUpdate(update) }
			.setNegativeButton(R.string.update_available_later, null)
			.setCancelable(true)
			.setOnDismissListener { dismissed.complete(Unit) }
			.show()

		// Suspend until the dialog is dismissed (Install now, Later, cancel, or back press all
		// trigger the dismiss listener above) so navigation can't proceed while it's on screen.
		dismissed.await()
	}

	private fun installUpdate(update: UpdateCheckResult.UpdateAvailable) {
		// canRequestPackageInstalls()/ACTION_MANAGE_UNKNOWN_APP_SOURCES are API 26+; below that,
		// "install from unknown sources" is a single device-wide legacy setting we can't query
		// or deep-link to per-app, so just attempt the install directly.
		if (AndroidVersion.isAtLeastO && !packageManager.canRequestPackageInstalls()) {
			Toast.makeText(this, R.string.update_available_permission_required, Toast.LENGTH_LONG).show()
			startActivity(
				Intent(
					Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
					Uri.parse("package:$packageName")
				)
			)
			return
		}

		lifecycleScope.launch {
			val installer = UpdateInstaller(applicationContext)
			val apkFile = installer.downloadApk(update.apkDownloadUrl, update.apkAssetName)

			if (apkFile == null) {
				Toast.makeText(this@StartupActivity, R.string.update_available_download_failed, Toast.LENGTH_LONG).show()
			} else {
				installer.startInstall(apkFile)
			}
		}
	}

	override fun onResume() {
		super.onResume()

		applyTheme()
	}

	private fun onPermissionsGranted() = sessionRepository.state
		.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
		.filter { it == SessionRepositoryState.READY }
		.map { sessionRepository.currentSession.value }
		.distinctUntilChanged()
		.onEach { session ->
			if (session != null) {
				Timber.i("Found a session in the session repository, waiting for the currentUser in the application class.")

				showSplash()

				val currentUser = userRepository.currentUser.first { it != null }
				Timber.i("CurrentUser changed to ${currentUser?.id} while waiting for startup.")

				// Must not finish this activity (openNextActivity() does, via
				// finishAfterTransition()) while the update-available dialog is showing or about
				// to show - see updateCheckJob's assignment in onCreate() for why.
				updateCheckJob.await()

				lifecycleScope.launch {
					openNextActivity()
				}
			} else {
				// Clear audio queue in case left over from last run
				mediaManager.clearAudioQueue()

				val server = startupViewModel.getLastServer()
				if (server != null) showServer(server.id)
				else showServerSelection()
			}
		}.launchIn(lifecycleScope)

	private suspend fun openNextActivity() {
		val itemId = when {
			intent.action == Intent.ACTION_VIEW && intent.data != null -> intent.data.toString()
			else -> intent.getStringExtra(EXTRA_ITEM_ID)
		}?.toUUIDOrNull()
		val itemIsUserView = intent.getBooleanExtra(EXTRA_ITEM_IS_USER_VIEW, false)

		Timber.i("Determining next activity (action=${intent.action}, itemId=$itemId, itemIsUserView=$itemIsUserView)")

		// Update background worker
		with(ProcessLifecycleOwner.get().lifecycleScope) {
			launch {
				// Cancel all current workers
				workManager.cancelAllWork().await()

				// Recreate periodic workers
				LeanbackChannelWorker.enqueue(workManager)
			}

			// Update WebSockets
			launch { socketListener.updateSession() }
		}

		// Create destination
		val destination = when {
			// Search is requested
			intent.action == Intent.ACTION_SEARCH -> Destinations.search(
				query = intent.getStringExtra(SearchManager.QUERY)
			)
			// User view item is requested
			itemId != null && itemIsUserView -> runCatching {
				val item = withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemId = itemId).content
				}
				itemLauncher.getUserViewDestination(item)
			}.onFailure { throwable ->
				Timber.w(throwable, "Failed to retrieve item $itemId from server.")
			}.getOrNull()
			// Other item is requested
			itemId != null -> Destinations.itemDetails(itemId)
			// No destination requested, use default
			else -> null
		}

		navigationRepository.reset(destination, true)

		val intent = Intent(this, MainActivity::class.java)
		// Clear navigation history
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
		Timber.i("Opening next activity $intent")
		startActivity(intent)
		finishAfterTransition()
	}

	// Fragment switching
	private fun showSplash() {
		// Prevent progress bar flashing
		if (supportFragmentManager.findFragmentById(R.id.content_view) is SplashFragment) return

		supportFragmentManager.commit {
			replace<SplashFragment>(R.id.content_view)
		}
	}

	private fun showServer(id: UUID) = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<ServerFragment>(
			R.id.content_view, null, createBundle {
				putString(ServerFragment.ARG_SERVER_ID, id.toString())
			}
		)
	}

	private fun showServerSelection() = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<SelectServerFragment>(R.id.content_view)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
	}
}
