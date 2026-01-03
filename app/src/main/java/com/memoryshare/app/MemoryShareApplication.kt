package com.memoryshare.app

import android.app.Application
import com.memoryshare.app.data.local.AppDatabase

class MemoryShareApplication : Application() {
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }
}
