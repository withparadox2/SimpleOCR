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
import androidx.core.content.edit
import androidx.core.graphics.applyCanvas
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.edit.Editor
import com.withparadox2.simpleocr.support.store.AppDatabase
import com.withparadox2.simpleocr.support.store.BookInfo
import com.withparadox2.simpleocr.support.store.BookInfoDao
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
    private val tvTitle: TextView by bind(R.id.tv_title)
    private val tvAuthor: TextView by bind(R.id.tv_author)
    private val etContent: EditText by bind(R.id.et_content)
    private val tvDate: TextView by bind(R.id.tv_date)
    private val btnEdit: View by bind(R.id.btn_edit_content)

    private lateinit var mContentEditor: Editor
    private var mBookInfo: BookInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        btnEdit.setOnClickListener(this)

        tvTitle.setOnClickListener(this)
        tvAuthor.setOnClickListener(this)

        val rawContent = intent.getStringExtra("content")
        etContent.setText(rawContent)

        tvDate.text = getDateStr()

        getLastBookInfo()?.also {
            setBookInfoView(it)
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

    override fun onStop() {
        super.onStop()
        setLastBookInfoId(mBookInfo?.id)
    }

    private suspend fun share() {
        btnEdit.visibility = View.INVISIBLE
        etContent.isCursorVisible = false
        val filePath = getBasePath() + "share_${System.currentTimeMillis()}.png"

        if (doExport(filePath)) {
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(filePath))))
            startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).also {
                        it.type = "image/*"
                        it.putExtra(Intent.EXTRA_STREAM, buildUri(this, File(filePath), it))
                    }, "Share"))
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

        var updateListAction: (() -> Unit)? = null
        var dismissDialogAction: (() -> Unit)? = null

        val adapter = object : BaseAdapter() {
            @SuppressLint("SetTextI18n")
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val cv = convertView ?: inflate(R.layout.item_book_info, parent)
                (cv.findViewById(R.id.tv_title) as TextView).text = list[position].title
                (cv.findViewById(R.id.tv_author) as TextView).text = list[position].author
                cv.findViewById<View>(R.id.btn_edit).setOnClickListener {
                    showEditBookInfoDialog(list[position]) { bookInfo ->
                        getBookInfoDao().update(bookInfo)
                        updateListAction?.invoke()
                    }
                }
                cv.findViewById<View>(R.id.btn_del).setOnClickListener {
                    val bookInfo = mBookInfo
                    bookInfo?.takeIf { it.id == list[position].id }?.also { it.id = null }
                    getBookInfoDao().delete(list[position])
                    updateListAction?.invoke()
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

        updateListAction = {
            list = getBookInfoList()
            adapter.notifyDataSetChanged()
        }

        layout.findViewById<View>(R.id.btn_add).setOnClickListener {
            showEditBookInfoDialog(null) { bookInfo ->
                getBookInfoDao().insert(bookInfo)
                updateListAction.invoke()
            }
        }
        val listView = layout.findViewById<ListView>(R.id.lv_bookinfo)
        listView.setOnItemClickListener { adapterView, view, i, l ->
            setBookInfoView(list[i])
            dismissDialogAction?.invoke()
        }
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this).setTitle("Manage Book").setView(layout).show()

        dismissDialogAction = {
            if (dialog.isShowing) dialog.dismiss()
        }
    }

    private fun showEditBookInfoDialog(info: BookInfo?, callback: (BookInfo) -> Unit) {
        val layout: View = inflate(R.layout.layout_edit_bookinfo)
        val etTitle = layout.findViewById<EditText>(R.id.et_title)
        val etAuthor = layout.findViewById<EditText>(R.id.et_author)
        info?.apply {
            etTitle.setText(this.title)
            etAuthor.setText(this.author)
        }
        AlertDialog.Builder(this).setTitle("${if (info == null) "Add" else "Edit"} Book Info").setView(layout).setPositiveButton(R.string.dialog_confirm) { _, _ ->
            val title = etTitle.text.toString()
            val author = etAuthor.text.toString()
            callback(info?.also {
                info.author = author
                info.title = title
            } ?: BookInfo(null, title, author))
        }.setNegativeButton(R.string.dialog_cancel) { _, _ -> }.show()
    }

    private fun setBookInfoView(info: BookInfo) {
        tvAuthor.text = info.author
        tvTitle.text = info.title
        mBookInfo = info
    }

    private fun getBookInfoDao(): BookInfoDao = AppDatabase.getInstance().bookInfoDao()
    private fun getBookInfoList(): List<BookInfo> = getBookInfoDao().getAll()
    private fun getLastBookInfo(): BookInfo? {
        return getBookInfoDao().getBookInfoById(getLastBookInfoId())
                ?: getBookInfoList().elementAtOrNull(0)
    }

    private fun getLastBookInfoId(): Long {
        return getSp().getLong("last_book_id", 0)
    }

    private fun setLastBookInfoId(id: Long?) {
        getSp().edit { putLong("last_book_id", id ?: 0) }
    }
}

//https://www.cbsd.org/cms/lib/PA01916442/Centricity/Domain/2295/time.pdf.pdf
private fun getDateStr(): String {
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
    return Intent(context, EditActivity::class.java).apply { this.putExtra("content", content) }
}