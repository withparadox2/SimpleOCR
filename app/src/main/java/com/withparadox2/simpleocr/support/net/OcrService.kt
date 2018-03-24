package com.withparadox2.simpleocr.support.net

import android.content.Context
import com.withparadox2.simpleocr.App
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

/**
 * Created by withparadox2 on 2018/3/22.
 */
const val OCR_ID = "mDcZYbQzIZYrsHh4cGXric4c"
const val OCR_SECRET = "dgIt03inKtFrhCA6tyMjaXSQ4eLfq1Cc"

const val KEY_TOEKN = "access_token"

interface OcrService {
    @GET("oauth/2.0/token")
    fun getToken(@Query("client_id") clientId: String = OCR_ID,
                 @Query("client_secret") clientSecret: String = OCR_SECRET,
                 @Query("grant_type") grantType: String = "client_credentials"): Call<TokenResult>

    @FormUrlEncoded
    @POST("rest/2.0/ocr/v1/general_basic")
    fun sendOcr(@Query("access_token") accessToken: String, @Field("image") image: String): Call<OcrResult>

    companion object {
        var instance: OcrService = Retrofit.Builder().baseUrl("https://aip.baidubce.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OcrService::class.java)

        fun requestOcr(image: String, call: Callback<OcrResult>) {
            val sp = App.instance.getSharedPreferences(App.instance.packageName, Context.MODE_PRIVATE)
            val token = sp.getString(KEY_TOEKN, null)

            fun execution() = instance.sendOcr(sp.getString(KEY_TOEKN, null), image).enqueue(call)

            if (token == null) {
                instance.getToken().enqueue(object : Callback<TokenResult> {
                    override fun onResponse(call: Call<TokenResult>?, response: Response<TokenResult>?) {
                        sp.edit().putString(KEY_TOEKN, response?.body()?.accessToken).apply()
                        execution()
                    }
                    override fun onFailure(call: Call<TokenResult>?, t: Throwable?) = Unit
                })
            } else {
                execution()
            }

        }
    }
}

