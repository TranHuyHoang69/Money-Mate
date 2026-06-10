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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moneymate.StringRes
import com.example.moneymate.data.local.ReminderEntity
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date

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
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = onAddReminderClick,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer, // ✅ Loại bỏ màu vàng nạp cứng, sử dụng Tertiary tương thích theme
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(StringRes.reminder_title), // ✅ Sửa lỗi compile nhãn id =
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
                            color = MaterialTheme.colorScheme.primaryContainer, // ✅ Sử dụng Container hệ thống thay vì nạp cứng HEX
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
                                Text(
                                    text = stringResource(StringRes.cancel_btn), // ✅ Sửa lỗi compile nhãn id =
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = pluralStringResource(
                                    StringRes.reminder_selected_count, // ✅ Sửa lỗi compile nhãn id =
                                    selectedReminderIds.size,
                                    selectedReminderIds.size
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = {
                            reminders.filter { it.id in selectedReminderIds }.forEach { reminder ->
                                viewModel.deleteReminder(reminder)
                            }
                            selectedReminderIds = emptySet()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(StringRes.reminder_delete_selected_desc), // ✅ Sửa lỗi compile nhãn id =
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                // Header trạng thái hiển thị bình thường ban đầu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer, // ✅ Sử dụng Container hệ thống thay vì nạp cứng HEX
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
                                contentDescription = stringResource(StringRes.menu_icon_desc), // ✅ Sửa lỗi compile nhãn id =
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(StringRes.reminder_title), // ✅ Sửa lỗi compile nhãn id =
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LIST REMINDERS ---
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(StringRes.reminder_empty_list_hint), // ✅ Sửa lỗi compile nhãn id =
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
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
                                    selectedReminderIds = if (isSelected) {
                                        selectedReminderIds - reminder.id
                                    } else {
                                        selectedReminderIds + reminder.id
                                    }
                                } else {
                                    navController.navigate("update_reminder_screen?reminderId=${reminder.id}")
                                }
                            },
                            onItemLongClick = {
                                if (!isMultiSelectMode) {
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

// ==================== HÀM PHỤ VẼ ITEM DÒNG NHẮC NHỞ (ĐÃ ĐỒNG BỘ THEME) ====================
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
    val currentLocale = LocalConfiguration.current.locales[0]
    val dateTimePattern = stringResource(StringRes.reminder_date_time_pattern) // ✅ Sửa lỗi compile nhãn id =

    val formattedTime = remember(reminder.reminderDateTime, currentLocale, dateTimePattern) {
        val sdf = SimpleDateFormat(dateTimePattern, currentLocale)
        sdf.format(Date(reminder.reminderDateTime))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = isMultiSelectMode) {
            Row {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary, // ✅ Loại bỏ màu vàng nạp cứng ở Checkbox
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(StringRes.reminder_date_icon_desc), // ✅ Sửa lỗi compile nhãn id =
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedTime,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        AnimatedVisibility(visible = !isMultiSelectMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary, // ✅ Sử dụng màu tương phản của Primary thay cho White tĩnh
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }
    }
}