/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.data_store

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore("data_store")

class DataStoreImpl @Inject constructor(context: Application) : DataStore {
    private val dataStore = context.dataStore

    override suspend fun snapshot(): Map<String, Any> = dataStore.data.first().asMap().mapKeys { it.key.name }

    override suspend fun replaceAll(values: Map<String, Any>) {
        dataStore.edit { preferences ->
            preferences.clear()
            values.forEach { (key, value) ->
                when (value) {
                    is Boolean -> preferences[booleanPreferencesKey(key)] = value
                    is Int -> preferences[intPreferencesKey(key)] = value
                    is Long -> preferences[longPreferencesKey(key)] = value
                    is Float -> preferences[floatPreferencesKey(key)] = value
                    is Double -> preferences[doublePreferencesKey(key)] = value
                    is String -> preferences[stringPreferencesKey(key)] = value
                    is Set<*> -> preferences[stringSetPreferencesKey(key)] = value.map { it as? String ?: error("Invalid preference set") }.toSet()
                    else -> error("Unsupported preference")
                }
            }
        }
    }

    override suspend fun <T> getNullableData(key: Preferences.Key<T>): T? {
        return try {
            dataStore.data.firstOrNull()?.get(key)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun <T> putData(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
