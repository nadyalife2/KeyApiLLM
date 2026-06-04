package com.example.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object GitHubSyncService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch a raw README or markdown document from GitHub raw content URLs
     */
    @Throws(IOException::class)
    fun fetchMarkdown(url: String, personalAccessToken: String?): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "FreeLlmApiHub-Android")

        if (!personalAccessToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "token $personalAccessToken")
        }

        val request = requestBuilder.build()
        client.newCall(request).execute().use { response ->
            if (response.code == 403) {
                val rateLimitRemaining = response.header("X-RateLimit-Remaining")
                if (rateLimitRemaining == "0") {
                    throw IOException("GitHub API Rate Limit exceeded. Please add a GitHub PAT token in Settings.")
                }
            }
            if (!response.isSuccessful) {
                throw IOException("Fetch unsuccessful: HTTP Code ${response.code}")
            }
            return response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    /**
     * Conducts a HEAD test first, falling back to a quick POST to measure AI API endpoint health
     */
    fun verifyEndpointHealth(endpointUrl: String, apiKey: String?): HealthCheckResult {
        if (endpointUrl.isBlank()) {
            return HealthCheckResult("UNHEALTHY", 400, 0L, "Base URL is blank or invalid")
        }

        val startTime = System.currentTimeMillis()
        try {
            // First attempt: HEAD check
            val headRequest = Request.Builder()
                .url(endpointUrl)
                .head()
                .apply {
                    if (!apiKey.isNullOrBlank()) {
                        header("Authorization", "Bearer $apiKey")
                    }
                }
                .build()

            client.newCall(headRequest).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    return HealthCheckResult("HEALTHY", response.code, latency, "Connection successful (HEAD)")
                }
                
                // Fallback attempt: Quick empty POST to trigger auth response
                val postBody = "{\"model\":\"test\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}]}"
                val requestBody = postBody.toRequestBody("application/json".toMediaTypeOrNull())
                val postRequest = Request.Builder()
                    .url(endpointUrl)
                    .post(requestBody)
                    .apply {
                        if (!apiKey.isNullOrBlank()) {
                            header("Authorization", "Bearer $apiKey")
                        }
                    }
                    .build()

                val fallbackStartTime = System.currentTimeMillis()
                client.newCall(postRequest).execute().use { postResponse ->
                    val totalLatency = System.currentTimeMillis() - fallbackStartTime
                    return when (postResponse.code) {
                        200 -> HealthCheckResult("HEALTHY", postResponse.code, totalLatency, "Response 200 OK")
                        401 -> HealthCheckResult("UNHEALTHY", postResponse.code, totalLatency, "Unauthenticated: API Key is invalid")
                        403 -> HealthCheckResult("UNHEALTHY", postResponse.code, totalLatency, "Forbidden: Key or plan restriction")
                        429 -> HealthCheckResult("HEALTHY", postResponse.code, totalLatency, "Active: Rate limited by provider (429)")
                        else -> HealthCheckResult("UNHEALTHY", postResponse.code, totalLatency, "HTTP Error Code ${postResponse.code}")
                    }
                }
            }
        } catch (e: Exception) {
            val totalLatency = System.currentTimeMillis() - startTime
            return HealthCheckResult("UNHEALTHY", null, totalLatency, e.localizedMessage ?: "Unknown connection failure")
        }
    }
}

data class HealthCheckResult(
    val status: String, // "UNKNOWN", "HEALTHY", "UNHEALTHY"
    val httpCode: Int?,
    val latencyMs: Long,
    val message: String
)
