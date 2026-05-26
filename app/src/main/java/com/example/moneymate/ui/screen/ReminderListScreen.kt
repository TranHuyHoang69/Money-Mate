package com.example.moneymate.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    navController: NavController,
    onAddReminderClick: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()

    // --- QUẢN LÝ TRẠNG THÁI CHỌN NHIỀU ---
    var selectedReminderIds by remember { mutableStateOf(setOf<Int>()) }
    val isMultiSelectMode = selectedReminderIds.isNotEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF131A05),
        floatingActionButton = {
            // Ẩn nút Thêm khi đang trong chế độ chọn nhiều để tránh xung đột trải nghiệm
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = onAddReminderClick,
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm nhắc nhở",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // --- TOP HEADER BAR BIẾN ĐỔI THEO TRẠNG THÁI ---
            if (isMultiSelectMode) {
                // Header khi đang chọn nhiều để xóa
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF2A1B1B), // Nền đỏ tối cảnh báo xóa
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedReminderIds = emptySet() }) {
                                Text("Hủy", color = Color.White, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Đã chọn ${selectedReminderIds.size}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = {
                            // Thực hiện xóa toàn bộ các mục đã được tích chọn
                            reminders.filter { it.id in selectedReminderIds }.forEach { reminder ->
                                viewModel.deleteReminder(reminder)
                            }
                            selectedReminderIds = emptySet() // Thoát chế độ chọn nhiều
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa toàn bộ mục đã chọn",
                                tint = Color(0xFFED5E5E),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                // Header bình thường lúc đầu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF1E3E2F),
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Nhắc nhở",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Tạo khoảng trống đệm nhỏ cố định ngăn cách giữa Header và List Item đầu tiên
            Spacer(modifier = Modifier.height(16.dp))

            // --- LIST REMINDERS ---
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Chưa có danh sách nhắc nhở", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        val isSelected = reminder.id in selectedReminderIds

                        ReminderRowItem(
                            reminder = reminder,
                            isSelected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleReminder(reminder, isChecked)
                            },
                            onItemClick = {
                                if (isMultiSelectMode) {
                                    // Đang trong chế độ chọn nhiều: Nhấn vào thì đảo ngược trạng thái Tích / Bỏ tích
                                    selectedReminderIds = if (isSelected) {
                                        selectedReminderIds - reminder.id
                                    } else {
                                        selectedReminderIds + reminder.id
                                    }
                                } else {
                                    // Chế độ bình thường: Nhấn vào điều hướng đến màn hình sửa
                                    navController.navigate("update_reminder_screen?reminderId=${reminder.id}")
                                }
                            },
                            onItemLongClick = {
                                if (!isMultiSelectMode) {
                                    // Giữ lâu lần đầu: Kích hoạt chế độ chọn nhiều, tự động đưa ID này vào danh sách chọn
                                    selectedReminderIds = setOf(reminder.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== HÀM PHỤ VẼ ITEM DÒNG NHẮC NHỞ (ĐÃ UPDATE LONG CLICK) ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderRowItem(
    reminder: ReminderEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    val formattedTime = remember(reminder.reminderDateTime) {
        val sdf = SimpleDateFormat("d 'tháng' M, yyyy HH:mm", Locale("vi", "VN"))
        sdf.format(Date(reminder.reminderDateTime))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isSelected) Color(0xFF2C4E3A) else Color(0xFF262620) // Đổi nền khi dòng được chọn
            )
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 💥 CHỐT KIỂM SOÁT HOẠT HỌA: Hiện Checkbox khi vào trạng thái chọn nhiều
        AnimatedVisibility(visible = isMultiSelectMode) {
            Row {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFFC107), // Màu vàng hổ phách đồng bộ
                        uncheckedColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        // Cột hiển thị thông tin chữ
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Lịch hẹn",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedTime,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Các nút chức năng điều khiển riêng lẻ (Tự động ẩn đi khi đang trong tiến trình chọn nhiều)
        AnimatedVisibility(visible = !isMultiSelectMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Spacer(modifier = Modifier.width(8.dp))

                // Nút công tắc bật tắt trạng thái độc lập
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CB080),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF3A3A35)
                    )
                )
            }
        }
    }
}