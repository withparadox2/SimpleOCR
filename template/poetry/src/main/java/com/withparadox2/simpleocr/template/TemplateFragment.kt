package com.withparadox2.simpleocr.template

import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.withparadox2.simpleocr.baselib.template.BaseTemplateFragment
import com.withparadox2.simpleocr.baselib.template.dp2px
import com.withparadox2.simpleocr.templatepoetry.R

/**
 * Created by withparadox2 on 2018/11/17.
 */
class TemplateFragment : BaseTemplateFragment() {
    private lateinit var tvTitleAndAuthor: TextView
    private lateinit var tvDate: TextView
    private var mTitle: String? = null
    private var mAuthor: String? = null

    private var mLineHeight = 0f

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
        mLineHeight = dp2px(36f, activity)
        val container = rootView.findViewById<View>(R.id.layout_container)
        container.background = BackgroundDrawable()

        tvTitleAndAuthor = rootView.findViewById(R.id.tv_title_author)
        tvDate = rootView.findViewById(R.id.tv_date)

        tvTitleAndAuthor.setOnClickListener {
            delegate?.onSelectBookInfo()
        }

        val resources = getSelfResources()
        if (resources != null) {
            rootView.setBackgroundColor(resources.getColor(R.color.white))
            tvTitleAndAuthor.hint = resources.getText(R.string.edit_hint_title_author)
            etContent.hint = resources.getText(R.string.edit_hint_content)

            val font = Typeface.createFromAsset(getSelfAssetManager(), "lyric008.ttf")
            etContent.typeface = font

            etContent.setLineSpacing(0f, mLineHeight / etContent.textSize)
            var lp = etContent.layoutParams as LinearLayout.LayoutParams
            lp.topMargin = (mLineHeight + (mLineHeight - etContent.textSize) / 2).toInt()
            etContent.layoutParams = lp
            (etContent as LineEditText).setCursorDrawable(resources.getColor(R.color.colorAccent), etContent.textSize.toInt())

            tvTitleAndAuthor.typeface = font
            tvTitleAndAuthor.setLineSpacing(0f, mLineHeight / tvTitleAndAuthor.textSize)
            lp = tvTitleAndAuthor.layoutParams as LinearLayout.LayoutParams
            lp.height = mLineHeight.toInt()
            tvTitleAndAuthor.layoutParams

            lp = tvDate.layoutParams as LinearLayout.LayoutParams
            lp.topMargin = ((mLineHeight + mLineHeight - tvDate.textSize).toInt())
            tvDate.layoutParams = lp

            layoutContainer.setPadding(layoutContainer.paddingLeft, layoutContainer.paddingTop, layoutContainer.paddingRight, (mLineHeight * 3 / 4).toInt())

            tvTitleAndAuthor.setTextColor(resources.getColor(R.color.edit_main))
            etContent.setTextColor(resources.getColor(R.color.edit_main))
            tvDate.setTextColor(resources.getColor(R.color.edit_light))
        }
    }

    override fun setTitle(title: String?) {
        mTitle = title
        updateTitleAndAuthor()
    }

    override fun setAuthor(author: String?) {
        mAuthor = author
        updateTitleAndAuthor()
    }

    private fun updateTitleAndAuthor() {
        var text: String? = null
        if (mTitle != null) {
            text = mTitle
        }
        if (mAuthor != null) {
            if (text != null) {
                text += "•"
            }
            text += mAuthor
        }

        if (text != null) {
            text = "—$text"
        }
        tvTitleAndAuthor.text = text
    }

    override fun setDate(date: String?) {
        tvDate.text = date
    }

    inner class BackgroundDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val padding = dp2px(20f, this@TemplateFragment.activity)

        init {
            paint.color = -3355444
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
        }

        override fun draw(canvas: Canvas) {
            val layout = etContent.layout ?: return
            val lineCount = layout.lineCount

            canvas.drawColor(Color.WHITE)

            var lastLineY = 0f
            for (i in 0..(lineCount + 2)) {
                if (i >= lineCount) {
                    lastLineY += mLineHeight
                } else {
                    lastLineY = layout.getLineTop(i).toFloat() + mLineHeight
                }
                canvas.drawLine(padding, lastLineY, (bounds.width() - padding), lastLineY, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
        }

        override fun getOpacity(): Int {
            return PixelFormat.UNKNOWN
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }
    }
}