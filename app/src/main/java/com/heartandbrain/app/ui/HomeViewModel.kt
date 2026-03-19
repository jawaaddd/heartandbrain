package com.heartandbrain.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.heartandbrain.app.data.AppDatabase
import com.heartandbrain.app.data.Clip
import com.heartandbrain.app.data.ClipRepository
import com.heartandbrain.app.worker.ProcessingWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ClipRepository(AppDatabase.getInstance(app).clipDao())

    val pinnedClips = repo.pinnedClips()
    val recentClips = repo.recentClips()

    fun togglePin(clip: Clip) {
        viewModelScope.launch { repo.setPinned(clip, !clip.isPinned) }
    }

    /** Current processing step label, or null when idle. */
    val processingState: Flow<ProcessingState> =
        WorkManager.getInstance(app).getWorkInfosByTagFlow(ProcessingWorker.TAG).map { infos ->
            val active = infos.firstOrNull {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            }
            val failed = infos.firstOrNull { it.state == WorkInfo.State.FAILED }

            when {
                active != null -> {
                    val step = if (active.state == WorkInfo.State.RUNNING)
                        active.progress.getString(ProcessingWorker.KEY_STEP) ?: "Processing…"
                    else
                        "Queued…"
                    ProcessingState.Running(step)
                }
                failed != null -> {
                    val error = failed.outputData.getString(ProcessingWorker.KEY_ERROR) ?: "Processing failed"
                    ProcessingState.Failed(error)
                }
                else -> ProcessingState.Idle
            }
        }
}

sealed interface ProcessingState {
    data object Idle : ProcessingState
    data class Running(val step: String) : ProcessingState
    data class Failed(val error: String) : ProcessingState
}
