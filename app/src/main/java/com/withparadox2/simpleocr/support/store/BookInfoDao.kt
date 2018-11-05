package com.withparadox2.simpleocr.support.store

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

/**
 * Created by withparadox2 on 2018/11/5.
 */
@Dao
interface BookInfoDao {

    @Query("select * from BookInfo")
    fun getAll(): List<BookInfo>

    @Insert(onConflict = REPLACE)
    fun insert(bookInfo: BookInfo)

    @Delete
    fun delete(bookInfo: BookInfo)
}