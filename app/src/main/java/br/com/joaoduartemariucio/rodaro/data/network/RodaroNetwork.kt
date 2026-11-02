package br.com.joaoduartemariucio.rodaro.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RodaroNetwork {
    fun api(baseUrl: String): RodaroApi =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient
                    .Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build(),
            ).addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }.asConverterFactory("application/json".toMediaType()),
            ).build()
            .create(RodaroApi::class.java)
}
