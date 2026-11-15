package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analytics.FirebaseAnalyticsService
import com.example.data.AppDatabase
import com.example.data.NexoraRepository
import com.example.data.firebase.FirestoreOrderRepository
import com.example.data.firebase.FirestoreSellerRepository
import com.example.data.firebase.FirestoreUserRepository
import com.example.ui.NexoraAppContent
import com.example.ui.theme.NexoraTheme
import com.example.viewmodel.MarketViewModel
import com.example.viewmodel.MarketViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val databaseId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val database = AppDatabase.getDatabase(this)
        val repository = NexoraRepository(database)
        val userRepository = FirestoreUserRepository(firestore)
        val sellerRepository = FirestoreSellerRepository(firestore)
        val orderRepository = FirestoreOrderRepository(firestore)
        val analyticsService = FirebaseAnalyticsService(applicationContext)

        val factory = MarketViewModelFactory(
            repository = repository,
            analyticsService = analyticsService,
            userRepository = userRepository,
            sellerRepository = sellerRepository,
            orderRepository = orderRepository
        )
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
