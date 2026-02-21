package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI修正缓存
 * 存储用户通过AI修复后的章节内容，优先于原始内容显示
 */
@Entity(
    tableName = "ai_repair_cache",
    indices = [
        Index(value = ["bookUrl", "chapterIndex"], unique = true)
    ]
)
data class AiRepairCache(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    // 书籍URL
    @ColumnInfo(defaultValue = "")
    var bookUrl: String = "",
    // 章节索引
    @ColumnInfo(defaultValue = "0")
    var chapterIndex: Int = 0,
    // 章节标题
    @ColumnInfo(defaultValue = "")
    var chapterTitle: String = "",
    // AI修正后的内容
    @ColumnInfo(defaultValue = "")
    var repairedContent: String = "",
    // 原始内容哈希（用于验证原始内容是否变化）
    @ColumnInfo(defaultValue = "")
    var originalHash: String = "",
    // 创建时间
    @ColumnInfo(defaultValue = "0")
    var createTime: Long = System.currentTimeMillis(),
    // 更新时间
    @ColumnInfo(defaultValue = "0")
    var updateTime: Long = System.currentTimeMillis()
) {

    companion object {
        /**
         * 生成内容哈希
         */
        fun generateHash(content: String): String {
            return content.hashCode().toString()
        }
    }

    /**
     * 验证缓存是否仍然有效（原始内容未变化）
     */
    fun isValid(originalContent: String): Boolean {
        return originalHash == generateHash(originalContent)
    }
}
