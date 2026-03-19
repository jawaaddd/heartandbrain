package com.heartandbrain.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heartandbrain.app.data.Clip
import com.heartandbrain.app.data.ClipType
import com.heartandbrain.app.ui.theme.Subtle

@Composable
fun HomeScreen(
    onFabClick: () -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val pinned by vm.pinnedClips.collectAsState(initial = emptyList())
    val recent by vm.recentClips.collectAsState(initial = emptyList())
    val processingState by vm.processingState.collectAsState(initial = ProcessingState.Idle)
    var selectedClip by remember { mutableStateOf<Clip?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload vlog")
            }
        },
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Processing / error banner
            if (processingState != ProcessingState.Idle) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ProcessingBanner(processingState)
                }
            }

            if (pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionHeader("Pinned") }
                items(pinned, key = { it.id }) { clip ->
                    ClipCard(clip, onClick = { selectedClip = clip })
                }
            }

            if (recent.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { SectionHeader("Recent") }
                items(recent, key = { it.id }) { clip ->
                    ClipCard(clip, onClick = { selectedClip = clip })
                }
            }

            if (pinned.isEmpty() && recent.isEmpty() && processingState == ProcessingState.Idle) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(modifier = Modifier.padding(top = 120.dp, start = 32.dp, end = 32.dp)) {
                        Text(
                            text = "No clips yet.\nTap + to upload your first vlog.",
                            color = Subtle,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
    }

    selectedClip?.let { clip ->
        ClipPlayerModal(
            clip = clip,
            onDismiss = { selectedClip = null },
            onTogglePin = { vm.togglePin(clip) },
        )
    }
}

@Composable
private fun ProcessingBanner(state: ProcessingState) {
    val isError = state is ProcessingState.Failed
    val label = when (state) {
        is ProcessingState.Running -> state.step
        is ProcessingState.Failed -> state.error
        ProcessingState.Idle -> return
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) Color(0xFF2A1010) else Color(0xFF12121F),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                color = if (isError) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = if (isError) 0.dp else 8.dp),
            )
            if (!isError) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF2A2A3A),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 2.dp),
    )
}

@Composable
private fun ClipCard(clip: Clip, onClick: () -> Unit) {
    when (clip.type) {
        ClipType.QUOTE -> QuoteCard(clip = clip, onClick = onClick)
        ClipType.CLIP -> VideoThumbnailCard(clip = clip, onClick = onClick)
    }
}
