package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.koin.compose.koinInject
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes

private val REFETCH_INTERVAL = 5.minutes

private fun gaugeColor(usedPercentage: Double): Color = when {
	usedPercentage >= 90 -> Tokens.Color.colorRed500
	usedPercentage >= 80 -> Tokens.Color.colorOrange400
	else -> Tokens.Color.colorGreen500
}

@Composable
fun DiskSpaceGauge(modifier: Modifier = Modifier) {
	val api = koinInject<ApiClient>()
	val userRepository = koinInject<UserRepository>()
	val context = LocalContext.current

	val currentUser by userRepository.currentUser.collectAsState()
	val isAdministrator = currentUser?.policy?.isAdministrator == true
	val lifecycleOwner = LocalLifecycleOwner.current

	var storage by remember { mutableStateOf<AggregateStorage?>(null) }

	LaunchedEffect(api, isAdministrator, lifecycleOwner) {
		if (!isAdministrator) return@LaunchedEffect

		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			while (isActive) {
				val libraries = try {
					withContext(Dispatchers.IO) {
						api.systemApi.getSystemStorage().content.libraries
					}
				} catch (error: ApiClientException) {
					Timber.e(error, "Failed to load system storage")
					null
				}

				if (libraries != null) storage = aggregateLibraryStorage(libraries)
				delay(REFETCH_INTERVAL)
			}
		}
	}

	if (!isAdministrator) return

	val aggregate = storage
	if (aggregate == null || !aggregate.hasData) return

	val description = context.getString(
		R.string.disk_space_gauge_description,
		android.text.format.Formatter.formatFileSize(context, aggregate.usedSpace),
		android.text.format.Formatter.formatFileSize(context, aggregate.freeSpace),
		android.text.format.Formatter.formatFileSize(context, aggregate.totalSpace),
	)

	Box(
		modifier = modifier
			.width(96.dp)
			.height(6.dp)
			.clip(JellyfinTheme.shapes.extraSmall)
			.background(Tokens.Color.colorBluegrey700)
			.semantics { contentDescription = description },
	) {
		Box(
			modifier = Modifier
				.width(96.dp * (aggregate.usedPercentage / 100).toFloat().coerceIn(0f, 1f))
				.height(6.dp)
				.clip(JellyfinTheme.shapes.extraSmall)
				.background(gaugeColor(aggregate.usedPercentage)),
		)
	}
}
