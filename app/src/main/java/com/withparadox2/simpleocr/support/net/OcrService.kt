package com.withparadox2.simpleocr.support.net

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by withparadox2 on 2018/3/22.
 */
const val OCR_ID = "mDcZYbQzIZYrsHh4cGXric4c"
const val OCR_SECRET = "dgIt03inKtFrhCA6tyMjaXSQ4eLfq1Cc"

interface OcrService {
    @GET("oauth/2.0/token")
    fun getToken(@Query("client_id") clientId: String = OCR_ID,
                 @Query("client_secret") clientSecret: String = OCR_SECRET,
                 @Query("grant_type") grantType: String = "client_credentials"): Call<String>
    companion object {
        var instance: OcrService = Retrofit.Builder().baseUrl("https://aip.baidubce.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(OcrService::class.java)
    }
}