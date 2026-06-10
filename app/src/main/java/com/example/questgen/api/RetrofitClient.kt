package com.example.questgen.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://192.168.18.9/ApiQuestGen/"
    private var apiService: ApiService? = null

    fun getInstance(context: Context): ApiService {
        if (apiService == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Interceptor que injeta automaticamente o user_id salvo nas requisições
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val sharedPrefs = context.getSharedPreferences("QuestGenPrefs", Context.MODE_PRIVATE)
                    val userId = sharedPrefs.getInt("USER_ID", -1)
                    
                    val originalRequest = chain.request()
                    val newUrl = originalRequest.url.newBuilder().apply {
                        if (userId != -1 && originalRequest.url.queryParameter("user_id") == null) {
                            addQueryParameter("user_id", userId.toString())
                        }
                    }.build()

                    val newRequest = originalRequest.newBuilder().url(newUrl).build()
                    chain.proceed(newRequest)
                }
                .build()

            apiService = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
        return apiService!!
    }
}
