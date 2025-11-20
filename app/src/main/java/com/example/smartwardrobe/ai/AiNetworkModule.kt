package com.example.smartwardrobe.ai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AiNetworkModule {

    // TODO: replace with your real backend URL when it's ready
    // Make sure this ends with a slash `/`
    private const val BASE_URL = "https://example.com/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // For debugging – you can change to NONE for release builds
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: AiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApi::class.java)
    }
}
