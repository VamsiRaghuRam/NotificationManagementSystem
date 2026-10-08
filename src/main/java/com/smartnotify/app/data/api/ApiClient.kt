package com.smartnotify.app.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit Network Client for SmartNotify ML Backend.
 * Supports configurable server URL for physical Android devices on LAN / Tunnels.
 */
object ApiClient {

    private const val PREF_NAME = "smartnotify_api_prefs"
    private const val KEY_BASE_URL = "backend_base_url"
    const val DEFAULT_EMULATOR_URL = "http://192.168.1.6:8000/"

    var baseUrl: String = DEFAULT_EMULATOR_URL
        private set

    private var retrofit: Retrofit? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_BASE_URL, null)
        if (!savedUrl.isNullOrBlank()) {
            updateBaseUrl(context, savedUrl)
        }
    }

    val api: PredictionApi
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!.create(PredictionApi::class.java)
        }

    fun updateBaseUrl(context: Context, newUrl: String) {
        var formatted = newUrl.trim()
        if (formatted.isNotEmpty() && !formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://$formatted"
        }
        if (!formatted.endsWith("/")) {
            formatted = "$formatted/"
        }
        
        baseUrl = formatted
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, baseUrl)
            .apply()
    }
}
