package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.*
import com.example.ui.MedicalViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MedicalViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateDarkBackground
                ) {
                    when (currentScreen) {
                        "landing" -> {
                            WelcomeScreen(
                                onGetStarted = { viewModel.navigateToScreen("onboarding") },
                                onOpenAdmin = { viewModel.navigateToScreen("admin") }
                            )
                        }
                        "onboarding" -> {
                            OnboardingScreen(
                                onComplete = { name, email, age, gender, blood, conditions ->
                                    viewModel.completeOnboarding(name, email, age, gender, blood, conditions)
                                }
                            )
                        }
                        "admin" -> {
                            AdminSystemScreen(
                                onExitAdmin = { viewModel.navigateToScreen("landing") }
                            )
                        }
                        "main" -> {
                            MainAppScaffold(viewModel = viewModel)
                        }
                        else -> {
                            WelcomeScreen(
                                onGetStarted = { viewModel.navigateToScreen("onboarding") },
                                onOpenAdmin = { viewModel.navigateToScreen("admin") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: MedicalViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val adherencePercent by viewModel.adherencePercent.collectAsStateWithLifecycle()
    
    val todayDoses by viewModel.todayDoses.collectAsStateWithLifecycle()
    val doseHistory by viewModel.doseHistory.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MEDICARE AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Good morning, ${userProfile?.name ?: "Alex"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground,
                    titleContentColor = SlateTextPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateToScreen("admin") },
                        modifier = Modifier.testTag("scaffold_admin_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = PrimaryTeal
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateCardSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "home",
                    onClick = { viewModel.navigateToTab("home") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary,
                        indicatorColor = SlateOutline
                    ),
                    modifier = Modifier.testTag("tab_home")
                )

                NavigationBarItem(
                    selected = activeTab == "search",
                    onClick = { viewModel.navigateToTab("search") },
                    icon = { Icon(Icons.Default.Search, contentDescription = "FDA DB") },
                    label = { Text("FDA DB", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary,
                        indicatorColor = SlateOutline
                    ),
                    modifier = Modifier.testTag("tab_search")
                )

                NavigationBarItem(
                    selected = activeTab == "meds",
                    onClick = { viewModel.navigateToTab("meds") },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Scheduler") },
                    label = { Text("Schedules", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary,
                        indicatorColor = SlateOutline
                    ),
                    modifier = Modifier.testTag("tab_schedules")
                )

                NavigationBarItem(
                    selected = activeTab == "assistant",
                    onClick = { viewModel.navigateToTab("assistant") },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant") },
                    label = { Text("AI Helper", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary,
                        indicatorColor = SlateOutline
                    ),
                    modifier = Modifier.testTag("tab_assistant")
                )

                NavigationBarItem(
                    selected = activeTab == "profile",
                    onClick = { viewModel.navigateToTab("profile") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Set") },
                    label = { Text("Profile", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        unselectedIconColor = SlateTextSecondary,
                        unselectedTextColor = SlateTextSecondary,
                        indicatorColor = SlateOutline
                    ),
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.background(SlateDarkBackground)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SlateDarkBackground)
        ) {
            when (activeTab) {
                "home" -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        user = userProfile,
                        adherence = adherencePercent,
                        todayDoses = todayDoses,
                        onAddMedicineClick = { viewModel.navigateToTab("meds") },
                        onScanPrescriptionClick = { viewModel.navigateToTab("ocr") }
                    )
                }
                "search" -> {
                    MedicineSearchScreen(
                        viewModel = viewModel,
                        searchQuery = searchQuery,
                        searchResults = searchResults
                    )
                }
                "meds" -> {
                    ScheduleScreen(
                        viewModel = viewModel,
                        onSaveSuccess = { viewModel.navigateToTab("home") }
                    )
                }
                "ocr" -> {
                    PrescriptionScanScreen(
                        viewModel = viewModel,
                        onFinishedPattern = { viewModel.navigateToTab("home") }
                    )
                }
                "reports" -> {
                    ReportsAnalyticsScreen(
                        adherence = adherencePercent,
                        history = doseHistory
                    )
                }
                "family" -> {
                    FamilyNetworkScreen(
                        viewModel = viewModel,
                        members = familyMembers
                    )
                }
                "assistant" -> {
                    AIChatAssistantScreen(
                        viewModel = viewModel,
                        messages = chatMessages,
                        isLoading = isChatLoading
                    )
                }
                "profile" -> {
                    ProfileTabsShell(
                        viewModel = viewModel,
                        user = userProfile,
                        familyMembers = familyMembers,
                        adherence = adherencePercent,
                        history = doseHistory
                    )
                }
                else -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        user = userProfile,
                        adherence = adherencePercent,
                        todayDoses = todayDoses,
                        onAddMedicineClick = { viewModel.navigateToTab("meds") },
                        onScanPrescriptionClick = { viewModel.navigateToTab("ocr") }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTabsShell(
    viewModel: MedicalViewModel,
    user: User?,
    familyMembers: List<FamilyMember>,
    adherence: Int,
    history: List<MedicationDose>
) {
    var nestedTab by remember { mutableStateOf("vitals") } // vitals, reports, family

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Segmented Swapper Header
        TabRow(
            selectedTabIndex = when (nestedTab) {
                "vitals" -> 0
                "reports" -> 1
                "family" -> 2
                else -> 0
            },
            containerColor = SlateCardSurface,
            contentColor = PrimaryTeal,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[
                        when (nestedTab) {
                            "vitals" -> 0
                            "reports" -> 1
                            "family" -> 2
                            else -> 0
                        }
                    ]),
                    color = PrimaryTeal
                )
            }
        ) {
            Tab(
                selected = nestedTab == "vitals",
                onClick = { nestedTab = "vitals" },
                text = { Text("My Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = PrimaryTeal,
                unselectedContentColor = SlateTextSecondary,
                modifier = Modifier.testTag("nested_tab_vitals")
            )
            Tab(
                selected = nestedTab == "reports",
                onClick = { nestedTab = "reports" },
                text = { Text("Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = PrimaryTeal,
                unselectedContentColor = SlateTextSecondary,
                modifier = Modifier.testTag("nested_tab_analytics")
            )
            Tab(
                selected = nestedTab == "family",
                onClick = { nestedTab = "family" },
                text = { Text("Family circles", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = PrimaryTeal,
                unselectedContentColor = SlateTextSecondary,
                modifier = Modifier.testTag("nested_tab_family")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (nestedTab) {
                "vitals" -> ProfileScreen(user = user, onLogout = { viewModel.logout() })
                "reports" -> ReportsAnalyticsScreen(adherence = adherence, history = history)
                "family" -> FamilyNetworkScreen(viewModel = viewModel, members = familyMembers)
            }
        }
    }
}
