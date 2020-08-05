package com.withparadox2.simpleocr.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.withparadox2.simpleocr.R

@SuppressLint("ValidFragment")
/**
 * Created by withparadox2 on 2018/4/29.
 */
class PermissionDialog constructor(private val confirmListener: DialogInterface.OnClickListener?) : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(activity)
        .setTitle(R.string.tip_title)
        .setMessage(R.string.permission_request_msg)
        .setPositiveButton("确定", confirmListener)
        .create()
  }
}