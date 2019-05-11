package com.withparadox2.simpleocr.ui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.withparadox2.simpleocr.App
import com.withparadox2.simpleocr.R
import com.withparadox2.simpleocr.support.edit.Editor
import com.withparadox2.simpleocr.support.pref.getLastBookInfoId
import com.withparadox2.simpleocr.support.pref.getLastTemplateKey
import com.withparadox2.simpleocr.support.pref.setLastBookInfoId
import com.withparadox2.simpleocr.support.pref.setLastTemplateKey
import com.withparadox2.simpleocr.support.store.AppDatabase
import com.withparadox2.simpleocr.support.store.BookInfo
import com.withparadox2.simpleocr.support.store.BookInfoDao
import com.withparadox2.simpleocr.support.template.*
import com.withparadox2.simpleocr.support.view.TemplateLayout
import com.withparadox2.simpleocr.template.Callback
import com.withparadox2.simpleocr.template.ITemplate
import com.withparadox2.simpleocr.ui.BaseActivity
import com.withparadox2.simpleocr.ui.getCameraIntent
import com.withparadox2.simpleocr.util.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_MORE_TEXT = 1
private const val KEY_INTENT_CONTENT = "content"

private const val INSERT_START = 0
private const val INSERT_CURSOR = 1
private const val INSERT_END = 2


@SuppressLint("SetTextI18n")
class EditActivity : BaseActivity(), View.OnClickListener {
    private val btnEdit: View by bind(R.id.btn_edit_content)
    private val btnMore: View by bind(R.id.btn_edit_more)
    private val layoutTemplateWrapper: ViewGroup by bind(R.id.layout_template_wrapper)

    private lateinit var mContentEditor: Editor
    private var mBookInfo: BookInfo? = null

    private var mFragment: ITemplate? = null
    private var mActiveTemplate: Template? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rawContent = intent.getStringExtra(KEY_INTENT_CONTENT) ?: ""
        mContentEditor = Editor(rawContent, object : Editor.Callback {
            override fun onContentChange(content: String) {
                mFragment?.setContent(content)
            }
        })

        setContentView(R.layout.activity_edit)
        btnEdit.setOnClickListener(this)
        btnMore.setOnClickListener(this)
        findViewById<View>(R.id.btn_edit_template).setOnClickListener(this)
    }

    override fun onGetPermission() {
        super.onGetPermission()
        loadLocalFragment()?.apply {
            configFragment(this)
            setupTemplate()
        } ?: fromBundleTemplate()
    }

    private fun fromBundleTemplate() {
        launchUI {
            if (getTemplateList().isEmpty()) {
                preloadTemplates()
            }
            val template = getTargetTemplate(getLastTemplateKey()) ?: getDefaultTemplate()
            template?.apply { selectTemplate(this) }
            setupTemplate()
        }
    }

    private fun selectTemplate(template: Template?): Boolean {
        return template?.newFragment()?.run {
            mActiveTemplate = template
            configFragment(this)
            return true
        } ?: false
    }

    private fun configFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.layout_fragment, fragment, null).commit()
        mFragment = fragment as ITemplate

        val localFragment = fragment as ITemplate
        localFragment.setCallback(object : Callback {
            override fun onViewCreated() {
                localFragment.setContent(mContentEditor.getLastChangeContent())
                localFragment.setDate(getDateStr())

                mBookInfo?.apply { setBookInfoView(this) } ?: getLastBookInfo()?.apply {
                    setBookInfoView(this)
                }

                localFragment.setContentTextWatcher(object : TextWatcher {
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
    }

    private fun loadLocalFragment(): Fragment? {
        try {
            return Class.forName(FRAGMENT_NAME).newInstance() as Fragment
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.share) {
            share()
        } else if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        setLastBookInfoId(mBookInfo?.id)
    }

    private fun share() {
        if (mFragment == null) {
            toast("nothing to share")
            return
        }
        val filePath = getBasePath() + "share_${System.currentTimeMillis()}.png"

        if (mFragment?.renderBitmapAndSave(filePath) == true) {
            AlertDialog.Builder(this).setItems(R.array.items_share) { _, which ->
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(filePath))))
                if (which == 0) {
                    toast("Picture is saved in : $filePath")
                } else if (which == 1) {
                    startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).also {
                                it.type = "image/*"
                                it.putExtra(Intent.EXTRA_STREAM, buildUri(this, File(filePath), it))
                            }, "Share"))
                }
            }.show()
        } else {
            toast("Export png failed")
        }
    }

    private fun showEditDialog() {
        val items: Array<String> = resources.getStringArray(R.array.items_edit_content)
        items[6] = "${items[6]}(${mContentEditor.getBackStepCount()})"
        AlertDialog.Builder(this).setItems(items) { _, which ->
            when (which) {
                0 -> mContentEditor.reset()
                1 -> mContentEditor.joinLines()
                2 -> mContentEditor.toChinese()
                3 -> {
                    mContentEditor.toChinese()
                    mContentEditor.joinLines()
                }
                4 -> mContentEditor.toEnglish()
                5 -> {
                    mContentEditor.toEnglish()
                    mContentEditor.joinLines()
                }
                6 -> mContentEditor.lastStep()
                7 -> mContentEditor.copy()
            }
        }.show()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_edit_content -> showEditDialog()
            R.id.btn_edit_more -> {
                startActivityForResult(getCameraIntent(this), REQUEST_MORE_TEXT)
            }
            R.id.btn_edit_template -> {
                animateTemplate()
            }
        }
    }

    private fun animateTemplate() {
        (layoutTemplateWrapper.parent.parent as TemplateLayout).toggleAnimation()
    }

    private fun setupTemplate() {
        val array = getTemplateList()

        val layout = layoutTemplateWrapper
        if (layout.childCount == 0) {
            if (array.isEmpty()) {
                layout.layoutParams = layout.layoutParams.also { it.width = MATCH_PARENT }
                layout.addView(TextView(this).apply {
                    text = getString(R.string.template_empty)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                })
            } else {
                var preCheckedView: TextView? = null
                val configView = { view: TextView, isActive: Boolean ->
                    view.setTextColor(resources.getColor(if (isActive) R.color.white else R.color.colorAccent))
                    view.setBackgroundResource(if (isActive) R.drawable.bg_btn_edit_template_item_text_active else R.drawable.bg_btn_edit_template_item_text)
                    (view.parent as View).setBackgroundResource(if (isActive) R.drawable.bg_edit_template_item_active else R.drawable.bg_edit_template_item)
                }

                array.forEach {
                    val view = LayoutInflater.from(this).inflate(R.layout.item_edit_template, layout, false)
                    view.findViewById<TextView>(R.id.tv_template_name).apply {
                        if (it == mActiveTemplate) {
                            preCheckedView = this
                            configView(preCheckedView!!, true)
                        }

                        text = it.name
                        setBackgroundResource(R.drawable.bg_btn_edit_template_item_text)
                        setOnClickListener { tv ->
                            if (selectTemplate(it)) {
                                setLastTemplateKey(it.key)
                                preCheckedView?.apply { configView(this, false) }
                                configView(tv as TextView, true)
                                preCheckedView = tv
                            }
                        }
                    }
                    layout.addView(view)
                }
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
        val listView = layout.findViewById(R.id.lv_bookinfo) as ListView
        listView.setOnItemClickListener { _, _, i, _ ->
            setBookInfoView(list[i])
            dismissDialogAction?.invoke()
        }
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this).setTitle(R.string.manage_book).setView(layout).show()

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
        val dialog = AlertDialog.Builder(this).setTitle(if (info == null) R.string.add_book else R.string.edit_book).setView(layout).create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cancel)) { _, _ ->
            hideKeyboard(this, dialog.window?.currentFocus)
        }
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dialog_confirm)) { _, _ ->
            val title = etTitle.text.toString()
            val author = etAuthor.text.toString()
            callback(info?.also {
                it.author = author
                it.title = title
            } ?: BookInfo(null, title, author))
            hideKeyboard(this, dialog.window?.currentFocus)
        }
        dialog.show()
    }

    private fun setBookInfoView(info: BookInfo) {
        mFragment?.setAuthor(info.author)
        mFragment?.setTitle(info.title)
        mBookInfo = info
    }

    private fun getBookInfoDao(): BookInfoDao = AppDatabase.getInstance().bookInfoDao()
    private fun getBookInfoList(): List<BookInfo> = getBookInfoDao().getAll()
    private fun getLastBookInfo(): BookInfo? {
        return getBookInfoDao().getBookInfoById(getLastBookInfoId())
                ?: getBookInfoList().elementAtOrNull(0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MORE_TEXT && resultCode == Activity.RESULT_OK) {
            val text = data?.getStringExtra("data") ?: ""
            mFragment?.apply {
                if (editTextContent.isEmpty()) {
                    setContent(text)
                    editSelection = text.length
                } else {
                    showInsertDialog(text)
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun showInsertDialog(text: String) {
        val fragment = mFragment ?: return
        AlertDialog.Builder(this).setTitle(R.string.text_insert_where).setItems(R.array.items_text_insert_place) { _, i ->
            var resultIndex = i
            if (fragment.editTextContent.isEmpty() && resultIndex == 1) {
                resultIndex = 0
            }
            when (resultIndex) {
                INSERT_START -> fragment.setContent(text + fragment.editTextContent)
                INSERT_CURSOR -> {
                    val selection = fragment.editSelection
                    fragment.setContent(fragment.editTextContent.toString().let {
                        it.subSequence(0, selection).toString() + text + it.subSequence(selection, it.length - 1)
                    })
                    fragment.editSelection = selection
                }
                INSERT_END -> fragment.setContent(fragment.editTextContent.toString() + text)
            }
        }.show()
    }

    override fun onBackPressed() {
        if (mFragment?.editTextContent?.isNotEmpty() == true) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_exit_edit)
                    .setMessage(R.string.dialog_message_exit_edit)
                    .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                        finish()
                    }.setNegativeButton(R.string.dialog_cancel) { _, _ ->
                    }.show()
            return
        }
        super.onBackPressed()
    }
}

//https://www.cbsd.org/cms/lib/PA01916442/Centricity/Domain/2295/time.pdf.pdf
private fun getDateStr(): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    val index = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 1..4 -> 0 //"凌晨"
        in 5..7 -> 1 //"清晨"
        in 8..10 -> 2 //"上午"
        in 11..13 -> 3 //"中午"
        in 14..16 -> 4 //"下午"
        in 17..18 -> 5 //"傍晚"
        in 19..21 -> 6 //"晚上"
        else -> 7 //"深夜"
    }
    val sections = App.instance.resources.getStringArray(R.array.items_time_section)
    return format.format(Date()) + " " + sections[index]
}

fun getEditIntent(context: Context, content: String): Intent {
    return Intent(context, EditActivity::class.java).apply { putExtra(KEY_INTENT_CONTENT, content) }
}