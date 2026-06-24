package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Medicine::class,
        Reminder::class,
        ReminderTime::class,
        MedicationDose::class,
        Prescription::class,
        FamilyMember::class,
        ChatMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicalDao(): MedicalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medicare_ai_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.medicalDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: MedicalDao) {
            // Seed a default high-stick profile
            dao.saveUserProfile(
                User(
                    email = "alex@example.com",
                    name = "Alex Johnson",
                    role = "Patient",
                    age = 28,
                    gender = "Male",
                    bloodGroup = "O+",
                    allergies = "Shellfish, Penicillin",
                    medicalConditions = "Mild Hypertension, Acid Reflux",
                    emergencyContacts = "Primary: Dr. Sarah Thompson (+1-555-0199)",
                    currentStreak = 5,
                    onboardingCompleted = true
                )
            )

            // Seed sample family members
            dao.insertFamilyMember(
                FamilyMember(
                    id = 1,
                    name = "Mom",
                    avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDxEzhYA2rCNiRgk-WkNAhOp-s0x0o9LJ2O1r-8sb5KBsKTInMw7-JIQh9mnygb4FnnWOpUCZrw8s7tDEKUsbQbrszjytKCwruHsbl6vqpEEdYax5jFQ-cCOGH8neyaCQS_WuLR-V81zF6hCEN_nrgUlQojjLFmxHt_v99RGrSoxVwiFgWRk_MahCBdRFiweG7JNLspU5ACrNi4DPzLTG6knbLa9XtlxyUvj_40wOoO0qVGvjtFleuLWZj6eGyBc3scUXtn_JWJbkTx",
                    role = "Primary Caretaker",
                    adherenceToday = 100,
                    lastSync = "14 mins ago"
                )
            )
            dao.insertFamilyMember(
                FamilyMember(
                    id = 2,
                    name = "Leo",
                    avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAVAM7b5T8ahVVvR-o3A-W7dgc0daQNKMhuCM6NLGj0h4GJgMy-bbD-XZf8HuzxdektavxX7_TAPYpF54Reok1P1ExdH7E1V3FP0LaCPz8nA09-61qsPYS6u5jUvqt3ZDsUKPP-hVLdjdZuQSAxHqp6lC3njnsFsNFCmoreiSoJMwb5ROu8cEAWPH64RMDO-vyj8unLJ1_K7xqkMKLw5fr1-7jQDKEtpvqrCCw1jk-mPUlUlIaFzj1bVhDRa73pMf4ihIFWsyaHlo9u",
                    role = "Dependent",
                    adherenceToday = 75,
                    lastSync = "Next Dose at 8:00 PM"
                )
            )
            dao.insertFamilyMember(
                FamilyMember(
                    id = 3,
                    name = "Dad",
                    avatarUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAcHMCpIgnZ520XX5s5Y0PIiJCBuXdGPLL0H56743CrETlj_6P_0sj9rQgA5wo36JJ7MLNVnhwtMcemivj7R8zb4BCyHmfKF-hFSd-qER7hsUI7sBrlcuIdWmTfu2JXFhHQbMEbQDbCCOyF5ZsSxetRn8WAcMCuVipm3l2M13yjtXQJnhhKjmwS9FWC6LhdSzOxntQgMTnmvlKOPGiYCM3xrj4oJRwNSYF-bfPHQhkpvAXwL_28KIn1r_d_iqcYxVVagglU-IJwnZhs",
                    role = "Shared Access",
                    adherenceToday = 100,
                    lastSync = "On Track"
                )
            )

            // Seed default clinical drugs references
            val initialMedicines = listOf(
                Medicine(
                    id = "lipitor",
                    name = "Lipitor",
                    brandName = "Lipitor",
                    genericName = "Atorvastatin Calcium",
                    usages = "High cholesterol prevention, coronary heart disease risk Reduction",
                    dosage = "10mg to 80mg once daily",
                    instructions = "Take once daily with or without food, preferably in the evening.",
                    sideEffects = "Muscle pain, diarrhea, joint discomfort, stuffy nose",
                    warnings = "Do not take if pregnant or with active liver disease. Avoid grapefruit juice in large quantities.",
                    type = "Tablet",
                    strengthOptions = "10mg, 20mg, 40mg"
                ),
                Medicine(
                    id = "paracetamol",
                    name = "Paracetamol",
                    brandName = "Calpol / Tylenol",
                    genericName = "Acetaminophen",
                    usages = "Relief of mild to moderate pain (headache, toothache, muscle aches) and reduction of fever.",
                    dosage = "500mg to 1000mg every 4-6 hours (Max 4g/day)",
                    instructions = "Can be taken with or without food. Absorption is faster on an empty stomach.",
                    sideEffects = "Typically well tolerated. Rare allergic skin reactions.",
                    warnings = "CRITICAL: Do not exceed 4g (8 tablets) in 24 hours. Risk of severe liver damage if overdose occurs. Refrain from taking other paracetamol products concurrently.",
                    type = "Tablet",
                    strengthOptions = "5000mg"
                ),
                Medicine(
                    id = "ventolin",
                    name = "Ventolin",
                    brandName = "Ventolin Evohaler",
                    genericName = "Salbutamol",
                    usages = "Relief of bronchospasm in patients with asthma, chronic obstructive pulmonary disease (COPD).",
                    dosage = "1 to 2 puffs as needed (100mcg/puff)",
                    instructions = "Shake inhaler well before use. Clean mouthpiece weekly.",
                    sideEffects = "Tremor, headache, localized muscle cramps, palpitations",
                    warnings = "Seek emergency assistance if breathing difficulties do not resolve promptly. Avoid overheating canisters.",
                    type = "Inhaler",
                    strengthOptions = "100mcg"
                ),
                Medicine(
                    id = "metformin",
                    name = "Metformin",
                    brandName = "Glucophage",
                    genericName = "Metformin Hydrochloride",
                    usages = "First line management of Type 2 Diabetes Mellitus; improves insulin sensitivity.",
                    dosage = "500mg to 1000mg twice daily with meals",
                    instructions = "Must be taken with meals to minimize gastrointestinal discomfort.",
                    sideEffects = "Nausea, flatulence, bloating, loose stools, diarrhea.",
                    warnings = "Discontinue before contrast studies. Risk of rare but dangerous lactic acidosis in cases of renal failure.",
                    type = "Tablet",
                    strengthOptions = "500mg, 850mg"
                ),
                Medicine(
                    id = "amoxicillin",
                    name = "Amoxicillin",
                    brandName = "Amoxil",
                    genericName = "Amoxicillin Trihydrate",
                    usages = "Treatment of bacterial infections including otitis media, strep throat, pneumonia, and UTIs.",
                    dosage = "250mg to 500mg three times daily for 7-10 days",
                    instructions = "Take at evenly spaced intervals. Complete the full prescribed course even if symptoms resolve.",
                    sideEffects = "Stomach upset, generalized rash, yeast infection.",
                    warnings = "Contraindicated for individuals with penicillin allergies. Discontinue immediately if hives occur.",
                    type = "Tablet",
                    strengthOptions = "250mg, 500mg"
                )
            )
            dao.insertMedicines(initialMedicines)

            // Seed default reminders to match the demo UI
            val sampleReminderIdLisinopril = dao.insertReminder(
                Reminder(
                    medicineName = "Lisinopril",
                    strength = "10mg",
                    frequency = "Daily",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                    instructions = "Take with water before breakfast",
                    preReminderEnabled = true
                )
            ).toInt()

            val sampleReminderIdMetformin = dao.insertReminder(
                Reminder(
                    medicineName = "Metformin",
                    strength = "500mg",
                    frequency = "Twice Daily",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                    instructions = "During lunch for absorption",
                    preReminderEnabled = true
                )
            ).toInt()

            val sampleReminderIdVitaminD3 = dao.insertReminder(
                Reminder(
                    medicineName = "Vitamin D3",
                    strength = "2000 IU",
                    frequency = "Daily",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                    instructions = "Take with main meal of the day",
                    preReminderEnabled = false
                )
            ).toInt()

            // Seed corresponding times matching the schedules
            dao.insertReminderTimes(
                listOf(
                    ReminderTime(reminderId = sampleReminderIdLisinopril, timeString = "08:00 AM"),
                    ReminderTime(reminderId = sampleReminderIdMetformin, timeString = "12:30 PM"),
                    ReminderTime(reminderId = sampleReminderIdVitaminD3, timeString = "07:15 AM")
                )
            )

            // Seed today's historical dose track logs so the adherence ring displays 75%
            dao.insertDose(
                MedicationDose(
                    reminderId = sampleReminderIdVitaminD3,
                    medicineName = "Vitamin D3",
                    scheduledTime = "07:15 AM",
                    dateString = "2026-06-23",
                    status = "TAKEN",
                    takenTime = "07:15 AM"
                )
            )
            dao.insertDose(
                MedicationDose(
                    reminderId = sampleReminderIdLisinopril,
                    medicineName = "Lisinopril",
                    scheduledTime = "08:00 AM",
                    dateString = "2026-06-23",
                    status = "PENDING"
                )
            )
            dao.insertDose(
                MedicationDose(
                    reminderId = sampleReminderIdMetformin,
                    medicineName = "Metformin",
                    scheduledTime = "12:30 PM",
                    dateString = "2026-06-23",
                    status = "PENDING"
                )
            )

            // Seed a helpful starting notification and a default chat message
            dao.insertChatMessage(
                ChatMessage(
                    id = 1,
                    role = "AI",
                    messageText = "Hello Alex! I'm your MediCare Assistant. How can I help with your medications or health schedule today?"
                )
            )
        }
    }
}
