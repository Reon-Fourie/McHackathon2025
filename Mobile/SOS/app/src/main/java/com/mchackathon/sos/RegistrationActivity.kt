package com.mchackathon.sos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Simple data class to hold a single emergency contact's info.
 */
data class Contact(
    val name: String,
    val mobile: String
)

/**
 * A single-file RegistrationActivity:
 *  - Data parsing / saving logic
 *  - A scrollable screen with personal details + multiple emergency contacts
 *  - Pre-population from user_data.txt
 *  - The first emergency contact cannot be removed, ensuring at least one remains
 *  - White background, black borders around contacts, and a purple trashcan icon for removal
 */
class RegistrationActivity : ComponentActivity() {

    private val fileName = "user_data.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataFile = File(filesDir, fileName)
        val skipCheck = intent.getBooleanExtra("skipCheck", false)

        // If NOT skipping and file is non-empty -> jump to SOS
        if (!skipCheck && dataFile.exists() && dataFile.readText().isNotBlank()) {
            startActivity(Intent(this, SosActivity::class.java))
            finish()
            return
        }

        // Parse existing CSV (if any) to pre-fill
        val parts = if (dataFile.exists()) dataFile.readText().split(",") else emptyList()

        // Personal info: indices [0..2]
        val existingFirstName = parts.getOrNull(0) ?: ""
        val existingLastName = parts.getOrNull(1) ?: ""
        val existingMobileNumber = parts.getOrNull(2) ?: ""

        // Remaining items => chunked in pairs => each contact has (name, mobile)
        val contactsParts = if (parts.size > 3) parts.drop(3) else emptyList()
        val existingContacts = contactsParts.chunked(2).map {
            Contact(
                name = it.getOrElse(0) { "" },
                mobile = it.getOrElse(1) { "" }
            )
        }

        setContent {
            RegistrationScreen(
                initialFirstName = existingFirstName,
                initialLastName = existingLastName,
                initialMobileNumber = existingMobileNumber,
                initialContacts = existingContacts,
                onRegister = { firstName, lastName, personalMobile, contacts ->
                    // Validate personal fields
                    if (firstName.isBlank() || lastName.isBlank() || personalMobile.isBlank()) {
                        Toast.makeText(
                            this,
                            "Personal fields cannot be empty!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RegistrationScreen
                    }
                    // Must have at least one contact
                    if (contacts.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Please add at least one contact!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RegistrationScreen
                    }
                    // Validate that no contact field is blank
                    if (contacts.any { it.name.isBlank() || it.mobile.isBlank() }) {
                        Toast.makeText(
                            this,
                            "No contact field can be blank!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RegistrationScreen
                    }

                    // Save CSV
                    saveUserDetails(dataFile, firstName, lastName, personalMobile, contacts)

                    // Then go to SOS
                    startActivity(Intent(this, SosActivity::class.java))
                    finish()
                }
            )
        }
    }

    /**
     * Writes personal info + all contacts into a CSV line:
     * [firstName, lastName, personalMobile, name1, mobile1, name2, mobile2, ...]
     */
    private fun saveUserDetails(
        dataFile: File,
        firstName: String,
        lastName: String,
        personalMobile: String,
        contacts: List<Contact>
    ) {
        val csvList = mutableListOf(firstName, lastName, personalMobile)
        contacts.forEach { c ->
            csvList.add(c.name)
            csvList.add(c.mobile)
        }
        dataFile.writeText(csvList.joinToString(","))
    }
}

/**
 * RegistrationScreen with personal details, multiple contacts, and a "Save" button.
 */
@Composable
fun RegistrationScreen(
    initialFirstName: String,
    initialLastName: String,
    initialMobileNumber: String,
    initialContacts: List<Contact>,
    onRegister: (
        firstName: String,
        lastName: String,
        personalMobile: String,
        contacts: List<Contact>
    ) -> Unit
) {
    // States for personal details
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var personalMobile by remember { mutableStateOf(initialMobileNumber) }

    // Dynamic list of contacts (name + mobile)
    var contacts by remember {
        mutableStateOf(
            // Ensure there's at least 1 contact. If empty, create one blank contact.
            if (initialContacts.isNotEmpty()) initialContacts else listOf(Contact("", ""))
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White  // Force background to stay white
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // PERSONAL DETAILS
            Text("Personal Details", style = MaterialTheme.typography.titleLarge)
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
                value = personalMobile,
                onValueChange = { personalMobile = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth()
            )

            // EMERGENCY CONTACTS
            Text("Emergency Contacts", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                contacts.forEachIndexed { index, contact ->
                    // The first contact (index = 0) is not removable
                    val canRemove = (index > 0)
                    ContactRow(
                        contact = contact,
                        isRemovable = canRemove,
                        onChange = { newContact ->
                            // Replace the contact at 'index'
                            contacts = contacts.mapIndexed { i, old ->
                                if (i == index) newContact else old
                            }
                        },
                        onRemove = {
                            // Remove this contact if allowed
                            contacts = contacts.filterIndexed { i, _ -> i != index }
                        }
                    )
                }

                Button(
                    onClick = {
                        // Add another blank contact
                        contacts = contacts + Contact(name = "", mobile = "")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Another Contact")
                }
            }

            // REGISTER BUTTON
            Button(
                onClick = {
                    onRegister(firstName, lastName, personalMobile, contacts)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

/**
 * A row (or small column) that lets the user edit a single Contact's name + mobile,
 * with a black border. If isRemovable=false (the first contact), we hide the trashcan icon.
 */
@Composable
fun ContactRow(
    contact: Contact,
    isRemovable: Boolean,
    onChange: (Contact) -> Unit,
    onRemove: () -> Unit
) {
    var name by remember { mutableStateOf(contact.name) }
    var mobile by remember { mutableStateOf(contact.mobile) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.Black),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onChange(contact.copy(name = it))
                },
                label = { Text("Contact Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = {
                    mobile = it
                    onChange(contact.copy(mobile = it))
                },
                label = { Text("Contact Mobile") },
                modifier = Modifier.fillMaxWidth()
            )

            if (isRemovable) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // A trashcan icon button for removing (tinted purple)
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove Contact",
                            tint = Color(0xFF6200EE) // Purple color
                        )
                    }
                }
            }
        }
    }
}
