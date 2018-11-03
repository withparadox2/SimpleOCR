package com.withparadox2.simpleocr.ui.edit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.edit.Editor
import com.withparadox2.simpleocr.ui.BaseActivity
import com.withparadox2.simpleocr.util.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvTitle: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var etContent: EditText
    private lateinit var tvDate: TextView
    private lateinit var mContentEditor: Editor
    private lateinit var btnEdit: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        tvTitle = findViewById(R.id.tv_title) as TextView
        tvAuthor = findViewById(R.id.tv_author) as TextView
        etContent = findViewById(R.id.et_content) as EditText
        tvDate = findViewById(R.id.tv_date) as TextView

        btnEdit = findViewById(R.id.btn_edit_content)
        btnEdit.setOnClickListener(this)

        val rawContent = intent.getStringExtra("content")
        etContent.setText(intent.getStringExtra("content"))

        tvDate.text = getDateStr()
        tvAuthor.text = getLastAuthor()
        tvTitle.text = getLastBookName()

        mContentEditor = Editor(rawContent, object : Editor.Callback {
            override fun onContentChange(content: String) {
                etContent.setText(content)
            }
        })
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mContentEditor.updateContent(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.share) {
            share()
        } else if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        saveAuthor(tvAuthor.text.toString())
        saveBookName(tvTitle.text.toString())
    }

    private fun share() {
        btnEdit.visibility = View.INVISIBLE
        tvTitle.isCursorVisible = false
        tvAuthor.isCursorVisible = false
        etContent.isCursorVisible = false
        val filePath = getBasePath() + "share_${System.currentTimeMillis()}.png"
        if (doExport(filePath)) {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, buildUri(this, File(filePath), intent))
            startActivity(Intent.createChooser(intent, "Share"))
        } else {
            toast("Export png failed")
        }
        btnEdit.visibility = View.VISIBLE
        tvTitle.isCursorVisible = true
        tvAuthor.isCursorVisible = true
        etContent.isCursorVisible = true
    }

    private fun doExport(filePath: String): Boolean {
        var bitmap: Bitmap? = null
        var outputStream: FileOutputStream? = null
        try {
            val view = findViewById(R.id.layout_container)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            outputStream = FileOutputStream(filePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            closeQuietly(outputStream)
            bitmap?.recycle()
        }
        return File(filePath).exists()
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


    private fun saveBookName(name: String) {
        saveSpString("last_book_name", name)
    }

    private fun saveAuthor(name: String) {
        saveSpString("last_author", name)
    }

    private fun getLastBookName(): String {
        return getSpString("last_book_name", "")
    }

    private fun getLastAuthor(): String {
        return getSpString("last_author", "")
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