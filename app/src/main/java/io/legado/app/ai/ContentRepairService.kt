package io.legado.app.ai

import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.GSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ContentRepairService {

    suspend fun repair(previousContext: String, paragraph: String): String =
        withContext(Dispatchers.IO) {
            if (!AppConfig.aiContentRepairEnabled) return@withContext paragraph
            val apiKey = AppConfig.aiRepairApiKey ?: return@withContext paragraph
            try {
                val systemPrompt = "你是一个专业的文本校对助手，收到前文上下文和当前段落后，修复当前段落里出现的错乱、错字、错序，使其成为通顺、连贯的中文段落。只返回修正后的段落内容，不要带额外说明。"
                val userContent = "前文上下文:\n" + previousContext.takeLast(4000) + "\n---\n当前段落:\n" + paragraph
                val payload = GSON.toJson(mapOf(
                    "model" to "gpt-3.5-turbo",
                    "messages" to listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to userContent)
                    ),
                    "temperature" to 0.2,
                    "max_tokens" to 512
                ))

                val req = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .build()

                val resp = okHttpClient.newCall(req).execute()
                val body = resp.body?.string() ?: return@withContext paragraph
                val map = GSON.fromJsonObject<Map<String, Any?>>(body).getOrNull() ?: return@withContext paragraph
                val choices = map["choices"] as? List<*>
                val first = choices?.firstOrNull() as? Map<*, *>
                val message = first?.get("message") as? Map<*, *>
                val content = message?.get("content") as? String
                return@withContext content?.trim() ?: paragraph
            } catch (e: Throwable) {
                e.printStackTrace()
                return@withContext paragraph
            }
        }
}
