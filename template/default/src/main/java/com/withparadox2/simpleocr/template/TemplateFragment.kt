package com.withparadox2.simpleocr.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.withparadox2.simpleocr.baselib.template.BaseTemplateFragment
import com.withparadox2.simpleocr.templatedefault.R

/**
 * Created by withparadox2 on 2018/11/17.
 */
class TemplateFragment : BaseTemplateFragment() {
    override fun onBeforeRender() {
        etContent.isCursorVisible = false
    }

    override fun onAfterRender() {
        etContent.isCursorVisible = true
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_template
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View? = super.onCreateView(inflater, container, savedInstanceState)
        val resources = getSelfResources()
        if (resources != null) {
            rootView.setBackgroundColor(resources.getColor(R.color.white))
            tvTitle.hint = resources.getText(R.string.edit_hint_title)
            tvAuthor.hint = resources.getText(R.string.edit_hint_author)
            etContent.hint = resources.getText(R.string.edit_hint_content)

            tvTitle.setTextColor(resources.getColor(R.color.edit_main))
            tvAuthor.setTextColor(resources.getColor(R.color.edit_brown))
            etContent.setTextColor(resources.getColor(R.color.edit_main))
            tvDate.setTextColor(resources.getColor(R.color.edit_light))

            (rootView.findViewById<ImageView>(R.id.tv_left_quote)).setImageDrawable(resources.getDrawable(R.drawable.quote_begin))
            (rootView.findViewById<ImageView>(R.id.tv_right_quote)).setImageDrawable(resources.getDrawable(R.drawable.quote_end))
        }
        return view
    }
}