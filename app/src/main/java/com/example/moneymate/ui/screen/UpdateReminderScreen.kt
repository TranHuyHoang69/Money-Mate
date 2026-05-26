package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateReminderScreen(
    reminderId: Int, // Nhận ID của lời nhắc cần chỉnh sửa
    viewModel: ReminderViewModel,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var repeatExpanded by remember { mutableStateOf(false) }
    val repeatOptions = listOf(
        "Một lần", "Hàng ngày", "Hàng tuần", "Mỗi 2 tuần",
        "Mỗi 4 tuần", "Hàng tháng", "Mỗi 2 tháng", "Hàng quý",
        "Mỗi 6 tháng", "Mỗi năm"
    )
    var repeatInterval by remember { mutableStateOf(repeatOptions[0]) }

    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Giữ lại đối tượng gốc để tiến hành cập nhật qua cơ chế .copy()
    var originalReminder by remember { mutableStateOf<com.example.moneymate.data.local.ReminderEntity?>(null) }

    // --- ĐỔ DỮ LIỆU CŨ VÀO FORM ---
    LaunchedEffect(reminderId) {
        viewModel.getReminderById(reminderId).collect { reminder ->
            reminder?.let {
                originalReminder = it
                title = it.title
                note = it.note
                repeatInterval = if (repeatOptions.contains(it.repeatInterval)) it.repeatInterval else repeatOptions[0]

                val savedCalendar = Calendar.getInstance().apply {
                    timeInMillis = it.reminderDateTime
                }
                selectedDate = savedCalendar.time
                selectedHour = savedCalendar.get(Calendar.HOUR_OF_DAY)
                selectedMinute = savedCalendar.get(Calendar.MINUTE)
            }
        }
    }

    val dateDisplayString = remember(selectedDate) {
        SimpleDateFormat("d 'tháng' M, yyyy", Locale("vi", "VN")).format(selectedDate)
    }

    val timeDisplayString = remember(selectedHour, selectedMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    // --- DIALOG CHỌN NGÀY ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selectedMillis
                        }
                        val localCalendar = Calendar.getInstance().apply {
                            set(utcCalendar.get(Calendar.YEAR), utcCalendar.get(Calendar.MONTH), utcCalendar.get(Calendar.DAY_OF_MONTH))
                        }
                        selectedDate = localCalendar.time
                    }
                    showDatePicker = false
                }) {
                    Text("XÁC NHẬN", color = Color(0xFF4CB080))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("HỦY", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- DIALOG CHỌN GIỜ ---
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTimePicker = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3E2F))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Chọn giờ nhắc nhở", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("HỦY", color = Color.Gray)
                        }
                        TextButton(onClick = {
                            selectedHour = timePickerState.hour
                            selectedMinute = timePickerState.minute
                            showTimePicker = false
                        }) {
                            Text("XÁC NHẬN", color = Color(0xFF4CB080))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF131A05)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- TOP HEADER BAR ---
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
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sửa lời nhắc",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- VÙNG ĐIỀN THÔNG TIN ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Trường 1: Tên lời nhắc
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Tên lời nhắc", color = Color(0xFF8E8E93), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF4CB080)),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (title.isEmpty()) {
                                    Text("Tên", color = Color(0xFF555A4F), fontSize = 18.sp)
                                } else {
                                    innerTextField()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(if (title.isNotEmpty()) Color(0xFF4CB080) else Color(0xFF555A4F))
                                )
                            }
                        }
                    )
                }

                // Trường 2: Tần suất nhắc nhở
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Tần suất nhắc nhở", color = Color(0xFF8E8E93), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { repeatExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = repeatInterval, color = Color(0xFF4CB080), fontSize = 18.sp)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatExpanded)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF555A4F))
                            )
                        }

                        DropdownMenu(
                            expanded = repeatExpanded,
                            onDismissRequest = { repeatExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.6f).background(Color(0xFF1E3E2F))
                        ) {
                            repeatOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = if(option == repeatInterval) Color(0xFFFFC107) else Color.White,
                                            fontSize = 16.sp
                                        )
                                    },
                                    onClick = {
                                        repeatInterval = option
                                        repeatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Trường 3: Bộ chọn Ngày
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Text(text = "Ngày", color = Color(0xFF8E8E93), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = dateDisplayString, color = Color(0xFF4CB080), fontSize = 18.sp)
                }

                // Trường 4: Bộ chọn Giờ
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    Text(text = "Giờ", color = Color(0xFF8E8E93), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = timeDisplayString, color = Color(0xFF4CB080), fontSize = 18.sp)
                }

                // Trường 5: Ghi chú
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Ghi chú", color = Color(0xFF8E8E93), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = SolidColor(Color(0xFF4CB080)),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (note.isEmpty()) {
                                    Text("Ghi chú", color = Color(0xFF555A4F), fontSize = 18.sp)
                                } else {
                                    innerTextField()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(if (note.isNotEmpty()) Color(0xFF4CB080) else Color(0xFF555A4F))
                                )
                            }
                        }
                    )
                }
            }

            // --- NÚT LƯU THAY ĐỔI ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick = {
                        val finalCalendar = Calendar.getInstance().apply {
                            val dateNav = Calendar.getInstance().apply { time = selectedDate }

                            set(Calendar.YEAR, dateNav.get(Calendar.YEAR))
                            set(Calendar.MONTH, dateNav.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, dateNav.get(Calendar.DAY_OF_MONTH))

                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (finalCalendar.timeInMillis <= System.currentTimeMillis()) {
                            finalCalendar.add(Calendar.MINUTE, 1)
                        }

                        // Thực hiện Update bản ghi cũ thông qua sao chép trạng thái (.copy)
                        originalReminder?.let {
                            val updatedReminder = it.copy(
                                title = title,
                                note = note,
                                reminderDateTime = finalCalendar.timeInMillis,
                                repeatInterval = repeatInterval
                            )
                            viewModel.updateReminder(updatedReminder)
                        }

                        onBackClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107), // Đồng bộ màu vàng hổ phách của nút Tạo cũ
                        contentColor = Color.Black
                    ),
                    enabled = title.isNotBlank()
                ) {
                    Text(text = "Lưu thay đổi", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}