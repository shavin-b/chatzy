package com.orderflow.admin.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orderflow.admin.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val KEY_ADMIN_TOKEN = stringPreferencesKey(Constants.PREF_KEY_ADMIN_TOKEN)
    private val KEY_REMEMBER_LOGIN = booleanPreferencesKey(Constants.PREF_KEY_REMEMBER_LOGIN)
    private val KEY_THEME_MODE = stringPreferencesKey(Constants.PREF_KEY_THEME_MODE)
    private val KEY_DEFAULT_SUB_DAYS = intPreferencesKey(Constants.PREF_KEY_DEFAULT_SUB_DAYS)

    val adminToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ADMIN_TOKEN]
    }

    val rememberLogin: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REMEMBER_LOGIN] ?: true
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "System"
    }

    val defaultSubDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SUB_DAYS] ?: 30
    }

    suspend fun saveSession(token: String, remember: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ADMIN_TOKEN] = token
            prefs[KEY_REMEMBER_LOGIN] = remember
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ADMIN_TOKEN)
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDefaultSubDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SUB_DAYS] = days
        }
    }
}
