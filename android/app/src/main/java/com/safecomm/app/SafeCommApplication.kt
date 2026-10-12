package com.safecomm.app

import android.app.Application
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class SafeCommApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configure OSMDroid cache and user-agent
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
    }
}
