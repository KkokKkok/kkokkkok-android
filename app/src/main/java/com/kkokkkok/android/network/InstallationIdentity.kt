package com.kkokkkok.android.network

import android.content.Context
import java.util.UUID

/** Stable local pilot identifier; it is not an account credential. */
class InstallationIdentity(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("pilot_identity", Context.MODE_PRIVATE)

    fun get(): String = synchronized(this) {
        com.kkokkkok.android.pilot.PilotCredentials.installationId()?.let { return@synchronized it }
        preferences.getString(KEY, null) ?: UUID.randomUUID().toString().also { id ->
            preferences.edit().putString(KEY, id).apply()
        }
    }

    companion object { private const val KEY = "installation_id" }
}
