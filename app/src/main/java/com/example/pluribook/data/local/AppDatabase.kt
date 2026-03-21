package com.example.pluribook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.pluribook.data.model.User
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.Comment

@TypeConverters(Converters::class)
@Database(entities = [User::class, Post::class, Comment::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
}