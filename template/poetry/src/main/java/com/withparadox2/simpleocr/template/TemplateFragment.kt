package com.withparadox2.simpleocr.template

import android.graphics.Typeface
import android.widget.TextView
import com.withparadox2.simpleocr.baselib.template.BaseTemplateFragment
import com.withparadox2.simpleocr.templatedefault.R

/**
 * Created by withparadox2 on 2018/11/17.
 */
class TemplateFragment : BaseTemplateFragment() {

    lateinit var tvTitleAndAuthor: TextView
    lateinit var tvDate: TextView

    override fun onBeforeRender() {
        etContent.isCursorVisible = false
    }

    override fun onAfterRender() {
        etContent.isCursorVisible = true
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_template
    }

    override fun onCreateViewInternal() {
        tvTitleAndAuthor = rootView.findViewById(R.id.tv_title_author)
        tvDate = rootView.findViewById(com.withparadox2.simpleocr.baselib.R.id.tv_date)

        val resources = getSelfResources()
        if (resources != null) {
            rootView.setBackgroundColor(resources.getColor(R.color.white))
            tvTitleAndAuthor.hint = resources.getText(R.string.edit_hint_title)
            etContent.hint = resources.getText(R.string.edit_hint_content)

            val font = Typeface.createFromAsset(getSelfAssetManager(), "lyric008.ttf")
            etContent.typeface = font

            tvTitleAndAuthor.setTextColor(resources.getColor(R.color.edit_main))
            etContent.setTextColor(resources.getColor(R.color.edit_main))
            tvDate.setTextColor(resources.getColor(R.color.edit_light))
        }
    }

    override fun setTitle(title: String?) {
    }

    override fun setAuthor(author: String?) {
    }

    override fun setDate(date: String?) {
    }
}