package com.mchackathon.sos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

class RegistrationActivity : ComponentActivity() {

    // Name of the local file where we'll store user data
    private val fileName = "user_data.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataFile = File(filesDir, fileName)

        // 1. Check if the file exists (and optionally if it has content)
        if (dataFile.exists() && dataFile.readText().isNotBlank()) {
            // If data file already exists, go directly to SosActivity
            startActivity(Intent(this, SosActivity::class.java))
            finish()
            return
        }

        // Otherwise, show the registration screen
        enableEdgeToEdge()
        setContent {
            RegistrationScreen(
                onRegister = { firstName, lastName, mobile, eContactName, eContactMobile ->
                    // Basic validation
                    if (firstName.isBlank() || lastName.isBlank() || mobile.isBlank() ||
                        eContactName.isBlank() || eContactMobile.isBlank()) {
                        Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                    } else {
                        // 2. Save details in local file
                        saveUserDetails(
                            dataFile,
                            firstName,
                            lastName,
                            mobile,
                            eContactName,
                            eContactMobile
                        )

                        // 3. Navigate to SosActivity
                        startActivity(Intent(this, SosActivity::class.java))
                        finish()
                    }
                }
            )
        }
    }

    /**
     * Writes user details to a local file in internal storage.
     */
    private fun saveUserDetails(
        dataFile: File,
        firstName: String,
        lastName: String,
        mobile: String,
        eContactName: String,
        eContactMobile: String
    ) {
        // Example: write them in a simple comma-separated form:
        // firstName,lastName,mobile,eContactName,eContactMobile
        // Or you could write JSON or any format you prefer
        val combinedData = listOf(
            firstName,
            lastName,
            mobile,
            eContactName,
            eContactMobile
        ).joinToString(separator = ",")

        // Write text to the file (overwriting existing content, if any)
        dataFile.writeText(combinedData)
    }
}

/**
 * Composable screen that collects user details.
 */
@Composable
fun RegistrationScreen(
    onRegister: (String, String, String, String, String) -> Unit
) {
    // State for each input field
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactMobile by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = emergencyContactName,
                onValueChange = { emergencyContactName = it },
                label = { Text("Emergency Contact Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = emergencyContactMobile,
                onValueChange = { emergencyContactMobile = it },
                label = { Text("Emergency Contact Mobile") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    // Invoke callback to handle registration logic
                    onRegister(
                        firstName,
                        lastName,
                        mobileNumber,
                        emergencyContactName,
                        emergencyContactMobile
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }
        }
    }
}
