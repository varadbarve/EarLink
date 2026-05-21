package com.example.earlink.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfileFlow(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAll()

    suspend fun setActiveProfile(profileId: Long) {
        deactivateAll()
        QuerySetActive(profileId)
    }

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun QuerySetActive(profileId: Long)
}

@Dao
interface MappingDao {
    @Query("SELECT * FROM mappings WHERE profileId = :profileId")
    suspend fun getMappingsForProfile(profileId: Long): List<MappingEntity>

    @Query("SELECT * FROM mappings WHERE profileId = :profileId")
    fun getMappingsForProfileFlow(profileId: Long): Flow<List<MappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MappingEntity): Long

    @Query("DELETE FROM mappings WHERE id = :mappingId")
    suspend fun deleteMapping(mappingId: Long)

    @Query("DELETE FROM mappings WHERE profileId = :profileId AND eventType = :eventType")
    suspend fun deleteMappingForEvent(profileId: Long, eventType: String)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogsFlow(): Flow<List<LogEntity>>

    @Insert
    suspend fun insertLog(log: LogEntity): Long

    @Query("DELETE FROM event_logs")
    suspend fun clearLogs()
}
