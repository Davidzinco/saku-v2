package com.saku.v2.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuSeedData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val Context.sakuDataStore: DataStore<Preferences> by preferencesDataStore(name = "saku_v2_prefs")

/**
 * Single source of truth backing [SakuAppState] in Jetpack DataStore Preferences.
 */
class SakuStore(
    private val context: Context,
    private val onStatePersisted: (SakuAppState) -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private val KEY_STATE_JSON = stringPreferencesKey("saku_state_json")

        val json: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _state = MutableStateFlow(SakuSeedData.createDefaultState())
    val state: StateFlow<SakuAppState> = _state.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private var initialized = false

    init {
        scope.launch {
            ensureLoadedLocked()
        }
    }

    private suspend fun ensureLoadedLocked() {
        mutex.withLock {
            if (initialized) return
            val prefs = withContext(ioDispatcher) {
                context.sakuDataStore.data.first()
            }
            val rawJson = prefs[KEY_STATE_JSON]
            val loaded = if (rawJson.isNullOrBlank()) {
                val seed = SakuSeedData.createDefaultState()
                persistLocked(seed)
                seed
            } else {
                runCatching { json.decodeFromString<SakuAppState>(rawJson) }
                    .getOrElse {
                        val fallback = SakuSeedData.createDefaultState()
                        persistLocked(fallback)
                        fallback
                    }
            }
            _state.value = loaded
            initialized = true
            _isLoaded.value = true
            onStatePersisted(loaded)
        }
    }

    /**
     * Atomically transforms the persisted [SakuAppState] and notifies listeners.
     */
    suspend fun mutateState(transform: (SakuAppState) -> SakuAppState): SakuAppState {
        if (!initialized) {
            ensureLoadedLocked()
        }
        return mutex.withLock {
            val current = _state.value
            val transformed = transform(current).copy(updatedAtEpochMs = System.currentTimeMillis())
            persistLocked(transformed)
            _state.value = transformed
            onStatePersisted(transformed)
            transformed
        }
    }

    /**
     * Replaces the entire persisted [SakuAppState] (e.g. after ZIP backup restore).
     */
    suspend fun replaceState(newState: SakuAppState): SakuAppState {
        if (!initialized) {
            ensureLoadedLocked()
        }
        return mutex.withLock {
            val normalized = newState.copy(updatedAtEpochMs = System.currentTimeMillis())
            persistLocked(normalized)
            _state.value = normalized
            onStatePersisted(normalized)
            normalized
        }
    }

    private suspend fun persistLocked(newState: SakuAppState) {
        val encoded = json.encodeToString(SakuAppState.serializer(), newState)
        withContext(ioDispatcher) {
            context.sakuDataStore.edit { prefs ->
                prefs[KEY_STATE_JSON] = encoded
            }
        }
    }
}
