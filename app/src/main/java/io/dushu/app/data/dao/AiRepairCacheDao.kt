package io.dushu.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.dushu.app.data.entities.AiRepairCache

@Dao
interface AiRepairCacheDao {

    /**
     * 根据书籍URL和章节索引获取缓存
     */
    @Query("SELECT * FROM ai_repair_cache WHERE bookUrl = :bookUrl AND chapterIndex = :chapterIndex LIMIT 1")
    fun get(bookUrl: String, chapterIndex: Int): AiRepairCache?

    /**
     * 获取书籍的所有缓存
     */
    @Query("SELECT * FROM ai_repair_cache WHERE bookUrl = :bookUrl ORDER BY chapterIndex ASC")
    fun getByBook(bookUrl: String): List<AiRepairCache>

    /**
     * 插入或替换缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cache: AiRepairCache): Long

    /**
     * 更新缓存
     */
    @Update
    fun update(cache: AiRepairCache)

    /**
     * 删除指定章节的缓存
     */
    @Query("DELETE FROM ai_repair_cache WHERE bookUrl = :bookUrl AND chapterIndex = :chapterIndex")
    fun delete(bookUrl: String, chapterIndex: Int)

    /**
     * 删除整本书的缓存
     */
    @Query("DELETE FROM ai_repair_cache WHERE bookUrl = :bookUrl")
    fun deleteByBook(bookUrl: String)

    /**
     * 删除所有缓存
     */
    @Query("DELETE FROM ai_repair_cache")
    fun deleteAll()

    /**
     * 获取缓存数量
     */
    @Query("SELECT COUNT(*) FROM ai_repair_cache WHERE bookUrl = :bookUrl")
    fun getCountByBook(bookUrl: String): Int

    /**
     * 检查是否存在缓存
     */
    @Query("SELECT EXISTS(SELECT 1 FROM ai_repair_cache WHERE bookUrl = :bookUrl AND chapterIndex = :chapterIndex)")
    fun exists(bookUrl: String, chapterIndex: Int): Boolean
}
