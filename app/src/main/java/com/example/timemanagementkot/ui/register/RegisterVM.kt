package com.example.timemanagementkot.ui.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timemanagementkot.data.model.User
import com.example.timemanagementkot.data.repo.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterVM : ViewModel() {

    private val authRepo = AuthRepo()

    // Input fields
    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> get() = _displayName

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> get() = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> get() = _password

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> get() = _confirmPassword

    // Errors
    private val _displayNameError = MutableStateFlow<String?>(null)
    val displayNameError: StateFlow<String?> get() = _displayNameError

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> get() = _emailError

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> get() = _passwordError

    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> get() = _confirmPasswordError

    // Other states
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> get() = _user

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    // onChange handlers
    fun onDisplayNameChange(value: String) {
        _displayName.value = value
    }

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun onConfirmPasswordChange(value: String) {
        _confirmPassword.value = value
    }

    fun clearAllErrors() {
        _displayNameError.value = null
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _errorMessage.value = null
    }

    fun register() {
        clearAllErrors()

        val name = _displayName.value.trim()
        val email = _email.value.trim()
        val pass = _password.value
        val confirmPass = _confirmPassword.value

        if (name.isBlank()) {
            _displayNameError.value = "Tên hiển thị không được bỏ trống"
        }

        if (email.isBlank()) {
            _emailError.value = "Email không được bỏ trống"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Email không hợp lệ"
        }

        if (pass.isBlank()) {
            _passwordError.value = "Mật khẩu không được bỏ trống"
        } else if (pass.length < 6) {
            _passwordError.value = "Mật khẩu phải có ít nhất 6 ký tự"
        }

        if (confirmPass.isBlank()) {
            _confirmPasswordError.value = "Xác nhận mật khẩu không được bỏ trống"
        } else if (confirmPass != pass) {
            _confirmPasswordError.value = "Mật khẩu và xác nhận không khớp"
        }

        if (_emailError.value != null || _passwordError.value != null ||
            _confirmPasswordError.value != null || _displayNameError.value != null) {
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            authRepo.register(email, pass, name) { result ->
                result.fold(
                    onSuccess = { user ->
                        _user.value = user
                        _isLoading.value = false
                    },
                    onFailure = { exception ->
                        val errorMsg = when (exception.message) {
                            "USER_EXISTS" -> "Tài khoản đã tồn tại"
                            else -> "Đăng ký thất bại, vui lòng thử lại"
                        }
                        _errorMessage.value = errorMsg
                        _isLoading.value = false
                    }
                )
            }
        }
    }

    fun setIsLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}
