package com.example.earlink.data

import android.content.Context
import com.example.earlink.database.AppDatabase
import com.example.earlink.database.LogEntity
import com.example.earlink.database.MappingEntity
import com.example.earlink.database.ProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

interface DataRepository {
    val allProfilesFlow: Flow<List<ProfileEntity>>
    val activeProfileFlow: Flow<ProfileEntity?>
    val recentLogsFlow: Flow<List<LogEntity>>

    fun getMappingsForProfileFlow(profileId: Long): Flow<List<MappingEntity>>
    suspend fun insertProfile(name: String): Long
    suspend fun deleteProfile(profile: ProfileEntity)
    suspend fun setActiveProfile(profileId: Long)
    suspend fun insertMapping(profileId: Long, eventType: String, actionType: String, actionData: String): Long
    suspend fun deleteMapping(mappingId: Long)
    suspend fun clearLogs()
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val db = AppDatabase.getDatabase(context)
    private val profileDao = db.profileDao()
    private val mappingDao = db.mappingDao()
    private val logDao = db.logDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profiles = profileDao.getAllProfiles()
                if (profiles.isEmpty()) {
                    val musicId = profileDao.insertProfile(ProfileEntity(name = "Music"))
                    val gamingId = profileDao.insertProfile(ProfileEntity(name = "Gaming"))
                    val studyId = profileDao.insertProfile(ProfileEntity(name = "Study"))
                    
                    // Music mappings (all default)
                    mappingDao.insertMapping(MappingEntity(profileId = musicId, eventType = "SINGLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = musicId, eventType = "LEFT_DOUBLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = musicId, eventType = "RIGHT_DOUBLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = musicId, eventType = "LEFT_LONG_PRESS", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = musicId, eventType = "RIGHT_LONG_PRESS", actionType = "DEFAULT", actionData = ""))

                    // Gaming mappings
                    mappingDao.insertMapping(MappingEntity(profileId = gamingId, eventType = "SINGLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = gamingId, eventType = "LEFT_DOUBLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = gamingId, eventType = "RIGHT_DOUBLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = gamingId, eventType = "LEFT_LONG_PRESS", actionType = "DESKTOP_COMMAND", actionData = "mute_discord"))
                    mappingDao.insertMapping(MappingEntity(profileId = gamingId, eventType = "RIGHT_LONG_PRESS", actionType = "DEFAULT", actionData = ""))

                    // Study mappings
                    mappingDao.insertMapping(MappingEntity(profileId = studyId, eventType = "SINGLE_TAP", actionType = "TOGGLE_DND", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = studyId, eventType = "LEFT_DOUBLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = studyId, eventType = "RIGHT_DOUBLE_TAP", actionType = "DEFAULT", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = studyId, eventType = "LEFT_LONG_PRESS", actionType = "START_RECORDING", actionData = ""))
                    mappingDao.insertMapping(MappingEntity(profileId = studyId, eventType = "RIGHT_LONG_PRESS", actionType = "LAUNCH_ASSISTANT", actionData = ""))

                    // Set Music active
                    profileDao.setActiveProfile(musicId)
                }
            } catch (e: Exception) {
                android.util.Log.e("DefaultDataRepository", "Failed to seed default profiles", e)
            }
        }
    }

    override val allProfilesFlow: Flow<List<ProfileEntity>> = profileDao.getAllProfilesFlow()
    override val activeProfileFlow: Flow<ProfileEntity?> = profileDao.getActiveProfileFlow()
    override val recentLogsFlow: Flow<List<LogEntity>> = logDao.getRecentLogsFlow()

    override fun getMappingsForProfileFlow(profileId: Long): Flow<List<MappingEntity>> {
        return mappingDao.getMappingsForProfileFlow(profileId)
    }

    override suspend fun insertProfile(name: String): Long {
        val profile = ProfileEntity(name = name)
        return profileDao.insertProfile(profile)
    }

    override suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao.deleteProfile(profile)
    }

    override suspend fun setActiveProfile(profileId: Long) {
        profileDao.setActiveProfile(profileId)
    }

    override suspend fun insertMapping(
        profileId: Long,
        eventType: String,
        actionType: String,
        actionData: String
    ): Long {
        // First delete any existing mapping for this event in this profile to prevent duplicates
        mappingDao.deleteMappingForEvent(profileId, eventType)
        
        val mapping = MappingEntity(
            profileId = profileId,
            eventType = eventType,
            actionType = actionType,
            actionData = actionData
        )
        return mappingDao.insertMapping(mapping)
    }

    override suspend fun deleteMapping(mappingId: Long) {
        mappingDao.deleteMapping(mappingId)
    }

    override suspend fun clearLogs() {
        logDao.clearLogs()
    }
}
