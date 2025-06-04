package com.example.timemanagementkot.ui.login

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.repo.AuthRepo
import com.example.timemanagementkot.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
class LoginVM : ViewModel() {

    private val authRepo = AuthRepo()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> get() = _email

    private val _forgotEmail = MutableStateFlow("")
    val forgotEmail: StateFlow<String> get() = _forgotEmail

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> get() = _password

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> get() = _emailError

    private val _forgotEmailError = MutableStateFlow<String?>(null)
    val forgotEmailError: StateFlow<String?> get() = _forgotEmailError

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> get() = _passwordError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> get() = _showDialog

    fun showDialog() {
        _showDialog.value = true
        _forgotEmailError.value = null
    }

    fun dismissDialog() {
        _showDialog.value = false
    }

    fun login() {
        clearAllErrors()

        val email = _email.value.trim()
        val password = _password.value

        var hasError = false
        if (email.isBlank()) {
            _emailError.value = "Email không được bỏ trống"
            hasError = true
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Email không hợp lệ"
            hasError = true
        }
        if (password.isBlank()) {
            _passwordError.value = "Mật khẩu không được bỏ trống"
            hasError = true
        }
        if (password.length < 6) {
            _passwordError.value = "Mật khẩu phải có ít nhất 6 ký tự"
            hasError = true
        }

        if (hasError) {
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = authRepo.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _user.value = user
                    _errorMessage.value = null
                    _isLoading.value = false
                    _snackbarMessage.emit("Đăng nhập thành công!")
                },
                onFailure = { exception ->
                    Log.e("ViewModelError", "Error: ${exception.message}", exception)
                    val errorMsg = when {
                        exception.message?.contains("USER_NOT_FOUND") == true -> "Email không tồn tại"
                        exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> "Mật khẩu không đúng"
                        else -> "Đăng nhập thất bại"
                    }
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                }
            )
        }
    }

    fun resetPassword() {
        _forgotEmailError.value = null
        val email = _forgotEmail.value.trim()

        var hasError = false
        if (email.isBlank()) {
            _forgotEmailError.value = "Email không được bỏ trống"
            Log.d("ViewModelError", "Email trống")
            hasError = true
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotEmailError.value = "Email không hợp lệ"
            Log.d("ViewModelError", "Email không hợp lệ")
            hasError = true
        }
        if (email.length > 100) {
            _forgotEmailError.value = "Email không được vượt quá 100 ký tự"
            Log.d("ViewModelError", "Email vượt quá 100 ký tự")
            hasError = true
        }

        if (hasError) {
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            Log.d("ViewModelError", "Calling resetPassword for email: $email")
            val result = authRepo.resetPassword(email)
            result.fold(
                onSuccess = {
                    Log.d("ViewModelError", "Reset password success")
                    _snackbarMessage.emit("Đã gửi email đặt lại mật khẩu")
                    onForgotEmailChanged("")
                    _forgotEmailError.value = null
                    dismissDialog()
                },
                onFailure = { exception ->
                    Log.e("ViewModelError", "Reset password error: ${exception.message}", exception)
                    _forgotEmailError.value = when {
                        exception.message?.contains("Email chưa được đăng ký") == true -> "Email không tồn tại trong hệ thống"
                        else -> "Lỗi gửi email đặt lại mật khẩu"
                    }
                }
            )
            _isLoading.value = false
        }
    }

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
    }

    fun onForgotEmailChanged(newForgotEmail: String) {
        _forgotEmail.value = newForgotEmail
    }

    fun clearAllErrors() {
        _errorMessage.value = null
        _emailError.value = null
        _passwordError.value = null
    }

    fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}
