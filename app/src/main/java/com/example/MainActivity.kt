package com.example

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import com.example.data.*
import com.example.ui.PaisaTheme
import com.example.ui.screens.*
import com.example.viewmodel.PaisaViewModel
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    companion object {
        var isCreated = false
        var isResumed = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isCreated = true
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload immediately on App Launch
        preloadStartIoInterstitialAd()

        setContent {
            val viewModel: PaisaViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkModeState.collectAsState()

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme(
                    primary = PaisaTheme.DeepPurple,
                    background = Color(0xFF1C1B1F),
                    surface = Color(0xFF252427)
                ) else lightColorScheme(
                    primary = PaisaTheme.DeepPurple,
                    background = PaisaTheme.LightLavender,
                    surface = Color.White
                )
            ) {
                // Main App shell with edge-to-edge support
                PaisaAppShell(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isCreated = false
    }

    // Preload Start.io interstitial ad
    fun preloadStartIoInterstitialAd() {
        runOnUiThread {
            AdManager.preloadAd(this)
        }
    }

    // Displays the true Start.io interstitial ad inside the transaction flow
    fun triggerStartIoInterstitialAd(viewModel: PaisaViewModel) {
        runOnUiThread {
            AdManager.showAd(this) {
                viewModel.resetAdCounter()
            }
        }
    }

    // Displays the true Start.io interstitial ad manually with anti-bypass loading handling
    fun triggerStartIoInterstitialAdManual(viewModel: PaisaViewModel) {
        runOnUiThread {
            viewModel.setAdLoading(true)
            AdManager.showAd(this) {
                viewModel.incrementPdfAdsCount()
                viewModel.setAdLoading(false)
                Toast.makeText(this@MainActivity, "🎉 Ad watched successfully! PDF Progress updated.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PaisaAppShell(viewModel: PaisaViewModel) {
    val activity = LocalContext.current as? MainActivity
    var currentScreen by remember { mutableStateOf(PaisaScreen.Splash) }
    
    val isDark by viewModel.isDarkModeState.collectAsState()
    val currencyCode by viewModel.selectedCurrencyState.collectAsState()
    val currencySign = getCurrencySymbol(currencyCode)

    // Dialog state controllers
    var showAddDialog by remember { mutableStateOf(false) }
    var incomeToEdit by remember { mutableStateOf<Income?>(null) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    // Check and trigger notification permission dialog for Android 13+
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("PaisaNotification", "NOTIFICATION_PERMISSION_GRANTED")
            Toast.makeText(context, "Notification permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("PaisaNotification", "NOTIFICATION_PERMISSION_DENIED")
            Toast.makeText(context, "Notifications are disabled. You can configure them in Settings.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                showNotificationPermissionDialog = true
            }
        }
    }

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = PaisaTheme.DeepPurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Notifications")
                }
            },
            text = {
                Text("Paisa Tracker requires permission to display dynamic push updates, budget milestone alerts, and your daily transacting reminders.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotificationPermissionDialog = false }
                ) {
                    Text("Not Now", color = Color.Gray)
                }
            }
        )
    }

    // Interstitial Ad Trigger subscriber
    LaunchedEffect(Unit) {
        viewModel.showInterstitialTrigger.collect { trigger ->
            if (trigger) {
                activity?.triggerStartIoInterstitialAd(viewModel)
            }
        }
    }

    // Manual Interstitial Ad Trigger subscriber (with loader protection)
    LaunchedEffect(Unit) {
        viewModel.showManualInterstitialTrigger.collect { trigger ->
            if (trigger) {
                activity?.triggerStartIoInterstitialAdManual(viewModel)
            }
        }
    }

    // Preload ad when Dashboard Screen is opened or on successful login mapping
    LaunchedEffect(currentScreen) {
        if (currentScreen == PaisaScreen.Dashboard) {
            activity?.preloadStartIoInterstitialAd()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Render beautiful Bottom Navigation only if on Home/Dashboard, History, Analytics, Budget, Profile or Settings pages
            val navScreens = listOf(
                PaisaScreen.Dashboard, PaisaScreen.History, PaisaScreen.Analytics,
                PaisaScreen.Budget, PaisaScreen.Profile, PaisaScreen.Settings
            )
            if (currentScreen in navScreens) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF252427) else Color.White),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left navigation items
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                                NavigationBarIcon(
                                    icon = Icons.Default.Home,
                                    label = "Home",
                                    isSelected = currentScreen == PaisaScreen.Dashboard,
                                    onClick = { currentScreen = PaisaScreen.Dashboard }
                                )
                                 NavigationBarIcon(
                                    icon = Icons.Default.List,
                                    label = "Ledger",
                                    isSelected = currentScreen == PaisaScreen.History,
                                    onClick = { currentScreen = PaisaScreen.History }
                                )
                                NavigationBarIcon(
                                    icon = Icons.Default.DateRange,
                                    label = "Charts",
                                    isSelected = currentScreen == PaisaScreen.Analytics,
                                    onClick = { currentScreen = PaisaScreen.Analytics }
                                )
                            }

                            // Centered FAB spacer buffer
                            Spacer(modifier = Modifier.width(68.dp))

                            // Right navigation items
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                                NavigationBarIcon(
                                    icon = Icons.Default.ShoppingCart,
                                    label = "Budget",
                                    isSelected = currentScreen == PaisaScreen.Budget,
                                    onClick = { currentScreen = PaisaScreen.Budget }
                                )
                                NavigationBarIcon(
                                    icon = Icons.Default.Person,
                                    label = "Profile",
                                    isSelected = currentScreen == PaisaScreen.Profile,
                                    onClick = { currentScreen = PaisaScreen.Profile }
                                )
                                NavigationBarIcon(
                                    icon = Icons.Default.Settings,
                                    label = "Settings",
                                    isSelected = currentScreen == PaisaScreen.Settings,
                                    onClick = { currentScreen = PaisaScreen.Settings }
                                )
                            }
                        }

                        // Floating Action Button overlaps center perfectly
                        Box(
                            modifier = Modifier
                                .offset(y = (-20).dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PaisaTheme.DeepPurple)
                                .clickable { showAddDialog = true }
                                .shadow(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                                contentDescription = "Add Transaction"
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Animate page content transitions under 300ms
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (currentScreen in listOf(PaisaScreen.Splash, PaisaScreen.Onboarding, PaisaScreen.Auth)) 0.dp else 48.dp)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(200))
                },
                label = "page_transitions"
            ) { target ->
                when (target) {
                    PaisaScreen.Splash -> SplashScreen(viewModel, onNavigate = { currentScreen = it })
                    PaisaScreen.Onboarding -> OnboardingScreen(viewModel, onNavigate = { currentScreen = it })
                    PaisaScreen.Auth -> AuthScreen(viewModel, onNavigate = { currentScreen = it })
                    PaisaScreen.Dashboard -> DashboardScreen(viewModel, onShowAddDialog = { showAddDialog = true })
                    PaisaScreen.History -> HistoryScreen(
                        viewModel = viewModel,
                        onEditIncome = { incomeToEdit = it },
                        onEditExpense = { expenseToEdit = it }
                    )
                    PaisaScreen.Analytics -> AnalyticsScreen(viewModel)
                    PaisaScreen.Budget -> BudgetScreen(viewModel)
                    PaisaScreen.Profile -> ProfileScreen(viewModel, onNavigate = { currentScreen = it })
                    PaisaScreen.Settings -> SettingsScreen(viewModel, onNavigate = { currentScreen = it })
                }
            }
        }

        // Add Transaction Dialog popup
        if (showAddDialog) {
            TransactionActionDialog(
                viewModel = viewModel,
                currency = currencySign,
                isDark = isDark,
                onDismiss = { showAddDialog = false }
            )
        }

        // Edit Income Dialog popup
        if (incomeToEdit != null) {
            TransactionActionDialog(
                viewModel = viewModel,
                currency = currencySign,
                isDark = isDark,
                income = incomeToEdit,
                onDismiss = { incomeToEdit = null }
            )
        }

        // Edit Expense Dialog popup
        if (expenseToEdit != null) {
            TransactionActionDialog(
                viewModel = viewModel,
                currency = currencySign,
                isDark = isDark,
                expense = expenseToEdit,
                onDismiss = { expenseToEdit = null }
            )
        }
    }
}

@Composable
fun NavigationBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PaisaTheme.DeepPurple else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PaisaTheme.DeepPurple else Color.Gray,
            fontSize = 9.sp
        )
    }
}

// ----------------------------------------------------
// POPUP DIALOG FOR SUBMISSIONS / CORRECTIONS (INCOME & OUTGOINGS)
// ----------------------------------------------------
@Composable
fun TransactionActionDialog(
    viewModel: PaisaViewModel,
    currency: String,
    isDark: Boolean,
    income: Income? = null,
    expense: Expense? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    val isEditMode = income != null || expense != null

    var isExpenseTab by remember { mutableStateOf(expense != null || (!isEditMode)) }

    // Form inputs
    var amountText by remember { mutableStateOf(income?.amount?.toString() ?: expense?.amount?.toString() ?: "") }
    var noteText by remember { mutableStateOf(income?.note ?: expense?.note ?: "") }
    var selectedDateMs by remember { mutableStateOf(income?.date ?: expense?.date ?: System.currentTimeMillis()) }

    var selectedCategory by remember { mutableStateOf(expense?.category ?: ExpenseCategory.FOOD.displayName) }
    var selectedPaymentMethod by remember { mutableStateOf(expense?.paymentMethod ?: PaymentMethod.CASH.displayName) }
    var selectedSource by remember { mutableStateOf(income?.source ?: "") }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Transaction Entry" else "Log Inflows / Outgoings",
                fontWeight = FontWeight.Bold,
                color = PaisaTheme.DeepPurple,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Type selector (only in creation mode)
                if (!isEditMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isExpenseTab) PaisaTheme.DeepPurple else Color.Transparent)
                                .clickable { isExpenseTab = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Expense", fontWeight = FontWeight.Bold, color = if (isExpenseTab) Color.White else Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!isExpenseTab) PaisaTheme.DeepPurple else Color.Transparent)
                                .clickable { isExpenseTab = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Income", fontWeight = FontWeight.Bold, color = if (!isExpenseTab) Color.White else Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                // Amount Text Field (pill-shaped)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker trigger button
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDateMs))
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selected = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth)
                                }
                                selectedDateMs = selected.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.OffWhite),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ledger Date:", color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(dateStr, color = PaisaTheme.DeepPurple, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.DateRange, null, tint = PaisaTheme.DeepPurple)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isExpenseTab) {
                    // Category Chip tiles layout
                    Text("Select Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpenseCategory.values().forEach { category ->
                            val isSelected = selectedCategory == category.displayName
                            Card(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .clickable { selectedCategory = category.displayName },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PaisaTheme.DeepPurple else Color.White
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null
                            ) {
                                Text(
                                    text = category.displayName,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method Segmented pill toggle cards side-by-side
                    Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.values().forEach { mode ->
                            val isSelected = selectedPaymentMethod == mode.displayName
                            Card(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .clickable { selectedPaymentMethod = mode.displayName },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PaisaTheme.DeepPurple else Color.White
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null
                            ) {
                                Text(
                                    text = mode.displayName,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                } else {
                    // Source field for income
                    OutlinedTextField(
                        value = selectedSource,
                        onValueChange = { selectedSource = it },
                        label = { Text("Income Source") },
                        placeholder = { Text("E.g., Salary, Rent, Dividends...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note description box (pill-style)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Memo / Notes") },
                    placeholder = { Text("Short details...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            if (isSaving) {
                CircularProgressIndicator(color = PaisaTheme.DeepPurple, modifier = Modifier.size(24.dp))
            } else {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            errorMsg = "Please input a positive numeric transaction amount."
                            return@Button
                        }
                        isSaving = true
                        errorMsg = null

                        if (isExpenseTab) {
                            if (isEditMode && expense != null) {
                                viewModel.editExpense(
                                    id = expense.id, amount = amt, category = selectedCategory,
                                    note = noteText, date = selectedDateMs, paymentMethod = selectedPaymentMethod,
                                    onSuccess = { onDismiss() }, onError = { err -> errorMsg = err; isSaving = false }
                                )
                            } else {
                                viewModel.addExpense(
                                    amount = amt, category = selectedCategory, note = noteText,
                                    date = selectedDateMs, paymentMethod = selectedPaymentMethod,
                                    onSuccess = { onDismiss() }, onError = { err -> errorMsg = err; isSaving = false }
                                )
                            }
                        } else {
                            if (selectedSource.isEmpty()) {
                                errorMsg = "Please specify an income source title."
                                isSaving = false
                                return@Button
                            }
                            if (isEditMode && income != null) {
                                viewModel.editIncome(
                                    id = income.id, amount = amt, source = selectedSource,
                                    note = noteText, date = selectedDateMs,
                                    onSuccess = { onDismiss() }, onError = { err -> errorMsg = err; isSaving = false }
                                )
                            } else {
                                viewModel.addIncome(
                                    amount = amt, source = selectedSource, note = noteText,
                                    date = selectedDateMs,
                                    onSuccess = { onDismiss() }, onError = { err -> errorMsg = err; isSaving = false }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaTheme.DeepPurple)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
