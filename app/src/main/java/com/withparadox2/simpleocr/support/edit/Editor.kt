package com.withparadox2.simpleocr.support.edit

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
        onContentChange(false)
    }

    fun joinLines() {
        newContent = newContent.split("\n").joinToString("")
        onContentChange(true)
    }

    fun toChinese() {
        newContent = newContent.replace(",", "，")
                .replace(".", "。")
                .replace(Regex("\"(.*?)\""), "“$1”")
                .replace(":", "：")
                .replace(";", "；")
                .replace("(", "（")
                .replace(")", "）")
                .replace("?", "？")
                .replace("!", "！")
        onContentChange(true)
    }

    fun toEnglish() {
        newContent = newContent.replace("，", ",")
                .replace("。", ",")
                .replace(Regex("“(.*?)”"), "\"$1\"")
                .replace("：", ":")
                .replace("；", ";")
                .replace("（", "(")
                .replace("）", ")")
                .replace("？", "?")
                .replace("!", "！")

        onContentChange(true)
    }

    fun updateContent(content: String) {
        newContent = content
    }

    private fun onContentChange(backup: Boolean) {
        if (backup) {
            stepBackup.add(newContent)
            newContent = String(newContent.toCharArray())
        }
        callback?.onContentChange(newContent)
    }

    fun lastStep() {
        if (hasLastStep()) {
            newContent = stepBackup.removeAt(stepBackup.size - 1)
            onContentChange(false)
        }
    }

    fun hasLastStep(): Boolean {
        return stepBackup.size > 0
    }
}