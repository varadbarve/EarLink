package com.example.earlink.ui.main

import com.example.earlink.data.DataRepository
import com.example.earlink.database.LogEntity
import com.example.earlink.database.MappingEntity
import com.example.earlink.database.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

    // Helper to await a condition on a flow during concurrent test execution
    private suspend fun <T> Flow<T>.awaitCondition(timeoutMs: Long = 2000, condition: (T) -> Boolean): T {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val value = this.first()
            if (condition(value)) {
                return value
            }
            delay(10)
        }
        return this.first()
    }

    @Test
    fun testProfileLifecycle() = runTest {
        val repository = FakeDataRepository()
        val viewModel = MainScreenViewModel(repository)

        // Activate StateFlows using backgroundScope
        backgroundScope.launch { viewModel.allProfiles.collect {} }
        backgroundScope.launch { viewModel.activeProfile.collect {} }

        // 1. Initial profiles should be empty
        var profiles = viewModel.allProfiles.value
        assertTrue(profiles.isEmpty())

        // 2. Create profile
        viewModel.createProfile("Gaming")
        
        // Wait for flow updates
        profiles = repository.allProfilesFlow.awaitCondition { it.size == 1 }
        assertEquals(1, profiles.size)
        assertEquals("Gaming", profiles[0].name)

        // 3. Create another profile
        viewModel.createProfile("Study")
        profiles = repository.allProfilesFlow.awaitCondition { it.size == 2 }
        assertEquals(2, profiles.size)

        // 4. Select profile
        viewModel.selectProfile(profiles[1].id)
        val active = repository.activeProfileFlow.awaitCondition { it?.id == profiles[1].id }
        assertNotNull(active)
        assertEquals("Study", active?.name)
    }

    @Test
    fun testMappingsLifecycle() = runTest {
        val repository = FakeDataRepository()
        val viewModel = MainScreenViewModel(repository)

        // Activate StateFlows using backgroundScope
        backgroundScope.launch { viewModel.allProfiles.collect {} }
        backgroundScope.launch { viewModel.activeProfile.collect {} }

        // Create profile
        viewModel.createProfile("Work")
        val profiles = repository.allProfilesFlow.awaitCondition { it.size == 1 }
        
        // Select profile
        viewModel.selectProfile(profiles[0].id)
        repository.activeProfileFlow.awaitCondition { it?.id == profiles[0].id }

        // Give StateFlow a small delay to propagate the selection internally in viewModel
        viewModel.activeProfile.awaitCondition { it?.id == profiles[0].id }

        // Save mapping
        viewModel.saveMapping("SINGLE_TAP", "TOGGLE_DND", "")
        
        val mappings = repository.getMappingsForProfileFlow(profiles[0].id).awaitCondition { it.size == 1 }
        assertEquals(1, mappings.size)
        assertEquals("SINGLE_TAP", mappings[0].eventType)
        assertEquals("TOGGLE_DND", mappings[0].actionType)
    }
}

private class FakeDataRepository : DataRepository {
    private val _profiles = MutableStateFlow<List<ProfileEntity>>(emptyList())
    private val _mappings = MutableStateFlow<List<MappingEntity>>(emptyList())
    private val _logs = MutableStateFlow<List<LogEntity>>(emptyList())

    override val allProfilesFlow: Flow<List<ProfileEntity>> = _profiles.asStateFlow()
    override val activeProfileFlow: Flow<ProfileEntity?> = _profiles.map { list -> list.find { it.isActive } }
    override val recentLogsFlow: Flow<List<LogEntity>> = _logs.asStateFlow()

    override fun getMappingsForProfileFlow(profileId: Long): Flow<List<MappingEntity>> {
        return _mappings.map { list -> list.filter { it.profileId == profileId } }
    }

    override suspend fun insertProfile(name: String): Long {
        val nextId = (_profiles.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val profile = ProfileEntity(id = nextId, name = name, isActive = _profiles.value.isEmpty())
        _profiles.value = _profiles.value + profile
        return nextId
    }

    override suspend fun deleteProfile(profile: ProfileEntity) {
        _profiles.value = _profiles.value.filter { it.id != profile.id }
    }

    override suspend fun setActiveProfile(profileId: Long) {
        _profiles.value = _profiles.value.map {
            it.copy(isActive = it.id == profileId)
        }
    }

    override suspend fun insertMapping(
        profileId: Long,
        eventType: String,
        actionType: String,
        actionData: String
    ): Long {
        val nextId = (_mappings.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val filtered = _mappings.value.filterNot { it.profileId == profileId && it.eventType == eventType }
        val mapping = MappingEntity(id = nextId, profileId = profileId, eventType = eventType, actionType = actionType, actionData = actionData)
        _mappings.value = filtered + mapping
        return nextId
    }

    override suspend fun deleteMapping(mappingId: Long) {
        _mappings.value = _mappings.value.filter { it.id != mappingId }
    }

    override suspend fun clearLogs() {
        _logs.value = emptyList()
    }
}
