package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.NexoraRepository
import com.example.ui.NexoraAppContent
import com.example.ui.theme.NexoraTheme
import com.example.viewmodel.MarketViewModel
import com.example.viewmodel.MarketViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val database = AppDatabase.getDatabase(this)
    val repository = NexoraRepository(database)
    val factory = MarketViewModelFactory(repository)
    val viewModel: MarketViewModel by viewModels { factory }

    enableEdgeToEdge()
    setContent {
      val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
      NexoraTheme(darkTheme = darkTheme) {
        NexoraAppContent(viewModel = viewModel)
      }
    }
  }
}

