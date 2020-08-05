package com.withparadox2.simpleocr.support.net

import com.google.gson.annotations.SerializedName

/**
 * Created by withparadox2 on 2018/3/22.
 */
class TokenResult {
  @SerializedName("access_token")
  lateinit var accessToken: String

  @SerializedName("expires_in")
  var expiresIn: Long = 0
}
