package br.com.joaoduartemariucio.rodaro.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface RodaroApi {
    @POST("v1/routes")
    suspend fun routes(
        @Body request: RodaroRouteRequest,
    ): RodaroRoutesResponse
}
