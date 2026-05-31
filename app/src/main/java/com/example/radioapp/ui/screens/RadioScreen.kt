package com.example.radioapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radioapp.data.RadioStation
import com.example.radioapp.viewmodel.RadioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RadioScreen(viewModel: RadioViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStation by remember { mutableStateOf<RadioStation?>(null) }
    var stationToDelete by remember { mutableStateOf<RadioStation?>(null) }
    
    // Auto play logic
    LaunchedEffect(uiState.autoPlay) {
        if (uiState.autoPlay && uiState.stations.isNotEmpty() && !uiState.isPlaying) {
            viewModel.playStation(uiState.stations.first())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网络收音机") },
                actions = {
                    IconButton(onClick = { viewModel.toggleAutoPlay() }) {
                        Icon(
                            imageVector = if (uiState.autoPlay) Icons.Filled.PlayArrow else Icons.Filled.Settings,
                            contentDescription = "自动播放"
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加电台")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Now Playing Bar
            if (uiState.currentStation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "正在播放",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = uiState.currentStation.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.togglePlayPause() }
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "暂停" else "播放"
                            )
                        }
                    }
                }
            }
            
            // Station List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiState.stations,
                    key = { _, station -> station.id }
                ) { index, station ->
                    StationItem(
                        station = station,
                        isSelected = station.id == uiState.currentStation?.id,
                        isPlaying = uiState.isPlaying && station.id == uiState.currentStation?.id,
                        onStationClick = { viewModel.playStation(station) },
                        onEditClick = { editingStation = station },
                        onDeleteClick = { stationToDelete = station },
                        onMoveUp = { if (index > 0) viewModel.moveStation(index, index - 1) },
                        onMoveDown = { if (index < uiState.stations.size - 1) viewModel.moveStation(index, index + 1) },
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.stations.size - 1
                    )
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog || editingStation != null) {
        StationEditDialog(
            station = editingStation,
            onDismiss = {
                showAddDialog = false
                editingStation = null
            },
            onSave = { name: String, url: String ->
                if (editingStation != null) {
                    viewModel.updateStation(editingStation!!.id, name, url)
                } else {
                    viewModel.addCustomStation(name, url)
                }
                showAddDialog = false
                editingStation = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (stationToDelete != null) {
        AlertDialog(
            onDismissRequest = { stationToDelete = null },
            title = { Text("删除电台") },
            text = { Text("确定要删除 ${stationToDelete?.name} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStation(stationToDelete!!)
                        stationToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { stationToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
