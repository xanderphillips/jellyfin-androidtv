package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.VideoRangeType

private fun sub(
	index: Int,
	language: String?,
	path: String? = null,
	external: Boolean = true,
	title: String? = null,
	forced: Boolean = false,
	hearingImpaired: Boolean = false,
	text: Boolean = true,
	default: Boolean = false,
	type: MediaStreamType = MediaStreamType.SUBTITLE,
) = MediaStream(
	index = index,
	type = type,
	language = language,
	path = path,
	title = title,
	displayTitle = title,
	isExternal = external,
	isInterlaced = false,
	isDefault = default,
	isForced = forced,
	isHearingImpaired = hearingImpaired,
	isTextSubtitleStream = text,
	supportsExternalStream = true,
	videoRangeType = VideoRangeType.UNKNOWN,
	audioSpatialFormat = org.jellyfin.sdk.model.api.AudioSpatialFormat.NONE,
)

private fun audio(index: Int, language: String?) = sub(index, language, external = false, type = MediaStreamType.AUDIO)

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

	test("single untagged subtitle is selected") {
		listOf(sub(1, null, external = false)).findPreferredEnglishSubtitleIndex() shouldBe 1
		listOf(sub(1, "und", external = false)).findPreferredEnglishSubtitleIndex() shouldBe 1
	}

	test("single foreign subtitle is not selected") {
		listOf(sub(1, "fre", external = false)).findPreferredEnglishSubtitleIndex() shouldBe null
	}

	test("english language variants are detected") {
		listOf("eng", "en", "EN-us", "en_GB").forEach { code ->
			listOf(sub(1, "fre"), sub(2, code)).findPreferredEnglishSubtitleIndex() shouldBe 2
		}
	}

	test("untagged track detected as english by title or filename") {
		listOf(sub(1, "fre"), sub(2, "und", title = "English")).findPreferredEnglishSubtitleIndex() shouldBe 2
		listOf(sub(1, "fre"), sub(2, null, "/tv/Show.en.srt")).findPreferredEnglishSubtitleIndex() shouldBe 2
		listOf(sub(1, "fre"), sub(2, null, external = false)).findPreferredEnglishSubtitleIndex() shouldBe null
	}

	test("plain beats cc beats sdh beats hi beats forced") {
		val plain = sub(1, "eng", "/tv/Show.en.srt")
		val cc = sub(2, "eng", "/tv/Show.en.cc.srt")
		val sdh = sub(3, "eng", title = "English SDH", external = false)
		val hi = sub(4, "eng", external = false, hearingImpaired = true)
		val forced = sub(5, "eng", external = false, forced = true)
		listOf(forced, hi, sdh, cc, plain).findPreferredEnglishSubtitleIndex() shouldBe 1
		listOf(forced, hi, sdh, cc).findPreferredEnglishSubtitleIndex() shouldBe 2
		listOf(forced, hi, sdh).findPreferredEnglishSubtitleIndex() shouldBe 3
		listOf(forced, hi).findPreferredEnglishSubtitleIndex() shouldBe 4
		listOf(forced).findPreferredEnglishSubtitleIndex() shouldBe 5
	}

	test("mislabeled hi.srt is selected as english but below plain english") {
		listOf(sub(1, "fre"), sub(2, "hin", "/tv/Show.hi.srt")).findPreferredEnglishSubtitleIndex() shouldBe 2
		listOf(sub(1, "hin", "/tv/Show.hi.srt"), sub(2, "eng", "/tv/Show.en.srt")).findPreferredEnglishSubtitleIndex() shouldBe 2
	}

	test("external hindi without path is mislabeled only when there is no hindi audio") {
		val hindiSub = sub(2, "hin")
		listOf(audio(0, "eng"), hindiSub).isMislabeledHearingImpaired(hindiSub) shouldBe true
		listOf(audio(0, "hin"), hindiSub).isMislabeledHearingImpaired(hindiSub) shouldBe false
		listOf(audio(0, "eng"), hindiSub).findPreferredEnglishSubtitleIndex() shouldBe 2
	}

	test("commentary loses to non commentary") {
		listOf(sub(1, "eng", title = "English Commentary", external = false), sub(2, "eng", title = "English SDH", external = false))
			.findPreferredEnglishSubtitleIndex() shouldBe 2
	}

	test("text beats image within a tier") {
		listOf(sub(1, "eng", external = false, text = false), sub(2, "eng", external = false)).findPreferredEnglishSubtitleIndex() shouldBe 2
	}

	test("cc marker does not match inside words") {
		listOf(sub(1, "eng", title = "English Accented", external = false), sub(2, "eng", title = "English", external = false, default = true))
			.findPreferredEnglishSubtitleIndex() shouldBe 2
		val accented = sub(1, "eng", title = "Accented")
		listOf(accented).captionTier(accented) shouldBe CaptionTier.PLAIN
	}

	test("english language request uses ranking") {
		listOf(sub(1, "eng", title = "English SDH", external = false), sub(2, "eng", external = false))
			.findSubtitleIndexForLanguage("eng") shouldBe 2
	}

	test("display title relabels mislabeled streams only") {
		val mislabeled = sub(1, "hin", "/tv/Show.hi.srt", title = "Hindi - SRT - External")
		val hindi = sub(2, "hin", "/tv/Show.hin.srt", title = "Hindi - SRT - External")
		val streams = listOf(mislabeled, hindi)
		streams.subtitleDisplayTitle(mislabeled) shouldBe "English (Hearing Impaired) - SRT - External"
		streams.subtitleDisplayTitle(hindi) shouldBe "Hindi - SRT - External"
		streams.subtitleLanguage(mislabeled) shouldBe "eng"
		streams.subtitleLanguage(hindi) shouldBe "hin"
		val untitled = sub(3, "hin", "/tv/Show.hi.srt")
		listOf(untitled).subtitleDisplayTitle(untitled) shouldBe "English (Hearing Impaired)"
	}
})
