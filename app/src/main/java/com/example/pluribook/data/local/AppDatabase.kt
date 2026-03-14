package com.example.pluribook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pluribook.data.model.User
import com.example.pluribook.data.model.Post

@Database(entities = [User::class,Post::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postsDao(): PostsDao
}