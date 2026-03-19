package com.heartandbrain.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VideoOnly
import androidx.activity.result.PickVisualMediaRequest
import androidx.lifecycle.lifecycleScope
import com.heartandbrain.app.data.AppDatabase
import com.heartandbrain.app.data.Vlog
import com.heartandbrain.app.ui.HomeScreen
import com.heartandbrain.app.ui.theme.HeartAndBrainTheme
import com.heartandbrain.app.worker.ProcessingWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pickVideo = registerForActivityResult(PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val vlogId = db.vlogDao().insert(Vlog(filePath = uri.toString()))
            ProcessingWorker.enqueue(this@MainActivity, vlogId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeartAndBrainTheme {
                HomeScreen(
                    onFabClick = { pickVideo.launch(PickVisualMediaRequest(VideoOnly)) },
                )
            }
        }
    }
}
