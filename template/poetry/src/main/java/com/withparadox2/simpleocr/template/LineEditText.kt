package com.withparadox2.simpleocr.template

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import android.widget.Editor
import android.widget.TextView
import com.withparadox2.simpleocr.baselib.template.dp2px
import com.withparadox2.simpleocr.templatedefault.R
import java.lang.reflect.AccessibleObject.setAccessible


class LineEditText(context: Context, attributeSet: AttributeSet) : EditText(context, attributeSet) {
    private var textWatcher: TextWatcher? = null
    private var spaceAdd = 0f
    private var spaceMult = 1f

    init {
        super.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textWatcher?.afterTextChanged(s)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                textWatcher?.beforeTextChanged(s, start, count, after)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                super@LineEditText.setLineSpacing(0f, 1f)
                super@LineEditText.setLineSpacing(spaceAdd, spaceMult)
                textWatcher?.onTextChanged(s, start, before, count)
            }
        })
    }

    override fun setLineSpacing(add: Float, mult: Float) {
        spaceAdd = add
        spaceMult = mult
        super.setLineSpacing(add, mult)
    }

    override fun addTextChangedListener(watcher: TextWatcher?) {
        textWatcher = watcher
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (layout != null && lineCount > 0) {
            setMeasuredDimension(measuredWidth, (lineCount * lineHeight - (spaceMult - 1) / 2 * textSize).toInt())
        }
    }

    fun setCursorDrawable(color: Int, textSize: Int) {
        try {
            val method = TextView::class.java.getDeclaredMethod("createEditorIfNeeded")
            method.isAccessible = true
            method.invoke(this)
            val field1 = TextView::class.java.getDeclaredField("mEditor")
            val field2 = Editor::class.java.getDeclaredField("mCursorDrawable")
            field1.isAccessible = true
            field2.isAccessible = true
            val arr = field2.get(field1.get(this))

            @Suppress("UNCHECKED_CAST")
            (arr as Array<Drawable>)[0] = CursorDrawable(color, dp2px(1, context), textSize)
            arr[1] = CursorDrawable(color, dp2px(1, context), textSize)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    class CursorDrawable(color: Int, width: Int, private var height: Int) : ShapeDrawable() {
        init {
            super.getPaint().color = color
            intrinsicWidth = width
        }

        override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
            super.setBounds(left, top, right, top + height)
        }
    }
}