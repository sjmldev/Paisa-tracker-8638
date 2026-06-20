package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class PaisaRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    // Dynamically connecting to the Realtime Database configured in google-services.json
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // ----------------------------------------------------
    // AUTHENTICATION OPERATIONS
    // ----------------------------------------------------

    suspend fun registerUser(email: String, password: String, fullName: String): FirebaseUser = suspendCancellableCoroutine { continuation ->
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        // Create initial profile in Realtime Database
                        val profileRef = database.child("users").child(user.uid).child("profile")
                        val joinedDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        val profile = UserProfile(uid = user.uid, displayName = fullName, email = email, joinedDate = joinedDate)
                        
                        profileRef.setValue(profile).addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                continuation.resume(user)
                            } else {
                                // Realtime Database write rules might be restricted or pending setup, but the Auth user itself has been created.
                                // We resume successfully so the user can use the app immediately, logging the warning gracefully
                                android.util.Log.e("PaisaRepository", "Database profile write failed: ${profileTask.exception?.message}. Continuing with Auth anyway.")
                                continuation.resume(user)
                            }
                        }
                    } else {
                        continuation.resumeWithException(Exception("Empty Firebase user response"))
                    }
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Registration failed"))
                }
            }
    }

    suspend fun loginUser(email: String, password: String): FirebaseUser = suspendCancellableCoroutine { continuation ->
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        continuation.resume(user)
                    } else {
                        continuation.resumeWithException(Exception("Empty authenticated user payload"))
                    }
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Authentication failed"))
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String): Unit = suspendCancellableCoroutine { continuation ->
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Password reset link failure"))
                }
            }
    }

    suspend fun sendEmailVerification(): Unit = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user != null) {
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(task.exception ?: Exception("Verification email failure"))
                    }
                }
        } else {
            continuation.resumeWithException(Exception("No currently authenticated user sessions"))
        }
    }

    suspend fun deleteAccount(): Unit = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            // 1. Delete DB structure for the user first to conform with security
            database.child("users").child(uid).removeValue().addOnCompleteListener { dbTask ->
                // Proceed with actual Auth account deletion
                user.delete().addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        continuation.resume(Unit)
                    } else {
                        // Even if DB deletion failed or completed, capture the Auth failure
                        continuation.resumeWithException(authTask.exception ?: Exception("Account deletion failed"))
                    }
                }
            }
        } else {
            continuation.resumeWithException(Exception("No active user session to delete"))
        }
    }

    // ----------------------------------------------------
    // DATABASE READ FLOWS
    // ----------------------------------------------------

    fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val profileRef = database.child("users").child(uid).child("profile")
        profileRef.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                trySend(profile)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        profileRef.addValueEventListener(listener)
        awaitClose { profileRef.removeEventListener(listener) }
    }

    fun observeIncome(uid: String): Flow<List<Income>> = callbackFlow {
        val incomeRef = database.child("users").child(uid).child("income")
        incomeRef.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val incomeList = mutableListOf<Income>()
                for (child in snapshot.children) {
                    val income = child.getValue(Income::class.java)
                    if (income != null) {
                        incomeList.add(income)
                    }
                }
                trySend(incomeList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        incomeRef.addValueEventListener(listener)
        awaitClose { incomeRef.removeEventListener(listener) }
    }

    fun observeExpenses(uid: String): Flow<List<Expense>> = callbackFlow {
        val expensesRef = database.child("users").child(uid).child("expenses")
        expensesRef.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenseList = mutableListOf<Expense>()
                for (child in snapshot.children) {
                    val expense = child.getValue(Expense::class.java)
                    if (expense != null) {
                        expenseList.add(expense)
                    }
                }
                trySend(expenseList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        expensesRef.addValueEventListener(listener)
        awaitClose { expensesRef.removeEventListener(listener) }
    }

    fun observeBudgets(uid: String): Flow<Map<String, Double>> = callbackFlow {
        val budgetsRef = database.child("users").child(uid).child("budgets")
        budgetsRef.keepSynced(true)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val budgetMap = mutableMapOf<String, Double>()
                for (child in snapshot.children) {
                    val value = child.getValue(Double::class.java) ?: child.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    child.key?.let { budgetMap[it] = value }
                }
                trySend(budgetMap)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        budgetsRef.addValueEventListener(listener)
        awaitClose { budgetsRef.removeEventListener(listener) }
    }

    // ----------------------------------------------------
    // DATABASE WRITE CONCURRENCY Operations
    // ----------------------------------------------------

    suspend fun saveIncome(uid: String, income: Income): Unit = suspendCancellableCoroutine { continuation ->
        val id = if (income.id.isEmpty()) database.child("users").child(uid).child("income").push().key ?: java.util.UUID.randomUUID().toString() else income.id
        val finalIncome = income.copy(id = id)
        database.child("users").child(uid).child("income").child(id).setValue(finalIncome)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to write Income"))
                }
            }
    }

    suspend fun deleteIncome(uid: String, incomeId: String): Unit = suspendCancellableCoroutine { continuation ->
        database.child("users").child(uid).child("income").child(incomeId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to delete Income"))
                }
            }
    }

    suspend fun saveExpense(uid: String, expense: Expense): Unit = suspendCancellableCoroutine { continuation ->
        val id = if (expense.id.isEmpty()) database.child("users").child(uid).child("expenses").push().key ?: java.util.UUID.randomUUID().toString() else expense.id
        val finalExpense = expense.copy(id = id)
        database.child("users").child(uid).child("expenses").child(id).setValue(finalExpense)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to write Expense"))
                }
            }
    }

    suspend fun deleteExpense(uid: String, expenseId: String): Unit = suspendCancellableCoroutine { continuation ->
        database.child("users").child(uid).child("expenses").child(expenseId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to delete Expense"))
                }
            }
    }

    suspend fun saveBudget(uid: String, yearMonth: String, amount: Double): Unit = suspendCancellableCoroutine { continuation ->
        database.child("users").child(uid).child("budgets").child(yearMonth).setValue(amount)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to save Budget"))
                }
            }
    }

    suspend fun updateUserProfileName(uid: String, name: String, email: String, joinedDate: String): Unit = suspendCancellableCoroutine { continuation ->
        val profile = UserProfile(uid = uid, displayName = name, email = email, joinedDate = joinedDate)
        database.child("users").child(uid).child("profile").setValue(profile)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(task.exception ?: Exception("Failed to update profile name"))
                }
            }
    }
}
