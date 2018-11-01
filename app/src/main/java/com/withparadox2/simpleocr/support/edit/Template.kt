package com.withparadox2.simpleocr.support.edit

import android.graphics.drawable.Drawable
import android.widget.RelativeLayout

class Template {
    var title: TextProperty? = null
    var author: TextProperty? = null
    var content: TextProperty? = null
    var date: TextProperty? = null
    var background: Drawable? = null
}

class TextProperty {
    var color: Int = 0
    // px
    var textSize: Int = 14
    var layout: RelativeLayout.LayoutParams? = null
}