package io.legado.app.ai.repair

import io.legado.app.help.config.AppConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * AI 修复结果缓存管理器
 */
object RepairCacheManager {

    private const val MAX_CACHE_SIZE = 1000
    private const val MAX_TEXT_LENGTH = 500

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val accessOrder = mutableListOf<String>()
    private val mutex = Mutex()

    data class CacheEntry(
        val originalText: String,
        val repairedText: String,
        val timestamp: Long = System.currentTimeMillis(),
        var accessCount: Int = 1
    )

    suspend fun get(previousContext: String, currentParagraph: String): String? {
        if (!AppConfig.aiContentRepairCacheEnabled) return null

        val key = generateCacheKey(previousContext, currentParagraph)

        return mutex.withLock {
            val entry = cache[key]
            if (entry != null) {
                entry.accessCount++
                accessOrder.remove(key)
                accessOrder.add(key)
                entry.repairedText
            } else {
                null
            }
        }
    }

    suspend fun put(previousContext: String, currentParagraph: String, repairedText: String) {
        if (!AppConfig.aiContentRepairCacheEnabled) return
        if (currentParagraph.length > MAX_TEXT_LENGTH) return

        val key = generateCacheKey(previousContext, currentParagraph)

        mutex.withLock {
            if (cache.size >= MAX_CACHE_SIZE && !cache.containsKey(key)) {
                removeOldestEntry()
            }

            cache[key] = CacheEntry(
                originalText = currentParagraph,
                repairedText = repairedText,
                timestamp = System.currentTimeMillis()
            )

            if (!accessOrder.contains(key)) {
                accessOrder.add(key)
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            cache.clear()
            accessOrder.clear()
        }
    }

    private fun generateCacheKey(previousContext: String, currentParagraph: String): String {
        val contextSnippet = previousContext.takeLast(200)
        val combined = "$contextSnippet|||$currentParagraph"
        return md5Hash(combined)
    }

    private fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun removeOldestEntry() {
        if (accessOrder.isEmpty()) return
        val oldestKey = accessOrder.removeAt(0)
        cache.remove(oldestKey)
    }
}
