package br.com.joaoduartemariucio.rodaro.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object OrsNetwork {
    private const val BASE_URL = "https://api.openrouteservice.org/"

    fun api(
        apiKey: String,
        baseUrl: String = BASE_URL,
    ): OrsApi =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain
                                .request()
                                .newBuilder()
                                .addHeader("Authorization", apiKey)
                                .build(),
                        )
                    }.connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build(),
            ).addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OrsApi::class.java)
}
