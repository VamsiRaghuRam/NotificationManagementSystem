package com.smartnotify.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit Interface for SmartNotify FastAPI ML Backend endpoints.
 */
interface PredictionApi {

    @GET("/health")
    suspend fun checkHealth(): Response<HealthCheckResponse>

    @GET("/model-info")
    suspend fun getModelInfo(): Response<ModelInfoResponse>

    @POST("/predict")
    suspend fun predictNotification(
        @Body request: PredictionApiRequest
    ): Response<PredictionApiResponse>

    @POST("/feedback")
    suspend fun submitFeedback(
        @Body request: FeedbackApiRequest
    ): Response<Map<String, Any>>

    @POST("/sessions/start")
    suspend fun startSession(): Response<Map<String, Any>>

    @POST("/sessions/stop")
    suspend fun stopSession(): Response<Map<String, Any>>
}
