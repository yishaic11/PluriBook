package com.example.pluribook

import android.app.Application
import androidx.room.Room
import com.example.pluribook.data.local.AppDatabase

const val TAG = "PluribookApplication"

class PluribookApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "pluribook_database"
        ).fallbackToDestructiveMigration()
            .build()
    }
}