package com.example.pluribook

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.pluribook.data.local.AppDatabase

const val TAG = "PluribookApplication"

class PluribookApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "pluribook_database"
        ).fallbackToDestructiveMigration().build()
    }

    val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("pluribook_prefs", Context.MODE_PRIVATE)
    }
}