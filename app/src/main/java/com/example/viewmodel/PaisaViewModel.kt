package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.TaskStackBuilder
import com.example.MainActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PaisaApplication
import com.example.data.*
import com.example.data.paisaPrefsDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PaisaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PaisaRepository()
    private val dataStore = PaisaDataStore(application)

    // Current Auth States
    private val _currentUserState = MutableStateFlow<FirebaseUser?>(repository.currentUser)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    private val _userProfileState = MutableStateFlow<UserProfile?>(null)
    val userProfileState: StateFlow<UserProfile?> = _userProfileState.asStateFlow()

    // Database Collections
    private val _rawIncome = MutableStateFlow<List<Income>>(emptyList())
    private val _rawExpenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _rawBudgets = MutableStateFlow<Map<String, Double>>(emptyMap())

    val rawIncomeState = _rawIncome.asStateFlow()
    val rawExpensesState = _rawExpenses.asStateFlow()

    // Search, Filters & Sorting variables
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("All") // "All", "Today", "This Week", "This Month", "Custom"
    val sortType = MutableStateFlow("Latest") // "Latest", "Oldest", "Highest Amount", "Lowest Amount"
    val customDateSelectedStart = MutableStateFlow<Long?>(null)
    val customDateSelectedEnd = MutableStateFlow<Long?>(null)

    // Local Configuration Flags (DataStore)
    val isDarkModeState = dataStore.isDarkModeEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isNotificationEnabledState = dataStore.isNotificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val selectedCurrencyState = dataStore.selectedCurrency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")
    val adCounterState = dataStore.adTransactionCounter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val isOnboardingCompleted = dataStore.isOnboardingCompleted.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val watchedPdfAdsCount = dataStore.watchedPdfAdsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isDailyRemindersEnabledState = dataStore.isDailyRemindersEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isBudgetAlertsEnabledState = dataStore.isBudgetAlertsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isMonthlySummaryEnabledState = dataStore.isMonthlySummaryEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val isWelcomeMessagesEnabledState = dataStore.isWelcomeMessagesEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderFrequencyState = dataStore.reminderFrequency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // Dynamic analytics date range filtering
    val analyticsStartDateState = MutableStateFlow<Long?>(null)
    val analyticsEndDateState = MutableStateFlow<Long?>(null)

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            dataStore.setOnboardingCompleted(completed)
        }
    }

    // UI Trigger for Start.io Interstitial Ads
    private val _showInterstitialTrigger = MutableSharedFlow<Boolean>(replay = 0)
    val showInterstitialTrigger: SharedFlow<Boolean> = _showInterstitialTrigger

    // UI Trigger for Start.io Manual Interstitial Ads
    private val _showManualInterstitialTrigger = MutableSharedFlow<Boolean>(replay = 0)
    val showManualInterstitialTrigger: SharedFlow<Boolean> = _showManualInterstitialTrigger

    // Ad loading/showing state to prevent multiple/double clicks or bypasses
    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    fun setAdLoading(loading: Boolean) {
        _isAdLoading.value = loading
    }

    // Skeleton loaders (Shimmer activation states)
    private val _isDashboardLoading = MutableStateFlow(true)
    val isDashboardLoading: StateFlow<Boolean> = _isDashboardLoading.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(true)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    // Local In-Memory record of budget notifications dispatched so we don't duplicate alerts in a single session
    private val dispatchedAlerts = mutableSetOf<String>()

    init {
        // Automatically fetch database references if user is logged in
        val user = repository.currentUser
        if (user != null) {
            Log.d("PaisaAuth", "AUTH_CURRENT_USER: UID = ${user.uid}, Email = ${user.email ?: ""}")
            startRealtimeListeners(user.uid)
        } else {
            _isDashboardLoading.value = false
            _isHistoryLoading.value = false
        }
    }

    fun startRealtimeListeners(uid: String) {
        _isDashboardLoading.value = true
        _isHistoryLoading.value = true
        
        Log.d("PaisaAuth", "PROFILE_UID: $uid")
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        Log.d("PaisaAuth", "PROFILE_EMAIL: ${currentUser?.email ?: ""}")
        
        // Trigger Welcome message notification if enabled and not shown yet
        viewModelScope.launch {
            try {
                val hasShown = dataStore.isWelcomeShown.first()
                val masterNotify = dataStore.isNotificationsEnabled.first()
                val welcomeNotify = dataStore.isWelcomeMessagesEnabled.first()
                if (!hasShown && masterNotify && welcomeNotify) {
                    val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = PaisaApplication.CHANNEL_GENERAL
                    val clickIntent = Intent(getApplication(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val clickPendingIntent = TaskStackBuilder.create(getApplication()).run {
                        addNextIntentWithParentStack(clickIntent)
                        getPendingIntent(
                            777,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                    val notification = NotificationCompat.Builder(getApplication(), channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Welcome to Paisa Tracker")
                        .setContentText("Track every rupee and build every dream.")
                        .setStyle(NotificationCompat.BigTextStyle().bigText("Track every rupee and build every dream."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(clickPendingIntent)
                        .build()
                    manager.notify(777, notification)
                    
                    dataStore.setWelcomeShown(true)
                    Log.d("PaisaNotification", "NOTIFICATION_SENT: Welcome Message")
                }
            } catch (e: Exception) {
                Log.e("PaisaNotification", "Welcome notification issue: ${e.message}")
            }
        }
        
        viewModelScope.launch {
            // Observe profile details
            repository.observeUserProfile(uid)
                .catch { e ->
                    Log.e("PaisaAuth", "PROFILE_FETCH_FAILED: ${e.message}")
                    _isDashboardLoading.value = false
                }
                .collect { profile ->
                    if (profile == null) {
                        Log.d("PaisaAuth", "PROFILE_EXISTS: false")
                        // Create missing profile node
                        val creationTime = currentUser?.metadata?.creationTimestamp ?: System.currentTimeMillis()
                        val joinedDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(creationTime))
                        val email = currentUser?.email ?: ""
                        val name = currentUser?.displayName ?: email.substringBefore("@")
                        
                        val newProfile = com.example.data.UserProfile(
                            uid = uid,
                            displayName = if (name.isEmpty()) "User" else name,
                            email = email,
                            joinedDate = joinedDate
                        )
                        Log.d("PaisaAuth", "PROFILE_MIGRATION_CREATED: Creating fallback profile for user $uid")
                        com.google.firebase.database.FirebaseDatabase.getInstance().reference
                            .child("users").child(uid).child("profile").setValue(newProfile)
                    } else {
                        Log.d("PaisaAuth", "PROFILE_EXISTS: true")
                        Log.d("PaisaAuth", "PROFILE_FETCH_SUCCESS")
                        
                        // Check missing fields for patching
                        var needsPatch = false
                        var patchedName = profile.displayName
                        var patchedEmail = profile.email
                        var patchedJoinedDate = profile.joinedDate
                        
                        if (patchedName.isEmpty()) {
                            patchedName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
                            needsPatch = true
                        }
                        if (patchedEmail.isEmpty() && currentUser?.email != null) {
                            patchedEmail = currentUser.email!!
                            needsPatch = true
                        }
                        if (patchedJoinedDate.isEmpty()) {
                            val creationTime = currentUser?.metadata?.creationTimestamp ?: System.currentTimeMillis()
                            patchedJoinedDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(creationTime))
                            needsPatch = true
                        }
                        
                        if (needsPatch) {
                            val patchedProfile = profile.copy(
                                displayName = patchedName,
                                email = patchedEmail,
                                joinedDate = patchedJoinedDate
                            )
                            Log.d("PaisaAuth", "PROFILE_MIGRATION_CREATED: Patching missing fields in existing profile of user $uid")
                            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                                .child("users").child(uid).child("profile").setValue(patchedProfile)
                        } else {
                            _userProfileState.value = profile
                        }
                    }
                    _isDashboardLoading.value = false
                }
        }

        viewModelScope.launch {
            // Observe parallel datasets using combined flows
            launch {
                repository.observeIncome(uid)
                    .catch { Log.e("PaisaViewModel", "Income sync fail: ${it.message}") }
                    .collect { incomeList ->
                        _rawIncome.value = incomeList
                        _isDashboardLoading.value = false
                        _isHistoryLoading.value = false
                        evaluateMonthlySummaryNotification()
                    }
            }

            launch {
                repository.observeExpenses(uid)
                    .catch { Log.e("PaisaViewModel", "Expenses sync fail: ${it.message}") }
                    .collect { expenseList ->
                        _rawExpenses.value = expenseList
                        // Recalculate budgets alerts
                        evaluateBudgetUsageAlerts(expenseList, _rawBudgets.value)
                        _isDashboardLoading.value = false
                        _isHistoryLoading.value = false
                        evaluateMonthlySummaryNotification()
                    }
            }

            launch {
                repository.observeBudgets(uid)
                    .catch { Log.e("PaisaViewModel", "Budgets sync fail: ${it.message}") }
                    .collect { budgetMap ->
                        _rawBudgets.value = budgetMap
                        evaluateBudgetUsageAlerts(_rawExpenses.value, budgetMap)
                    }
            }
        }
    }

    // AUTHENTICATION UTILS
    fun register(email: String, password: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = repository.registerUser(email, password, name)
                _currentUserState.value = user
                startRealtimeListeners(user.uid)
                onSuccess()
            } catch (e: Exception) {
                var friendlyError = "Registration error occurred."
                if (e is com.google.firebase.auth.FirebaseAuthException) {
                    val errorCode = e.errorCode
                    friendlyError = when (errorCode) {
                        "ERROR_INVALID_EMAIL", "invalid-email" -> "Please enter a valid email address."
                        "ERROR_WEAK_PASSWORD", "weak-password" -> "The password must be at least 6 characters long."
                        "ERROR_EMAIL_ALREADY_IN_USE", "email-already-in-use" -> "This email address is already in use by another account."
                        else -> e.localizedMessage ?: friendlyError
                    }
                } else {
                    friendlyError = e.localizedMessage ?: friendlyError
                }
                onError(friendlyError)
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Log.d("PaisaAuth", "AUTH_LOGIN_STARTED")
        viewModelScope.launch {
            try {
                val user = repository.loginUser(email, password)
                Log.d("PaisaAuth", "AUTH_LOGIN_SUCCESS")
                Log.d("PaisaAuth", "AUTH_CURRENT_USER: UID = ${user.uid}, Email = ${user.email ?: ""}")
                _currentUserState.value = user
                startRealtimeListeners(user.uid)
                onSuccess()
            } catch (e: Exception) {
                Log.e("PaisaAuth", "AUTH_LOGIN_FAILED")
                Log.e("PaisaAuth", "AUTH_EXCEPTION: ${e.message}", e)
                
                var friendlyError = "Authentication failed. Please try again."
                if (e is com.google.firebase.auth.FirebaseAuthException) {
                    val errorCode = e.errorCode
                    Log.e("PaisaAuth", "AUTH_ERROR_CODE: $errorCode")
                    friendlyError = when (errorCode) {
                        "ERROR_INVALID_EMAIL", "invalid-email" -> "Please enter a valid email address."
                        "ERROR_WRONG_PASSWORD", "wrong-password" -> "Incorrect password."
                        "ERROR_USER_NOT_FOUND", "user-not-found" -> "No account found with this email."
                        "ERROR_USER_DISABLED", "user-disabled" -> "This account has been disabled."
                        "ERROR_INVALID_CREDENTIAL", "invalid-credential" -> "Invalid login credentials. Please try again."
                        else -> e.localizedMessage ?: friendlyError
                    }
                } else {
                    friendlyError = e.localizedMessage ?: friendlyError
                }
                onError(friendlyError)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        repository.logout()
        _currentUserState.value = null
        _userProfileState.value = null
        _rawIncome.value = emptyList()
        _rawExpenses.value = emptyList()
        _rawBudgets.value = emptyMap()
        onComplete()
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val trimmedEmail = email.trim()
        Log.d("PaisaAuth", "PASSWORD_RESET_STARTED")
        if (trimmedEmail.isEmpty()) {
            Log.d("PaisaAuth", "PASSWORD_RESET_FAILED: Empty email")
            onError("Email address cannot be empty.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            Log.d("PaisaAuth", "PASSWORD_RESET_FAILED: Invalid format")
            onError("Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            try {
                repository.sendPasswordResetEmail(trimmedEmail)
                Log.d("PaisaAuth", "PASSWORD_RESET_SUCCESS")
                onSuccess()
            } catch (e: Exception) {
                var friendlyError = "Failed to dispatch password recovery email."
                if (e is com.google.firebase.auth.FirebaseAuthException) {
                    val errorCode = e.errorCode
                    friendlyError = when (errorCode) {
                        "ERROR_INVALID_EMAIL", "invalid-email" -> "Please enter a valid email address."
                        "ERROR_USER_NOT_FOUND", "user-not-found" -> "No account found with this email."
                        else -> e.localizedMessage ?: friendlyError
                    }
                } else {
                    friendlyError = e.localizedMessage ?: friendlyError
                }
                Log.d("PaisaAuth", "PASSWORD_RESET_FAILED: $friendlyError")
                onError(friendlyError)
            }
        }
    }

    fun sendEmailVerification(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.sendEmailVerification()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to dispatch email verification link.")
            }
        }
    }

    fun deleteUserAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAccount()
                _currentUserState.value = null
                _userProfileState.value = null
                _rawIncome.value = emptyList()
                _rawExpenses.value = emptyList()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete user account securely.")
            }
        }
    }

    fun updateProfile(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value
        val profile = _userProfileState.value
        if (user != null && profile != null) {
            viewModelScope.launch {
                try {
                    repository.updateUserProfileName(user.uid, name, profile.email, profile.joinedDate)
                    onSuccess()
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Profile revision fail.")
                }
            }
        }
    }

    // TRANSACTIONS SUBMISSIONS (Income / Expenses)
    fun addIncome(amount: Double, source: String, note: String, date: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        if (amount <= 0.0) {
            onError("Amount must always remain positive.")
            return
        }
        viewModelScope.launch {
            try {
                val income = Income(amount = amount, source = source, note = note, date = date)
                repository.saveIncome(user.uid, income)
                triggerInterstitialsCounter()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save Income record.")
            }
        }
    }

    fun addExpense(amount: Double, category: String, note: String, date: Long, paymentMethod: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        if (amount <= 0.0) {
            onError("Amount must always remain positive.")
            return
        }
        viewModelScope.launch {
            try {
                val expense = Expense(amount = amount, category = category, note = note, date = date, paymentMethod = paymentMethod)
                repository.saveExpense(user.uid, expense)
                triggerInterstitialsCounter()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save Expense record.")
            }
        }
    }

    fun editIncome(id: String, amount: Double, source: String, note: String, date: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        if (amount <= 0.0) {
            onError("Amount must always remain positive.")
            return
        }
        viewModelScope.launch {
            try {
                val income = Income(id = id, amount = amount, source = source, note = note, date = date)
                repository.saveIncome(user.uid, income)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to edit Income record.")
            }
        }
    }

    fun editExpense(id: String, amount: Double, category: String, note: String, date: Long, paymentMethod: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        if (amount <= 0.0) {
            onError("Amount must always remain positive.")
            return
        }
        viewModelScope.launch {
            try {
                val expense = Expense(id = id, amount = amount, category = category, note = note, date = date, paymentMethod = paymentMethod)
                repository.saveExpense(user.uid, expense)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to edit Expense record.")
            }
        }
    }

    fun deleteIncomeRecord(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteIncome(user.uid, id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Deleted income record failure.")
            }
        }
    }

    fun deleteExpenseRecord(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteExpense(user.uid, id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Deleted expense record failure.")
            }
        }
    }

    // BUDGET MODIFICATION
    fun changeBudget(yearMonth: String, amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUserState.value ?: return
        viewModelScope.launch {
            try {
                repository.saveBudget(user.uid, yearMonth, amount)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update monthly budget threshold.")
            }
        }
    }

    // SETTINGS / LOCAL PREFS
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { dataStore.setDarkModeEnabled(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch { dataStore.setNotificationsEnabled(enabled) }
    }

    fun setDailyRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setDailyRemindersEnabled(enabled) }
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setBudgetAlertsEnabled(enabled) }
    }

    fun setMonthlySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setMonthlySummaryEnabled(enabled) }
    }

    fun setWelcomeMessagesEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setWelcomeMessagesEnabled(enabled) }
    }

    fun setReminderFrequency(freq: Int) {
        viewModelScope.launch { dataStore.setReminderFrequency(freq) }
    }

    fun changeCurrency(currency: String) {
        viewModelScope.launch { dataStore.setSelectedCurrency(currency) }
    }

    // INTERSTITIAL ADS COORDINATION
    private suspend fun triggerInterstitialsCounter() {
        // Record last transaction date
        dataStore.updateLastTransactionDate()
        // Increment count and retrieve the incremented count safely
        val count = dataStore.incrementAdTransactionCounter()
        Log.d("PaisaAds", "INTERSTITIAL_COUNTER: $count")
        if (count >= 2) {
            // Emits an active trigger to our UI to render StartApp interstitial ad after every 2 successful transactions
            _showInterstitialTrigger.emit(true)
        }
    }

    fun resetAdCounter() {
        viewModelScope.launch {
            dataStore.resetAdTransactionCounter()
            Log.d("PaisaAds", "INTERSTITIAL_COUNTER: 0")
        }
    }

    fun triggerManualInterstitial() {
        viewModelScope.launch {
            _showInterstitialTrigger.emit(true)
        }
    }

    fun triggerManualInterstitialAd() {
        if (_isAdLoading.value) return // Prevent bypass triggered during load
        viewModelScope.launch {
            _showManualInterstitialTrigger.emit(true)
        }
    }

    fun incrementPdfAdsCount() {
        viewModelScope.launch {
            dataStore.incrementWatchedPdfAdsCount()
        }
    }

    fun resetPdfAdsCount() {
        viewModelScope.launch {
            dataStore.resetWatchedPdfAdsCount()
        }
    }

    fun setAnalyticsDateRange(start: Long?, end: Long?) {
        analyticsStartDateState.value = start
        analyticsEndDateState.value = end
    }

    // REACTIVE DATA BLOCKS FOR STATE & UI CONSUMPTION
    
    // Core balances
    val overallStatistics = combine(_rawIncome, _rawExpenses) { incomes, expenses ->
        val totalIncome = incomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val balance = totalIncome - totalExpense
        
        // Calculate dynamic month specifics (current)
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        val monthlyIncomes = incomes.filter { sdf.format(Date(it.date)) == currentMonthPrefix }
        val monthlyExpenses = expenses.filter { sdf.format(Date(it.date)) == currentMonthPrefix }
        
        val sumMonthlyIncome = monthlyIncomes.sumOf { it.amount }
        val sumMonthlyExpense = monthlyExpenses.sumOf { it.amount }
        val monthlyBalance = sumMonthlyIncome - sumMonthlyExpense

        PaisaStatistics(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            currentBalance = balance,
            monthlyIncome = sumMonthlyIncome,
            monthlyExpense = sumMonthlyExpense,
            monthlyBalance = monthlyBalance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaisaStatistics())

    // All combined transactions map
    private val allTransactionsListFlow = combine(_rawIncome, _rawExpenses) { incomes, expenses ->
        val list = mutableListOf<TransactionItem>()
        incomes.forEach {
            list.add(
                TransactionItem(
                    id = it.id,
                    amount = it.amount,
                    title = it.source,
                    note = it.note,
                    date = it.date,
                    isIncome = true
                )
            )
        }
        expenses.forEach {
            list.add(
                TransactionItem(
                    id = it.id,
                    amount = it.amount,
                    title = it.category,
                    note = it.note,
                    date = it.date,
                    isIncome = false,
                    paymentMethod = it.paymentMethod,
                    category = it.category
                )
            )
        }
        // DEFAULT: Latest First
        list.sortedByDescending { it.date }
    }

    // History combined with reactive filters & search operations
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactionsState = combine(
        allTransactionsListFlow,
        searchQuery,
        filterType,
        sortType,
        combine(customDateSelectedStart, customDateSelectedEnd) { s, e -> Pair(s, e) }
    ) { baseList, query, filter, sort, range ->
        var list = baseList.toList()

        // 1. Filter by Search Query
        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.note.contains(query, ignoreCase = true) ||
                it.amount.toString().contains(query)
            }
        }

        // 2. Filter by Date Range type
        val nowCal = Calendar.getInstance()
        list = when (filter) {
            "Today" -> {
                list.filter { isSameDay(it.date, nowCal.timeInMillis) }
            }
            "This Week" -> {
                val startOfWeek = getStartOfWeek()
                list.filter { it.date >= startOfWeek }
            }
            "This Month" -> {
                val startOfMonth = getStartOfMonth()
                list.filter { it.date >= startOfMonth }
            }
            "Custom" -> {
                val start = range.first ?: 0L
                val end = range.second ?: Long.MAX_VALUE
                list.filter { it.date in start..end }
            }
            else -> list // All
        }

        // 3. Sort
        list = when (sort) {
            "Oldest" -> list.sortedBy { it.date }
            "Highest Amount" -> list.sortedByDescending { it.amount }
            "Lowest Amount" -> list.sortedBy { it.amount }
            else -> list.sortedByDescending { it.date } // Latest first
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // budgets monitoring
    val activeBudgetMonitoring = combine(_rawExpenses, _rawBudgets) { expenses, budgets ->
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        val budgetLimit = budgets[currentMonthPrefix] ?: 0.0
        val monthExpenseSum = expenses
            .filter { sdf.format(Date(it.date)) == currentMonthPrefix }
            .sumOf { it.amount }

        val ratio = if (budgetLimit > 0.0) monthExpenseSum / budgetLimit else 0.0
        val percent = (ratio * 100).coerceAtMost(100.0)

        BudgetSummary(
            limit = budgetLimit,
            spent = monthExpenseSum,
            percentage = percent,
            monthString = currentMonthPrefix
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetSummary())

    // Historical dynamic analytics values
    val currentAnalyticsState = combine(
        _rawIncome, _rawExpenses, analyticsStartDateState, analyticsEndDateState
    ) { incomes, expenses, start, end ->
        val hasCustomRange = start != null && end != null

        val monthIncomes = if (hasCustomRange) {
            incomes.filter { it.date in start!!..end!! }
        } else {
            val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            incomes.filter { sdf.format(Date(it.date)) == currentMonthPrefix }
        }

        val monthExpenses = if (hasCustomRange) {
            expenses.filter { it.date in start!!..end!! }
        } else {
            val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            expenses.filter { sdf.format(Date(it.date)) == currentMonthPrefix }
        }

        val totalInc = monthIncomes.sumOf { it.amount }
        val totalExp = monthExpenses.sumOf { it.amount }
        val maxInc = if (monthIncomes.isNotEmpty()) monthIncomes.maxOf { it.amount } else 0.0
        val maxExp = if (monthExpenses.isNotEmpty()) monthExpenses.maxOf { it.amount } else 0.0

        // Daily Averages calculation
        val daysInPeriod = if (hasCustomRange) {
            val diffMs = end!! - start!!
            val computedDays = (diffMs / (1000 * 60 * 60 * 24)).toInt() + 1
            computedDays.coerceAtLeast(1)
        } else {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        }

        val avgDailyInc = if (daysInPeriod > 0) totalInc / daysInPeriod else 0.0
        val avgDailyExp = if (daysInPeriod > 0) totalExp / daysInPeriod else 0.0

        // Category Breakdowns
        val breakdown = mutableMapOf<String, Double>()
        ExpenseCategory.values().forEach { category ->
            val sum = monthExpenses.filter { it.category.equals(category.displayName, ignoreCase = true) }.sumOf { it.amount }
            if (sum > 0.0) {
                breakdown[category.displayName] = sum
            }
        }
        val unmappedSum = monthExpenses.filter { exp -> 
            ExpenseCategory.values().none { it.displayName.equals(exp.category, ignoreCase = true) }
        }.sumOf { exp -> exp.amount }
        if (unmappedSum > 0.0) {
            breakdown["Other"] = (breakdown["Other"] ?: 0.0) + unmappedSum
        }

        AnalyticsBreakdown(
            monthlyIncome = totalInc,
            monthlyExpense = totalExp,
            netBalance = totalInc - totalExp,
            totalTransactions = monthIncomes.size + monthExpenses.size,
            highestIncome = maxInc,
            highestExpense = maxExp,
            averageDailyIncome = avgDailyInc,
            averageDailyExpense = avgDailyExp,
            categoryBreakdown = breakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsBreakdown())

    // ----------------------------------------------------
    // EVALUATING BUDGET MILESTONES (DYNAMIC NOTIFICATIONS)
    // ----------------------------------------------------
    private fun evaluateBudgetUsageAlerts(expensesList: List<Expense>, budgetLimits: Map<String, Double>) {
        if (!isNotificationEnabledState.value || !isBudgetAlertsEnabledState.value) return // User has disabled notifications in Preferences!

        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        val budgetLimit = budgetLimits[currentMonthPrefix] ?: return
        if (budgetLimit <= 0.0) return

        val monthExpenseSum = expensesList
            .filter { sdf.format(Date(it.date)) == currentMonthPrefix }
            .sumOf { it.amount }

        val ratio = monthExpenseSum / budgetLimit

        // Triggers alarm notices on milestones
        when {
            ratio >= 1.0 -> dispatchBudgetMilestoneNotification("100%", "You have used 100% of your Monthly Budget limit ($monthExpenseSum / $budgetLimit). Please manage carefully!")
            ratio >= 0.90 -> dispatchBudgetMilestoneNotification("90%", "Budget Warning: You have reached 90% of your Monthly Budget ($monthExpenseSum / $budgetLimit).")
            ratio >= 0.75 -> dispatchBudgetMilestoneNotification("75%", "Budget Advisory: You have consumed 75% of your Monthly Budget ($monthExpenseSum / $budgetLimit).")
            ratio >= 0.50 -> dispatchBudgetMilestoneNotification("50%", "Budgets Info: You have reached 50% of your Monthly Budget.")
        }
    }

    private fun dispatchBudgetMilestoneNotification(level: String, text: String) {
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val alertKey = "$currentMonthPrefix-$level"
        
        // Prevent excessive duplicate notifications within the same month/alert session
        if (dispatchedAlerts.contains(alertKey)) return

        try {
            val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val clickIntent = Intent(getApplication(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val clickPendingIntent = TaskStackBuilder.create(getApplication()).run {
                addNextIntentWithParentStack(clickIntent)
                getPendingIntent(
                    level.hashCode(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            val notification = NotificationCompat.Builder(getApplication(), PaisaApplication.CHANNEL_BUDGETS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Budget Alert: $level Reached!")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setContentIntent(clickPendingIntent)
                .build()

            manager.notify(level.hashCode() + currentMonthPrefix.hashCode(), notification)
            dispatchedAlerts.add(alertKey)
            Log.d("PaisaNotification", "NOTIFICATION_SENT: Budget Alert $level")
        } catch (e: Exception) {
            Log.e("PaisaViewModel", "Milestone notification issue: ${e.message}")
        }
    }

    fun evaluateMonthlySummaryNotification() {
        viewModelScope.launch {
            try {
                val masterNotify = dataStore.isNotificationsEnabled.first()
                val summaryNotify = dataStore.isMonthlySummaryEnabled.first()
                if (!masterNotify || !summaryNotify) return@launch
                
                val calendar = Calendar.getInstance()
                val today = calendar.get(Calendar.DAY_OF_MONTH)
                val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (today != lastDay) {
                    return@launch
                }
                
                val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val app = getApplication<Application>()
                val lastSummaryMonth = app.paisaPrefsDataStore.data.first()[stringPreferencesKey("last_summary_month")] ?: ""
                if (lastSummaryMonth == currentMonthPrefix) {
                    return@launch
                }
                
                val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthIncome = _rawIncome.value
                    .filter { sdf.format(Date(it.date)) == currentMonthPrefix }
                    .sumOf { it.amount }
                val currentMonthExpense = _rawExpenses.value
                    .filter { sdf.format(Date(it.date)) == currentMonthPrefix }
                    .sumOf { it.amount }
                val balance = currentMonthIncome - currentMonthExpense
                
                val currencyCode = selectedCurrencyState.value
                val currencySign = getCurrencySymbol(currencyCode)
                
                val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = PaisaApplication.CHANNEL_SUMMARY
                val text = "Income: $currencySign$currentMonthIncome, Expense: $currencySign$currentMonthExpense, Balance: $currencySign$balance"
                val clickIntent = Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val clickPendingIntent = TaskStackBuilder.create(app).run {
                    addNextIntentWithParentStack(clickIntent)
                    getPendingIntent(
                        888,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                val notification = NotificationCompat.Builder(app, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("This Month Summary")
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(clickPendingIntent)
                    .build()
                    
                manager.notify(888, notification)
                
                app.paisaPrefsDataStore.edit { preferences ->
                    preferences[stringPreferencesKey("last_summary_month")] = currentMonthPrefix
                }
                
                Log.d("PaisaNotification", "NOTIFICATION_SENT: Monthly Summary")
            } catch (e: Exception) {
                Log.e("PaisaNotification", "Failed to dispatch Monthly Summary: ${e.message}")
            }
        }
    }

    private fun getCurrencySymbol(code: String): String {
        return when (code) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }

    // DATE ASSISTANCE UTILS
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getStartOfWeek(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

// DATA WRAPPERS
data class PaisaStatistics(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyBalance: Double = 0.0
)

data class BudgetSummary(
    val limit: Double = 0.0,
    val spent: Double = 0.0,
    val percentage: Double = 0.0,
    val monthString: String = ""
)

data class AnalyticsBreakdown(
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalTransactions: Int = 0,
    val highestIncome: Double = 0.0,
    val highestExpense: Double = 0.0,
    val averageDailyIncome: Double = 0.0,
    val averageDailyExpense: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap()
)
