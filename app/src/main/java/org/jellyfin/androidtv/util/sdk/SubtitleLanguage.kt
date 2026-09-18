package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

private val hearingImpairedFileName = Regex("""\.hi\.[^./\\]+$""", RegexOption.IGNORE_CASE)
private val hindiCodes = setOf("hi", "hin")

/**
 * External subtitle files named like `Show.hi.srt` conventionally mean "hearing impaired", but the
 * server reports them as Hindi (ISO 639 `hi`).
 */
val MediaStream.isMislabeledHearingImpaired: Boolean
	get() = type == MediaStreamType.SUBTITLE &&
		isExternal &&
		language?.lowercase() in hindiCodes &&
		path?.let { hearingImpairedFileName.containsMatchIn(it) } == true

/**
 * Find the subtitle stream matching [language]. Exact language matches win; mislabeled
 * hearing-impaired streams are only used as a fallback when Hindi was not explicitly requested.
 */
fun List<MediaStream>.findSubtitleIndexForLanguage(language: String): Int? {
	val subtitles = filter { it.type == MediaStreamType.SUBTITLE }
	subtitles.firstOrNull { it.language == language && !it.isMislabeledHearingImpaired }?.let { return it.index }
	if (language.lowercase() in hindiCodes) return subtitles.firstOrNull { it.language == language }?.index
	return subtitles.firstOrNull { it.isMislabeledHearingImpaired }?.index
}
