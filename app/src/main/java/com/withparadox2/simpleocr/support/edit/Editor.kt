package com.withparadox2.simpleocr.support.edit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.util.toast

/**
 * Created by withparadox2 on 2018/11/3.
 */
class Editor constructor(val content: String, var callback: Callback?) {
    private var newContent = String(content.toCharArray())
    private val stepBackup = ArrayList<String>()

    interface Callback {
        fun onContentChange(content: String)
    }

    fun reset() {
        stepBackup.clear()
        newContent = String(content.toCharArray())
        onContentChange()
    }

    fun joinLines() {
        cacheBeforeChange(false)
        newContent = newContent.split("\n").joinToString("")
        onContentChange()
    }

    fun toChinese() {
        cacheBeforeChange(false)
        newContent = newContent.replace(",", "，")
                .replace(".", "。")
                .replace(Regex("\"(.*?)\""), "“$1”")
                .replace(":", "：")
                .replace(";", "；")
                .replace("(", "（")
                .replace(")", "）")
                .replace("?", "？")
                .replace("!", "！")
        onContentChange()
        joinLines()
    }

    fun toEnglish() {
        cacheBeforeChange(false)
        newContent = newContent.replace("，", ",")
                .replace("。", ",")
                .replace(Regex("“(.*?)”"), "\"$1\"")
                .replace("：", ":")
                .replace("；", ";")
                .replace("（", "(")
                .replace("）", ")")
                .replace("？", "?")
                .replace("！", "!")

        onContentChange()
        joinLines()
    }

    fun copy() {
        val clipboard = App.instance.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("simpleocr", newContent)
        clipboard.primaryClip = clip
        toast("copy success!")
    }

    fun updateContent(content: String) {
        newContent = content
    }

    private fun onContentChange() {
        callback?.onContentChange(newContent)
    }

    fun getLastChangeContent(): String {
        return newContent
    }

    private fun cacheBeforeChange(alloc: Boolean) {
        var text = newContent
        if (alloc) {
            text = String(newContent.toCharArray())
        }
        stepBackup.add(text)
    }

    fun lastStep() {
        if (hasLastStep()) {
            newContent = stepBackup.removeAt(stepBackup.size - 1)
            onContentChange()
        }
    }

    fun getBackStepCount(): Int {
        return stepBackup.size
    }

    fun hasLastStep(): Boolean {
        return stepBackup.size > 0
    }
}