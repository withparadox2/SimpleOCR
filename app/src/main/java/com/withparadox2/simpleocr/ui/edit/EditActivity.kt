package com.withparadox2.simpleocr.ui.edit

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.edit.Editor
import com.withparadox2.simpleocr.ui.BaseActivity
import com.withparadox2.simpleocr.util.closeQuietly
import com.withparadox2.simpleocr.util.getBasePath
import com.withparadox2.simpleocr.util.toast
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var etContent: EditText
    private lateinit var tvDate: TextView
    private var mRawContent: String = ""
    private lateinit var mContentEditor: Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        tvTitle = findViewById(R.id.tv_title) as TextView
        tvAuthor = findViewById(R.id.tv_author) as TextView
        etContent = findViewById(R.id.et_content) as EditText
        tvDate = findViewById(R.id.tv_date) as TextView

        findViewById(R.id.btn_edit_content).setOnClickListener(this)

        mRawContent = intent.getStringExtra("content")
        etContent.setText(mRawContent)
        etContent.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mContentEditor.updateContent(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        tvDate.text = getDateStr()

        mContentEditor = Editor(mRawContent, object : Editor.Callback {
            override fun onContentChange(content: String) {
                etContent.setText(content)
            }
        })
//        actionBar.setHomeButtonEnabled(true)
    }

    private fun share() {
        App.postDelayed(Runnable {
            val view = findViewById(R.id.layout_container)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val outputStream = FileOutputStream(getBasePath() + "temp2.jpg")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            closeQuietly(outputStream)
            toast("finish")
        }, 1000)
    }

    private fun showEditDialog() {
        val items: Array<String> = resources.getStringArray(R.array.items_edit_content)
        AlertDialog.Builder(this).setItems(items) { dialog, which ->
            when (which) {
                0 -> mContentEditor.reset()
                1 -> mContentEditor.joinLines()
                2 -> mContentEditor.toChinese()
                3 -> mContentEditor.toEnglish()
                4 -> mContentEditor.lastStep()
            }
        }.show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_title, R.id.tv_author -> showSetBookInfoDialog()
            R.id.btn_edit_content -> showEditDialog()
        }
    }

    private fun showSetBookInfoDialog() {
    }
}

fun getDateStr(): String {
    val format = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
    return format.format(Date())
}

fun getIntent(context: Context, content: String): Intent {
    val intent = Intent(context, EditActivity::class.java)
    intent.putExtra("content", content)
    return intent
}