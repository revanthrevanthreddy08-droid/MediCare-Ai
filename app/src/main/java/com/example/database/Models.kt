package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- 1. User & Profile ---
@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val name: String,
    val role: String = "Patient", // Patient, Caregiver, Admin
    val age: Int = 28,
    val gender: String = "Male",
    val bloodGroup: String = "O+",
    val allergies: String = "None declared",
    val medicalConditions: String = "None declared",
    val emergencyContacts: String = "Primary: Dr. Sarah Thompson",
    val currentStreak: Int = 5,
    val onboardingCompleted: Boolean = true
)

// --- 2. Medicine Reference Database ---
@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey val id: String,
    val name: String,
    val brandName: String,
    val genericName: String,
    val usages: String,
    val dosage: String,
    val instructions: String,
    val sideEffects: String,
    val warnings: String,
    val storageInstructions: String = "Store in a cool, dry place. Keep out of reach of children.",
    val type: String = "Tablet", // Tablet, Inhaler, Oral Suspension, vaccines, etc.
    val strengthOptions: String = "500mg"
)

// --- 3. Reminder & Medication Schedule ---
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineName: String,
    val strength: String,
    val frequency: String, // Daily, Twice Daily, Weekly, As needed
    val startDate: String,
    val endDate: String,
    val instructions: String,
    val preReminderEnabled: Boolean = true
)

// --- 4. Sub-times for Reminders ---
@Entity(tableName = "reminder_times")
data class ReminderTime(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val timeString: String // e.g., "08:00 AM", "01:00 PM"
)

// --- 5. Dose History Tracker ---
@Entity(tableName = "medication_history")
data class MedicationDose(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reminderId: Int,
    val medicineName: String,
    val scheduledTime: String,
    val dateString: String, // e.g., "2026-06-23" or "Oct 5, 2024"
    val status: String, // TAKEN, MISSED, PENDING
    val takenTime: String? = null // e.g., "07:15 AM"
)

// --- 6. OCR Prescriptions ---
@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,
    val ocrText: String,
    val accuracy: String = "98.4%",
    val prescriberId: String = "#MED-2209X",
    val dateUploaded: String = "2026-06-23"
)

// --- 7. Family Network ---
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarUrl: String,
    val role: String, // Caregiver, Dependent, Shared Access
    val adherenceToday: Int = 100,
    val lastSync: String = "Just now",
    val allowNotificationSharing: Boolean = true,
    val sharedDashboardEnabled: Boolean = true
)

// --- 8. AI Support Persistent Chats ---
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // AI, USER
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)
