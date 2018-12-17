package com.withparadox2.simpleocr.template

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.withparadox2.simpleocr.baselib.template.BaseTemplateFragment
import com.withparadox2.simpleocr.templatedream.R

/**
 * Created by withparadox2 on 2018/12/14.
 */
class TemplateFragment : BaseTemplateFragment() {

    private lateinit var tvTitle: TitleTextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvDate: TextView

    override fun onCreateViewInternal() {
        tvTitle = rootView.findViewById(R.id.tv_title)
        tvAuthor = rootView.findViewById(R.id.tv_author)
        tvDate = rootView.findViewById(R.id.tv_date)

        val action = View.OnClickListener {
            delegate?.onSelectBookInfo()
        }

        tvTitle.setOnClickListener(action)
        tvAuthor.setOnClickListener(action)
        (tvTitle.parent as View).setOnClickListener(action)

        val resources = getSelfResources()
        if (resources != null) {
            rootView.setBackgroundColor(resources.getColor(R.color.white))
            tvTitle.hint = resources.getText(R.string.edit_hint_title)
            tvAuthor.hint = resources.getText(R.string.edit_hint_author)
            etContent.hint = resources.getText(R.string.edit_hint_content)

            tvTitle.setTextColor(resources.getColor(R.color.black))
            tvAuthor.setTextColor(resources.getColor(R.color.black))
            etContent.setTextColor(resources.getColor(R.color.black))
            tvDate.setTextColor(resources.getColor(R.color.black))

            layoutContainer.background = BackgroundDrawable()
            rootView.findViewById<ImageView>(R.id.iv_flower).setImageDrawable(resources.getDrawable(R.drawable.flower))
        }
    }

    override fun setTitle(title: String) {
        tvTitle.setText2(title)
    }

    override fun setAuthor(author: String) {
        tvAuthor.text = author
    }

    override fun setDate(date: String) {
        tvDate.text = date
    }

    inner class BackgroundDrawable : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            val icon = BitmapFactory.decodeResource(getSelfResources(),
                    R.drawable.square)
            paint.shader = BitmapShader(icon, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }

        override fun draw(canvas: Canvas) {
            canvas.drawRect(0f, 0f, layoutContainer.measuredWidth.toFloat(), layoutContainer.measuredHeight.toFloat(), paint)
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

class TitleTextView(context: Context, attributeSet: AttributeSet) : TextView(context, attributeSet) {
    private var textHint: CharSequence? = null

    fun setText2(text: CharSequence?) {
        super.setText(text)
        if (textHint == null) {
            textHint = hint ?: ""
        }
        val newHint = if (TextUtils.isEmpty(text)) {
            textHint
        } else {
            ""
        }

        if (hint != newHint) {
            hint = newHint
            requestLayout()
        }
    }
}

class TitleLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = getChildAt(0).right
        val right = getChildAt(1).left
        canvas.drawLine(right.toFloat(), 0f, left.toFloat(), measuredHeight.toFloat(), paint)
    }
}