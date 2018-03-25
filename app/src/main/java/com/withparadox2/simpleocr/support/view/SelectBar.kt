package com.withparadox2.simpleocr.support.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import com.withparadox2.simpleocr.util.dp2px
import java.util.*

/**
 * Created by withparadox2 on 2018/3/24.
 */
class SelectBar : View {
    private lateinit var tvContent: TextView

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    private var paint: Paint = Paint()
    private val rectRadius = dp2px(2).toFloat()

    private var fromLine = -1
    private var toLine = -1
    private val sections: ArrayList<Section> = ArrayList()

    private var preScrollY: Int = 0

    init {
        paint.color = Color.RED
    }

    fun setTextView(tvContent: TextView) {
        this.tvContent = tvContent
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val tvLocal = tvContent
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(tvLocal.measuredHeight, MeasureSpec.EXACTLY))
        setupSections()
    }


    private fun setupSections() {
        val layout: Layout = tvContent.layout ?: return
        if (layout.lineCount == 0) return

        if (sections.isEmpty() && tvContent.text?.isNotEmpty() == true) {
            var preNewLine = true
            for (i in 0 until layout.lineCount) {
                if (preNewLine) {
                    sections.add(Section(i, i))
                } else {
                    // must not be null
                    sections.last().to = i
                }

                preNewLine = isNewLine(i)
            }

            sections.forEach { it.fixPosition() }

            restoreScrollPosition()
        }
    }

    private fun restoreScrollPosition() {
        post {
            (parent.parent as ScrollView).smoothScrollTo(0, preScrollY)
        }

    }

    private fun clearSections() = sections.clear()

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val right = width - paddingRight.toFloat()

        val from = validFromLine()
        val to = validToLine()
        sections.forEach {
            if (it.to < from || it.from > to) {
                paint.color = Color.MAGENTA
            } else {
                paint.color = Color.BLUE
            }
            canvas.drawRoundRect(left, it.top + rectRadius, right, it.bottom - rectRadius, rectRadius, rectRadius, paint)
        }
    }

    private fun validFromLine(): Int = if (fromLine < toLine) fromLine else toLine
    private fun validToLine(): Int = if (fromLine < toLine) toLine else fromLine

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val y: Float = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                fromLine = getLineIndex(y)
                toLine = fromLine
            }
            MotionEvent.ACTION_MOVE -> {
                toLine = getLineIndex(y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (fromLine != -1 && toLine != -1) {
                    joinLines(findSection(validFromLine()), findSection(validToLine()))
                } else {
                    resetLines()
                    invalidate()
                }
            }
        }

        if (fromLine != -1) {
            parent.requestDisallowInterceptTouchEvent(true)
            invalidate()
        }
        return true
    }

    private fun isNewLine(index: Int): Boolean {
        val end = tvContent.layout.getLineEnd(index)
        return if (end > 0) {
            tvContent.text[end - 1] == '\n'
        } else {
            false
        }
    }

    private fun joinLines(from: Int, to: Int) {
        if (from == to || from == -1 || to == -1) {
            resetLines()
            invalidate()
            return
        }
        val sb = StringBuffer(tvContent.text)
        (to - 1 downTo from)
                .map { sections[it].to }
                .map { tvContent.layout.getLineEnd(it) }
                .forEach {
                    if (sb[it - 1] == '\n') {
                        sb.replace(it - 1, it, "")
                    }
                }
        tvContent.text = sb
        preScrollY = (parent.parent as ScrollView).scrollY
        requestLayout()
        resetLines()
        clearSections()
    }

    private fun resetLines() {
        fromLine = -1
        toLine = -1
    }

    private fun getLineIndex(y: Float): Int {
        if (tvContent.layout == null) {
            return -1
        }

        val layout: Layout = tvContent.layout
        for (i in 0 until layout.lineCount) {
            val top = layout.getLineTop(i) + tvContent.paddingTop
            val bottom = layout.getLineBottom(i) + tvContent.paddingTop
            if (top < y && y < bottom) {
                return i
            }
        }
        return -1
    }

    private fun findSection(index: Int): Int {
        for (i in 0 until sections.size) {
            val sec: Section = sections[i]
            if (index in sec.from..sec.to) {
                return i
            }
        }
        return -1
    }

    inner class Section(var from: Int, var to: Int) {
        var top: Float = 0f
        var bottom: Float = 0f

        fun fixPosition() {
            val layout: Layout = tvContent.layout
            top = layout.getLineTop(from) + tvContent.paddingTop.toFloat()
            bottom = layout.getLineBottom(to) + tvContent.paddingTop.toFloat()
        }
    }
}

