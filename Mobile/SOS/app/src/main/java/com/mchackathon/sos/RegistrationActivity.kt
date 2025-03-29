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

    private val fileName = "user_data.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The local data file
        val dataFile = File(filesDir, fileName)

        // Should we skip the normal auto-jump to SOS?
        // (true if user clicked "Settings" in SosActivity to edit)
        val skipCheck = intent.getBooleanExtra("skipCheck", false)

        // If NOT skipping and the file is non-empty, jump to SOS
        if (!skipCheck && dataFile.exists() && dataFile.readText().isNotBlank()) {
            startActivity(Intent(this, SosActivity::class.java))
            finish()
            return
        }

        // Otherwise, parse existing data (if any) to prefill the text fields
        val parts = if (dataFile.exists()) dataFile.readText().split(",") else emptyList()
        val existingFirstName = parts.getOrNull(0) ?: ""
        val existingLastName = parts.getOrNull(1) ?: ""
        val existingMobileNumber = parts.getOrNull(2) ?: ""
        val existingEContactName = parts.getOrNull(3) ?: ""
        val existingEContactMobile = parts.getOrNull(4) ?: ""

        // Set the content with pre-filled data (if found)
        setContent {
            RegistrationScreen(
                initialFirstName = existingFirstName,
                initialLastName = existingLastName,
                initialMobileNumber = existingMobileNumber,
                initialEContactName = existingEContactName,
                initialEContactMobile = existingEContactMobile,
                onRegister = { firstName, lastName, mobile, eContactName, eContactMobile ->
                    // Validate
                    if (
                        firstName.isBlank() || lastName.isBlank() || mobile.isBlank() ||
                        eContactName.isBlank() || eContactMobile.isBlank()
                    ) {
                        Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Write the new data
                        saveUserDetails(
                            dataFile,
                            firstName,
                            lastName,
                            mobile,
                            eContactName,
                            eContactMobile
                        )

                        // Then go to SOS
                        startActivity(Intent(this, SosActivity::class.java))
                        finish()
                    }
                }
            )
        }
    }

    private fun saveUserDetails(
        dataFile: File,
        firstName: String,
        lastName: String,
        mobile: String,
        eContactName: String,
        eContactMobile: String
    ) {
        // e.g. first,last,mobile,eContactName,eContactMobile
        val combinedData = listOf(
            firstName,
            lastName,
            mobile,
            eContactName,
            eContactMobile
        ).joinToString(",")

        dataFile.writeText(combinedData)
    }
}

/**
 * RegistrationScreen that can be **pre-filled** with existing data.
 */
@Composable
fun RegistrationScreen(
    initialFirstName: String,
    initialLastName: String,
    initialMobileNumber: String,
    initialEContactName: String,
    initialEContactMobile: String,
    onRegister: (String, String, String, String, String) -> Unit
) {
    // States are initialized with the "existing" data
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var mobileNumber by remember { mutableStateOf(initialMobileNumber) }
    var emergencyContactName by remember { mutableStateOf(initialEContactName) }
    var emergencyContactMobile by remember { mutableStateOf(initialEContactMobile) }

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
                    // Pass back the final values
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
