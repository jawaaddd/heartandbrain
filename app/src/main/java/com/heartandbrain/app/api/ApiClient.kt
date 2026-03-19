package com.heartandbrain.app.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    val whisper: WhisperApi by lazy { buildRetrofit("https://api.openai.com/", 120) { builder ->
        builder.addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${ApiKeys.OPENAI}")
                    .build()
            )
        }
    }.create(WhisperApi::class.java) }

    val claude: ClaudeApi by lazy { buildRetrofit("https://api.anthropic.com/", 60) { builder ->
        builder.addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("x-api-key", ApiKeys.CLAUDE)
                    .addHeader("anthropic-version", "2023-06-01")
                    .build()
            )
        }
    }.create(ClaudeApi::class.java) }

    private fun buildRetrofit(
        baseUrl: String,
        timeoutSeconds: Long,
        configure: (OkHttpClient.Builder) -> Unit,
    ): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        configure(clientBuilder)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
