package com.memshot.app

import android.app.Application
import com.memshot.app.analysis.Analyzer
import com.memshot.app.data.AppDatabase
import com.memshot.app.data.Prefs
import com.memshot.app.importer.PhoneCleaner
import com.memshot.app.importer.ScreenshotImporter
import com.memshot.app.work.Notifications
import com.memshot.app.work.WorkScheduler

class MemShotApp : Application() {
    val db by lazy { AppDatabase.get(this) }
    val prefs by lazy { Prefs(this) }
    val backup by lazy { com.memshot.app.data.Backup(this, db.shots(), prefs) }
    val cleaner by lazy { PhoneCleaner(this, prefs) }
    val importer by lazy { ScreenshotImporter(this, db.shots(), cleaner) }
    val analyzer by lazy { Analyzer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        if (prefs.onboarded && prefs.autoImport) WorkScheduler.watch(this)
    }
}
