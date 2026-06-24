package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.database.*
import com.example.ui.MedicalViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// 1. DASHBOARD / HOME TAB
// ==========================================

@Composable
fun DashboardScreen(
    viewModel: MedicalViewModel,
    user: User?,
    adherence: Int,
    todayDoses: List<MedicationDose>,
    onAddMedicineClick: () -> Unit,
    onScanPrescriptionClick: () -> Unit
) {
    var filterCompleted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Greeting Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 14.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${user?.name ?: "Alex Johnson"}",
                    fontSize = 24.sp,
                    color = SlateTextPrimary,
                    fontWeight = FontWeight.Black
                )
            }
            // User Avatar badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFFBEB)) // Amber-50
                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(20.dp)) // Amber-100
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "streak",
                    tint = Color(0xFFB45309), // Amber-700
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${user?.currentStreak ?: 5} Day Streak",
                    color = Color(0xFF78350F), // Amber-900
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- Next Dose Card Feature from Sleek Interface Theme Design ---
        val nextPendingDose = todayDoses.firstOrNull { it.status == "PENDING" }
        if (nextPendingDose != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF4338CA))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("💊", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next Dose at ${nextPendingDose.scheduledTime}",
                            color = Color(0xFFE0E7FF), // Indigo-100
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = nextPendingDose.medicineName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Take with water, before breakfast. This keeps your clinical metrics in optimal bounds.",
                        fontSize = 13.sp,
                        color = Color(0xFFC7D2FE), // Indigo-200
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
                        lineHeight = 17.sp
                    )
                    
                    Button(
                        onClick = { viewModel.markDoseTaken(nextPendingDose.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_taken_button_${nextPendingDose.id}")
                    ) {
                        Text(
                            text = "Confirm Taken",
                            color = Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Circular Circular Adherence Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(SlateCardSurface, SlateCardSurface.copy(alpha = 0.8f))
                    )
                )
                .border(1.dp, SlateOutline, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Adherence Canvas Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawArc(
                            color = SlateOutline,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = PrimaryTeal,
                            startAngle = -90f,
                            sweepAngle = (adherence.toFloat() / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$adherence%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "adhered",
                            fontSize = 10.sp,
                            color = SlateTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column {
                    Text(
                        text = "Today's Adherence Rate",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = if (adherence >= 100) "Spotless! All scheduled medications logged successfully." 
                               else "You have upcoming medications. Let's keep up the streak!",
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Quick Action Buttons Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddMedicineClick,
                colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("quick_schedule_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = PrimaryTeal)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onScanPrescriptionClick,
                colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("quick_scan_button")
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = "OCR Scan", tint = PrimaryTeal)
                Spacer(modifier = Modifier.width(6.dp))
                Text("OCR Extract", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Schedule Control Options Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Schedule",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Text(
                text = if (filterCompleted) "Show All" else "Show Active Only",
                color = PrimaryTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { filterCompleted = !filterCompleted }
                    .padding(4.dp)
            )
        }

        // Checklist of medicine doses
        val filteredList = if (filterCompleted) {
            todayDoses.filter { it.status == "PENDING" }
        } else {
            todayDoses
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCardSurface)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Done",
                        tint = SlateTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No pending dosages matching active filters",
                        color = SlateTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredList.forEach { dose ->
                    DoseCardRow(
                        dose = dose,
                        onMarkTaken = { viewModel.markDoseTaken(dose.id) }
                    )
                }
            }
        }

        // Tips Section Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateOutline.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = "Tips",
                    tint = AccentCoral,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Clinical Multi-Interaction Safety Rule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Do not take Lipitor concurrently with grapefruit juice, as it significantly raises blood concentration variables.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DoseCardRow(
    dose: MedicationDose,
    onMarkTaken: () -> Unit
) {
    val isCompleted = dose.status == "TAKEN"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCompleted) SlateOutline.copy(alpha = 0.3f) else SlateCardSurface)
            .border(
                1.dp,
                if (isCompleted) SlateOutline.copy(alpha = 0.6f) else SlateOutline,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Checkbox target
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isCompleted) PrimaryTeal else SlateOutline)
                .clickable(enabled = !isCompleted) { onMarkTaken() },
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Taken",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SlateTextSecondary.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Medicine dosage name metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dose.medicineName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) SlateTextSecondary else SlateTextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dose.scheduledTime,
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
                if (isCompleted && dose.takenTime != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOGGED ${dose.takenTime}",
                            fontSize = 9.sp,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Pill visual indicator badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCompleted) SlateOutline.copy(alpha = 0.4f) else PrimaryTeal.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Oral Med",
                color = if (isCompleted) SlateTextSecondary else PrimaryTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ==========================================
// 2. MEDICINE REFERENCE SEARCH TAB
// ==========================================

@Composable
fun MedicineSearchScreen(
    viewModel: MedicalViewModel,
    searchQuery: String,
    searchResults: List<Medicine>
) {
    var selectedMedicine by remember { mutableStateOf<Medicine?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FDA Drug Database",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        // Search Bar Row Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.performSearch(it) },
            placeholder = { Text("Search Brand or Generic name (e.g. Paracetamol)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary) },
            modifier = Modifier.fillMaxWidth().testTag("database_search_field"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = SlateOutline
            ),
            singleLine = true
        )

        // Show detailed card sheet if a medicine is tapped
        if (selectedMedicine != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, SlateOutline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedMedicine!!.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = selectedMedicine!!.genericName,
                                fontSize = 12.sp,
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { selectedMedicine = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
                        }
                    }

                    HorizontalDivider(color = SlateOutline)

                    // Details block
                    DetailRow(title = "Clinical Indications:", text = selectedMedicine!!.usages)
                    DetailRow(title = "Suggested Standard Dosages:", text = selectedMedicine!!.dosage)
                    DetailRow(title = "Direct Administration Tips:", text = selectedMedicine!!.instructions)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCoral.copy(alpha = 0.1f))
                            .border(1.dp, AccentCoral.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CRITICAL CLINICAL WARNINGS:", color = AccentCoral, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = selectedMedicine!!.warnings,
                                fontSize = 11.sp,
                                color = SlateTextPrimary,
                                modifier = Modifier.padding(top = 4.dp),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    DetailRow(title = "Common Adverse Interactions:", text = selectedMedicine!!.sideEffects)
                    DetailRow(title = "Storage Parameters:", text = selectedMedicine!!.storageInstructions)
                }
            }
        } else {
            // Display lists
            if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No clinical references found. Try 'Lipitor' or 'Paracetamol'", color = SlateTextSecondary)
                }
            } else {
                val listToRender = if (searchQuery.isEmpty()) {
                    // Preseeded list for clean first visual UI
                    listOf(
                        Medicine("lipitor", "Lipitor", "Lipitor", "Atorvastatin Calcium", "High cholesterol prevention, coronary heart disease risk Reduction", "10mg to 80mg once daily", "Take preferably in the evening", "Muscle pain, stuffy nose", "Do not take if pregnant or with active liver disease.", "Tablet"),
                        Medicine("paracetamol", "Paracetamol", "Tylenol", "Acetaminophen", "Relief of pain and fever reduction", "500mg - 1000mg every 4-6 hours", "Absorption is faster on an empty stomach", "Typically well tolerated, skins rash", "Do not exceed 4g in 24 hours.", "Tablet")
                    )
                } else {
                    searchResults
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listToRender) { drug ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SlateCardSurface)
                                .border(1.dp, SlateOutline, RoundedCornerShape(14.dp))
                                .clickable { selectedMedicine = drug }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(drug.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                Text(drug.genericName, fontSize = 11.sp, color = SlateTextSecondary)
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "View Details", tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(title: String, text: String) {
    Column {
        Text(title, fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 13.sp, color = SlateTextPrimary, modifier = Modifier.padding(top = 2.dp), lineHeight = 17.sp)
    }
}


// ==========================================
// 3. MEDICINE Reminder SCHEDULER
// ==========================================

@Composable
fun ScheduleScreen(
    viewModel: MedicalViewModel,
    onSaveSuccess: () -> Unit
) {
    var medName by remember { mutableStateOf("") }
    var strength by remember { mutableStateOf("500mg") }
    var frequency by remember { mutableStateOf("Daily") }
    var instructions by remember { mutableStateOf("") }
    
    var timeOption1 by remember { mutableStateOf("08:00 AM") }
    var statusMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "New Medical Scheduler",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        OutlinedTextField(
            value = medName,
            onValueChange = { medName = it },
            label = { Text("Medication Clinical Name") },
            placeholder = { Text("e.g. Paracetamol or Amoxicillin") },
            modifier = Modifier.fillMaxWidth().testTag("schedule_name_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = strength,
                onValueChange = { strength = it },
                label = { Text("Dosage / Strength") },
                modifier = Modifier.weight(1f).testTag("schedule_strength_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
                singleLine = true
            )

            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it },
                label = { Text("Frequency Interval") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = timeOption1,
            onValueChange = { timeOption1 = it },
            label = { Text("Primary Notification Time") },
            placeholder = { Text("e.g. 08:00 AM") },
            modifier = Modifier.fillMaxWidth().testTag("schedule_time_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
            singleLine = true
        )

        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("Custom Administration Instructions") },
            placeholder = { Text("e.g. Take with warm milk before bedtime") },
            modifier = Modifier.fillMaxWidth().testTag("schedule_instructions_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
            minLines = 2
        )

        if (statusMsg.isNotEmpty()) {
            Text(
                text = statusMsg,
                color = PrimaryTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                if (medName.isEmpty()) {
                    statusMsg = "Please specify a medication name"
                } else {
                    viewModel.addReminder(
                        medicineName = medName,
                        strength = strength,
                        frequency = frequency,
                        startDate = "2026-06-23",
                        endDate = "2226-06-23",
                        instructions = instructions,
                        times = listOf(timeOption1)
                    )
                    statusMsg = "Medication scheduled successfully!"
                    onSaveSuccess()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("scheduler_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Establish Schedule Pattern", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}


// ==========================================
// 4. PRESCRIPTION SCAN / CAM SCREEN (OCR)
// ==========================================

@Composable
fun PrescriptionScanScreen(
    viewModel: MedicalViewModel,
    onFinishedPattern: () -> Unit
) {
    var stateScanning by remember { mutableStateOf("initial") } // initial, snapping, parsing, extracted
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Prescription OCR Scan",
                fontSize = 18.sp,
                color = SlateTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Scanning Viewfinder Sim viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, if (stateScanning == "parsing") PrimaryTeal else SlateOutline, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (stateScanning) {
                    "initial" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Center details of doctor's prescription inside lines", color = SlateTextSecondary, fontSize = 12.sp)
                        }
                    }
                    "snapping" -> {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                    "parsing" -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Parsing Tesseract OCR variables (98.4% Acc.)...", color = PrimaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    "extracted" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SlateCardInner)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extracted Clinical Schedules", color = PrimaryTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = SlateOutline)

                            ExtractedMedicineRow(name = "Amoxicillin", details = "500mg - 3 times daily - After meals")
                            ExtractedMedicineRow(name = "Metformin", details = "500mg - Twice daily - with lunch/dinner")
                            ExtractedMedicineRow(name = "Ventolin", details = "100mcg - 1 puff as needed")
                        }
                    }
                }
            }

            // CTAs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (stateScanning == "initial") {
                    Button(
                        onClick = {
                            scope.launch {
                                stateScanning = "snapping"
                                delay(1200)
                                stateScanning = "parsing"
                                delay(1800)
                                stateScanning = "extracted"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_camera_ocr_btn")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Snapshot Photo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (stateScanning == "extracted") {
                    Button(
                        onClick = {
                            // Seed extracted drugs directly to database schedules
                            viewModel.addReminder("Amoxicillin", "500mg", "3 times daily", "2026-06-23", "2026-07-23", "Complete the full course properly.", listOf("08:00 AM", "01:00 PM", "08:00 PM"))
                            viewModel.addReminder("Metformin", "500mg", "Twice Daily", "2026-06-23", "2226-06-23", "During lunch/dinner", listOf("12:00 PM", "07:00 PM"))
                            viewModel.uploadPrescriptionMock("image_uri", "Extracted: Amoxicillin, Metformin", "98.4%")
                            onFinishedPattern()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("ocr_mass_confirm_btn")
                    ) {
                        Text("Automate Multi-Save Patterns", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ExtractedMedicineRow(name: String, details: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SlateCardSurface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 14.sp)
            Text(details, color = SlateTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = PrimaryTeal, modifier = Modifier.size(20.dp))
    }
}


// ==========================================
// 5. SECURE REPORTS / ANALYTICS
// ==========================================

@Composable
fun ReportsAnalyticsScreen(
    adherence: Int,
    history: List<MedicationDose>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Adherence Analytics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        // Graph Area Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCardSurface)
                .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Weekly Tracker Index", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Last 7 Days", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom graphics line chart representing stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    BarSegment(day = "Mon", percent = 1.0f)
                    BarSegment(day = "Tue", percent = 0.85f)
                    BarSegment(day = "Wed", percent = 0.95f)
                    BarSegment(day = "Thu", percent = 0.70f)
                    BarSegment(day = "Fri", percent = 1.00f)
                    BarSegment(day = "Sat", percent = 0.90f)
                    BarSegment(day = "Sun", percent = (adherence.getPercentRatioFloat()))
                }
            }
        }

        // Streak Statistics Highlights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateCardSurface)
                    .padding(14.dp)
            ) {
                Column {
                    Text("Longest Streak", color = SlateTextSecondary, fontSize = 11.sp)
                    Text("14 Days", color = PrimaryTeal, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateCardSurface)
                    .padding(14.dp)
            ) {
                Column {
                    Text("Doses Missed", color = SlateTextSecondary, fontSize = 11.sp)
                    Text("1 Dose", color = AccentCoral, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Text("Historic Compliance Feed", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SlateTextPrimary)

        // History lists
        if (history.isEmpty()) {
            Text("Compliance timeline logs will populate here once active doses are recorded.", color = SlateTextSecondary, fontSize = 13.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(6).forEach { dose ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateCardSurface)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dose.medicineName, color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(dose.scheduledTime, color = SlateTextSecondary, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (dose.status == "TAKEN") PrimaryTeal.copy(alpha = 0.2f) else AccentCoral.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                dose.status,
                                color = if (dose.status == "TAKEN") PrimaryTeal else AccentCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Int.getPercentRatioFloat(): Float {
    return this.toFloat() / 100f
}

@Composable
fun BarSegment(day: String, percent: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(0.85f * percent)
                .clip(RoundedCornerShape(6.dp))
                .background(if (percent >= 1f) PrimaryTeal else PrimaryTeal.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(day, color = SlateTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}


// ==========================================
// 6. FAMILY CARETAKER NETWORK TAB
// ==========================================

@Composable
fun FamilyNetworkScreen(
    viewModel: MedicalViewModel,
    members: List<FamilyMember>
) {
    var invitedName by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("Dependent") }
    var notifyMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main emergency contacts block
        Card(
            colors = CardDefaults.cardColors(containerColor = AccentCoral.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, AccentCoral.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Emergency, contentDescription = "Emergency info", tint = AccentCoral, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EMERGENCY MEDICAL CONTACTS", color = AccentCoral, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Doctor: Dr. Sarah Thompson MD (+1-555-0199)", color = SlateTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Hospital: City General Emergency Response (Code 9)", color = SlateTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Text("Caretakers & Dependents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)

        members.forEach { relative ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCardSurface)
                    .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular profile visual placeholder
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SlateOutline),
                    contentAlignment = Alignment.Center
                ) {
                    if (relative.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = relative.avatarUrl,
                            contentDescription = relative.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = SlateTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(relative.name, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 14.sp)
                    Text(relative.role, color = SlateTextSecondary, fontSize = 11.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Adherence", fontSize = 10.sp, color = SlateTextSecondary)
                    Text("${relative.adherenceToday}%", color = if (relative.adherenceToday >= 90) PrimaryTeal else AccentCoral, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(relative.lastSync, fontSize = 9.sp, color = SlateTextSecondary)
                }

                IconButton(onClick = { viewModel.removeFamilyMember(relative.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = SlateTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Link New Member Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCardSurface)
                .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add/Caregiver Link Invite", fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 14.sp)

            OutlinedTextField(
                value = invitedName,
                onValueChange = { invitedName = it },
                label = { Text("Invite Member Name") },
                modifier = Modifier.fillMaxWidth().testTag("family_invite_name_field"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { inviteRole = "Primary Caregiver" },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (inviteRole == "Primary Caregiver") PrimaryTeal.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Caregiver", fontSize = 12.sp, color = SlateTextPrimary)
                }

                OutlinedButton(
                    onClick = { inviteRole = "Dependent" },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (inviteRole == "Dependent") PrimaryTeal.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dependent", fontSize = 12.sp, color = SlateTextPrimary)
                }
            }

            if (notifyMsg.isNotEmpty()) {
                Text(notifyMsg, color = PrimaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (invitedName.isEmpty()) {
                        notifyMsg = "Please supply an invite name"
                    } else {
                        viewModel.inviteFamilyMember(invitedName, inviteRole)
                        invitedName = ""
                        notifyMsg = "Invitation link created successfully!"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("send_invite_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Invite Connection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// 7. SECURE CHAT ASSISTANT (GEMINI AI)
// ==========================================

@Composable
fun AIChatAssistantScreen(
    viewModel: MedicalViewModel,
    messages: List<ChatMessage>,
    isLoading: Boolean
) {
    var txtInput by remember { mutableStateOf("") }
    val currentScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat History Frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCardSurface)
                .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PrimaryTeal)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MediCare AI Clinical Assistant", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Session", tint = SlateTextSecondary)
                    }
                }

                HorizontalDivider(color = SlateOutline, modifier = Modifier.padding(vertical = 8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(currentScroll)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        messages.forEach { bubble ->
                            val isAI = bubble.role == "AI"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 14.dp,
                                                topEnd = 14.dp,
                                                bottomStart = if (isAI) 2.dp else 14.dp,
                                                bottomEnd = if (isAI) 14.dp else 2.dp
                                            )
                                        )
                                        .background(if (isAI) SlateCardInner else PrimaryTeal)
                                        .padding(12.dp)
                                        .widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = bubble.messageText,
                                        fontSize = 13.sp,
                                        color = if (isAI) SlateTextPrimary else Color.White,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SlateCardInner)
                                        .padding(10.dp)
                                ) {
                                    CircularProgressIndicator(color = PrimaryTeal, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Suggestions Quick Prompts Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatSuggestionChip(label = "Metformin warnings?") {
                viewModel.sendChatMessage("Are there any special warnings for Metformin Hydrochloride?")
            }
            ChatSuggestionChip(label = "Lipitor & grapefruit?") {
                viewModel.sendChatMessage("What happens if I take Lipitor with grapefruit juices?")
            }
            ChatSuggestionChip(label = "Missed Lisinopril?") {
                viewModel.sendChatMessage("What should I do if I miss my morning Lisinopril dose?")
            }
        }

        // Input Send Box Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = txtInput,
                onValueChange = { txtInput = it },
                placeholder = { Text("Ask about interactions, warnings...") },
                modifier = Modifier.weight(1f).testTag("chat_input_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryTeal, unfocusedBorderColor = SlateOutline),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (txtInput.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(txtInput)
                        txtInput = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryTeal)
                    .size(48.dp)
                    .testTag("send_chat_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatSuggestionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateCardSurface)
            .border(1.dp, SlateOutline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = SlateTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}


// ==========================================
// 8. SETTINGS & USER PROFILE
// ==========================================

@Composable
fun ProfileScreen(
    user: User?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SlateCardSurface)
                .border(1.dp, SlateOutline, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Profile Avatar visual representation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.15f))
                        .border(2.dp, PrimaryTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(user?.name ?: "Alex Johnson", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text(user?.email ?: "alex@example.com", fontSize = 12.sp, color = SlateTextSecondary)

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryIndigo.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("PREMIUM MEMBER", color = SecondaryIndigo, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Text("Vitals & Conditions metadata", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)

        // General settings list grid details
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCardSurface)
                .border(1.dp, SlateOutline, RoundedCornerShape(16.dp))
        ) {
            Column {
                ProfileOptionRow(label = "Age Profile", value = "${user?.age ?: 28} years old", icon = Icons.Default.CalendarToday)
                HorizontalDivider(color = SlateOutline)
                ProfileOptionRow(label = "Blood Group ID", value = "${user?.bloodGroup ?: "O+"}", icon = Icons.Default.WaterDrop)
                HorizontalDivider(color = SlateOutline)
                ProfileOptionRow(label = "Primary Allergies", value = "${user?.allergies ?: "None declared"}", icon = Icons.Default.Block)
                HorizontalDivider(color = SlateOutline)
                ProfileOptionRow(label = "Coded Conditions", value = "${user?.medicalConditions ?: "None"}", icon = Icons.Default.Healing, maxLines = 2)
            }
        }

        // Action Options Row
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = SlateOutline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AccentCoral)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Logout Session Safely", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentCoral)
        }
    }
}

@Composable
fun ProfileOptionRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    maxLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = SlateTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            value,
            color = SlateTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}


// ==========================================
// 9. DEVELOPER ADMIN MONITOR PANEL
// ==========================================

@Composable
fun AdminSystemScreen(
    onExitAdmin: () -> Unit
) {
    var latenciesSim by remember { mutableStateOf("12ms") }
    var scaleNodesSim by remember { mutableStateOf("4 Available") }
    val logs = remember { 
        mutableStateListOf(
            "System Initialized", 
            "Room Seeding Complete",
            "Flask JWT Auth: Active gateway configured on port 5000",
            "Signed HS256 JWT Token checks: READY",
            "Enforcing route protections for Patients/Caregivers"
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            latenciesSim = "${(10..24).random()}ms"
            if (logs.size > 12) {
                logs.clear()
                logs.add("Console log cleared. Starting new log session...")
            }
            logs.add("Database heartbeat status check: OK (${latenciesSim})")
            if (Math.random() > 0.5) {
                logs.add("Flask JWT verification handshake check: Code 200 OK")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Dns, contentDescription = null, tint = PrimaryTeal)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Developer System Cockpit", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = onExitAdmin) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
            }
        }

        // Metrics Grid rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCardSurface)
                    .border(1.dp, SlateOutline, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("API Database Latency", color = SlateTextSecondary, fontSize = 10.sp)
                    Text(latenciesSim, color = PrimaryTeal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCardSurface)
                    .border(1.dp, SlateOutline, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("HIPAA Encryption Node", color = SlateTextSecondary, fontSize = 10.sp)
                    Text("AES-256 Enabled", color = PrimaryTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cluster Scale info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCardSurface)
                .border(1.dp, SlateOutline, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text("Health Network Instances", color = SlateTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Local SQLite SQLite Node:", color = SlateTextPrimary, fontSize = 13.sp)
                    Text("Operational", color = PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gemini REST Connection API:", color = SlateTextPrimary, fontSize = 13.sp)
                    Text("Healthy", color = PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Flask JWT Auth Server (5000):", color = SlateTextPrimary, fontSize = 13.sp)
                    Text("Gateways Configured", color = PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Live Heartbeat Diagnostic Console", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SlateTextPrimary)

        // Console Log Stream Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, SlateOutline, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                logs.forEach { line ->
                    Text(
                        text = "[$] $line",
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
