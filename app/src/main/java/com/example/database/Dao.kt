package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {

    // --- User & Profile ---
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserProfile(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(user: User)

    // --- Medicine Search ---
    @Query("SELECT * FROM medicines")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE name LIKE :query OR genericName LIKE :query")
    suspend fun searchMedicines(query: String): List<Medicine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<Medicine>)

    // --- Reminders & Schedules ---
    @Query("SELECT * FROM reminders")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Int)

    // --- Reminder Times ---
    @Query("SELECT * FROM reminder_times WHERE reminderId = :reminderId")
    suspend fun getTimesForReminder(reminderId: Int): List<ReminderTime>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderTimes(times: List<ReminderTime>)

    @Query("DELETE FROM reminder_times WHERE reminderId = :reminderId")
    suspend fun deleteTimesForReminder(reminderId: Int)

    // --- Dose History Tracker ---
    @Query("SELECT * FROM medication_history ORDER BY id DESC")
    fun getDoseHistory(): Flow<List<MedicationDose>>

    @Query("SELECT * FROM medication_history WHERE dateString = :dateString")
    fun getDosesForDate(dateString: String): Flow<List<MedicationDose>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDose(dose: MedicationDose)

    @Update
    suspend fun updateDose(dose: MedicationDose)

    @Query("DELETE FROM medication_history")
    suspend fun clearHistory()

    // --- OCR Prescriptions ---
    @Query("SELECT * FROM prescriptions ORDER BY id DESC")
    fun getPrescriptions(): Flow<List<Prescription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription)

    // --- Family Support Network ---
    @Query("SELECT * FROM family_members")
    fun getFamilyMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember)

    @Update
    suspend fun updateFamilyMember(member: FamilyMember)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun removeFamilyMember(id: Int)

    // --- Chat Dialogs ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}
