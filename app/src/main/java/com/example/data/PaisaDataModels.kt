package com.example.data

import java.io.Serializable

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val joinedDate: String = ""
) : Serializable

data class Income(
    val id: String = "",
    val amount: Double = 0.0,
    val source: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis()
) : Serializable

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: String = ""
) : Serializable

data class Budget(
    val amount: Double = 0.0,
    val month: String = "" // format "yyyy-MM", e.g., "2026-06"
) : Serializable

enum class ExpenseCategory(val displayName: String) {
    FOOD("Food"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    TRANSPORT("Transport"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    TRAVEL("Travel"),
    EDUCATION("Education"),
    OTHER("Other")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CARD("Card"),
    UPI("UPI"),
    NET_BANKING("Net Banking")
}

data class TransactionItem(
    val id: String,
    val amount: Double,
    val title: String, // Source for income, Category/Note for expense
    val note: String,
    val date: Long,
    val isIncome: Boolean,
    val paymentMethod: String = "",
    val category: String = ""
) : Serializable
