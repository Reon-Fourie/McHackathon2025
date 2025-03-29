package com.mchackathon.sos

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable
fun SosScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current

            // Our main "3D" button with countdown
            ThreeDSosButton(
                onIdlePress = {
                    // First time press => start countdown
                },
                onCountingPress = {
                    // If pressed during countdown => cancel
                },
                onCountdownFinished = {
                    // After 5s => show help is on the way
                    Toast.makeText(context, "Help is on the way!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * A composable that draws a 3D-like button with the following behavior:
 * - Idle (red): shows "SOS".
 * - On press: enters a 5-second countdown (orange). Press again during countdown to cancel.
 * - After 5s un-cancelled, shows "Help is on the way" (green).
 * - Underneath countdown digits, show "Press to cancel".
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

    // Animate scale: bigger => pressed or idle.
    // We'll only scale in Idle state for clarity.
    val scale by animateFloatAsState(
        targetValue = when {
            sosState != SosState.Idle -> 1.0f        // no scale if not idle
            isPressed -> 0.85f                       // pressed in Idle
            else -> 1.25f                            // bigger at rest in Idle
        },
        animationSpec = spring(stiffness = 500f)
    )

    // Start counting down whenever we enter Counting state
    // If the user or system changes state away from Counting,
    // this effect cancels automatically.
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

    // Decide the text + gradient color based on state
    val (buttonText, buttonGradient) = when (sosState) {
        SosState.Idle -> {
            "SOS" to Brush.linearGradient(
                colors = listOf(Color(0xFFFF4D4D), Color(0xFFB71C1C))  // red
            )
        }
        SosState.Counting -> {
            // e.g. "5", "4", "3" ...
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
            .size(200.dp) // base size, we scale it
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                when (sosState) {
                    SosState.Idle -> {
                        // first press => start countdown
                        sosState = SosState.Counting
                        onIdlePress()
                    }
                    SosState.Counting -> {
                        // press again => cancel => go back to idle
                        sosState = SosState.Idle
                        onCountingPress()
                    }
                    SosState.Confirmed -> {
                        // do nothing or revert?
                        // up to you, let's do nothing for now
                    }
                }
            }
            .scale(scale)
    ) {
        // The "3D" look is drawn in the Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val radius = diameter / 2f
            val center = Offset(diameter / 2, diameter / 2)

            // Shadow behind the circle for a 3D lift
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = radius,
                center = center + Offset(0f, 4f)
            )

            // Main circle with the chosen gradient
            // We'll do a top-left to bottom-right linear gradient
            // but you can tweak it as you like
            drawCircle(
                brush = buttonGradient,
                radius = radius,
                center = center
            )

            // Subtle highlight arc near the top
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
                // Main text (countdown or "SOS" or "Help is on the way")
                Text(
                    text = buttonText,
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                // If counting, show "Press to cancel"
                if (sosState == SosState.Counting) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Press to cancel",
                        color = Color.White.copy(alpha = 0.8f),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
