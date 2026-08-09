package com.kolvexo.kolaki.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kolaki_preferences")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class AppPreferences(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val theme = stringPreferencesKey("theme")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.onboardingDone] ?: false
    }

    val theme: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[Keys.theme]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun finishOnboarding() {
        context.dataStore.edit { it[Keys.onboardingDone] = true }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }
}
