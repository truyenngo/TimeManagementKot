package com.example.timemanagementkot.ui.login

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.R
import kotlinx.coroutines.delay
import android.Manifest
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.timemanagementkot.ui.home.HomeVM

@Composable
fun LoginScreen(
    viewModel: LoginVM? = null,
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null

    val email by finalViewModel?.email?.collectAsState() ?: remember { mutableStateOf("") }
    val password by finalViewModel?.password?.collectAsState() ?: remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val emailError by finalViewModel?.emailError?.collectAsState() ?: remember { mutableStateOf(null) }
    val passwordError by finalViewModel?.passwordError?.collectAsState() ?: remember { mutableStateOf(null) }

    val isLoading by finalViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val errorMessage by finalViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }

    val user by finalViewModel?.user?.collectAsState() ?: remember { mutableStateOf(null) }

    val showDialog by finalViewModel?.showDialog?.collectAsState() ?: remember { mutableStateOf(false) }

    val forgotEmail by finalViewModel?.forgotEmail?.collectAsState() ?: remember { mutableStateOf("") }
    val forgotEmailError by finalViewModel?.forgotEmailError?.collectAsState() ?: remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        finalViewModel?.snackbarMessage?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            finalViewModel?.setIsLoading(false)
            onLogin(email.trim(), password.trim())
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Quyền thông báo đã được cấp", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Ứng dụng cần quyền để gửi thông báo", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 40.dp, end = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_login),
                contentDescription = "Hình đăng nhập",
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(100.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = email,
                onValueChange = { finalViewModel?.onEmailChanged(it) },
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
                onValueChange = { finalViewModel?.onPasswordChanged(it) },
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
                keyboardActions = KeyboardActions(onDone = { finalViewModel?.login() }),
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
                Button(
                    onClick = { finalViewModel?.login() },
                    modifier = Modifier
                        .width(200.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD7566A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Đăng nhập", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("Chưa có tài khoản? Đăng ký ngay")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { finalViewModel?.showDialog() }) {
                Text("Quên mật khẩu?", color = Color(0xFF2196F3))
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { finalViewModel?.dismissDialog() },
            title = { Text("Lấy lại mật khẩu") },
            text = {
                Column {
                    Text("Vui lòng nhập email để đặt lại mật khẩu")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = forgotEmail,
                        onValueChange = { finalViewModel?.onForgotEmailChanged(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.height(30.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (forgotEmailError != null) {
                            Text(
                                text = forgotEmailError ?: "",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Spacer(modifier = Modifier.fillMaxHeight())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    finalViewModel?.resetPassword()
                }) {
                    Text("Gửi")
                }
            },
            dismissButton = {
                TextButton(onClick = { finalViewModel?.dismissDialog() }) {
                    Text("Thoát")
                }
            }
        )
    }

}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onLogin = { _, _ -> },
        onNavigateToRegister = {},
        onGoogleSignInClick = {}
    )
}
