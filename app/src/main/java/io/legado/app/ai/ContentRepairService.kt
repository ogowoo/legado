package io.legado.app.ai

import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.GSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * AI 内容修复服务
 */
object ContentRepairService {
    
    suspend fun repair(previousContext: String, paragraph: String): String {
        if (!AppConfig.aiContentRepairEnabled) {
            return paragraph
        }
        
        val apiKey = AppConfig.aiRepairApiKey
        if (apiKey.isNullOrBlank()) {
            return paragraph
        }
        
        // 检查缓存
        RepairCacheManager.get(previousContext, paragraph)?.let { cachedResult ->
            return cachedResult
        }
        
        return try {
            val result = executeRepair(previousContext, paragraph, apiKey)
            RepairCacheManager.put(previousContext, paragraph, result)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            paragraph
        }
    }
    
    private suspend fun executeRepair(
        previousContext: String,
        paragraph: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val providerType = AIProviderType.fromString(AppConfig.aiRepairProviderType)
        val provider = AIProviderFactory.create(providerType)
        
        val model = AppConfig.aiRepairModel?.takeIf { it.isNotBlank() } 
            ?: provider.defaultModel
        
        var lastException: Exception? = null
        
        repeat(3) { attempt ->
            try {
                val result = callApi(
                    provider = provider,
                    previousContext = previousContext,
                    paragraph = paragraph,
                    apiKey = apiKey,
                    model = model,
                    temperature = 0.2f,
                    maxTokens = 512,
                    customApiUrl = AppConfig.aiRepairApiUrl
                )
                return@withContext result
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < 2) delay(1000 * (attempt + 1).toLong())
            } catch (e: IOException) {
                lastException = e
                if (attempt < 2) delay(1000 * (attempt + 1).toLong())
            }
        }
        
        throw lastException ?: IOException("Repair failed after 3 attempts")
    }
    
    private fun callApi(
        provider: AIProvider,
        previousContext: String,
        paragraph: String,
        apiKey: String,
        model: String,
        temperature: Float,
        maxTokens: Int,
        customApiUrl: String?
    ): String {
        val requestBody = provider.buildRequestBody(
            previousContext = previousContext,
            paragraph = paragraph,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens
        )
        
        val request = Request.Builder()
            .url(provider.getApiUrl(customApiUrl))
            .apply {
                provider.getHeaders(apiKey).forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string()
        
        if (!response.isSuccessful) {
            throw IOException("API error: ${response.code}")
        }
        
        return provider.parseResponse(responseBody ?: "") 
            ?: throw IOException("Failed to parse response")
    }
}
