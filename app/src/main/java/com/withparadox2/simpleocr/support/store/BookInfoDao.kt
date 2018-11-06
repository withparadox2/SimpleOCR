package com.withparadox2.simpleocr.support.store

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

/**
 * Created by withparadox2 on 2018/11/5.
 */
@Dao
interface BookInfoDao {

    @Query("select * from BookInfo")
    fun getAll(): List<BookInfo>

    @Insert(onConflict = REPLACE)
    fun insert(bookInfo: BookInfo)

    @Update
    fun update(bookInfo: BookInfo)

    @Delete
    fun delete(bookInfo: BookInfo)

    @Query("select * from BookInfo where id = :id")
    fun getBookInfoById(id: Long?): BookInfo?
}