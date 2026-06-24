package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ==========================================
// REQUEST & RESPONSE DTOs FOR AUTHENTICATION
// ==========================================

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "bloodGroup") val bloodGroup: String? = null,
    @Json(name = "medicalConditions") val medicalConditions: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "user") val user: AuthUserResponse? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "token") val token: String? = null,
    @Json(name = "expiresIn") val expiresIn: Long? = null,
    @Json(name = "user") val user: AuthUserResponse? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthUserResponse(
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String,
    @Json(name = "age") val age: Int = 28,
    @Json(name = "gender") val gender: String = "Male",
    @Json(name = "bloodGroup") val bloodGroup: String = "O+",
    @Json(name = "allergies") val allergies: String = "None declared",
    @Json(name = "medicalConditions") val medicalConditions: String = "None declared"
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "user") val user: AuthUserResponse
)

@JsonClass(generateAdapter = true)
data class AssignPatientRequest(
    @Json(name = "patientEmail") val patientEmail: String
)

@JsonClass(generateAdapter = true)
data class AssignPatientResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String
)

// ==========================================
// RETROFIT SERVICE INTERFACE
// ==========================================

interface AuthApiService {

    @POST("api/auth/register")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): RegisterResponse

    @POST("api/auth/login")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") bearerToken: String
    ): ProfileResponse

    @POST("api/caregiver/assign")
    suspend fun assignPatientToRoster(
        @Header("Authorization") bearerToken: String,
        @Body request: AssignPatientRequest
    ): AssignPatientResponse
}

// ==========================================
// APIS SERVICE GENERATOR
// ==========================================

object AuthClient {
    // Standard Flask development default host URL (accessible globally or via local emulator routing)
    private const val BASE_URL = "http://10.0.2.2:5000/" // standard emulator to local host loopback link

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}
