package io.legado.app.ai.repair

import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.GSON
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * AI 内容修复服务
 */
object AIContentRepairService {

    private val aiHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var totalRequests = 0
    private var successfulRequests = 0
    private var cachedRequests = 0
    private var failedRequests = 0

    suspend fun repair(previousContext: String, currentParagraph: String): String {
        if (!AppConfig.aiContentRepairEnabled) {
            return currentParagraph
        }

        if (currentParagraph.length < MIN_TEXT_LENGTH) {
            return currentParagraph
        }

        val cachedResult = RepairCacheManager.get(previousContext, currentParagraph)
        if (cachedResult != null) {
            cachedRequests++
            return cachedResult
        }

        totalRequests++

        val result = performRepair(previousContext, currentParagraph)

        when (result) {
            is RepairResult.Success -> successfulRequests++
            is RepairResult.Cached -> cachedRequests++
            is RepairResult.Error -> failedRequests++
        }

        return when (result) {
            is RepairResult.Success -> {
                RepairCacheManager.put(previousContext, currentParagraph, result.repairedText)
                result.repairedText
            }
            is RepairResult.Cached -> result.repairedText
            is RepairResult.Error -> currentParagraph
        }
    }

    private suspend fun performRepair(
        previousContext: String,
        currentParagraph: String
    ): RepairResult {
        val config = buildRepairConfig()
        val provider = createProvider(config.providerType)

        var lastError: Throwable? = null

        repeat(config.retryCount + 1) { attempt ->
            try {
                if (attempt > 0) {
                    delay(config.retryDelayMs * attempt)
                }

                val result = executeRepair(provider, previousContext, currentParagraph, config)
                if (result.isSuccess) {
                    return RepairResult.Success(result.getOrThrow())
                }

                lastError = result.exceptionOrNull()

            } catch (e: Exception) {
                lastError = e
            }
        }

        return RepairResult.Error(
            lastError ?: Exception("Unknown error"),
            lastError?.message ?: "修复失败"
        )
    }

    private suspend fun executeRepair(
        provider: AIProvider,
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!provider.validateConfig(config)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid configuration"))
            }

            val requestBody = provider.buildRequestBody(previousContext, currentParagraph, config)

            val request = when (provider) {
                is OpenAIProvider -> provider.buildRequest(config, requestBody)
                is GeminiProvider -> provider.buildRequest(config, requestBody)
                is CustomProvider -> provider.buildRequest(config, requestBody)
                else -> throw IllegalStateException("Unknown provider type")
            }

            val response = withTimeout(config.timeoutMs) {
                aiHttpClient.newCall(request).execute()
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val repairedText = provider.parseResponse(responseBody)
                ?: return@withContext Result.failure(Exception("Failed to parse response"))

            if (!validateRepairResult(currentParagraph, repairedText)) {
                return@withContext Result.success(currentParagraph)
            }

            Result.success(repairedText.trim())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createProvider(providerType: AIProviderType): AIProvider {
        return when (providerType) {
            AIProviderType.OPENAI -> OpenAIProvider(AIProviderType.OPENAI)
            AIProviderType.GEMINI -> GeminiProvider()
            AIProviderType.CLAUDE -> OpenAIProvider(AIProviderType.CLAUDE)
            AIProviderType.DEEPSEEK -> OpenAIProvider(AIProviderType.DEEPSEEK)
            AIProviderType.CUSTOM -> CustomProvider()
        }
    }

    private fun buildRepairConfig(): RepairConfig {
        return RepairConfig(
            apiKey = AppConfig.aiRepairApiKey ?: "",
            apiUrl = AppConfig.aiRepairApiUrl ?: "",
            model = AppConfig.aiRepairModel ?: "",
            temperature = AppConfig.aiRepairTemperature,
            maxTokens = AppConfig.aiRepairMaxTokens,
            contextLength = AppConfig.aiRepairContextLength,
            timeoutMs = AppConfig.aiRepairTimeoutMs,
            retryCount = AppConfig.aiRepairRetryCount,
            retryDelayMs = AppConfig.aiRepairRetryDelayMs,
            systemPrompt = AppConfig.aiRepairSystemPrompt
        )
    }

    private fun validateRepairResult(original: String, repaired: String): Boolean {
        val lengthDiffRatio = kotlin.math.abs(original.length - repaired.length).toFloat() / original.length
        if (lengthDiffRatio > MAX_LENGTH_DIFF_RATIO) {
            return false
        }

        val commonChars = original.toSet().intersect(repaired.toSet()).size
        val charRetentionRatio = commonChars.toFloat() / original.toSet().size
        if (charRetentionRatio < MIN_CHAR_RETENTION_RATIO) {
            return false
        }

        return true
    }

    fun getStats(): ServiceStats {
        return ServiceStats(
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            cachedRequests = cachedRequests,
            failedRequests = failedRequests,
            successRate = if (totalRequests > 0) successfulRequests.toFloat() / totalRequests else 0f,
            cacheHitRate = if (totalRequests > 0) cachedRequests.toFloat() / totalRequests else 0f
        )
    }

    fun resetStats() {
        totalRequests = 0
        successfulRequests = 0
        cachedRequests = 0
        failedRequests = 0
    }

    suspend fun clearCache() {
        RepairCacheManager.clear()
    }

    private val RepairConfig.providerType: AIProviderType
        get() = AIProviderType.fromName(AppConfig.aiRepairProviderType ?: "OPENAI")

    private const val MIN_TEXT_LENGTH = 10
    private const val MAX_LENGTH_DIFF_RATIO = 0.5f
    private const val MIN_CHAR_RETENTION_RATIO = 0.3f

    data class ServiceStats(
        val totalRequests: Int,
        val successfulRequests: Int,
        val cachedRequests: Int,
        val failedRequests: Int,
        val successRate: Float,
        val cacheHitRate: Float
    )
}
