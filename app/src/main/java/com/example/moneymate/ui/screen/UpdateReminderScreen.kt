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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymate.StringRes
import com.example.moneymate.ui.theme.AppTopBarColor
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.util.ReminderRepeat
import com.example.moneymate.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateReminderScreen(
    reminderId: Int,
    viewModel: ReminderViewModel,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var repeatExpanded by remember { mutableStateOf(false) }

    // ✅ i18n: Đã sửa lỗi biên dịch nhãn id =
    val repeatOptions = stringArrayResource(StringRes.reminder_repeat_options)
    var repeatInterval by remember { mutableStateOf("") }

    // Đảm bảo khởi tạo giá trị mặc định đầu tiên khi mảng chuỗi được tải lên thành công
    LaunchedEffect(repeatOptions) {
        if (repeatInterval.isEmpty() && repeatOptions.isNotEmpty()) {
            repeatInterval = repeatOptions[0]
        }
    }

    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var originalReminder by remember { mutableStateOf<com.example.moneymate.data.local.ReminderEntity?>(null) }

    LaunchedEffect(reminderId, repeatOptions) {
        viewModel.getReminderById(reminderId).collect { reminder ->
            reminder?.let {
                originalReminder = it
                title = it.title
                note = it.note
                // Xác thực giá trị tần suất cũ có khớp trong tập danh sách ngôn ngữ mới hay không
                val repeatIndex = ReminderRepeat.fromStored(it.repeatInterval).ordinal
                repeatInterval = repeatOptions.getOrElse(repeatIndex) { repeatOptions.getOrNull(0) ?: "" }

                val savedCalendar = Calendar.getInstance().apply {
                    timeInMillis = it.reminderDateTime
                }
                selectedDate = savedCalendar.time
                selectedHour = savedCalendar.get(Calendar.HOUR_OF_DAY)
                selectedMinute = savedCalendar.get(Calendar.MINUTE)
            }
        }
    }

    val currentLocale = LocalConfiguration.current.locales[0]
    val datePattern = stringResource(StringRes.reminder_date_pattern) // ✅ Sửa lỗi biên dịch nhãn id =

    val dateDisplayString = remember(selectedDate, currentLocale, datePattern) {
        SimpleDateFormat(datePattern, currentLocale).format(selectedDate)
    }

    val timeDisplayString = remember(selectedHour, selectedMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    // --- DIALOG CHỌN NGÀY (THÍCH ỨNG THEME CỦA HỆ THỐNG) ---
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
                    Text(text = stringResource(StringRes.confirm_action_text_upper), color = MaterialTheme.colorScheme.primary) // ✅ Sửa lỗi biên dịch nhãn id =
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(StringRes.cancel_action_text_upper), color = MaterialTheme.colorScheme.onSurfaceVariant) // ✅ Sửa lỗi biên dịch nhãn id =
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- DIALOG CHỌN GIỜ (THÍCH ỨNG THEME CỦA HỆ THỐNG) ---
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(StringRes.reminder_time_picker_title), // ✅ Sửa lỗi biên dịch nhãn id =
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(text = stringResource(StringRes.cancel_action_text_upper), color = MaterialTheme.colorScheme.onSurfaceVariant) // ✅ Sửa lỗi biên dịch nhãn id =
                        }
                        TextButton(onClick = {
                            selectedHour = timePickerState.hour
                            selectedMinute = timePickerState.minute
                            showTimePicker = false
                        }) {
                            Text(text = stringResource(StringRes.confirm_action_text_upper), color = MaterialTheme.colorScheme.primary) // ✅ Sửa lỗi biên dịch nhãn id =
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
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
                        color = AppTopBarColor,
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
                            contentDescription = stringResource(StringRes.back_btn_desc), // ✅ Sửa lỗi biên dịch nhãn id =
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(StringRes.update_reminder_header_title), // ✅ Sửa lỗi biên dịch nhãn id =
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                    Text(text = stringResource(StringRes.reminder_title_label), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) // ✅ Sửa lỗi biên dịch nhãn id =
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (title.isEmpty()) {
                                    Text(text = stringResource(StringRes.reminder_title_placeholder), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), fontSize = 18.sp) // ✅ Sửa lỗi biên dịch nhãn id =
                                } else {
                                    innerTextField()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            if (title.isNotEmpty()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    )
                }

                // Trường 2: Tần suất nhắc nhở
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(StringRes.reminder_repeat_label), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) // ✅ Sửa lỗi biên dịch nhãn id =
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
                                Text(text = repeatInterval, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatExpanded)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            )
                        }

                        DropdownMenu(
                            expanded = repeatExpanded,
                            onDismissRequest = { repeatExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            repeatOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = if(option == repeatInterval) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = if(option == repeatInterval) FontWeight.Bold else FontWeight.Normal
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
                    Text(text = stringResource(StringRes.reminder_date_label), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) // ✅ Sửa lỗi biên dịch nhãn id =
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = dateDisplayString, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }

                // Trường 4: Bộ chọn Giờ
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    Text(text = stringResource(StringRes.reminder_time_label), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) // ✅ Sửa lỗi biên dịch nhãn id =
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = timeDisplayString, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }

                // Trường 5: Ghi chú
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(StringRes.reminder_note_label), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) // ✅ Sửa lỗi biên dịch nhãn id =
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (note.isEmpty()) {
                                    Text(text = stringResource(StringRes.reminder_note_placeholder), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), fontSize = 18.sp) // ✅ Sửa lỗi biên dịch nhãn id =
                                } else {
                                    innerTextField()
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            if (note.isNotEmpty()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
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
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer, // ✅ Đã gỡ màu vàng cứng, dùng Tertiary tương ứng theme
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    enabled = title.isNotBlank()
                ) {
                    Text(text = stringResource(StringRes.reminder_save_changes_btn), fontSize = 20.sp, fontWeight = FontWeight.Medium) // ✅ Sửa lỗi biên dịch nhãn id =
                }
            }
        }
    }
}
