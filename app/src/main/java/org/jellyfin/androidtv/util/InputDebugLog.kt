package org.jellyfin.androidtv.util

import android.app.Activity
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import org.jellyfin.androidtv.BuildConfig
import timber.log.Timber
import kotlin.math.abs

/** Temporary diagnostics for controller input. Does nothing outside debug builds. */
object InputDebugLog {
	private const val TAG = "QuestInput"
	private const val CHANGE_DELTA = 0.1f
	private const val MIN_MOVE_INTERVAL_MS = 100L
	private const val SOURCE_HASH = 31
	private const val CALLER_FRAMES = 8
	private val axes = intArrayOf(
		MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, MotionEvent.AXIS_RX, MotionEvent.AXIS_RY,
		MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y, MotionEvent.AXIS_HSCROLL, MotionEvent.AXIS_VSCROLL,
		MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_BRAKE, MotionEvent.AXIS_GAS,
	)
	private var lastMove = FloatArray(axes.size)
	private var lastMoveKey = 0
	private var lastMoveAt = 0L

	private fun focus(activity: Activity?): String {
		if (activity == null) return "activity=null"
		val touchMode = activity.window?.decorView?.isInTouchMode
		return "touchMode=$touchMode focus=${activity.currentFocus?.javaClass?.simpleName}"
	}

	private fun device(deviceId: Int) = InputDevice.getDevice(deviceId)?.name ?: "id=$deviceId"

	fun key(where: String, activity: Activity?, event: KeyEvent) {
		if (!BuildConfig.DEBUG) return
		Timber.tag(TAG).i(
			"%s KEY action=%d key=%s source=0x%x device=%s repeat=%d flags=0x%x %s",
			where, event.action, KeyEvent.keyCodeToString(event.keyCode), event.source,
			device(event.deviceId), event.repeatCount, event.flags, focus(activity)
		)
	}

	fun motion(where: String, activity: Activity?, event: MotionEvent) {
		if (!BuildConfig.DEBUG) return
		val values = FloatArray(axes.size) { event.getAxisValue(axes[it]) }
		if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
			val now = SystemClock.uptimeMillis()
			val key = event.source * SOURCE_HASH + event.actionMasked
			val changed = key != lastMoveKey || values.indices.any { abs(values[it] - lastMove[it]) > CHANGE_DELTA }
			if (!changed || now - lastMoveAt < MIN_MOVE_INTERVAL_MS) return
			lastMoveKey = key
			lastMove = values
			lastMoveAt = now
		}
		val axisText = axes.indices.filter { values[it] != 0f }
			.joinToString(" ") { "${MotionEvent.axisToString(axes[it])}=${values[it]}" }
		Timber.tag(TAG).i(
			"%s MOTION action=%s source=0x%x device=%s tool=%d %s %s",
			where, MotionEvent.actionToString(event.action), event.source, device(event.deviceId),
			event.getToolType(0), axisText, focus(activity)
		)
	}

	/** Logs who called the current method (debug builds only), for tracking down unexpected pauses. */
	fun caller(where: String) {
		if (!BuildConfig.DEBUG) return
		val frames = Throwable().stackTrace.drop(2).take(CALLER_FRAMES).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
		Timber.tag(TAG).i("%s called from %s", where, frames)
	}

	fun state(where: String, message: String) {
		if (!BuildConfig.DEBUG) return
		Timber.tag(TAG).i("%s %s", where, message)
	}
}
