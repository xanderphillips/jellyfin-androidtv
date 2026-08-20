package org.jellyfin.androidtv.update

/**
 * Parsed representation of this project's versionName scheme: MAJOR.MINOR.PATCH[-type.N]
 * (see buildSrc/src/main/kotlin/VersionUtils.kt for the Gradle-side equivalent used to
 * derive versionCode at build time). Mirrored here because buildSrc classes are not part
 * of the app's runtime classpath.
 *
 * Sample input -> output:
 * "2.0.0"          -> AppVersion(2, 0, 0, null)
 * "2.0.0-rc.3"     -> AppVersion(2, 0, 0, 3)
 * "0.19.0-dev.959" -> AppVersion(0, 19, 0, 959)
 */
data class AppVersion(
	val major: Int,
	val minor: Int,
	val patch: Int,
	val preRelease: Int?,
) : Comparable<AppVersion> {
	override fun compareTo(other: AppVersion): Int {
		major.compareTo(other.major).let { if (it != 0) return it }
		minor.compareTo(other.minor).let { if (it != 0) return it }
		patch.compareTo(other.patch).let { if (it != 0) return it }

		// A version with no pre-release part is newer than one with a pre-release part
		// at the same core version (matches semver precedence rules).
		val thisPreRelease = preRelease ?: Int.MAX_VALUE
		val otherPreRelease = other.preRelease ?: Int.MAX_VALUE
		return thisPreRelease.compareTo(otherPreRelease)
	}

	companion object {
		/**
		 * Parse a versionName or a GitHub release tag (with optional leading "v") into an
		 * [AppVersion]. Returns null when the input does not match the expected scheme
		 * rather than throwing, since this is used on untrusted network input.
		 */
		fun parseOrNull(input: String): AppVersion? {
			val versionName = input.trim().removePrefix("v")
			if (versionName.isEmpty()) return null

			val dashIndex = versionName.indexOf('-')
			val core = if (dashIndex == -1) versionName else versionName.substring(0, dashIndex)
			val preReleasePart = if (dashIndex == -1) null else versionName.substring(dashIndex + 1)

			val coreParts = core.split('.')
			if (coreParts.size != 3) return null

			val major = coreParts[0].toIntOrNull() ?: return null
			val minor = coreParts[1].toIntOrNull() ?: return null
			val patch = coreParts[2].toIntOrNull() ?: return null

			// Pre-release part is "type.number" (e.g. "dev.959", "rc.3"); only the number matters here.
			val preRelease = preReleasePart?.substringAfterLast('.')?.toIntOrNull()

			return AppVersion(major, minor, patch, preRelease)
		}
	}
}
