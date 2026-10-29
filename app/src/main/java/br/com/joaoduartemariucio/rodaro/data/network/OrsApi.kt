package br.com.joaoduartemariucio.rodaro.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OrsApi {
    @GET("geocode/search")
    suspend fun geocode(
        @Query("text") text: String,
        @Query("size") size: Int = 1,
    ): GeocodeResponse

    @POST("v2/directions/driving-car")
    suspend fun directions(
        @Body request: DirectionsRequest,
    ): DirectionsResponse
}
