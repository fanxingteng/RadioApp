package com.example.radioapp.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.radioapp.data.DefaultStations
import com.example.radioapp.data.PreferencesManager
import com.example.radioapp.data.RadioStation
import com.example.radioapp.service.RadioPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val autoPlay: Boolean = false
)

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesManager = PreferencesManager(application)
    
    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()
    
    private val _builtInStations = mutableStateListOf<RadioStation>().apply {
        addAll(DefaultStations.stations)
    }
    
    private val _customStations = mutableStateListOf<RadioStation>()
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            // Load auto-play setting
            preferencesManager.autoPlay.collect { autoPlay ->
                _uiState.value = _uiState.value.copy(autoPlay = autoPlay)
            }
        }
        
        viewModelScope.launch {
            // Load custom stations
            preferencesManager.customStations.collect { customStations ->
                _customStations.clear()
                _customStations.addAll(customStations)
                updateStationsList()
            }
        }
    }
    
    private fun updateStationsList() {
        val allStations = _builtInStations + _customStations
        _uiState.value = _uiState.value.copy(stations = allStations)
    }
    
    fun playStation(station: RadioStation) {
        _uiState.value = _uiState.value.copy(
            currentStation = station,
            isPlaying = true
        )
        
        val intent = Intent(getApplication(), RadioPlayerService::class.java).apply {
            action = RadioPlayerService.ACTION_PLAY
            putExtra(RadioPlayerService.EXTRA_STATION_URL, station.url)
            putExtra(RadioPlayerService.EXTRA_STATION_NAME, station.name)
        }
        getApplication<Application>().startService(intent)
    }
    
    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pauseStation()
        } else {
            _uiState.value.currentStation?.let { station ->
                playStation(station)
            }
        }
    }
    
    private fun pauseStation() {
        _uiState.value = _uiState.value.copy(isPlaying = false)
        
        val intent = Intent(getApplication(), RadioPlayerService::class.java).apply {
            action = RadioPlayerService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }
    
    fun addCustomStation(name: String, url: String) {
        val newStation = RadioStation(
            id = System.currentTimeMillis().toString(),
            name = name,
            url = url,
            isBuiltIn = false
        )
        _customStations.add(newStation)
        saveCustomStations()
        updateStationsList()
    }
    
    fun updateStation(id: String, name: String, url: String) {
        val station = (_builtInStations + _customStations).find { it.id == id }
        station?.let {
            val updatedStation = it.copy(name = name, url = url)
            if (it.isBuiltIn) {
                val index = _builtInStations.indexOf(it)
                if (index != -1) {
                    _builtInStations[index] = updatedStation
                }
            } else {
                val index = _customStations.indexOf(it)
                if (index != -1) {
                    _customStations[index] = updatedStation
                    saveCustomStations()
                }
            }
            updateStationsList()
        }
    }
    
    fun deleteStation(station: RadioStation) {
        if (station.isBuiltIn) {
            _builtInStations.remove(station)
        } else {
            _customStations.remove(station)
            saveCustomStations()
        }
        
        // If deleting current station, stop playback
        if (_uiState.value.currentStation?.id == station.id) {
            stopPlayback()
        }
        
        updateStationsList()
    }
    
    fun moveStation(fromIndex: Int, toIndex: Int) {
        val allStations = _builtInStations + _customStations
        if (fromIndex in allStations.indices && toIndex in allStations.indices) {
            val station = allStations[fromIndex]
            if (station.isBuiltIn) {
                val builtInIndex = _builtInStations.indexOf(station)
                if (builtInIndex != -1) {
                    _builtInStations.removeAt(builtInIndex)
                    val newBuiltInIndex = (toIndex - (if (toIndex > fromIndex) 0 else 0)).coerceIn(0, _builtInStations.size)
                    _builtInStations.add(newBuiltInIndex, station)
                }
            } else {
                val customIndex = _customStations.indexOf(station)
                if (customIndex != -1) {
                    _customStations.removeAt(customIndex)
                    val newCustomIndex = customIndex.coerceIn(0, _customStations.size)
                    _customStations.add(newCustomIndex, station)
                    saveCustomStations()
                }
            }
            updateStationsList()
        }
    }
    
    private fun stopPlayback() {
        _uiState.value = _uiState.value.copy(
            currentStation = null,
            isPlaying = false
        )
        
        val intent = Intent(getApplication(), RadioPlayerService::class.java).apply {
            action = RadioPlayerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
    
    fun toggleAutoPlay() {
        val newAutoPlay = !_uiState.value.autoPlay
        _uiState.value = _uiState.value.copy(autoPlay = newAutoPlay)
        viewModelScope.launch {
            preferencesManager.setAutoPlay(newAutoPlay)
        }
    }
    
    private fun saveCustomStations() {
        viewModelScope.launch {
            preferencesManager.saveCustomStations(_customStations.toList())
        }
    }
}
