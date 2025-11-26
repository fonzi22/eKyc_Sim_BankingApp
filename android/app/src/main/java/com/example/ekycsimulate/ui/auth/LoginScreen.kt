package com.example.ekycsimulate.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ekycsimulate.zkp.ZKPEnrollmentManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (Int) -> Unit // Returns User ID
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enrollmentManager = remember { ZKPEnrollmentManager(context) }
    
    var isEnrolled by remember { mutableStateOf(enrollmentManager.isEnrolled()) }
    var isLoading by remember { mutableStateOf(false) }
    var loginStatus by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Đăng nhập ZKP",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (!isEnrolled) {
            Text(
                text = "Bạn chưa đăng ký tài khoản ZKP trên thiết bị này.",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đang xác thực...")
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        loginStatus = null
                        isError = false
                        
                        scope.launch {
                            try {
                                // 1. Get Challenge from Server
                                val challengeResult = enrollmentManager.getChallenge()
                                
                                challengeResult.onSuccess { sessionId ->
                                    // 2. Generate Proof
                                    val verificationPayload = enrollmentManager.performVerification(sessionId)
                                    
                                    if (verificationPayload != null) {
                                        // 3. Send to Server
                                        val verifyResult = enrollmentManager.sendVerification(verificationPayload, sessionId)
                                        
                                        verifyResult.onSuccess { response ->
                                            isLoading = false
                                            loginStatus = "Đăng nhập thành công! User ID: ${response.userId}"
                                            isError = false
                                            response.userId?.let { onLoginSuccess(it) }
                                        }.onFailure { e ->
                                            isLoading = false
                                            loginStatus = "Lỗi xác thực: ${e.message}"
                                            isError = true
                                        }
                                    } else {
                                        isLoading = false
                                        loginStatus = "Lỗi: Không thể tạo ZKP Proof (Private Key missing?)"
                                        isError = true
                                    }
                                }.onFailure { e ->
                                    isLoading = false
                                    loginStatus = "Lỗi kết nối Server: ${e.message}"
                                    isError = true
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                loginStatus = "Lỗi không xác định: ${e.message}"
                                isError = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đăng nhập bằng Face ID (ZKP)")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            loginStatus?.let {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
