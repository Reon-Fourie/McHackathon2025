package com.mchackathon.sos

import android.content.Intent
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
import kotlinx.coroutines.delay
import java.io.File

class SosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SosScreen()
        }
    }
}

/**
 * States for our button logic.
 */
private enum class SosState {
    Idle,       // "SOS" (red)
    Counting,   // 5-second countdown (orange)
    Confirmed   // "Help is on the way" (green)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { /* Optional title here, or remove entirely. */ },
                actions = {
                    // Settings icon
                    IconButton(
                        onClick = {
                            val intent = Intent(context, RegistrationActivity::class.java)
                            // This tells RegistrationActivity we want to edit (skip auto-jump to SOS)
                            intent.putExtra("skipCheck", true)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }

                    // Notification icon to navigate to NotificationHistoryActivity
                    IconButton(
                        onClick = {
                            val intent = Intent(context, NotificationHistoryActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notification History"
                        )
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
            // The main "3D" SOS button with countdown
            ThreeDSosButton(
                onIdlePress = { /* first press => start countdown */ },
                onCountingPress = { /* press again => cancel countdown */ },
                onCountdownFinished = {
                    Toast.makeText(context, "Help is on the way!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}


/**
 * A composable that draws a 3D-like button with:
 * - Idle (red) "SOS"
 * - On press: 5-second countdown (orange) + "Press to cancel"
 * - Second press during countdown => cancel & revert to Idle
 * - After countdown ends => "Help is on the way" (green)
 */
@Composable
fun ThreeDSosButton(
    onIdlePress: () -> Unit,
    onCountingPress: () -> Unit,
    onCountdownFinished: () -> Unit
) {
    // Current state of the button
    var sosState by remember { mutableStateOf(SosState.Idle) }

    // Time left in the countdown (only used in Counting state)
    var timeLeft by remember { mutableStateOf(5) }

    // Manage "pressed" for scale animation (only for Idle).
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate scale only in Idle state
    val scale by animateFloatAsState(
        targetValue = when {
            sosState != SosState.Idle -> 1.0f
            isPressed -> 0.85f
            else -> 1.25f
        },
        animationSpec = spring(stiffness = 500f)
    )

    // Start counting down whenever we enter Counting
    if (sosState == SosState.Counting) {
        LaunchedEffect(sosState) {
            timeLeft = 5
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            // If we finished the loop and never got canceled, we confirm
            sosState = SosState.Confirmed
            onCountdownFinished()
        }
    }

    // Decide button text & gradient
    val (buttonText, buttonGradient) = when (sosState) {
        SosState.Idle -> {
            "SOS" to Brush.linearGradient(
                colors = listOf(Color(0xFFFF4D4D), Color(0xFFB71C1C)) // red
            )
        }
        SosState.Counting -> {
            "$timeLeft" to Brush.linearGradient(
                colors = listOf(Color(0xFFFF9800), Color(0xFFE65100)) // orange
            )
        }
        SosState.Confirmed -> {
            "Help is on the way" to Brush.linearGradient(
                colors = listOf(Color(0xFF4CAF50), Color(0xFF087F23)) // green
            )
        }
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                when (sosState) {
                    SosState.Idle -> {
                        sosState = SosState.Counting
                        onIdlePress()
                    }
                    SosState.Counting -> {
                        sosState = SosState.Idle
                        onCountingPress()
                    }
                    SosState.Confirmed -> {
                        // do nothing or revert - ignoring for now
                    }
                }
            }
            .scale(scale)
    ) {
        // The "3D" circle background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val radius = diameter / 2f
            val center = Offset(diameter / 2, diameter / 2)

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = radius,
                center = center + Offset(0f, 4f)
            )

            // Main circle gradient
            drawCircle(
                brush = buttonGradient,
                radius = radius,
                center = center
            )

            // Highlight arc near the top
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = -200f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = center - Offset(radius, radius),
                size = Size(diameter, diameter)
            )
        }

        // Text in the center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Show countdown number or "SOS"/"Help is on the way"
                Text(
                    text = buttonText,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                // If counting, show "Press to cancel" beneath the number
                if (sosState == SosState.Counting) {
                    Spacer(modifier = Modifier.height(4.dp))
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
