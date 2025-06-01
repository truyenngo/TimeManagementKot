package com.example.timemanagementkot.data.repo

import android.util.Log
import com.example.timemanagementkot.data.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class AuthRepo {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun register(email: String, password: String, displayName: String, callback: (Result<User>) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    firebaseUser.updateProfile(profileUpdates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = User(
                                    userId = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    displayName = firebaseUser.displayName ?: "Unknown"
                                )

                                Firebase.firestore.collection("users")
                                    .document(user.userId)
                                    .set(user)
                                    .addOnSuccessListener {
                                        callback(Result.success(user))
                                    }
                                    .addOnFailureListener { exception ->
                                        callback(Result.failure(Exception("Lỗi lưu vào Firestore: ${exception.message}")))
                                    }
                            } else {
                                callback(Result.failure(Exception("Không thể cập nhật tên")))
                            }
                        }
                } else {
                    callback(Result.failure(Exception("Đăng ký thất bại: không tạo được người dùng")))
                }
            }
            .addOnFailureListener { exception ->
                val errorKey = when (exception) {
                    is FirebaseAuthUserCollisionException -> "USER_EXISTS"
                    else -> "UNKNOWN"
                }
                callback(Result.failure(Exception(errorKey)))
            }
    }

    suspend fun login(email: String, password: String): Result<User> {
        val firestore = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()

        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("USER_NOT_FOUND"))
            }

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("LOGIN_FAILED"))

            val user = User(
                userId = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "Unknown"
            )
            Result.success(user)

        } catch (e: Exception) {
            val errorType = when {
                e is FirebaseAuthInvalidUserException -> "USER_NOT_FOUND"
                e is FirebaseAuthInvalidCredentialsException || e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "INVALID_LOGIN_CREDENTIALS"
                else -> "LOGIN_FAILED"
            }
            Result.failure(Exception(errorType))
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(user?.email ?: "", currentPassword)

        user?.reauthenticate(credential)?.addOnCompleteListener { reAuthTask ->
            if (reAuthTask.isSuccessful) {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError("Lỗi khi đặt mật khẩu mới: ${e.message}")
                    }
            } else {
                onError("Mật khẩu hiện tại không đúng")
            }
        } ?: onError("User chưa đăng nhập")
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        val firestore = FirebaseFirestore.getInstance()
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.e("RepoError", "Reset password error: Email not found in Firestore")
                return Result.failure(Exception("Email không tồn tại trong hệ thống."))
            }

            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RepoError", "Reset password error: ${e.javaClass.name}, message: ${e.message}", e)
            val errorMessage = when (e) {
                is FirebaseAuthInvalidUserException -> "Email không tồn tại trong hệ thống."
                else -> e.message ?: "Lỗi gửi email đặt lại mật khẩu"
            }
            Result.failure(Exception(errorMessage))
        }
    }
}
