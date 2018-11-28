package com.withparadox2.simpleocr.baselib.template

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.*
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.withparadox2.simpleocr.baselib.R
import com.withparadox2.simpleocr.template.Callback
import com.withparadox2.simpleocr.template.ITemplate
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream


/**
 * Created by withparadox2 on 2018/11/17.
 */
abstract class BaseTemplateFragment : Fragment(), ITemplate {
    private var mAssetManager: AssetManager? = null
    private var mResources: Resources? = null

    lateinit var etContent: EditText
    lateinit var layoutContainer: View

    lateinit var rootView: View

    private var mIsStandalone = false

    var delegate: Callback? = null
    private var mScreenWidth = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mIsStandalone = arguments?.getString(APK_PATH)?.let { it.trim().isNotEmpty() } ?: false

        rootView = inflater.inflate(getSelfResources()?.getLayout(getLayoutResourceId()), container, false)
        etContent = rootView.findViewById(R.id.et_content)
        layoutContainer = rootView.findViewById(R.id.layout_container)
        onCreateViewInternal()
        delegate?.onViewCreated()
        return rootView
    }

    open fun onCreateViewInternal() {}

    abstract fun getLayoutResourceId(): Int

    fun getSelfAssetManager(): AssetManager? {
        if (mAssetManager == null) {
            val apkPath = arguments?.getString(APK_PATH)
            if (mIsStandalone) {
                mAssetManager = createAssetManager(apkPath!!)
            } else {
                mAssetManager = activity.assets
            }

        }
        return mAssetManager
    }

    fun getSelfResources(): Resources? {
        if (mResources == null) {
            if (mIsStandalone) {
                mResources = createResource(activity, getSelfAssetManager())
            } else {
                mResources = activity.resources
            }
        }
        return mResources
    }

    override fun setContent(content: String) {
        etContent.setText(content)
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
                val view: View = layoutContainer
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

    fun getScaleSize(size: Int): Int {
        if (this.mScreenWidth == 0) {
            this.mScreenWidth = activity.resources.displayMetrics.widthPixels
        }
        return (this.mScreenWidth * size) / 1080
    }
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