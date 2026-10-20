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
		KeyEvent.KEYCODE_BUTTON_THUMBL -> PlaybackInputAction.PREVIOUS_ITEM
		KeyEvent.KEYCODE_BUTTON_THUMBR -> PlaybackInputAction.NEXT_ITEM
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
