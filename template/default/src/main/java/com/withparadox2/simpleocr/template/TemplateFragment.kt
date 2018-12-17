package com.withparadox2.simpleocr.template

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.withparadox2.simpleocr.baselib.template.BaseTemplateFragment
import com.withparadox2.simpleocr.templatedefault.R

/**
 * Created by withparadox2 on 2018/11/17.
 */
class TemplateFragment : BaseTemplateFragment() {

    lateinit var tvTitle: TextView
    lateinit var tvAuthor: TextView
    lateinit var tvDate: TextView

    override fun onCreateViewInternal() {
        tvTitle = rootView.findViewById(R.id.tv_title)
        tvAuthor = rootView.findViewById(R.id.tv_author)
        tvDate = rootView.findViewById(R.id.tv_date)

        val action = View.OnClickListener {
            delegate?.onSelectBookInfo()
        }

        tvTitle.setOnClickListener(action)
        tvAuthor.setOnClickListener(action)

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
    }

    override fun setTitle(title: String) {
        tvTitle.text = title
    }

    override fun setAuthor(author: String) {
        tvAuthor.text = author
    }

    override fun setDate(date: String) {
        tvDate.text = date
    }
}