package com.withparadox2.simpleocr.support.store

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by withparadox2 on 2018/11/5.
 */
@Entity
class BookInfo(@PrimaryKey(autoGenerate = true) var id: Long?,
               @ColumnInfo(name = "title") var title: String,
               @ColumnInfo(name = "author") var author: String) {
  constructor() : this(null, "", "") {}
}