package com.ytlibre.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// Entités
// ---------------------------------------------------------------------------

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val publishedAt: String,
    val addedAt: Long
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val publishedAt: String,
    val watchedAt: Long
)

/**
 * Cache persistant des résultats de recherche (page 1).
 * Permet de répondre à une recherche déjà effectuée — même après
 * redémarrage de l'app ou hors ligne — sans consommer de quota API.
 */
@Entity(tableName = "search_cache")
data class SearchCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val createdAt: Long
)

// ---------------------------------------------------------------------------
// DAO
// ---------------------------------------------------------------------------

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoId = :videoId)")
    fun observeIsFavorite(videoId: String): Flow<Boolean>

    @Query("SELECT * FROM favorites WHERE videoId = :videoId LIMIT 1")
    suspend fun findById(videoId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface SearchCacheDao {

    @Query("SELECT * FROM search_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): SearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SearchCacheEntity)

    @Query("DELETE FROM search_cache WHERE cacheKey NOT IN " +
        "(SELECT cacheKey FROM search_cache ORDER BY createdAt DESC LIMIT :keep)")
    suspend fun prune(keep: Int)

    @Query("DELETE FROM search_cache")
    suspend fun clear()
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM history WHERE videoId = :videoId)")
    fun observeContains(videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE videoId NOT IN (SELECT videoId FROM history ORDER BY watchedAt DESC LIMIT :limit)")
    suspend fun trimTo(limit: Int)
}

// ---------------------------------------------------------------------------
// Base
// ---------------------------------------------------------------------------

// exportSchema = false : pas d'export de schéma (aucune migration à versionner
// à ce stade — fallbackToDestructiveMigration). Cela évite surtout la course
// lecture/écriture du fichier de schéma entre les variantes debug/release.
@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class, SearchCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun searchCacheDao(): SearchCacheDao
}
