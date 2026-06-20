package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property on Context
val Context.paisaPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "paisa_preferences")

class PaisaDataStore(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        val AD_TRANSACTION_COUNTER = intPreferencesKey("ad_transaction_counter")
        val WATCHED_PDF_ADS_COUNT = intPreferencesKey("watched_pdf_ads_count")
        
        val DAILY_REMINDERS_ENABLED = booleanPreferencesKey("daily_reminders_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val MONTHLY_SUMMARY_ENABLED = booleanPreferencesKey("monthly_summary_enabled")
        val WELCOME_MESSAGES_ENABLED = booleanPreferencesKey("welcome_messages_enabled")
        val REMINDER_FREQUENCY = intPreferencesKey("reminder_frequency")
        val WELCOME_SHOWN = booleanPreferencesKey("welcome_shown")
    }

    // 1. Onboarding Flow
    val isOnboardingCompleted: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    // 2. Dark Mode
    val isDarkModeEnabled: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[DARK_MODE_ENABLED] ?: false // Default to light mode
        }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[DARK_MODE_ENABLED] = enabled
        }
    }

    // 3. Notifications Enabled
    val isNotificationsEnabled: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true // Default enabled
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // 4. Selected Currency (INR, USD, EUR, GBP)
    val selectedCurrency: Flow<String> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[SELECTED_CURRENCY] ?: "INR" // Default is Indian Rupee (₹)
        }

    suspend fun setSelectedCurrency(currency: String) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[SELECTED_CURRENCY] = currency
        }
    }

    // 5. Ad Transaction Counter
    val adTransactionCounter: Flow<Int> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[AD_TRANSACTION_COUNTER] ?: 0
        }

    suspend fun incrementAdTransactionCounter(): Int {
        var currentCounter = 0
        context.paisaPrefsDataStore.edit { preferences ->
            val nextCounter = (preferences[AD_TRANSACTION_COUNTER] ?: 0) + 1
            preferences[AD_TRANSACTION_COUNTER] = nextCounter
            currentCounter = nextCounter
        }
        return currentCounter
    }

    suspend fun resetAdTransactionCounter() {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[AD_TRANSACTION_COUNTER] = 0
        }
    }

    suspend fun updateLastTransactionDate() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[stringPreferencesKey("last_transaction_date")] = today
        }
    }

    // 6. Watched PDF Ads Counter
    val watchedPdfAdsCount: Flow<Int> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[WATCHED_PDF_ADS_COUNT] ?: 0
        }

    suspend fun incrementWatchedPdfAdsCount(): Int {
        var currentCounter = 0
        context.paisaPrefsDataStore.edit { preferences ->
            val nextCounter = (preferences[WATCHED_PDF_ADS_COUNT] ?: 0) + 1
            preferences[WATCHED_PDF_ADS_COUNT] = nextCounter
            currentCounter = nextCounter
        }
        return currentCounter
    }

    suspend fun resetWatchedPdfAdsCount() {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[WATCHED_PDF_ADS_COUNT] = 0
        }
    }

    // 7. Custom Notification Settings
    val isDailyRemindersEnabled: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[DAILY_REMINDERS_ENABLED] ?: true
        }

    suspend fun setDailyRemindersEnabled(enabled: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[DAILY_REMINDERS_ENABLED] = enabled
        }
    }

    val isBudgetAlertsEnabled: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[BUDGET_ALERTS_ENABLED] ?: true
        }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[BUDGET_ALERTS_ENABLED] = enabled
        }
    }

    val isMonthlySummaryEnabled: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[MONTHLY_SUMMARY_ENABLED] ?: true
        }

    suspend fun setMonthlySummaryEnabled(enabled: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[MONTHLY_SUMMARY_ENABLED] = enabled
        }
    }

    val isWelcomeMessagesEnabled: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[WELCOME_MESSAGES_ENABLED] ?: true
        }

    suspend fun setWelcomeMessagesEnabled(enabled: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[WELCOME_MESSAGES_ENABLED] = enabled
        }
    }

    val reminderFrequency: Flow<Int> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[REMINDER_FREQUENCY] ?: 5
        }

    suspend fun setReminderFrequency(freq: Int) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[REMINDER_FREQUENCY] = freq
        }
    }

    val isWelcomeShown: Flow<Boolean> = context.paisaPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { preferences ->
            preferences[WELCOME_SHOWN] ?: false
        }

    suspend fun setWelcomeShown(shown: Boolean) {
        context.paisaPrefsDataStore.edit { preferences ->
            preferences[WELCOME_SHOWN] = shown
        }
    }
}
