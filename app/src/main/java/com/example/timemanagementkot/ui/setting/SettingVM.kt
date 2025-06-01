package com.example.timemanagementkot.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.timemanagementkot.data.repo.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingVM : ViewModel() {
    private val authRepo = AuthRepo()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("••••••••")
    val password: StateFlow<String> = _password

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError

    private val _onSaveSuccess = MutableStateFlow<String?>(null)
    val onSaveSuccess: StateFlow<String?> = _onSaveSuccess

    fun onCurrentPassChanged(currentPass: String) {
        _currentPassword.value = currentPass
        _passwordError.value = null
    }

    fun onNewPassChanged(newPass: String) {
        _newPassword.value = newPass
        _passwordError.value = null
    }

    fun onConfirmPassChanged(confirmPass: String) {
        _confirmPassword.value = confirmPass
        _passwordError.value = null
    }

    private fun validatePasswords(): Pair<Boolean, String?> {
        return when {
            _currentPassword.value.isEmpty() ->
                false to "Vui lòng nhập mật khẩu hiện tại"
            _newPassword.value.length < 6 ->
                false to "Mật khẩu mới phải có ít nhất 6 ký tự"
            _newPassword.value != _confirmPassword.value ->
                false to "Mật khẩu xác nhận không khớp"
            else ->
                true to null
        }
    }

    fun changePassword(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val (isValid, errorMessage) = validatePasswords()
        if (!isValid) {
            _passwordError.value = errorMessage
            onError(errorMessage ?: "Lỗi không xác định")
            return
        }

        authRepo.changePassword(
            currentPassword = _currentPassword.value,
            newPassword = _newPassword.value,
            onSuccess = {
                resetPasswordFields()
                _onSaveSuccess.value = "Đổi mật khẩu thành công"
                onSuccess()
            },
            onError = { error ->
                _passwordError.value = error
                onError(error)
            }
        )
    }

    fun resetSaveSuccess() {
        _onSaveSuccess.value = null
    }

    private fun resetPasswordFields() {
        _currentPassword.value = "••••••••"
        _newPassword.value = "••••••••"
        _confirmPassword.value = "••••••••"
        _passwordError.value = null
    }

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> get() = _showDialog

    fun showDialog() {
        _showDialog.value = true
    }

    fun dismissDialog() {
        _showDialog.value = false
    }

    init {
        loadUserData()
    }

    private fun loadUserData() {
        authRepo.getCurrentUser()?.let { firebaseUser ->
            _displayName.value = firebaseUser.displayName ?: "Không có tên"
            _email.value = firebaseUser.email ?: "Chưa có email"

        }
    }

    fun refreshUserData() {
        loadUserData()
    }

    fun logout() {
        authRepo.logout()
        clearUserData() // Xóa dữ liệu người dùng trong VM
    }

    private fun clearUserData() {
        _displayName.value = ""
        _email.value = ""
    }
}