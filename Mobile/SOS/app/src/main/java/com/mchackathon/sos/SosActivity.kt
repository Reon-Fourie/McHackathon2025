package com.mchackathon.sos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class SosActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()

        // Request permission at runtime if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }

        setContent {
            SosScreen(fusedLocationClient)
        }
    }
}

private enum class SosState {
    Idle, Counting, Confirmed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, RegistrationActivity::class.java)
                        intent.putExtra("skipCheck", true)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, NotificationHistoryActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notification History")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            ThreeDSosButton(
                onIdlePress = {},
                onCountingPress = {},
                onCountdownFinished = {
                    getLocationAndSendAlert(context, fusedLocationClient)
                }
            )
        }
    }
}

fun getLocationAndSendAlert(context: android.content.Context, fusedLocationClient: FusedLocationProviderClient) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Location permission not granted", Toast.LENGTH_SHORT).show()
        return
    }

    // Read local file "user_data.txt" from the app's files directory
    val dataFile = File(context.filesDir, "user_data.txt")
    if (!dataFile.exists() || dataFile.readText().isBlank()) {
        Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
        return
    }
    val tokens = dataFile.readText().split(",")
    // Expecting tokens: [firstName, lastName, personalMobile, contact1Name, contact1Mobile, ...]
    val firstName = tokens.getOrNull(0) ?: ""
    val lastName = tokens.getOrNull(1) ?: ""
    val personalMobile = tokens.getOrNull(2) ?: ""
    // Extract mobile numbers from contacts (i.e. tokens at positions 4, 6, 8, …)
    val contacts = mutableListOf<String>()
    for (i in 3 until tokens.size step 2) {
        // Make sure there's a mobile number following the contact name
        val mobile = tokens.getOrNull(i + 1) ?: ""
        if (mobile.isNotBlank()) {
            contacts.add(mobile)
        }
    }

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latLong = "${location.latitude},${location.longitude}"
                val json = JSONObject().apply {
                    put("name", firstName)
                    put("surname", lastName)
                    put("coordinates", latLong)
                    put("callMeAt", personalMobile)
                    // Keeping emergencyType hardcoded
                    put("emergencyType", "Send an ambulance")
                    put("contacts", org.json.JSONArray(contacts))
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val client = OkHttpClient()
                        val mediaType = "application/json".toMediaTypeOrNull()
                        val body = json.toString().toRequestBody(mediaType)
                        val request = Request.Builder()
                            .url("https://app-swiftly-brcffxgscnhpejdg.southafricanorth-01.azurewebsites.net/sos") // Replace with actual endpoint URL
                            .post(body)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Help is on the way!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Failed to send alert", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
}

@Composable
fun ThreeDSosButton(
    onIdlePress: () -> Unit,
    onCountingPress: () -> Unit,
    onCountdownFinished: () -> Unit
) {
    var sosState by remember { mutableStateOf(SosState.Idle) }
    var timeLeft by remember { mutableStateOf(5) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            sosState != SosState.Idle -> 1.0f
            isPressed -> 0.85f
            else -> 1.25f
        },
        animationSpec = spring(stiffness = 500f), label = "scaleAnim"
    )

    if (sosState == SosState.Counting) {
        LaunchedEffect(sosState) {
            timeLeft = 5
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            sosState = SosState.Confirmed
            onCountdownFinished()
        }
    }

    val (buttonText, buttonGradient) = when (sosState) {
        SosState.Idle -> "SOS" to Brush.linearGradient(listOf(Color(0xFFFF4D4D), Color(0xFFB71C1C)))
        SosState.Counting -> "$timeLeft" to Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFE65100)))
        SosState.Confirmed -> "Help is on the way" to Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF087F23)))
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clickable(interactionSource = interactionSource, indication = null) {
                when (sosState) {
                    SosState.Idle -> {
                        sosState = SosState.Counting
                        onIdlePress()
                    }
                    SosState.Counting -> {
                        sosState = SosState.Idle
                        onCountingPress()
                    }
                    SosState.Confirmed -> {}
                }
            }
            .scale(scale)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val radius = diameter / 2f
            val center = Offset(diameter / 2, diameter / 2)
            drawCircle(Color.Black.copy(alpha = 0.4f), radius, center + Offset(0f, 4f))
            drawCircle(buttonGradient, radius, center)
            drawArc(
                Color.White.copy(alpha = 0.15f), -200f, 100f, false,
                topLeft = center - Offset(radius, radius), size = Size(diameter, diameter)
            )
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (sosState == SosState.Counting) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Press to cancel",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
