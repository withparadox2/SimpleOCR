package com.withparadox2.simpleocr.support.pref

import androidx.core.content.edit
import com.withparadox2.simpleocr.util.getSp

const val KEY_LAST_BOOK_ID = "last_book_id"
const val KEY_LAST_TEMPLATE = "last_template"

fun getLastBookInfoId(): Long {
    return getSp().getLong(KEY_LAST_BOOK_ID, 0)
}

fun setLastBookInfoId(id: Long?) {
    getSp().edit { putLong(KEY_LAST_BOOK_ID, id ?: 0) }
}

fun getLastTemplateKey(): String? {
    return getSp().getString(KEY_LAST_TEMPLATE, null)
}

fun setLastTemplateKey(key: String?) {
    getSp().edit { putString(KEY_LAST_TEMPLATE, key) }
}