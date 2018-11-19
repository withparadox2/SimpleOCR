package com.withparadox2.simpleocr.ui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.baselib.template.Callback
import com.withparadox2.simpleocr.baselib.template.ITemplate
import com.withparadox2.simpleocr.baselib.template.loadFragmentFromApk
import com.withparadox2.simpleocr.support.edit.Editor
import com.withparadox2.simpleocr.support.store.AppDatabase
import com.withparadox2.simpleocr.support.store.BookInfo
import com.withparadox2.simpleocr.support.store.BookInfoDao
import com.withparadox2.simpleocr.ui.BaseActivity
import com.withparadox2.simpleocr.ui.getCameraIntent
import com.withparadox2.simpleocr.util.*
import kotlinx.coroutines.GlobalScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_MORE_TEXT = 1
private const val KEY_INTENT_CONTENT = "content"

@SuppressLint("SetTextI18n")
class EditActivity : BaseActivity(), View.OnClickListener {
    private val btnEdit: View by bind(R.id.btn_edit_content)
    private val btnMore: View by bind(R.id.btn_edit_more)

    private lateinit var mContentEditor: Editor
    private var mBookInfo: BookInfo? = null

    private lateinit var mFragment: ITemplate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        btnEdit.setOnClickListener(this)
        btnMore.setOnClickListener(this)

        val fragment = loadFragmentFromApk(this, getBasePath() + "templatedefault.apk", Bundle())
                ?: return
        supportFragmentManager.beginTransaction().add(R.id.layout_fragment, fragment, null).commit()

        mFragment = fragment as ITemplate

        val rawContent = intent.getStringExtra(KEY_INTENT_CONTENT)
        mFragment.setCallback(object : Callback {
            override fun onViewCreated() {
                mFragment.setContent(rawContent)
                mFragment.setDate(getDateStr())
                getLastBookInfo()?.also {
                    setBookInfoView(it)
                }

                mContentEditor = Editor(rawContent, object : Editor.Callback {
                    override fun onContentChange(content: String) {
                        mFragment.setContent(content)
                    }
                })

                mFragment.setContentTextWatcher(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        mContentEditor.updateContent(s.toString())
                    }

                    override fun afterTextChanged(s: Editable?) {
                    }
                })
            }

            override fun onSelectBookInfo() {
                showBookInfoDialog()
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
        setLastBookInfoId(mBookInfo?.id)
    }

    private fun share() {
        val filePath = getBasePath() + "share_${System.currentTimeMillis()}.png"

        if (mFragment.renderBitmapAndSave(filePath)) {
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(filePath))))
            startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).also {
                        it.type = "image/*"
                        it.putExtra(Intent.EXTRA_STREAM, buildUri(this, File(filePath), it))
                    }, "Share"))
        } else {
            toast("Export png failed")
        }
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
            R.id.btn_edit_more -> {
                startActivityForResult(getCameraIntent(this), REQUEST_MORE_TEXT)
            }
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
        info?.also {
            etTitle.setText(it.title)
            etAuthor.setText(it.author)
        }
        AlertDialog.Builder(this).setTitle("${if (info == null) "Add" else "Edit"} Book Info").setView(layout).setPositiveButton(R.string.dialog_confirm) { _, _ ->
            val title = etTitle.text.toString()
            val author = etAuthor.text.toString()
            callback(info?.also {
                it.author = author
                it.title = title
            } ?: BookInfo(null, title, author))
        }.setNegativeButton(R.string.dialog_cancel) { _, _ -> }.show()
    }

    private fun setBookInfoView(info: BookInfo) {
        mFragment.setAuthor(info.author)
        mFragment.setTitle(info.title)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MORE_TEXT && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("data") ?: ""
            showInsertDialog(text)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showInsertDialog(text: String) {
        AlertDialog.Builder(this).setTitle("Insert where?").setItems(arrayOf("Begin", "Cursor", "End")) { _, i ->
            when (i) {
                0 -> mFragment.setContent(text + mFragment.getEditTextContent())
                1 -> {
                    val selection = mFragment.getEditSelection()
                    mFragment.setContent(mFragment.getEditTextContent().toString().let {
                        it.subSequence(0, selection).toString() + text + it.subSequence(selection, it.length - 1)
                    })
                    mFragment.setEditSelection(selection)
                }
                2 -> mFragment.setContent(mFragment.getEditTextContent().toString() + text)
            }
        }.show()
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

fun getEditIntent(context: Context, content: String): Intent {
    return Intent(context, EditActivity::class.java).apply { putExtra(KEY_INTENT_CONTENT, content) }
}