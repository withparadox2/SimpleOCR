package com.withparadox2.simpleocr.support.store

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.withparadox2.simpleocr.App

/**
 * Created by withparadox2 on 2018/11/5.
 */
@Database(entities = [BookInfo::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookInfoDao(): BookInfoDao

    companion object {
        private var instance : AppDatabase? = null
        fun getInstance() : AppDatabase {
            if (instance == null) {
                synchronized(AppDatabase::class) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(App.instance, AppDatabase::class.java, "simpleorc.db")
                                .allowMainThreadQueries()
                                .build()
                    }
                }
            }
            return instance!!
        }
    }
}