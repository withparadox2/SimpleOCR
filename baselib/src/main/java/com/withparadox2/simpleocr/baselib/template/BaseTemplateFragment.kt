package com.withparadox2.simpleocr.baselib.template

import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment


/**
 * Created by withparadox2 on 2018/11/17.
 */
abstract class BaseTemplateFragment : Fragment(), ITemplate {
    private var mAssetManager: AssetManager? = null
    private var mResources: Resources? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(geResources()?.getLayout(getLayoutResourceId()), container, false)
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
}

interface ITemplate {

}