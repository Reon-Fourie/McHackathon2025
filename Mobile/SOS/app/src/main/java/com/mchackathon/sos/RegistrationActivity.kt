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
import androidx.compose.foundation.shape.CircleShape
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
 * - Data parsing/saving logic.
 * - A scrollable screen with personal details and multiple emergency contacts.
 * - Pre-population from user_data.txt.
 * - The first emergency contact cannot be removed.
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

        // Parse existing CSV (if any) to pre-fill personal info and contacts.
        val parts = if (dataFile.exists()) dataFile.readText().split(",") else emptyList()
        val existingFirstName = parts.getOrNull(0) ?: ""
        val existingLastName = parts.getOrNull(1) ?: ""
        val existingMobileNumber = parts.getOrNull(2) ?: ""
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
                    // Validate personal fields.
                    if (firstName.isBlank() || lastName.isBlank() || personalMobile.isBlank()) {
                        Toast.makeText(
                            this,
                            "Personal fields cannot be empty!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RegistrationScreen
                    }
                    // Must have at least one contact.
                    if (contacts.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Please add at least one contact!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RegistrationScreen
                    }
                    // Validate that no contact field is blank.
                    if (contacts.any { it.name.isBlank() || it.mobile.isBlank() }) {
                        Toast.makeText(
                            this,
                            "No contact field can be blank!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@RegistrationScreen
                    }

                    // Save CSV.
                    saveUserDetails(dataFile, firstName, lastName, personalMobile, contacts)
                    // Then go to SOS.
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
 * RegistrationScreen that handles personal details and multiple emergency contacts.
 * The content (personal details and contact list) is scrollable, while the bottom bar remains fixed.
 */
@Composable
fun RegistrationScreen(
    initialFirstName: String,
    initialLastName: String,
    initialMobileNumber: String,
    initialContacts: List<Contact>,
    onRegister: (firstName: String, lastName: String, personalMobile: String, contacts: List<Contact>) -> Unit
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var personalMobile by remember { mutableStateOf(initialMobileNumber) }
    var contacts by remember {
        mutableStateOf(
            if (initialContacts.isNotEmpty()) initialContacts else listOf(Contact("", ""))
        )
    }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            // Fixed bottom bar with "Add Another Contact" and "Save" buttons.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { contacts = contacts + Contact("", "") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Another Contact")
                }
                Button(
                    onClick = { onRegister(firstName, lastName, personalMobile, contacts) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Personal details section.
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

            // Emergency contacts section.
            Text("Emergency Contacts", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                contacts.forEachIndexed { index, contact ->
                    val canRemove = index > 0
                    ContactRow(
                        contact = contact,
                        isRemovable = canRemove,
                        onChange = { newContact ->
                            contacts = contacts.mapIndexed { i, old ->
                                if (i == index) newContact else old
                            }
                        },
                        onRemove = { contacts = contacts.filterIndexed { i, _ -> i != index } }
                    )
                }
            }
        }
    }
}

/**
 * A composable row that lets the user edit a single contact's name and mobile.
 * It is wrapped in a Card with a black border.
 * If the contact is removable (i.e. not the first contact), a round Material button with a trash icon is displayed.
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
                    // A round Material button with only a trash icon.
                    Button(
                        onClick = onRemove,
                        shape = CircleShape,
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.size(40.dp)  // Adjust the size as needed.
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove Contact"
                        )
                    }
                }
            }
        }
    }
}
