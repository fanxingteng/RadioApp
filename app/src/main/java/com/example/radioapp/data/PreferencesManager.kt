package com.example.radioapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "radio_preferences")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val CUSTOM_STATIONS = stringPreferencesKey("custom_stations")
        val STATION_ORDER = stringPreferencesKey("station_order")
    }
    
    val autoPlay: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PLAY] ?: false
    }
    
    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY] = enabled
        }
    }
    
    val customStations: Flow<List<RadioStation>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[CUSTOM_STATIONS] ?: "[]"
        parseStationsJson(jsonString)
    }
    
    suspend fun saveCustomStations(stations: List<RadioStation>) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_STATIONS] = stationsToJson(stations)
        }
    }
    
    private fun parseStationsJson(json: String): List<RadioStation> {
        val stations = mutableListOf<RadioStation>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                stations.add(
                    RadioStation(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        url = obj.getString("url"),
                        isBuiltIn = false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stations
    }
    
    private fun stationsToJson(stations: List<RadioStation>): String {
        val jsonArray = JSONArray()
        stations.forEach { station ->
            val obj = JSONObject().apply {
                put("id", station.id)
                put("name", station.name)
                put("url", station.url)
                put("isBuiltIn", station.isBuiltIn)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
