package com.example.cafeshopassignment.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    /**
     * Builds the [CafeApi] client.
     *
     * @param baseUrl backend root, from `BuildConfig.BACKEND_BASE_URL` (a trailing slash is added if missing)
     * @param enableLogging logs request lines/status codes only; the Authorization header is redacted
     */
    fun create(
        baseUrl: String,
        tokenProvider: IdTokenProvider,
        enableLogging: Boolean = false,
    ): CafeApi {
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthInterceptor(tokenProvider))
                .apply {
                    if (enableLogging) {
                        addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BASIC
                                redactHeader("Authorization")
                            },
                        )
                    }
                }
                // LLM replies can take a while; keep the connect timeout short and the read timeout generous.
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(90, TimeUnit.SECONDS)
                .build()

        return Retrofit
            .Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CafeApi::class.java)
    }

    fun normalizeBaseUrl(url: String): String = url.trim().let { if (it.endsWith("/")) it else "$it/" }
}
