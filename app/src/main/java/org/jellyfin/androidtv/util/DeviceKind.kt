package org.jellyfin.androidtv.util

import android.os.Build

/** True on Meta Quest headsets (reported as manufacturer "Oculus" or "Meta"). */
val isMetaHeadset: Boolean
	get() = Build.MANUFACTURER.equals("Oculus", ignoreCase = true) || Build.MANUFACTURER.equals("Meta", ignoreCase = true)
