package com.mchackathon.sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Notification data model
data class Notification(
    val id: String,
    val message: String,
    val timestamp: String
)

class NotificationHistoryActivity : ComponentActivity() {
    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SosTheme {
                NotificationHistoryScreen(notificationViewModel)
            }
        }
    }
}

@Composable
fun NotificationHistoryScreen(notificationViewModel: NotificationViewModel) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()

    // Loading state
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Notifications list
        Scaffold(
            topBar = {
                // Top app bar with back button
                CenterAlignedTopAppBar(
                    title = { Text("Notification History") },
                    navigationIcon = {
                        val context = LocalContext.current
                        IconButton(onClick = {
                            // Using finish() to close the activity
                            (context as? android.app.Activity)?.finish()
                        }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(notifications.size) { index ->
                    val notification = notifications[index]
                    NotificationItem(notification)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp) // Padding between the notifications
            .border(2.dp, Color.Gray, RoundedCornerShape(8.dp)) // Border around the notification
            .clip(RoundedCornerShape(8.dp)) // Ensure content is clipped to rounded corners
            .background(Color(0xFFE0E0E0)) // Gray background applied before padding
            .padding(16.dp) // Padding inside the notification's border (content padding)
    ) {
        Text(text = "ID: ${notification.id}")
        Text(text = "Message: ${notification.message}")
        Text(text = "Time: ${notification.timestamp}")
    }
}

class NotificationViewModel : androidx.lifecycle.ViewModel() {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Simulated network call to fetch notifications
    fun fetchNotifications() {
        _isLoading.value = true
        viewModelScope.launch {
            // Simulate network delay
            delay(2000L)

            // Mocked response
            _notifications.value = listOf(
                Notification("1", "Emergency Alert Sent", "2025-03-29 10:00"),
                Notification("2", "Help is on the way", "2025-03-29 10:05")
            )

            _isLoading.value = false
        }
    }

    init {
        fetchNotifications()
    }
}

// Define your custom color scheme
val primaryColor = Color(0xFFFF4D4D) // Red
val secondaryColor = Color(0xFFB71C1C) // Dark Red

val colorScheme = lightColorScheme(
    primary = primaryColor,
    secondary = secondaryColor,
)

@Composable
fun SosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class) // OptIn annotation for experimental usage
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null // Make it optional here
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon ?: {}, // Provide a default empty composable if null
        modifier = Modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.mediumTopAppBarColors()
    )
}
