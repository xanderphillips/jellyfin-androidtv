package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType

private fun sub(index: Int, language: String?, path: String? = null, external: Boolean = true) = MediaStream(
	index = index,
	type = MediaStreamType.SUBTITLE,
	language = language,
	path = path,
	isExternal = external,
	isInterlaced = false,
	isDefault = false,
	isForced = false,
	isHearingImpaired = false,
	isTextSubtitleStream = true,
	supportsExternalStream = true,
	videoRangeType = VideoRangeType.UNKNOWN,
	audioSpatialFormat = org.jellyfin.sdk.model.api.AudioSpatialFormat.NONE,
)

class SubtitleLanguageTest : FunSpec({
	test("detects .hi.srt external Hindi as mislabeled") {
		sub(1, "hin", "/tv/Show - S02E03.hi.srt").isMislabeledHearingImpaired shouldBe true
		sub(1, "hi", "/tv/Show - S02E03.HI.SRT").isMislabeledHearingImpaired shouldBe true
	}

	test("does not flag real Hindi or non-matching files") {
		sub(1, "hin", "/tv/Show.hin.srt").isMislabeledHearingImpaired shouldBe false
		sub(1, "hin", "/tv/Show.hi.srt", external = false).isMislabeledHearingImpaired shouldBe false
		sub(1, "eng", "/tv/Show.hi.srt").isMislabeledHearingImpaired shouldBe false
	}

	test("falls back to mislabeled stream for english") {
		listOf(sub(1, "hin", "/tv/Show.hi.srt")).findSubtitleIndexForLanguage("eng") shouldBe 1
	}

	test("prefers exact match over mislabeled stream") {
		listOf(sub(1, "hin", "/tv/Show.hi.srt"), sub(2, "eng", "/tv/Show.en.srt")).findSubtitleIndexForLanguage("eng") shouldBe 2
	}

	test("hindi request does not select mislabeled stream over real hindi") {
		listOf(sub(1, "hin", "/tv/Show.hi.srt"), sub(2, "hin", "/tv/Show.hin.srt")).findSubtitleIndexForLanguage("hin") shouldBe 2
	}

	test("returns null when nothing matches") {
		listOf(sub(1, "fra", "/tv/Show.fr.srt")).findSubtitleIndexForLanguage("eng") shouldBe null
	}
})
