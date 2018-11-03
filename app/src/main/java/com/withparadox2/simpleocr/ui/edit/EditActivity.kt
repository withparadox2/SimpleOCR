package com.withparadox2.simpleocr.ui.edit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.TextView
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
    private lateinit var btnTitleHistory: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        tvTitle = findViewById(R.id.tv_title) as TextView
        tvAuthor = findViewById(R.id.tv_author) as TextView
        etContent = findViewById(R.id.et_content) as EditText
        tvDate = findViewById(R.id.tv_date) as TextView

        btnEdit = findViewById(R.id.btn_edit_content)
        btnEdit.setOnClickListener(this)
        btnTitleHistory = findViewById(R.id.btn_edit_title)
        btnTitleHistory.setOnClickListener(this)

        val rawContent = intent.getStringExtra("content")
        etContent.setText(rawContent)

        tvDate.text = getDateStr()

        val list = getBookInfoList()
        if (list.isNotEmpty()) {
            tvAuthor.text = list[0]?.second
            tvTitle.text = list[0]?.first
        } else {
            btnTitleHistory.visibility = View.INVISIBLE
        }

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
        addBookInfo(tvTitle.text.toString(), tvAuthor.text.toString())
    }

    private fun share() {
        btnEdit.visibility = View.INVISIBLE
        val titleHistoryVisibility = btnTitleHistory.visibility
        btnTitleHistory.visibility = View.INVISIBLE
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
        btnTitleHistory.visibility = titleHistoryVisibility
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
            canvas.clipPath(createRoundedPath(view.width.toFloat(), view.height.toFloat(), dp2px(8).toFloat()))
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

    private fun createRoundedPath(width: Float, height: Float, radius: Float): Path {
        val path = Path()
        path.addRoundRect(0f, 0f, width, height, radius, radius, Path.Direction.CCW)
        return path
    }

    private fun showEditDialog() {
        val items: Array<String> = resources.getStringArray(R.array.items_edit_content)
        AlertDialog.Builder(this).setItems(items) { _, which ->
            when (which) {
                0 -> mContentEditor.reset()
                1 -> mContentEditor.joinLines()
                2 -> mContentEditor.toChinese()
                3 -> mContentEditor.toEnglish()
                4 -> mContentEditor.lastStep()
                5 -> mContentEditor.copy()
            }
        }.show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_edit_content -> showEditDialog()
            R.id.btn_edit_title -> showBookInfoDialog()
        }
    }

    private fun showBookInfoDialog() {
        val list = getBookInfoList()
        AlertDialog.Builder(this).setAdapter(object : BaseAdapter() {
            @SuppressLint("SetTextI18n")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val cv = convertView
                        ?: LayoutInflater.from(this@EditActivity).inflate(R.layout.item_book_info, parent, false)
                val pair = list[position]
                (cv.findViewById(R.id.tv_title) as TextView).text = "Title : " + pair?.first
                (cv.findViewById(R.id.tv_author) as TextView).text = "Author: " + pair?.second
                cv.findViewById(R.id.btn_edit).setOnClickListener({

                })
                cv.findViewById(R.id.btn_del).setOnClickListener({
                    list.removeAt(position)
                    saveBookInfo(list)
                    notifyDataSetChanged()
                })
                return cv
            }

            override fun getItem(position: Int): Pair<String, String> {
                return list[position]!!
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun getCount(): Int {
                return list.size
            }
        }) { _, which ->
            tvTitle.text = list[which]?.first
            tvAuthor.text = list[which]?.second
        }.show()
    }

    private fun addBookInfo(title: String, author: String) {
        val list = getBookInfoList()
        if (!TextUtils.isEmpty(title) && !list.any { it?.first.equals(title) }) {
            list.add(0, Pair(title, author))
            saveBookInfo(list)
        }
    }

    private fun saveBookInfo(list: List<Pair<String, String>?>) {
        saveSpString("book_info_list", list.joinToString("+&") { it?.first + "-&" + it?.second })
    }

    private fun getBookInfoList(): MutableList<Pair<String, String>?> {
        return getSpString("book_info_list", "").split("+&").map {
            val info = it.split("-&")
            if (info.size == 2) {
                return@map Pair(info[0], info[1])
            } else {
                return@map null
            }
        }.filter {
            it != null
        }.toMutableList()
    }
}

//https://www.cbsd.org/cms/lib/PA01916442/Centricity/Domain/2295/time.pdf.pdf
fun getDateStr(): String {
    val format = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA)
    return format.format(Date()) + " " + when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 1..4 -> "凌晨"
        in 5..7 -> "清晨"
        in 8..10 -> "上午"
        in 11..13 -> "中午"
        in 14..16 -> "下午"
        in 17..18 -> "傍晚"
        in 19..21 -> "晚上"
        else -> "深夜"
    }
}

fun getIntent(context: Context, content: String): Intent {
    val intent = Intent(context, EditActivity::class.java)
    intent.putExtra("content", content)
    return intent
}