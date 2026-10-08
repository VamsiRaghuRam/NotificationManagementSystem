package com.smartnotify.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * API Request & Response DTOs matching Phase 4 FastAPI REST Contract.
 */

data class PredictionApiRequest(
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("app") val app: String,
    @SerializedName("category") val category: String = "General"
)

data class PredictionApiResponse(
    @SerializedName("priority") val priority: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("model") val model: String,
    @SerializedName("timestamp") val timestamp: String
)

data class HealthCheckResponse(
    @SerializedName("status") val status: String,
    @SerializedName("model_loaded") val modelLoaded: Boolean,
    @SerializedName("version") val version: String
)

data class ModelInfoResponse(
    @SerializedName("model") val model: String,
    @SerializedName("classes") val classes: List<String>,
    @SerializedName("test_accuracy") val testAccuracy: Float,
    @SerializedName("test_macro_f1") val testMacroF1: Float,
    @SerializedName("high_class_precision") val highClassPrecision: Float,
    @SerializedName("high_class_recall") val highClassRecall: Float,
    @SerializedName("high_class_f1") val highClassF1: Float,
    @SerializedName("training_samples") val trainingSamples: Int,
    @SerializedName("test_samples") val testSamples: Int
)

data class FeedbackApiRequest(
    @SerializedName("target_name") val targetName: String,
    @SerializedName("rating") val rating: String,
    @SerializedName("notification_id") val notificationId: Int? = null
)
