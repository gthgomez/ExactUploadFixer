package com.exactuploadfixer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.exactuploadfixer.export.ExportManager
import com.exactuploadfixer.processing.ImageProcessor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.exactuploadfixer.ui.ExactUploadFixerApp
import com.exactuploadfixer.ui.MainViewModel
import com.workspace.design.AppTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Manual wiring — no DI framework in V1
        // BillingModule is flavor-specific: googlePlay → PlayBillingGateway, amazon → RevenueCatBillingGateway
        val engine = ImageProcessor(applicationContext)
        val billing = BillingModule.create(applicationContext)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(application, engine, billing) as T
        })[MainViewModel::class.java]

        // Clear stale exports from last session (Gemini Phase 4 QA checklist item)
        ExportManager.cleanUpCache(applicationContext)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExactUploadFixerApp(vm = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-verify entitlement on every resume — billing service may have disconnected
        // while backgrounded. Overlay contract: connect() has isReady guard so repeated
        // calls are idempotent.
        viewModel.onResume()
    }
}
