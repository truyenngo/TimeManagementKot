package com.example.timemanagementkot.ui.setting

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timemanagementkot.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: SettingVM? = null,
    onBackClick: () -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val isInPreview = LocalInspectionMode.current
    val finalViewModel = viewModel ?: if (!isInPreview) viewModel() else null
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val displayName by finalViewModel?.displayName?.collectAsState() ?: remember { mutableStateOf("Truyen") }
    val email by finalViewModel?.email?.collectAsState() ?: remember { mutableStateOf("haha@gmail.com") }
    val password by finalViewModel?.password?.collectAsState() ?: remember { mutableStateOf("••••••••") }
    val currentPassword by finalViewModel?.currentPassword?.collectAsState() ?: remember { mutableStateOf("") }
    val newPassword by finalViewModel?.newPassword?.collectAsState() ?: remember { mutableStateOf("") }
    val confirmPassword by finalViewModel?.confirmPassword?.collectAsState() ?: remember { mutableStateOf("") }
    val passwordError by finalViewModel?.passwordError?.collectAsState() ?: remember { mutableStateOf(null) }
    val saveSuccessMessage by finalViewModel?.onSaveSuccess?.collectAsState() ?: remember { mutableStateOf(null) }
    val showDialog by finalViewModel?.showDialog?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccessMessage) {
        saveSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel?.resetSaveSuccess()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ){ padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFD58099)),
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    text = "Cài đặt",
                    fontSize = 22.sp,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFD58099))
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD58099).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Thông tin tài khoản",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD58099)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hiển thị tên người dùng
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tên:",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = displayName,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Email:",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = email,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChangePasswordClick() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mật khẩu",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(100.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) {
                                Text(
                                    text = password,
                                    color = Color.Gray
                                )

                                IconButton(
                                    onClick = {  finalViewModel?.showDialog()},
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(5.dp)
                                        .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_edit),
                                        contentDescription = "Xoá hoạt động",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .padding(5.dp)
                                            .border(1.dp, Color(0xFFD58099), RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFD58099).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFD58099)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ){
                Button(
                    onClick = { onBackClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                    modifier = Modifier.padding(10.dp),
                ) {
                    Text("Thoát")
                }

                Button(
                    onClick = {
                        finalViewModel?.logout()
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD58099)),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Đăng xuất")
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { finalViewModel?.dismissDialog() },
                    title = {
                        Text(
                            text = "Đổi mật khẩu",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFD58099)
                        )
                    },
                    text = {
                        Column {
                            passwordError?.let {
                                Text(
                                    text = it,
                                    color = Color.Red,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            OutlinedTextField(
                                value = currentPassword,
                                onValueChange = { finalViewModel?.onCurrentPassChanged(it) },
                                label = { Text("Mật khẩu hiện tại") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD58099),
                                    unfocusedBorderColor = Color(0xFFD58099)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { finalViewModel?.onNewPassChanged(it) },
                                label = { Text("Mật khẩu mới (tối thiểu 6 ký tự)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD58099),
                                    unfocusedBorderColor = Color(0xFFD58099)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { finalViewModel?.onConfirmPassChanged(it) },
                                label = { Text("Xác nhận mật khẩu mới") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD58099),
                                    unfocusedBorderColor = Color(0xFFD58099)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { finalViewModel?.dismissDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.LightGray
                                )
                            ) {
                                Text("Huỷ", color = Color.Black)
                            }

                            Button(
                                onClick = {
                                    finalViewModel?.changePassword(
                                        onSuccess = {
                                            finalViewModel?.dismissDialog()
                                        },
                                        onError = { error ->
                                            // Lỗi đã tự động hiển thị qua passwordError
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD58099)
                                )
                            ) {
                                Text("Xác nhận", color = Color.White)
                            }
                        }
                    }
                )
            }
        }
    }


}

@Preview(showBackground = true)
@Composable
fun SettingScreenPreview() {
    MaterialTheme {
        SettingScreen(
            onBackClick = {},
            onChangePasswordClick= {},
            onLogoutClick = {}
        )
    }
}

