package com.example.ekycsimulate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    userId: Int,
    onLogout: () -> Unit
) {
    android.util.Log.d("HomeScreen", "Rendering HomeScreen for user: $userId")
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Trang chủ") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("Lịch sử") },
                    selected = false,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Cá nhân") },
                    selected = false,
                    onClick = { }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // Header Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color(0xFF1565C0)) // Solid color instead of gradient
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Xin chào,",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "User #$userId",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = onLogout) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Balance Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Số dư khả dụng",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "50,000,000 VND",
                                    color = Color(0xFF1565C0),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dịch vụ nhanh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(icon = Icons.Default.Send, label = "Chuyển tiền", color = Color(0xFF4CAF50))
                        QuickActionItem(icon = Icons.Default.Payment, label = "Thanh toán", color = Color(0xFFFFA726))
                        QuickActionItem(icon = Icons.Default.QrCodeScanner, label = "Quét QR", color = Color(0xFF29B6F6))
                        QuickActionItem(icon = Icons.Default.MoreHoriz, label = "Xem thêm", color = Color(0xFFAB47BC))
                    }
                }
            }

            // Recent Transactions
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Giao dịch gần đây",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TransactionItem(
                        title = "Chuyển tiền cho Nguyen Van A",
                        date = "26/11/2025 10:30",
                        amount = "-500,000 VND",
                        isNegative = true
                    )
                    TransactionItem(
                        title = "Nhận tiền từ Tran Thi B",
                        date = "25/11/2025 15:45",
                        amount = "+2,000,000 VND",
                        isNegative = false
                    )
                    TransactionItem(
                        title = "Thanh toán tiền điện",
                        date = "24/11/2025 09:15",
                        amount = "-1,200,000 VND",
                        isNegative = true
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun TransactionItem(title: String, date: String, amount: String, isNegative: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(text = date, color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = amount,
                color = if (isNegative) Color.Red else Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
