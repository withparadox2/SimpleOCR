package com.withparadox2.simpleocr.ui.edit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.applyCanvas
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.edit.Editor
import com.withparadox2.simpleocr.support.store.AppDatabase
import com.withparadox2.simpleocr.support.store.BookInfo
import com.withparadox2.simpleocr.ui.BaseActivity
import com.withparadox2.simpleocr.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
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
    private var mBookInfo: BookInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        tvTitle = findViewById(R.id.tv_title)
        tvAuthor = findViewById(R.id.tv_author)
        etContent = findViewById(R.id.et_content)
        tvDate = findViewById(R.id.tv_date)

        btnEdit = findViewById(R.id.btn_edit_content)
        btnEdit.setOnClickListener(this)

        tvTitle.setOnClickListener(this)
        tvAuthor.setOnClickListener(this)

        val rawContent = intent.getStringExtra("content")
        etContent.setText(rawContent)

        tvDate.text = getDateStr()

        val list = getBookInfoList()
        if (list.isNotEmpty()) {
            setBookInfoView(list[0])
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
            GlobalScope.launchUI { share() }
        } else if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun share() {
        btnEdit.visibility = View.INVISIBLE
        etContent.isCursorVisible = false
        val filePath = getBasePath() + "share_${System.currentTimeMillis()}.png"

        if (doExport(filePath)) {
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(filePath))))

            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, buildUri(this, File(filePath), intent))
            startActivity(Intent.createChooser(intent, "Share"))
        } else {
            toast("Export png failed")
        }

        btnEdit.visibility = View.VISIBLE
        etContent.isCursorVisible = true
    }

    private suspend fun doExport(filePath: String): Boolean {
        return GlobalScope.async(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var outputStream: FileOutputStream? = null
            try {
                val view: View = findViewById(R.id.layout_container)
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                bitmap.applyCanvas {
                    clipPath(createRoundedPath(view.width.toFloat(), view.height.toFloat(), dp2px(8).toFloat()))
                    view.draw(this)
                }

                outputStream = FileOutputStream(filePath)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                closeQuietly(outputStream)
                bitmap?.recycle()
            }
            File(filePath).exists()
        }.await()
    }

    private fun createRoundedPath(width: Float, height: Float, radius: Float): Path {
        val path = Path()
        path.addRoundRect(0f, 0f, width, height, radius, radius, Path.Direction.CCW)
        return path
    }

    private fun showEditDialog() {
        val items: Array<String> = resources.getStringArray(R.array.items_edit_content)
        items[4] = "${items[4]}(${mContentEditor.getBackStepCount()})"
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
            R.id.tv_title, R.id.tv_author -> showBookInfoDialog()
        }
    }

    private fun showBookInfoDialog() {
        var list = getBookInfoList()
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_bookinfo_list, null)

        var updateListAction: Runnable? = null
        var dismissDialogAction: Runnable? = null

        val adapter = object : BaseAdapter() {
            @SuppressLint("SetTextI18n")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val cv = convertView
                        ?: LayoutInflater.from(this@EditActivity).inflate(R.layout.item_book_info, parent, false)
                (cv.findViewById(R.id.tv_title) as TextView).text = list[position].title
                (cv.findViewById(R.id.tv_author) as TextView).text = list[position].author
                cv.findViewById<View>(R.id.btn_edit).setOnClickListener {
                    showEditBookInfoDialog(list[position]) { bookInfo ->
                        AppDatabase.getInstance().bookInfoDao().update(bookInfo)
                        updateListAction?.run()
                    }
                }
                cv.findViewById<View>(R.id.btn_del).setOnClickListener {
                    val bookInfo = mBookInfo
                    if (bookInfo?.id == list[position].id) {
                        bookInfo?.id = null
                    }
                    AppDatabase.getInstance().bookInfoDao().delete(list[position])
                    updateListAction?.run()
                }
                return cv
            }

            override fun getItem(position: Int): BookInfo {
                return list[position]
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun getCount(): Int {
                return list.size
            }
        }

        updateListAction = Runnable {
            list = getBookInfoList()
            adapter.notifyDataSetChanged()
        }

        layout.findViewById<View>(R.id.btn_add).setOnClickListener {
            showEditBookInfoDialog(null) { bookInfo ->
                AppDatabase.getInstance().bookInfoDao().insert(bookInfo)
                updateListAction.run()
            }
        }
        val listView = layout.findViewById<ListView>(R.id.lv_bookinfo)
        listView.setOnItemClickListener { adapterView, view, i, l ->
            setBookInfoView(list[i])
            dismissDialogAction?.run()
        }
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this).setView(layout).show()

        dismissDialogAction = Runnable {
            if (dialog.isShowing) dialog.dismiss()
        }
    }

    private fun showEditBookInfoDialog(info: BookInfo?, callback: (BookInfo) -> Unit) {
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_edit_bookinfo, null)
        val etTitle = layout.findViewById<EditText>(R.id.et_title)
        val etAuthor = layout.findViewById<EditText>(R.id.et_author)
        if (info != null) {
            etTitle.setText(info.title)
            etAuthor.setText(info.author)
        }
        AlertDialog.Builder(this).setTitle("${if (info == null) "Add" else "Edit"} Book Info").setView(layout).setPositiveButton(R.string.dialog_confirm) { _, _ ->
            val title = etTitle.text.toString()
            val author = etAuthor.text.toString()
            if (info != null) {
                info.author = author
                info.title = title
                callback(info)
            } else {
                callback(BookInfo(null, title, author))
            }
        }.setNegativeButton(R.string.dialog_cancel) { _, _ -> }.show()
    }

    private fun setBookInfoView(info: BookInfo) {
        tvAuthor.text = info.author
        tvTitle.text = info.title
        mBookInfo = info
    }

    private fun getBookInfoList(): List<BookInfo> = AppDatabase.getInstance().bookInfoDao().getAll()
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