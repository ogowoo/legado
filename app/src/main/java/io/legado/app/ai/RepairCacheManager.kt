package io.legado.app.ai

import androidx.collection.LruCache
import io.legado.app.help.config.AppConfig
import java.security.MessageDigest

/**
 * 修复缓存管理器
 */
object RepairCacheManager {
    
    private val cache: LruCache<String, String> by lazy {
        LruCache(100)
    }
    
    fun generateKey(previousContext: String, paragraph: String): String {
        val content = previousContext.takeLast(100) + "||" + paragraph
        return md5(content)
    }
    
    fun get(previousContext: String, paragraph: String): String? {
        if (!AppConfig.aiContentRepairEnabled) return null
        val key = generateKey(previousContext, paragraph)
        return cache.get(key)
    }
    
    fun put(previousContext: String, paragraph: String, repairedText: String) {
        if (!AppConfig.aiContentRepairEnabled) return
        val key = generateKey(previousContext, paragraph)
        cache.put(key, repairedText)
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
