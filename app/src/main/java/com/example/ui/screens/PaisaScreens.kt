package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import com.example.PaisaApplication
import com.example.data.*
import com.example.ui.*
import com.example.viewmodel.PaisaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Navigation Destinations
enum class PaisaScreen {
    Splash,
    Onboarding,
    Auth,
    Dashboard,
    History,
    Analytics,
    Budget,
    Profile,
    Settings
}

// ----------------------------------------------------
// AD RENDER WIDGET (START.IO DIRECT BANNER)
// ----------------------------------------------------
@Composable
fun StartIoBannerAd(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .background(Color.White)
            .shadow(1.dp),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        AndroidView(
            factory = { ctx ->
                // Render true, fully functional Start.io Banner ad
                val banner = com.startapp.sdk.ads.banner.Banner(ctx)
                banner
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ----------------------------------------------------
// CURRENCY REPRESENTATIVE
// ----------------------------------------------------
fun getCurrencySymbol(code: String): String {
    return when (code) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "₹"
    }
}

// ----------------------------------------------------
// SPLASH SCREEN
// ----------------------------------------------------
@Composable
fun SplashScreen(
    viewModel: PaisaViewModel,
    onNavigate: (PaisaScreen) -> Unit
) {
    val user by viewModel.currentUserState.collectAsState()
    val onboardingCompleted by viewModel.isOnboardingCompleted.collectAsState(initial = false)
    val isDark by viewModel.isDarkModeState.collectAsState()

    var isLogoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLogoVisible = true
        delay(1500) // Strictly limited splash duration (1.5 seconds)
        if (user != null) {
            onNavigate(PaisaScreen.Dashboard)
        } else if (onboardingCompleted) {
            onNavigate(PaisaScreen.Auth)
        } else {
            onNavigate(PaisaScreen.Onboarding)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isLogoVisible,
                enter = fadeIn(animationSpec = spring()) + scaleIn(animationSpec = spring()),
                exit = fadeOut()
            ) {
                PaisaLogo(isDark = isDark)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Paisa Tracker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else PaisaTheme.DeepPurple
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Track Every Rupee, Build Every Dream",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = PaisaTheme.DeepPurple,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ----------------------------------------------------
// ONBOARDING PAGE CAROUSEL SCREEN
// ----------------------------------------------------
@Composable
fun OnboardingScreen(
    viewModel: PaisaViewModel,
    onNavigate: (PaisaScreen) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(0) }
    val isDark by viewModel.isDarkModeState.collectAsState()

    val onboardingPages = listOf(
        OnboardingData(
            title = "Track Expenses Easily",
            desc = "Log cash expenditures and bank transactions in a single tab. Keep eye of details in seconds.",
            img = Icons.Default.Add
        ),
        OnboardingData(
            title = "Manage Monthly Budgets",
            desc = "Set monthly budget thresholds, review metrics, and receive timely alerts at spending limits.",
            img = Icons.Default.ShoppingCart
        ),
        OnboardingData(
            title = "Achieve Savings Goals",
            desc = "Analyze net inflows vs outlays to save for future asset plans and milestones with ease.",
            img = Icons.Default.Lock
        ),
        OnboardingData(
            title = "Generate Smart Reports",
            desc = "Export polished statements to PDF reports instantly and share with your associates securely.",
            img = Icons.Default.Share
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header spacing
            Spacer(modifier = Modifier.height(16.dp))

            // Onboarding display card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .clickable {
                        // Carousel step forward
                        currentPage = (currentPage + 1) % onboardingPages.size
                    },
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(PaisaTheme.DeepPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = onboardingPages[currentPage].img,
                            contentDescription = null,
                            tint = PaisaTheme.DeepPurple,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        onboardingPages[currentPage].title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PaisaTheme.DeepPurple,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        onboardingPages[currentPage].desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Indicators row & Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    onboardingPages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (currentPage == index) 16.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentPage == index) PaisaTheme.DeepPurple else Color.LightGray)
                        )
                    }
                }

                // CTA Button
                Button(
                    onClick = {
                        if (currentPage < onboardingPages.size - 1) {
                            currentPage++
                        } else {
                            coroutineScope.launch {
                                viewModel.toggleNotifications(true)
                                viewModel.toggleDarkMode(isDark)
                                viewModel.setOnboardingCompleted(true)
                                onNavigate(PaisaScreen.Auth)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                ) {
                    Text(
                        text = if (currentPage == onboardingPages.size - 1) "Get Started" else "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

data class OnboardingData(
    val title: String,
    val desc: String,
    val img: androidx.compose.ui.graphics.vector.ImageVector
)

// ----------------------------------------------------
// AUTHENTICATION SCREEN (LOGIN / REGISTER WITH SPLINES)
// ----------------------------------------------------
@Composable
fun AuthScreen(
    viewModel: PaisaViewModel,
    onNavigate: (PaisaScreen) -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val isDark by viewModel.isDarkModeState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .verticalScroll(rememberScrollState())
    ) {
        // Aesthetic spline curve header decoration
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val h = size.height
            val w = size.width
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, h * 0.70f)
                quadraticTo(w * 0.5f, h * 1.15f, w, h * 0.70f)
                lineTo(w, 0f)
                close()
            }
            drawPath(path = path, color = PaisaTheme.DeepPurple)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "Paisa Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "Track Every Rupee, Build Every Dream",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.82f)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // White login/register panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Segmented Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.LightGray.copy(alpha = 0.35f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isLoginTab) PaisaTheme.DeepPurple else Color.Transparent)
                                .clickable { isLoginTab = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sign In",
                                fontWeight = FontWeight.Bold,
                                color = if (isLoginTab) Color.White else Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (!isLoginTab) PaisaTheme.DeepPurple else Color.Transparent)
                                .clickable { isLoginTab = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Register",
                                fontWeight = FontWeight.Bold,
                                color = if (!isLoginTab) Color.White else Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Text(
                            errorMessage!!,
                            color = PaisaTheme.SoftRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (successMessage != null) {
                        Text(
                            successMessage!!,
                            color = PaisaTheme.MintGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Fields
                    if (!isLoginTab) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = false
                        )
                    )

                    if (isLoginTab) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showForgotPasswordDialog = true }) {
                                Text(
                                    "Forgot Password?",
                                    fontSize = 12.sp,
                                    color = PaisaTheme.DeepPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (isLoading) {
                        CircularProgressIndicator(color = PaisaTheme.DeepPurple, modifier = Modifier.size(32.dp))
                    } else {
                        Button(
                            onClick = {
                                val trimmedEmail = email.trim().lowercase(java.util.Locale.getDefault())
                                val trimmedPassword = password.trim()
                                val trimmedName = fullName.trim()

                                if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
                                    errorMessage = "Please enter both email and password."
                                    return@Button
                                }

                                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                    errorMessage = "Please enter a valid email address (e.g., name@example.com)."
                                    return@Button
                                }

                                if (trimmedPassword.length < 6) {
                                    errorMessage = "Password must be at least 6 characters long."
                                    return@Button
                                }

                                if (!isLoginTab && trimmedName.isEmpty()) {
                                    errorMessage = "Please enter your full name."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                successMessage = null

                                if (isLoginTab) {
                                    viewModel.login(
                                        trimmedEmail, trimmedPassword,
                                        onSuccess = {
                                            isLoading = false
                                            onNavigate(PaisaScreen.Dashboard)
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                } else {
                                    viewModel.register(
                                        trimmedEmail, trimmedPassword, trimmedName,
                                        onSuccess = {
                                            isLoading = false
                                            successMessage = "Account created. Proceeding to Home."
                                            onNavigate(PaisaScreen.Dashboard)
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                        ) {
                            Text(
                                if (isLoginTab) "Sign In" else "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // FORGOT PASSWORD POPUP
    if (showForgotPasswordDialog) {
        var recoveryEmail by remember { mutableStateOf("") }
        var resetMessage by remember { mutableStateOf<String?>(null) }
        var resetError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Recover Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your email back below to dispatch a recovery password loop link.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (resetMessage != null) {
                        Text(resetMessage!!, color = PaisaTheme.MintGreen, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (resetError != null) {
                        Text(resetError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedRecoveryEmail = recoveryEmail.trim().lowercase(java.util.Locale.getDefault())
                        if (trimmedRecoveryEmail.isEmpty()) return@Button
                        viewModel.sendPasswordReset(
                            trimmedRecoveryEmail,
                            onSuccess = {
                                resetMessage = "Password reset link has been sent to your email."
                                resetError = null
                            },
                            onError = { err ->
                                resetError = err
                                resetMessage = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                ) {
                    Text("Send Email", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

fun getTransactionEmoji(isIncome: Boolean, categoryOrSource: String): String {
    val term = categoryOrSource.lowercase(Locale.getDefault())
    if (isIncome) {
        return when {
            "salary" in term -> "💼"
            "dividend" in term || "invest" in term || "stock" in term -> "📈"
            "rent" in term -> "🏠"
            "bonus" in term || "gift" in term -> "🎁"
            "interest" in term -> "🏦"
            else -> "💰"
        }
    } else {
        return when {
            "food" in term || "pizza" in term || "eat" in term || "dining" in term -> "🍕"
            "shopping" in term || "clothes" in term || "apparel" in term -> "🛍️"
            "bills" in term || "electric" in term || "water" in term || "recharge" in term -> "🧾"
            "transport" in term || "cab" in term || "taxi" in term || "fuel" in term || "gas" in term -> "🚗"
            "entertainment" in term || "movie" in term || "netflix" in term || "show" in term -> "🎬"
            "health" in term || "doctor" in term || "medicine" in term || "clinic" in term -> "🩺"
            "travel" in term || "flight" in term || "hotel" in term -> "✈️"
            "education" in term || "book" in term || "school" in term || "course" in term -> "📚"
            else -> "🍕"
        }
    }
}

// ----------------------------------------------------
// HOME DASHBOARD SCREEN
// ----------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: PaisaViewModel,
    onShowAddDialog: () -> Unit
) {
    val stats by viewModel.overallStatistics.collectAsState()
    val rawRecentList by viewModel.filteredTransactionsState.collectAsState()
    val recentTransactions = rawRecentList.take(5)

    val profile by viewModel.userProfileState.collectAsState()
    val currencyCode by viewModel.selectedCurrencyState.collectAsState()
    val currency = getCurrencySymbol(currencyCode)
    val isDashboardLoading by viewModel.isDashboardLoading.collectAsState()
    val isDark by viewModel.isDarkModeState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
    ) {
        // 1. Absolute Overlapping welcome header & balance card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 54.dp)
            ) {
                // Purple backdrop block (rounded-b-[40px])
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(
                            if (isDark) Color(0xFF231C42) else Color(0xFF5D3FD3)
                        )
                        .padding(top = 40.dp, bottom = 72.dp, start = 24.dp, end = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Rounded white icon (svg equivalent)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .shadow(elevation = 2.dp, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Wallet",
                                    tint = if (isDark) Color(0xFF4A2FBE) else Color(0xFF5D3FD3),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    "Welcome back,",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                )
                                Text(
                                    profile?.displayName ?: "User",
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // Initials Avatar
                        val initials = (profile?.displayName ?: "AS").split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .take(2)
                            .ifEmpty { "AS" }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .background(Color(0xFF8B5CF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                initials,
                                style = TextStyle(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "Track Every Rupee, Build Every Dream",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }

                // Overlapping balance drawer Card
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 40.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = if (!isDark) BorderStroke(1.dp, Color(0xFFE2DFFF)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CURRENT BALANCE",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currency${String.format("%,.2f", stats.currentBalance)}",
                                style = TextStyle(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            )
                        }

                        // Growth metric indicator (green SVG equivalent)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Trending Up",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Loader or Indicators
        if (isDashboardLoading) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    SkeletonDashboardCard()
                }
            }
        } else {
            // side-by-side indicator columns
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Column Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isDark) BorderStroke(1.dp, Color(0xFFEFEFF7)) else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "INCOME",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currency${String.format("%,.0f", stats.totalIncome)}",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTheme.MintGreen
                                )
                            )
                        }
                    }

                    // Expense Column Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isDark) BorderStroke(1.dp, Color(0xFFEFEFF7)) else null
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "EXPENSES",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currency${String.format("%,.0f", stats.totalExpense)}",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTheme.SoftRed
                                )
                            )
                        }
                    }
                }
            }

            // 3. Mini bar chart
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                    PaisaBarChart(
                        income = stats.monthlyIncome,
                        expense = stats.monthlyExpense,
                        currencySymbol = currency,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // 4. Recent Transactions section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "Recent Transactions",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                    )
                    Text(
                        "See All",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTheme.DeepPurple
                        )
                    )
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Receipt",
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No transactions recorded yet.",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions) { transaction ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isDark) BorderStroke(1.dp, Color(0xFFF1F0F7)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Emoji container with specific category background colors
                                val emoji = getTransactionEmoji(transaction.isIncome, if (transaction.isIncome) transaction.title else transaction.category)
                                val emojiBg = when {
                                    transaction.isIncome -> Color(0xFFE0F2FE)
                                    "food" in transaction.category.lowercase() -> Color(0xFFFFEDD5)
                                    "entertainment" in transaction.category.lowercase() -> Color(0xFFF3E8FF)
                                    else -> Color(0xFFF1F5F9)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(emojiBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        transaction.title,
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF1E293B)
                                        ),
                                        maxLines = 1
                                    )
                                    val formattedDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.date))
                                    Text(
                                        "$formattedDate • ${if (transaction.isIncome) "Income" else transaction.category}",
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            val flowColor = if (transaction.isIncome) PaisaTheme.MintGreen else PaisaTheme.SoftRed
                            val prefix = if (transaction.isIncome) "+" else "-"
                            Text(
                                "$prefix$currency${String.format("%,.0f", transaction.amount)}",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = flowColor
                                )
                            )
                        }
                    }
                }
            }
        }

        // Banner ad item without sponsored text as per user requirement
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StartIoBannerAd()
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

// ----------------------------------------------------
// TRANSACTION HISTORY LEDGER SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: PaisaViewModel,
    onEditIncome: (Income) -> Unit,
    onEditExpense: (Expense) -> Unit
) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkModeState.collectAsState()
    val transactions by viewModel.filteredTransactionsState.collectAsState()
    val currencyCode by viewModel.selectedCurrencyState.collectAsState()
    val currency = getCurrencySymbol(currencyCode)
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()

    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.filterType.collectAsState()
    val sort by viewModel.sortType.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Activity Ledger Log",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else PaisaTheme.DeepPurple
            )
            Text(
                "Filter, search, or review comprehensive expenses.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. Search Box
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                }
            )
        }

        // 2. Filter chips Segmented row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Today", "This Week", "This Month", "Custom")
                filters.forEach { filterItem ->
                    val isSelected = filter == filterItem
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (filterItem == "Custom") {
                                // Direct integration of native android DatePickerDialog
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val calStart = Calendar.getInstance().apply {
                                            set(year, month, dayOfMonth, 0, 0, 0)
                                        }
                                        DatePickerDialog(
                                            context,
                                            { _, ey, em, ed ->
                                                val calEnd = Calendar.getInstance().apply {
                                                    set(ey, em, ed, 23, 59, 59)
                                                }
                                                viewModel.customDateSelectedStart.value = calStart.timeInMillis
                                                viewModel.customDateSelectedEnd.value = calEnd.timeInMillis
                                                viewModel.filterType.value = "Custom"
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            } else {
                                viewModel.filterType.value = filterItem
                            }
                        },
                        label = { Text(filterItem) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PaisaTheme.DeepPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Ledger sorting selectors
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "List Entries (${transactions.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sort: $sort", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PaisaTheme.DeepPurple)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        val sorts = listOf("Latest", "Oldest", "Highest Amount", "Lowest Amount")
                        sorts.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    viewModel.sortType.value = item
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Render Skeletons or Contents
        if (isHistoryLoading) {
            items(6) {
                SkeletonTransactionItem()
                Divider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 1.dp)
            }
        } else {
            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.List, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No activities meet the criteria.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(transactions) { trans ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .combinedClickable(
                                onClick = {
                                    if (trans.isIncome) {
                                        onEditIncome(Income(id = trans.id, amount = trans.amount, source = trans.title, note = trans.note, date = trans.date))
                                    } else {
                                        onEditExpense(Expense(id = trans.id, amount = trans.amount, category = trans.category, note = trans.note, date = trans.date, paymentMethod = trans.paymentMethod))
                                    }
                                },
                                onLongClick = { showDeleteDialog = true }
                            ),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (trans.isIncome) PaisaTheme.LightGreen else PaisaTheme.LightRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (trans.isIncome) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                                        contentDescription = if (trans.isIncome) "Income" else "Expense",
                                        tint = if (trans.isIncome) PaisaTheme.MintGreen else PaisaTheme.SoftRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        trans.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(trans.date))
                                    Text(
                                        "$formattedDate${if (trans.paymentMethod.isNotEmpty()) " • ${trans.paymentMethod}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            val flowColor = if (trans.isIncome) PaisaTheme.MintGreen else PaisaTheme.SoftRed
                            val prefix = if (trans.isIncome) "+" else "-"
                            Text(
                                "$prefix$currency${String.format("%,.1f", trans.amount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = flowColor
                            )
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Entry") },
                            text = { Text("Are you completely certain you wish to delete this entry from your financial statement ledger permanently?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (trans.isIncome) {
                                            viewModel.deleteIncomeRecord(trans.id, {}, {})
                                        } else {
                                            viewModel.deleteExpenseRecord(trans.id, {}, {})
                                        }
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.SoftRed)
                                ) {
                                    Text("Delete", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            StartIoBannerAd(modifier = Modifier.padding(vertical = 16.dp))
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

// ----------------------------------------------------
// MONTHLY ANALYTICS ENGINE SCREEN
// ----------------------------------------------------
@Composable
fun AnalyticsScreen(
    viewModel: PaisaViewModel
) {
    val analytics by viewModel.currentAnalyticsState.collectAsState()
    val isDark by viewModel.isDarkModeState.collectAsState()
    val currencyCode by viewModel.selectedCurrencyState.collectAsState()
    val currency = getCurrencySymbol(currencyCode)
    val context = LocalContext.current

    val rawRecentList by viewModel.filteredTransactionsState.collectAsState()
    val watchedPdfAdsCount by viewModel.watchedPdfAdsCount.collectAsState()
    val isAdLoading by viewModel.isAdLoading.collectAsState()
    var showUnlockPdfDialog by remember { mutableStateOf(false) }

    if (showUnlockPdfDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockPdfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PaisaTheme.DeepPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock PDF Export", fontWeight = FontWeight.Bold, color = PaisaTheme.DeepPurple, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Please watch 4 interstitial advertisements to unlock the premium PDF export and statement sharing feature. This helps us support free hosting!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Progress Title Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Progress: $watchedPdfAdsCount / 4 Ads Watched",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (watchedPdfAdsCount >= 4) PaisaTheme.MintGreen else PaisaTheme.DeepPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (watchedPdfAdsCount.coerceAtMost(4).toFloat() / 4.0f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (watchedPdfAdsCount >= 4) PaisaTheme.MintGreen else PaisaTheme.DeepPurple,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (watchedPdfAdsCount < 4) {
                            if (!isAdLoading) {
                                viewModel.triggerManualInterstitialAd()
                                Toast.makeText(context, "Loading interstitial ad... Please wait.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showUnlockPdfDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (watchedPdfAdsCount >= 4) PaisaTheme.MintGreen else PaisaTheme.DeepPurple
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isAdLoading || watchedPdfAdsCount >= 4
                ) {
                    if (isAdLoading && watchedPdfAdsCount < 4) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading Ad...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = if (watchedPdfAdsCount >= 4) "Unlocked! Close Guides" else "Watch Ad (${watchedPdfAdsCount}/4)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockPdfDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    val activeStartDate by viewModel.analyticsStartDateState.collectAsState()
    val activeEndDate by viewModel.analyticsEndDateState.collectAsState()

    val dateDisplayFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Monthly Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else PaisaTheme.DeepPurple
                    )
                    Text(
                        if (activeStartDate != null && activeEndDate != null) {
                            "Custom filtered statement analysis"
                        } else {
                            "Analysis of income vs expenses breakdown."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // PDF EXPORT LINK ACTION (Locks on click, resets to 0 ads)
                IconButton(
                    onClick = {
                        if (watchedPdfAdsCount >= 4) {
                            PaisaPdfExporter.exportTransactionsToPdf(
                                context = context,
                                currencySymbol = currency,
                                totalIncome = analytics.monthlyIncome,
                                totalExpense = analytics.monthlyExpense,
                                netBalance = analytics.netBalance,
                                categoryBreakdown = analytics.categoryBreakdown,
                                transactions = rawRecentList
                            )
                            viewModel.resetPdfAdsCount()
                            Toast.makeText(context, "Statement generated successfully! PDF lock updated.", Toast.LENGTH_LONG).show()
                        } else {
                            showUnlockPdfDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PaisaTheme.DeepPurple.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = if (watchedPdfAdsCount >= 4) Icons.Default.Share else Icons.Default.Lock, 
                        contentDescription = "PDF Report", 
                        tint = PaisaTheme.DeepPurple
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful Custom Date Selection Row "Apne Man ki hisab se date select kare"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF252427) else Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = PaisaTheme.DeepPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Custom Date Range Filter",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                        if (activeStartDate != null || activeEndDate != null) {
                            TextButton(
                                onClick = { viewModel.setAnalyticsDateRange(null, null) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Filter", fontSize = 11.sp, color = PaisaTheme.SoftRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Date Button
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (activeStartDate != null) cal.timeInMillis = activeStartDate!!
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val sel = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                        viewModel.setAnalyticsDateRange(sel.timeInMillis, activeEndDate ?: System.currentTimeMillis())
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeStartDate != null) PaisaTheme.DeepPurple else if (isDark) Color(0xFF323135) else Color(0xFFE2E8F0),
                                contentColor = if (activeStartDate != null) Color.White else if (isDark) Color.LightGray else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (activeStartDate != null) dateDisplayFormat.format(Date(activeStartDate!!)) else "From Date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Text("to", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        // End Date Button
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                if (activeEndDate != null) cal.timeInMillis = activeEndDate!!
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val sel = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                        viewModel.setAnalyticsDateRange(activeStartDate ?: System.currentTimeMillis(), sel.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeEndDate != null) PaisaTheme.DeepPurple else if (isDark) Color(0xFF323135) else Color(0xFFE2E8F0),
                                contentColor = if (activeEndDate != null) Color.White else if (isDark) Color.LightGray else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (activeEndDate != null) dateDisplayFormat.format(Date(activeEndDate!!)) else "To Date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 1. Double charts drawing
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "Expense Category Outlays",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PaisaTheme.DeepPurple
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PaisaPieChart(
                        data = analytics.categoryBreakdown,
                        currencySymbol = currency
                    )
                }
            }
        }

        // 2. Multi-column KPIs statistical indicators
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Peak Income", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currency${String.format("%,.0f", analytics.highestIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaisaTheme.MintGreen
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Peak Expense", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currency${String.format("%,.0f", analytics.highestExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaisaTheme.SoftRed
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg Daily In", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currency${String.format("%,.0f", analytics.averageDailyIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Avg Daily Out", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$currency${String.format("%,.0f", analytics.averageDailyExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF2A2438) else Color(0xFFF3E8FF)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, PaisaTheme.DeepPurple.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaisaTheme.DeepPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = PaisaTheme.DeepPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Premium Statement PDF Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PaisaTheme.DeepPurple
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "To keep hosting free, we support statement sharing via standard advertisements. Access custom-filtered balance summaries, graphs, and complete list reports flawlessly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "👉 Lock Instructions: Click on the Lock icon at the top of this screen or watch the required ads below to increment your progress. You must watch 4 ads to generate your report. Each successful PDF share will lock the feature again to support free access.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = PaisaTheme.DeepPurple
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress Indicator bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ads Progress: $watchedPdfAdsCount / 4 Watched",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (watchedPdfAdsCount >= 4) PaisaTheme.MintGreen else PaisaTheme.DeepPurple
                        )
                        if (watchedPdfAdsCount >= 4) {
                            Icon(Icons.Default.CheckCircle, "Unlocked", tint = PaisaTheme.MintGreen, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Lock, "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { (watchedPdfAdsCount.coerceAtMost(4).toFloat() / 4.0f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (watchedPdfAdsCount >= 4) PaisaTheme.MintGreen else PaisaTheme.DeepPurple,
                        trackColor = if (isDark) Color(0xFF3B354D) else Color(0xFFE2D6F5)
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Watch Ad Button
                        Button(
                            onClick = {
                                if (watchedPdfAdsCount < 4) {
                                    if (!isAdLoading) {
                                        viewModel.triggerManualInterstitialAd()
                                        Toast.makeText(context, "Loading interstitial ad... Please wait.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Features are already unlocked!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (watchedPdfAdsCount >= 4) Color.Gray.copy(alpha = 0.3f) else PaisaTheme.DeepPurple,
                                contentColor = if (watchedPdfAdsCount >= 4) Color.DarkGray else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = watchedPdfAdsCount < 4 && !isAdLoading
                        ) {
                            if (isAdLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = if (watchedPdfAdsCount >= 4) Color.DarkGray else Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Loading...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Watch Ad (${watchedPdfAdsCount}/4)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Share/Generate PDF Button
                        Button(
                            onClick = {
                                if (watchedPdfAdsCount >= 4) {
                                    PaisaPdfExporter.exportTransactionsToPdf(
                                        context = context,
                                        currencySymbol = currency,
                                        totalIncome = analytics.monthlyIncome,
                                        totalExpense = analytics.monthlyExpense,
                                        netBalance = analytics.netBalance,
                                        categoryBreakdown = analytics.categoryBreakdown,
                                        transactions = rawRecentList
                                    )
                                    viewModel.resetPdfAdsCount()
                                    Toast.makeText(context, "Report exported! Center re-locked.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Locked: Please watch remaining ads first or click on lock icon above.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (watchedPdfAdsCount >= 4) PaisaTheme.MintGreen else Color.Gray.copy(alpha = 0.2f),
                                contentColor = if (watchedPdfAdsCount >= 4) Color.White else Color.Gray
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            StartIoBannerAd(modifier = Modifier.padding(vertical = 12.dp))
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

// ----------------------------------------------------
// BUDGET MANAGEMENT MODULE SCREEN
// ----------------------------------------------------
@Composable
fun BudgetScreen(
    viewModel: PaisaViewModel
) {
    val budgetSummary by viewModel.activeBudgetMonitoring.collectAsState()
    val isDark by viewModel.isDarkModeState.collectAsState()
    val currencyCode by viewModel.selectedCurrencyState.collectAsState()
    val currency = getCurrencySymbol(currencyCode)

    var newBudgetAmount by remember { mutableStateOf("") }
    var alertMsg by remember { mutableStateOf<String?>(null) }
    var isSavingBudget by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Budget Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else PaisaTheme.DeepPurple
            )
            Text(
                "Adjust threshold limits and track current outflows.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 1. Current Tracker Progress Indicators
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Monthly Outlay Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PaisaTheme.DeepPurple
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Outlay Spent", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                "$currency${String.format("%,.1f", budgetSummary.spent)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTheme.SoftRed
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Budget Threshold Limit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                "$currency${String.format("%,.1f", budgetSummary.limit)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PaisaTheme.DeepPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Usage status indicator bar
                    LinearProgressIndicator(
                        progress = (budgetSummary.percentage / 100).toFloat(),
                        color = if (budgetSummary.percentage >= 90.0) PaisaTheme.SoftRed else PaisaTheme.DeepPurple,
                        trackColor = Color.LightGray.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "${String.format("%.1f", budgetSummary.percentage)}% consumed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (budgetSummary.percentage >= 90.0) PaisaTheme.SoftRed else Color.Gray
                        )
                    }
                }
            }
        }

        // 2. Adjust Limit Card Control view
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Configure Limits ($currency)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else PaisaTheme.DeepPurple,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newBudgetAmount,
                        onValueChange = { newBudgetAmount = it },
                        label = { Text("E.g., 20000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (alertMsg != null) {
                        Text(
                            alertMsg!!,
                            color = PaisaTheme.MintGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (isSavingBudget) {
                        CircularProgressIndicator(color = PaisaTheme.DeepPurple, modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = {
                                val parsedAmount = newBudgetAmount.toDoubleOrNull()
                                if (parsedAmount == null || parsedAmount <= 0.1) {
                                    alertMsg = "Please input a positive numeric value."
                                    return@Button
                                }
                                isSavingBudget = true
                                alertMsg = null
                                viewModel.changeBudget(
                                    yearMonth = budgetSummary.monthString,
                                    amount = parsedAmount,
                                    onSuccess = {
                                        isSavingBudget = false
                                        alertMsg = "Budget Limit Saved Successfully!"
                                        newBudgetAmount = ""
                                    },
                                    onError = { err ->
                                        isSavingBudget = false
                                        alertMsg = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                        ) {
                            Text("Update Limit", color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            StartIoBannerAd(modifier = Modifier.padding(vertical = 12.dp))
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

// ----------------------------------------------------
// PROFILE MANAGEMENT SCREEN
// ----------------------------------------------------
@Composable
fun ProfileScreen(
    viewModel: PaisaViewModel,
    onNavigate: (PaisaScreen) -> Unit
) {
    val profile by viewModel.userProfileState.collectAsState()
    val isDark by viewModel.isDarkModeState.collectAsState()

    var editableName by remember { mutableStateOf("") }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let { editableName = it.displayName }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
            val displayEmail = remember(profile, currentUser) {
                currentUser?.email ?: profile?.email ?: "No email registered"
            }
            val displayJoined = remember(profile, currentUser) {
                val timestamp = currentUser?.metadata?.creationTimestamp
                if (timestamp != null && timestamp > 0) {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timestamp))
                } else {
                    profile?.joinedDate ?: "Unknown Date"
                }
            }
            val displayName = remember(profile, currentUser) {
                val fName = profile?.displayName ?: ""
                if (fName.isNotEmpty()) fName else {
                    currentUser?.displayName ?: (currentUser?.email?.substringBefore("@")) ?: "User Profile"
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Radial Avatar
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(PaisaTheme.DeepPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else PaisaTheme.DeepPurple
            )
            Text(
                displayEmail,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Joined: $displayJoined",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 1. Actions Panel List
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        title = "Edit Profile Name",
                        icon = Icons.Default.Edit,
                        onClick = { showEditProfileDialog = true }
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                    ProfileMenuRow(
                        title = "Verify Email Address",
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            viewModel.sendEmailVerification(
                                onSuccess = {
                                    Log.d("ProfileScreen", "Email loop dispatched")
                                },
                                onError = {
                                    Log.e("ProfileScreen", "Verification error")
                                }
                            )
                        }
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                    ProfileMenuRow(
                        title = "Reset Password Link",
                        icon = Icons.Default.Lock,
                        onClick = { showChangePasswordDialog = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Destructive security actions card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ProfileMenuRow(
                        title = "Sign Out Session",
                        icon = Icons.Default.ExitToApp,
                        tint = PaisaTheme.DeepPurple,
                        onClick = {
                            viewModel.logout {
                                onNavigate(PaisaScreen.Auth)
                            }
                        }
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                    ProfileMenuRow(
                        title = "Delete Account Permanently",
                        icon = Icons.Default.Delete,
                        tint = PaisaTheme.SoftRed,
                        onClick = { showDeleteAccountDialog = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // PROFILE DIALOGS
    if (showEditProfileDialog) {
        var localEditName by remember { mutableStateOf(editableName) }
        var isEditingProfileName by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Update Name") },
            text = {
                OutlinedTextField(
                    value = localEditName,
                    onValueChange = { localEditName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (localEditName.isEmpty()) return@Button
                        isEditingProfileName = true
                        viewModel.updateProfile(
                            localEditName,
                            onSuccess = {
                                isEditingProfileName = false
                                showEditProfileDialog = false
                            },
                            onError = {
                                isEditingProfileName = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Reset Password") },
            text = { Text("We will dispatch a secure link to your verified email address (${profile?.email ?: ""}) to reset passwords.") },
            confirmButton = {
                Button(
                    onClick = {
                        profile?.let {
                            viewModel.sendPasswordReset(it.email, { showChangePasswordDialog = false }, {})
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                ) {
                    Text("Dispatch Now", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Dismiss") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        var deleteConfirmationName by remember { mutableStateOf("") }
        var isDeletingAccountState by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Are you absolutely certain?") },
            text = {
                Column {
                    Text("This process is irreversible. Type your display name back below to finalize secure deletes.")
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = deleteConfirmationName,
                        onValueChange = { deleteConfirmationName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm Name") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteConfirmationName != profile?.displayName) return@Button
                        isDeletingAccountState = true
                        viewModel.deleteUserAccount(
                            onSuccess = {
                                isDeletingAccountState = false
                                showDeleteAccountDialog = false
                                onNavigate(PaisaScreen.Auth)
                            },
                            onError = {
                                isDeletingAccountState = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.SoftRed)
                ) {
                    Text("Delete Account", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Dismiss") }
            }
        )
    }
}

@Composable
fun ProfileMenuRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color.Gray,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = Color.LightGray)
    }
}

// ----------------------------------------------------
// SETTINGS PREFERENCES SCREEN
// ----------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: PaisaViewModel,
    onNavigate: (PaisaScreen) -> Unit
) {
    val isDark by viewModel.isDarkModeState.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationEnabledState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrencyState.collectAsState()
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    val isDailyRemindersEnabled by viewModel.isDailyRemindersEnabledState.collectAsState()
    val isBudgetAlertsEnabled by viewModel.isBudgetAlertsEnabledState.collectAsState()
    val isMonthlySummaryEnabled by viewModel.isMonthlySummaryEnabledState.collectAsState()
    val isWelcomeMessagesEnabled by viewModel.isWelcomeMessagesEnabledState.collectAsState()
    val reminderFrequency by viewModel.reminderFrequencyState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF1C1B1F) else PaisaTheme.LightLavender)
            .padding(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Preferences Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else PaisaTheme.DeepPurple
            )
            Text(
                "Personalize currencies, triggers, or theme overlays.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Configuration Rows
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    // Dark Mode row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTheme.DeepPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Settings, null, tint = PaisaTheme.DeepPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Dark Theme Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PaisaTheme.DeepPurple)
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                    // Notifications Toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTheme.DeepPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, null, tint = PaisaTheme.DeepPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Budget Warnings Alert", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = isNotificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PaisaTheme.DeepPurple)
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                    // Currency Selection Dropdown row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PaisaTheme.DeepPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, null, tint = PaisaTheme.DeepPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Default Ledger Currency", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        
                        Box {
                            TextButton(onClick = { isCurrencyMenuExpanded = true }) {
                                Text(
                                    text = "$selectedCurrency (${getCurrencySymbol(selectedCurrency)})",
                                    fontWeight = FontWeight.Bold,
                                    color = PaisaTheme.DeepPurple
                                )
                                Icon(Icons.Default.ArrowDropDown, null, tint = PaisaTheme.DeepPurple)
                            }
                            DropdownMenu(expanded = isCurrencyMenuExpanded, onDismissRequest = { isCurrencyMenuExpanded = false }) {
                                val currencies = listOf("INR", "USD", "EUR", "GBP")
                                currencies.forEach { string ->
                                    DropdownMenuItem(
                                        text = { Text("$string (${getCurrencySymbol(string)})") },
                                        onClick = {
                                            viewModel.changeCurrency(string)
                                            isCurrencyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Custom Notifications Preferences Card
        item {
            val context = LocalContext.current
            var hasPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasPermission = isGranted
                if (isGranted) {
                    Log.d("PaisaNotification", "NOTIFICATION_PERMISSION_GRANTED")
                    Log.d("PaisaNotification", "PERMISSION_GRANTED")
                    Toast.makeText(context, "Notification permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("PaisaNotification", "NOTIFICATION_PERMISSION_DENIED")
                    Log.d("PaisaNotification", "PERMISSION_DENIED")
                    Toast.makeText(context, "Notification permissions denied.", Toast.LENGTH_SHORT).show()
                }
            }

            var showRationaleDialog by remember { mutableStateOf(false) }

            if (showRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showRationaleDialog = false },
                    title = { Text("Notification Permission") },
                    text = { Text("Paisa Tracker requires permission to show smart daily reminders, budget alerts, and monthly summaries to keep you on track.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRationaleDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        ) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRationaleDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Notification Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTheme.DeepPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Configure alerts, reminder times, and monthly trackers to master your finances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission Request Banner if not granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                        Button(
                            onClick = { showRationaleDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔔 Grant Notification Permission", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    // Daily Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Reminders", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Smart local financial suggestions 3-7x daily", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = isDailyRemindersEnabled,
                            onCheckedChange = { viewModel.setDailyRemindersEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PaisaTheme.DeepPurple)
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

                    // Budget Alerts Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Budget Milestones Alerts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Receive local warnings at 50%, 75%, 90%, 100% usage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = isBudgetAlertsEnabled,
                            onCheckedChange = { viewModel.setBudgetAlertsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PaisaTheme.DeepPurple)
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

                    // Monthly Summary Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monthly Summaries", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Get detailed local summaries on last day of month", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = isMonthlySummaryEnabled,
                            onCheckedChange = { viewModel.setMonthlySummaryEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PaisaTheme.DeepPurple)
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

                    // Welcome Messages Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Welcome Messages", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Receive motivational welcome notification after sign-in", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = isWelcomeMessagesEnabled,
                            onCheckedChange = { viewModel.setWelcomeMessagesEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PaisaTheme.DeepPurple)
                        )
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)

                    // Frequency Preference Selector
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text("Daily Suggestions Frequency", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Control density of smart local financial notifications dynamically.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(3, 5, 7).forEach { freq ->
                                val isSelected = reminderFrequency == freq
                                Button(
                                    onClick = {
                                        viewModel.setReminderFrequency(freq)
                                        com.example.PaisaNotificationReceiver.scheduleAllAlarms(context)
                                        Toast.makeText(context, "Rescheduled reminders density to $freq daily", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PaisaTheme.DeepPurple else if (isDark) Color(0xFF323135) else Color(0xFFEFEFF3),
                                        contentColor = if (isSelected) Color.White else if (isDark) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                ) {
                                    Text("${freq}x Daily", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Dynamic local reminder notification generator card
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PaisaTheme.DeepPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = PaisaTheme.DeepPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Daily Push Reminders Center",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "To help you cultivate smart spending habits, the app automatically posts randomized, unique financial suggestions 5 to 7 times daily. Try triggering an instant alert below!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                val tips = arrayOf(
                                    "✨ Paisa Tracker reminder: Fast food, tea or coffee today? Record your expenses in 5 seconds!",
                                    "📈 Financial Check: Compare your start and end dates in Analytics to spot saving trends.",
                                    "💰 Save first: 'Do not save what is left after spending, but spend what is left after saving.'",
                                    "🛡️ Secure & Offline: Paisa keeps your records private on this device.",
                                    "☕ Spent on food or beverage today? Log it under correct categories in Paisa to keep budgets green.",
                                    "📊 Daily Tip: Reviewing custom date ranges under analytics reveals hidden leakage trends.",
                                    "🚀 Target near! Check if your current monthly bills are pushing closer to your budget limit.",
                                    "💡 Smart spending: think twice before clicking buy. Delay non-essential purchases by 48 hours to prevent impulse buyer buying remorse."
                                )
                                val selectedTip = tips[java.util.Random().nextInt(tips.size)]
                                val clickIntent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                val clickPendingIntent = androidx.core.app.TaskStackBuilder.create(context).run {
                                    addNextIntentWithParentStack(clickIntent)
                                    getPendingIntent(
                                        111,
                                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                    )
                                }
                                val notification = androidx.core.app.NotificationCompat.Builder(context, com.example.PaisaApplication.CHANNEL_REMINDERS)
                                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                                    .setContentTitle("Paisa Budget Assistant")
                                    .setContentText(selectedTip)
                                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(selectedTip))
                                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .setContentIntent(clickPendingIntent)
                                    .build()
                                manager.notify(java.util.Random().nextInt(1000, 99999), notification)
                                Toast.makeText(context, "Instant push notification dispatched successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to dispatch notification: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Instant Push Notification", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Support and Feedback segment
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Help, Support & Bugs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PaisaTheme.DeepPurple
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Spotted a discrepancy or want to report an issue? Message the administration and get your queries solved instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:")
                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("stapearnsupport@gmail.com"))
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Paisa Tracker - Bug Report & Fix Request")
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT, 
                                        "Hello Admin,\n\nI observed the following bug or feature request in the Paisa App:\n\n[Describe Bug/Issue Details here]\n\nApp Version: 1.0.0\nDevice: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n"
                                    )
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Send Email Support..."))
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "No active mail agent configured.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Support (stapearnsupport@gmail.com)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // About us segment
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PaisaLogo(modifier = Modifier.size(72.dp), isDark = isDark)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Paisa Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.0 (Stable Release)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Designed exclusively under modern Material Design 3 guidelines to track every transactions offline and online.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
