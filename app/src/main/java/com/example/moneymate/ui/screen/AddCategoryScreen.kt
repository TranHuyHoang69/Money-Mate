package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.moneymate.data.local.CategoryEntity
import com.example.moneymate.ui.item.IconItem
import com.example.moneymate.ui.utils.IconUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    initialType: String,
    onBack: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedColor by remember { mutableStateOf("#4B8361") }

    val groupedIcons = remember { IconUtils.getGroupedIcons() }
    var selectedIcon by remember {
        mutableStateOf(groupedIcons.values.firstOrNull()?.firstOrNull() ?: "")
    }

    val colors = listOf("#4B8361", "#2E5B8B", "#FFC107", "#E91E63", "#9C27B0", "#00BCD4", "#FF5722", "#795548", "#607D8B", "#8BC34A", "#3F51B5", "#FF9800")
    val currentColor = Color(android.graphics.Color.parseColor(selectedColor))

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Danh mục mới") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Tên danh mục") }, modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                RadioButton(
                    selected = selectedType == "SPEND",
                    onClick = { selectedType = "SPEND" }
                )
                Text("Chi phí")

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = selectedType == "INCOME",
                    onClick = { selectedType = "INCOME" }
                )
                Text("Thu nhập")
            }

            Text("Màu sắc", fontWeight = FontWeight.Bold)
            LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.height(100.dp)) {
                items(colors) { colorHex ->
                    Box(modifier = Modifier.size(40.dp).padding(4.dp).clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                        .clickable { selectedColor = colorHex }
                        .border(if (selectedColor == colorHex) 2.dp else 0.dp, Color.Black, CircleShape)
                    )
                }
            }

            Text("Biểu tượng", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                groupedIcons.forEach { (groupName, iconsInGroup) ->
                    item {
                        Text(groupName, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                        iconsInGroup.chunked(5).forEach { rowIcons ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                rowIcons.forEach { icon ->
                                    IconItem(icon, selectedIcon == icon, currentColor) { selectedIcon = icon }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        CategoryEntity(
                            categoryId = 0, // Để autoGenerate tự làm việc
                            title = name,
                            iconResName = selectedIcon, // Truyền đúng vào tham số iconResName
                            colorHex = selectedColor,
                            type = selectedType,
                            isDefault = false
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                enabled = name.isNotBlank() && selectedIcon.isNotBlank()
            ) {
                Text("LƯU DANH MỤC")
            }
        }
    }
}