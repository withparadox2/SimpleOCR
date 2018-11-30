package com.withparadox2.simpleocr.template

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText

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
}