package org.jellyfin.androidtv.ui.playback

import android.view.KeyEvent
import kotlin.math.abs

enum class PlaybackInputAction {
	SEEK_BACKWARD,
	SEEK_FORWARD,
	PLAY_PAUSE,
	TOGGLE_CONTROLS,
	PREVIOUS_ITEM,
	NEXT_ITEM,
}

/**
 * Maps gamepad-style controller buttons (Meta Quest controllers, regular gamepads) to playback actions.
 * Buttons that are already handled elsewhere (B/back, L1/R1/L2/R2 skip, D-pad) are intentionally absent.
 */
object GamepadPlaybackInput {
	fun actionForKey(keyCode: Int, controlsVisible: Boolean): PlaybackInputAction? = when (keyCode) {
		KeyEvent.KEYCODE_BUTTON_X -> PlaybackInputAction.PLAY_PAUSE
		// With the controls visible A activates the focused control instead
		KeyEvent.KEYCODE_BUTTON_A -> if (controlsVisible) null else PlaybackInputAction.PLAY_PAUSE
		KeyEvent.KEYCODE_BUTTON_Y -> PlaybackInputAction.TOGGLE_CONTROLS
		KeyEvent.KEYCODE_BUTTON_SELECT -> PlaybackInputAction.PREVIOUS_ITEM
		KeyEvent.KEYCODE_BUTTON_START -> PlaybackInputAction.NEXT_ITEM
		else -> null
	}
}

/**
 * Turns horizontal thumbstick deflection into discrete seek steps: one step when the stick is pushed
 * past [engageThreshold], then repeated every [repeatMs] after [initialDelayMs] while it is held.
 * The stick must fall back below [releaseThreshold] to re-arm, which avoids chatter around the edge.
 *
 * Joystick motion events are only delivered when an axis changes, so a held stick needs [tick] to be
 * called by a timer while [isEngaged] is true.
 */
class StickSeekRepeater(
	private val engageThreshold: Float = 0.6f,
	private val releaseThreshold: Float = 0.3f,
	private val initialDelayMs: Long = 450L,
	private val repeatMs: Long = 300L,
) {
	data class Result(val consumed: Boolean, val action: PlaybackInputAction?)

	private var direction = 0
	private var nextFireAt = 0L

	val isEngaged get() = direction != 0

	fun reset() {
		direction = 0
	}

	/** [hatX] is the digital D-pad axis, [y] is only used to ignore mostly-vertical pushes. */
	fun onAxes(x: Float, hatX: Float, y: Float, nowMs: Long): Result {
		val h = if (abs(hatX) > abs(x)) hatX else x
		val magnitude = abs(h)

		if (direction != 0) {
			if (magnitude < releaseThreshold) {
				direction = 0
				return Result(false, null)
			}
			if (magnitude >= engageThreshold && (if (h > 0) 1 else -1) != direction) return engage(h, nowMs)
			return Result(true, null)
		}

		if (magnitude < engageThreshold || abs(y) > magnitude) return Result(false, null)
		return engage(h, nowMs)
	}

	/** Returns a repeat step if the held stick is due one. */
	fun tick(nowMs: Long): PlaybackInputAction? {
		if (direction == 0 || nowMs < nextFireAt) return null
		nextFireAt = nowMs + repeatMs
		return actionFor(direction)
	}

	private fun engage(h: Float, nowMs: Long): Result {
		direction = if (h > 0) 1 else -1
		nextFireAt = nowMs + initialDelayMs
		return Result(true, actionFor(direction))
	}

	private fun actionFor(dir: Int) = if (dir > 0) PlaybackInputAction.SEEK_FORWARD else PlaybackInputAction.SEEK_BACKWARD
}

/**
 * Meta Quest delivers the left thumbstick to 2D apps as pointer-class ACTION_SCROLL events whose
 * AXIS_HSCROLL is small (roughly 0.1 to 0.3) and arrive about 60 times a second while the stick is held.
 * Negative skips backward and positive skips forward.
 *
 * A push gives one skip straight away. Holding the stick repeats after [holdDelayMs] at [repeatMs]. A
 * stray opposite-signed event inside a push is ignored until [flipConfirmCount] of them arrive in a row,
 * and a pause of more than [pushGapMs] between events starts a new push.
 */
class ScrollSeekMapper(
	private val deadZone: Float = 0.1f,
	private val holdDelayMs: Long = 600L,
	private val repeatMs: Long = 450L,
	private val pushGapMs: Long = 300L,
	private val flipConfirmCount: Int = 3,
) {
	private var direction = 0
	private var lastEventAt = Long.MIN_VALUE
	private var nextRepeatAt = 0L
	private var oppositeRun = 0

	/** True when the scroll is mostly horizontal and big enough to be a deliberate push. */
	fun isHorizontalPush(hScroll: Float, vScroll: Float) = abs(hScroll) >= deadZone && abs(hScroll) >= abs(vScroll)

	/** The step for this scroll event, or null when it is a dead zone value, jitter, or between repeats. */
	fun onScroll(hScroll: Float, nowMs: Long): PlaybackInputAction? {
		if (abs(hScroll) < deadZone) return null

		val sign = if (hScroll > 0) 1 else -1
		val newPush = lastEventAt == Long.MIN_VALUE || nowMs - lastEventAt > pushGapMs
		lastEventAt = nowMs

		if (newPush || direction == 0) return startPush(sign, nowMs)

		if (sign != direction) {
			oppositeRun++
			return if (oppositeRun >= flipConfirmCount) startPush(sign, nowMs) else null
		}
		oppositeRun = 0

		if (nowMs < nextRepeatAt) return null
		nextRepeatAt = nowMs + repeatMs
		return actionFor(direction)
	}

	fun reset() {
		direction = 0
		lastEventAt = Long.MIN_VALUE
		oppositeRun = 0
	}

	private fun startPush(sign: Int, nowMs: Long): PlaybackInputAction {
		direction = sign
		oppositeRun = 0
		nextRepeatAt = nowMs + holdDelayMs
		return actionFor(sign)
	}

	private fun actionFor(dir: Int) = if (dir > 0) PlaybackInputAction.SEEK_FORWARD else PlaybackInputAction.SEEK_BACKWARD
}

/** Decisions for pointer (touchscreen/mouse) interaction with the playback screen. */
object PointerPlayback {
	/** A tap on the video toggles the controls unless it landed on a control or a popup/guide is open. */
	fun shouldToggleControlsOnTap(
		isTap: Boolean,
		hitControl: Boolean,
		popupOrGuideVisible: Boolean,
	) = isTap && !hitControl && !popupOrGuideVisible

	/** A gesture is a tap when it ended close to where it started and quickly. */
	fun isTap(dx: Float, dy: Float, durationMs: Long, slopPx: Float, maxDurationMs: Long = 500L) =
		abs(dx) <= slopPx && abs(dy) <= slopPx && durationMs <= maxDurationMs

	/** True for a view that covers most of the screen, i.e. a container rather than a control. */
	fun isContainerSized(viewWidth: Int, viewHeight: Int, rootWidth: Int, rootHeight: Int, fraction: Float = 0.8f) =
		viewWidth >= rootWidth * fraction && viewHeight >= rootHeight * fraction

	/** Maps a horizontal touch position on the seek bar to a playback position in milliseconds. */
	fun seekPositionMs(x: Float, barLeft: Float, barWidth: Float, durationMs: Long): Long {
		if (barWidth <= 0f || durationMs <= 0L) return 0L
		val fraction = ((x - barLeft) / barWidth).coerceIn(0f, 1f)
		return (fraction * durationMs).toLong()
	}
}

/**
 * Collects skip requests from repeated clicks on the rewind and fast-forward buttons into one seek that
 * is issued once the clicks have stopped for [quietMs]. A seek that could not be issued is put back.
 */
class SkipAccumulator(private val quietMs: Long = 400L) {
	private var pendingMs = 0
	private var lastChangeAt = 0L

	val hasPending get() = pendingMs != 0

	fun add(deltaMs: Int, nowMs: Long) {
		pendingMs += deltaMs
		lastChangeAt = nowMs
	}

	/** The accumulated skip once no click has arrived for [quietMs], else null. Clears the accumulator. */
	fun takeIfQuiet(nowMs: Long): Int? {
		if (pendingMs == 0 || nowMs - lastChangeAt < quietMs) return null
		return pendingMs.also { pendingMs = 0 }
	}

	/** Puts a skip back after the player refused it (for example, a seek was still running). */
	fun restore(deltaMs: Int) {
		pendingMs += deltaMs
	}
}

/**
 * Left and right on the focused seek bar normally start Leanback's scrub mode, which pauses the video until
 * the position is confirmed. While the video is playing they skip instead, so playback never pauses; scrubbing
 * with thumbnails stays available when the video is paused.
 */
object SeekBarDpadSkip {
	fun actionFor(keyCode: Int, isPlaying: Boolean, seekBarFocused: Boolean): PlaybackInputAction? = when {
		!isPlaying || !seekBarFocused -> null
		keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> PlaybackInputAction.SEEK_FORWARD
		keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> PlaybackInputAction.SEEK_BACKWARD
		else -> null
	}
}

/** What the D-pad does while a video is playing with the controls hidden. */
enum class HiddenDpadAction { SKIP_BACKWARD, SKIP_FORWARD, SWALLOW, SHOW_CONTROLS, NONE }

/**
 * With the controls hidden and the video playing, left and right skip, up and down do nothing, and the centre
 * key only brings the controls up (it does not pause). Everything else (paused, live TV, guide, popups) keeps the normal behaviour.
 */
object HiddenOverlayDpad {
	fun actionFor(
		keyCode: Int,
		overlayHidden: Boolean,
		isPlaying: Boolean,
		isLiveTv: Boolean,
		otherUiVisible: Boolean,
	): HiddenDpadAction = when {
		!overlayHidden || !isPlaying || isLiveTv || otherUiVisible -> HiddenDpadAction.NONE
		keyCode == KeyEvent.KEYCODE_DPAD_LEFT -> HiddenDpadAction.SKIP_BACKWARD
		keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> HiddenDpadAction.SKIP_FORWARD
		keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> HiddenDpadAction.SWALLOW
		keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER -> HiddenDpadAction.SHOW_CONTROLS
		else -> HiddenDpadAction.NONE
	}
}

