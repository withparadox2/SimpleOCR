package com.withparadox2.simpleocr.util

/**
 * Created by withparadox2 on 2018/3/24.
 */

fun parseText(textList: List<Map<String, String>>?): String {
  return textList?.map { it["words"] }?.joinToString("\n") ?: ""
}
