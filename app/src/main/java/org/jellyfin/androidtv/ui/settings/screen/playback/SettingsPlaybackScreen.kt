package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.preference.SkipLengths
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
private fun formatSkipLength(ms: Int): String {
	val wholeMinutes = ms >= SkipLengths.MS_PER_MINUTE && ms % SkipLengths.MS_PER_MINUTE == 0
	return if (wholeMinutes) stringResource(R.string.skip_length_minutes, ms / SkipLengths.MS_PER_MINUTE)
	else stringResource(R.string.skip_length_seconds, ms / SkipLengths.MS_PER_SECOND)
}

@Composable
fun SettingsPlaybackScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val externalAppRepository = koinInject<ExternalAppRepository>()
	val userPreferences = koinInject<UserPreferences>()
	val userSettingPreferences = koinInject<UserSettingPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback)) },
			)
		}

		item {
			var skipBackLength by rememberPreference(userSettingPreferences, UserSettingPreferences.skipBackLength)

			ListButton(
				headingContent = { Text(stringResource(R.string.skip_back_length)) },
				captionContent = { Text(formatSkipLength(skipBackLength)) },
				onClick = { skipBackLength = SkipLengths.next(skipBackLength) },
				modifier = Modifier.focusKey("skip_back_length")
			)
		}

		item {
			var skipForwardLength by rememberPreference(userSettingPreferences, UserSettingPreferences.skipForwardLength)

			ListButton(
				headingContent = { Text(stringResource(R.string.skip_forward_length)) },
				captionContent = { Text(formatSkipLength(skipForwardLength)) },
				onClick = { skipForwardLength = SkipLengths.next(skipForwardLength) },
				modifier = Modifier.focusKey("skip_forward_length")
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_tv_play), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.playback_video_player)) },
				trailingContent = {
					val iconDrawable = remember(context) {
						externalAppRepository.getCurrentExternalPlayerApp(context)?.loadIcon(context.packageManager)
					}
					Image(
						painter = if (iconDrawable == null) rememberAsyncImagePainter(R.mipmap.app_icon)
						else rememberAsyncImagePainter(iconDrawable),
						contentDescription = null,
						modifier = Modifier
							.size(24.dp)
							.clip(LocalShapes.current.small)
					)
				},
				onClick = { router.push(Routes.PLAYBACK_PLAYER) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PLAYER)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_photo), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.photo_player)) },
				onClick = { router.push(Routes.PLAYBACK_PHOTO_PLAYER) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PHOTO_PLAYER)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_next_up), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_next_up)) },
				onClick = { router.push(Routes.PLAYBACK_NEXT_UP) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_NEXT_UP)
			)
		}

		item {
			var stillWatchingBehavior by rememberPreference(userPreferences, UserPreferences.stillWatchingBehavior)
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_zzz), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_inactivity_prompt)) },
				captionContent = { Text(stringResource(stillWatchingBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_INACTIVITY_PROMPT) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_INACTIVITY_PROMPT)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_trailer), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_prerolls)) },
				onClick = { router.push(Routes.PLAYBACK_PREROLLS) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_PREROLLS)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_subtitles), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_customization_subtitles)) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_clapperboard), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_media_segments)) },
				onClick = { router.push(Routes.PLAYBACK_MEDIA_SEGMENTS) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_MEDIA_SEGMENTS)
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_more), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) },
				onClick = { router.push(Routes.PLAYBACK_ADVANCED) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_ADVANCED)
			)
		}
	}
}
