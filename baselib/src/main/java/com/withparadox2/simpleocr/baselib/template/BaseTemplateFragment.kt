package com.withparadox2.simpleocr.baselib.template

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.*
import android.os.Bundle
import android.os.TokenWatcher
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.withparadox2.simpleocr.baselib.R
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream


/**
 * Created by withparadox2 on 2018/11/17.
 */
abstract class BaseTemplateFragment : Fragment(), ITemplate {
    private var mAssetManager: AssetManager? = null
    private var mResources: Resources? = null

    lateinit var tvTitle: TextView
    lateinit var tvAuthor: TextView
    lateinit var etContent: EditText
    lateinit var tvDate: TextView

    lateinit var rootView: View

    private var delegate: Callback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(geResources()?.getLayout(getLayoutResourceId()), container, false)
        tvTitle = rootView.findViewById(R.id.tv_title)
        tvAuthor = rootView.findViewById(R.id.tv_author)
        etContent = rootView.findViewById(R.id.et_content)
        tvDate = rootView.findViewById(R.id.tv_date)

        val action = View.OnClickListener {
            delegate?.onSelectBookInfo()
        }

        tvTitle.setOnClickListener(action)
        tvAuthor.setOnClickListener(action)
        delegate?.onViewCreated()
        return rootView
    }

    abstract fun getLayoutResourceId(): Int

    fun getAssetManager(): AssetManager? {
        if (mAssetManager == null) {
            val apkPath = arguments?.getString(APK_PATH)
            if (apkPath != null && apkPath.isNotEmpty()) {
                mAssetManager = createAssetManager(apkPath)
            }

        }
        return mAssetManager
    }


    fun geResources(): Resources? {
        if (mResources == null) {
            mResources = createResource(activity, getAssetManager())
        }
        return mResources
    }

    override fun setTitle(title: String) {
        tvTitle.text = title
    }

    override fun setAuthor(author: String) {
        tvAuthor.text = author
    }

    override fun setContent(content: String) {
        etContent.setText(content)
    }

    override fun setDate(date: String) {
        tvDate.text = date
    }

    abstract fun onBeforeRender()
    abstract fun onAfterRender()

    override fun renderBitmapAndSave(filePath: String): Boolean {
        onBeforeRender()

        var outOfMemoryErrorTimes = 0
        val renderAndOutput = { antiAlias: Boolean ->
            var bitmap: Bitmap? = null
            var bitmap2: Bitmap? = null
            var outputStream: FileOutputStream? = null
            val radius = dp2px(8, activity!!).toFloat()
            try {
                val view: View = rootView.findViewById(R.id.layout_container)
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                var canvas = Canvas(bitmap!!)
                if (!antiAlias) {
                    canvas.clipPath(createRoundedPath(view.width.toFloat(), view.height.toFloat(), radius))
                }
                view.draw(canvas)

                if (antiAlias) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
                        it.shader = BitmapShader(bitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    }
                    bitmap2 = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    canvas = Canvas(bitmap2!!)
                    canvas.drawRoundRect(RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()), radius, radius, paint)
                }

                outputStream = FileOutputStream(filePath)
                (if (antiAlias) bitmap2 else bitmap)?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: OutOfMemoryError) {
                outOfMemoryErrorTimes++
            } finally {
                closeQuietly(outputStream)
                bitmap?.recycle()
            }
        }
        renderAndOutput(true)
        if (outOfMemoryErrorTimes == 1) {
            System.gc()
            renderAndOutput(false)
        }

        onAfterRender()
        return File(filePath).exists()
    }

    private fun createRoundedPath(width: Float, height: Float, radius: Float): Path {
        return Path().apply { addRoundRect(RectF(0f, 0f, width, height), radius, radius, Path.Direction.CCW) }
    }

    override fun getEditSelection(): Int {
        return etContent.selectionStart
    }

    override fun getEditTextContent(): CharSequence {
        return etContent.text
    }

    override fun setCallback(callback: Callback) {
        this.delegate = callback
    }

    override fun setEditSelection(selection: Int) {
        this.etContent.setSelection(selection)
    }

    override fun setContentTextWatcher(watcher: TextWatcher) {
        this.etContent.addTextChangedListener(watcher)
    }
}

interface ITemplate {
    fun setTitle(title: String)
    fun setAuthor(author: String)
    fun setContent(content: String)
    fun setDate(date: String)
    fun renderBitmapAndSave(filePath: String): Boolean
    fun getEditSelection(): Int
    fun getEditTextContent(): CharSequence
    fun setEditSelection(selection: Int)
    fun setContentTextWatcher(watcher: TextWatcher)
    fun setCallback(callback: Callback)
}

interface Callback {
    fun onSelectBookInfo()
    fun onViewCreated()
}

fun closeQuietly(close: Closeable?) {
    try {
        close?.close()
    } catch (e: Exception) {
    }
}

fun dp2px(dip: Int, context: Context): Int {
    val scale = context.resources.displayMetrics.density
    return (dip * scale + 0.5f).toInt()
}

fun dp2px(dip: Float, context: Context): Float {
    val scale = context.resources.displayMetrics.density
    return (dip * scale + 0.5f)
}