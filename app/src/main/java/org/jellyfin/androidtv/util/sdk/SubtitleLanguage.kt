package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

private val hearingImpairedFileName = Regex("""\.hi\.[^./\\]+$""", RegexOption.IGNORE_CASE)
private val hindiCodes = setOf("hi", "hin")
private val undeterminedCodes = setOf("und", "unknown", "")

private val ccTitle = Regex("""\bCC\b|closed caption""", RegexOption.IGNORE_CASE)
private val sdhTitle = Regex("""\bSDH\b""", RegexOption.IGNORE_CASE)
private val hiTitle = Regex("""\bHI\b|hearing impaired""", RegexOption.IGNORE_CASE)
private val forcedTitle = Regex("""\bforced\b""", RegexOption.IGNORE_CASE)
private val commentaryTitle = Regex("""\bcommentary\b""", RegexOption.IGNORE_CASE)
private val englishTitle = Regex("""\benglish\b""", RegexOption.IGNORE_CASE)

const val HEARING_IMPAIRED_ENGLISH_LABEL = "English (Hearing Impaired)"

internal enum class CaptionTier { PLAIN, CC, SDH, HI, FORCED }

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
 * Like [MediaStream.isMislabeledHearingImpaired], but also covers servers that omit the subtitle path:
 * an external "Hindi" subtitle is then assumed to be hearing impaired unless the media has Hindi audio.
 */
fun List<MediaStream>.isMislabeledHearingImpaired(stream: MediaStream): Boolean {
	if (stream.isMislabeledHearingImpaired) return true
	if (stream.type != MediaStreamType.SUBTITLE || !stream.isExternal || !stream.path.isNullOrBlank()) return false
	if (stream.language?.lowercase() !in hindiCodes) return false
	return none { it.type == MediaStreamType.AUDIO && it.language?.lowercase() in hindiCodes }
}

private val MediaStream.fileNameTokens: List<String>
	get() = path?.substringAfterLast('/')?.substringAfterLast('\\')?.lowercase()?.split('.')?.drop(1)?.dropLast(1).orEmpty()

private val MediaStream.titleText: String
	get() = listOfNotNull(title, displayTitle).joinToString(" ")

private val MediaStream.hasUndeterminedLanguage: Boolean
	get() = language.isNullOrBlank() || language!!.lowercase() in undeterminedCodes

private fun isEnglishCode(code: String?): Boolean {
	val lower = code?.lowercase() ?: return false
	return lower == "en" || lower == "eng" || lower.startsWith("en-") || lower.startsWith("en_")
}

internal fun List<MediaStream>.isEnglishSubtitle(stream: MediaStream): Boolean = when {
	stream.type != MediaStreamType.SUBTITLE -> false
	isEnglishCode(stream.language) -> true
	isMislabeledHearingImpaired(stream) -> true
	stream.hasUndeterminedLanguage -> englishTitle.containsMatchIn(stream.titleText) ||
		stream.fileNameTokens.any { it == "en" || it == "eng" }
	else -> false
}

internal fun List<MediaStream>.captionTier(stream: MediaStream): CaptionTier {
	val title = stream.titleText
	val tokens = stream.fileNameTokens
	return when {
		stream.isForced || forcedTitle.containsMatchIn(title) || "forced" in tokens -> CaptionTier.FORCED
		ccTitle.containsMatchIn(title) || "cc" in tokens -> CaptionTier.CC
		sdhTitle.containsMatchIn(title) || "sdh" in tokens -> CaptionTier.SDH
		stream.isHearingImpaired || isMislabeledHearingImpaired(stream) || hiTitle.containsMatchIn(title) || "hi" in tokens -> CaptionTier.HI
		else -> CaptionTier.PLAIN
	}
}

/**
 * Pick the best English subtitle: plain, then CC, then SDH, then hearing impaired, with forced tracks as a
 * last resort. A lone subtitle without a language tag is assumed to be English.
 */
fun List<MediaStream>.findPreferredEnglishSubtitleIndex(): Int? {
	val subtitles = filter { it.type == MediaStreamType.SUBTITLE }
	val candidates = subtitles.filter { isEnglishSubtitle(it) }
		.ifEmpty { subtitles.singleOrNull()?.takeIf { it.hasUndeterminedLanguage }?.let(::listOf).orEmpty() }

	return candidates.minWithOrNull(
		compareBy<MediaStream> { captionTier(it) == CaptionTier.FORCED }
			.thenBy { commentaryTitle.containsMatchIn(it.titleText) }
			.thenBy { captionTier(it) }
			.thenBy { !it.isTextSubtitleStream }
			.thenBy { !it.isDefault }
			.thenBy { it.index }
	)?.index
}

/**
 * Find the subtitle stream matching [language]. English requests use [findPreferredEnglishSubtitleIndex]. Exact
 * language matches win; mislabeled hearing-impaired streams are only used as a fallback when Hindi was not
 * explicitly requested.
 */
fun List<MediaStream>.findSubtitleIndexForLanguage(language: String): Int? {
	if (isEnglishCode(language)) return findPreferredEnglishSubtitleIndex()
	val subtitles = filter { it.type == MediaStreamType.SUBTITLE }
	subtitles.firstOrNull { it.language == language && !isMislabeledHearingImpaired(it) }?.let { return it.index }
	if (language.lowercase() in hindiCodes) return subtitles.firstOrNull { it.language == language }?.index
	return subtitles.firstOrNull { isMislabeledHearingImpaired(it) }?.index
}

/**
 * Display title for a subtitle stream, relabeling mislabeled `.hi.srt` streams as hearing-impaired English.
 */
fun List<MediaStream>.subtitleDisplayTitle(stream: MediaStream): String? {
	if (!isMislabeledHearingImpaired(stream)) return stream.displayTitle ?: stream.title
	val suffix = stream.displayTitle?.substringAfter(" - ", missingDelimiterValue = "").orEmpty()
	return if (suffix.isBlank()) HEARING_IMPAIRED_ENGLISH_LABEL else "$HEARING_IMPAIRED_ENGLISH_LABEL - $suffix"
}

/**
 * Language code to report for a subtitle stream, reporting mislabeled `.hi.srt` streams as English.
 */
fun List<MediaStream>.subtitleLanguage(stream: MediaStream): String? =
	if (isMislabeledHearingImpaired(stream)) "eng" else stream.language
