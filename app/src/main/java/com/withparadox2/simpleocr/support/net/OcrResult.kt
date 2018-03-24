package com.withparadox2.simpleocr.support.net

import com.google.gson.annotations.SerializedName

/**
 * Created by withparadox2 on 2018/3/24.
 */
class OcrResult {
    @SerializedName("log_id")
    var logId: Long = 0

    @SerializedName("words_result_num")
    var resultNum: Int = 0

    @SerializedName("words_result")
    lateinit var resultList: List<Map<String, String>>

    override fun toString(): String = resultList.joinToString(",") { item -> item.toString() }
}