package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.ads.AdManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.QrRepository

class QRApplication : Application(), Application.ActivityLifecycleCallbacks {

    lateinit var database: AppDatabase
        private set
    lateinit var qrRepository: QrRepository
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    private var currentActivity: Activity? = null
    private var startedActivityCount = 0
    private var isAppInForeground = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Database & Repositories
        database = AppDatabase.getInstance(this)
        qrRepository = QrRepository(database.qrItemDao())
        userPreferencesRepository = UserPreferencesRepository(this)

        // Initialize AdMob
        AdManager.initialize(this)

        // Register Activity lifecycle callbacks
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        startedActivityCount++
        if (!isAppInForeground && startedActivityCount == 1) {
            isAppInForeground = true
            // Show App Open Ad on app foreground if ready and not already showing
            if (!AdManager.isShowingAd) {
                AdManager.showAppOpenAdIfAvailable(activity) {}
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount <= 0) {
            startedActivityCount = 0
            isAppInForeground = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    companion object {
        lateinit var instance: QRApplication
            private set
    }
}
