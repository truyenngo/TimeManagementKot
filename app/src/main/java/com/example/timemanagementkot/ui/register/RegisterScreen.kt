package com.example.timemanagementkot.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.VisualTransformation
import com.example.timemanagementkot.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.ui.login.LoginVM

@Composable
fun RegisterScreen(
    viewModel: RegisterVM? = null,
    onRegister: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val displayName by finalViewModel?.displayName?.collectAsState() ?: remember { mutableStateOf("") }
    val email by finalViewModel?.email?.collectAsState() ?: remember { mutableStateOf("") }
    val password by finalViewModel?.password?.collectAsState() ?: remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val confirmPassword by finalViewModel?.confirmPassword?.collectAsState() ?: remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val displayNameError by finalViewModel?.displayNameError?.collectAsState() ?: remember { mutableStateOf(null) }
    val emailError by finalViewModel?.emailError?.collectAsState() ?: remember { mutableStateOf(null) }
    val passwordError by finalViewModel?.passwordError?.collectAsState() ?: remember { mutableStateOf(null) }
    val confirmPasswordError by finalViewModel?.confirmPasswordError?.collectAsState() ?: remember { mutableStateOf(null) }

    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val errorMessage by finalViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }

    val user by finalViewModel?.user?.collectAsState() ?: remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user) {
        user?.let {
            finalViewModel?.setIsLoading(false)
            snackbarHostState.showSnackbar("Đăng ký thành công!")
            onRegister(email.trim(), password.trim())
        }
    }

    fun handleRegister() {
        finalViewModel?.register()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 40.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_register),
                contentDescription = "Register Image",
                modifier = Modifier
                    .padding(top = 40.dp)
                    .size(100.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            TextField(
                value = displayName,
                onValueChange = { finalViewModel?.onDisplayNameChange(it) },
                label = { Text("Tên hiển thị") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (displayNameError == null) Color.Gray else Color.Red,
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Row(
                modifier = Modifier.height(30.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (displayNameError != null) {
                    Text(
                        text = displayNameError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Spacer(modifier = Modifier.fillMaxHeight())
                }
            }

            TextField(
                value = email,
                onValueChange = { finalViewModel?.onEmailChange(it) },
                label = { Text("Email") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (emailError == null) Color.Gray else Color.Red,
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Row(
                modifier = Modifier.height(30.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (emailError != null) {
                    Text(
                        text = emailError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Spacer(modifier = Modifier.fillMaxHeight())
                }
            }

            TextField(
                value = password,
                onValueChange = { finalViewModel?.onPasswordChange(it) },
                label = { Text("Mật khẩu") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (passwordError == null) Color.Gray else Color.Red,
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Row(
                modifier = Modifier.height(30.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (passwordError != null) {
                    Text(
                        text = passwordError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Spacer(modifier = Modifier.fillMaxHeight())
                }
            }

            TextField(
                value = confirmPassword,
                onValueChange = { finalViewModel?.onConfirmPasswordChange(it) },
                label = { Text("Xác nhận mật khẩu") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (confirmPasswordError == null) Color.Gray else Color.Red,
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Row(
                modifier = Modifier.height(30.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (confirmPasswordError != null) {
                    Text(
                        text = confirmPasswordError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Spacer(modifier = Modifier.fillMaxHeight())
                }
            }

            Row(
                modifier = Modifier.height(30.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxHeight()
                    )
                } else {
                    Spacer(modifier = Modifier.fillMaxHeight())
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(20.dp))
            } else {
                // Nút Đăng ký
                Button(
                    onClick = { handleRegister() },
                    modifier = Modifier
                        .width(200.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD7566A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đăng ký", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chuyển tới màn hình đăng nhập
            TextButton(onClick = onNavigateToLogin) {
                Text("Bạn đã có tài khoản? Đăng nhập")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        onRegister = { _, _ -> },
        onNavigateToLogin = {}
    )
}
