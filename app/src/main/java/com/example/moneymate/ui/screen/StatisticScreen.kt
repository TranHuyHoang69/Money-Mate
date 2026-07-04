package com.example.moneymate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.moneymate.StringRes
import com.example.moneymate.ui.item.ExpenseTabContent
import com.example.moneymate.ui.item.IncomeTabContent
import com.example.moneymate.ui.item.OverviewTabContent
import com.example.moneymate.ui.theme.AppTopBarColor
import com.example.moneymate.ui.theme.stringResource
import com.example.moneymate.viewmodel.StatisticsTab
import com.example.moneymate.viewmodel.StatisticsTimeMode
import com.example.moneymate.viewmodel.StatisticsUiState
import com.example.moneymate.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab = viewModel.selectedTab
    val timeMode = viewModel.timeMode

    val brandColor = AppTopBarColor
    val titleText = stringResource(StringRes.nav_statistics)

    val overviewTabText = stringResource(StringRes.statistics_overview)
    val expenseTabText = stringResource(StringRes.statistics_expense)
    val incomeTabText = stringResource(StringRes.statistics_income)

    val dayModeText = stringResource(StringRes.statistics_day)
    val weekModeText = stringResource(StringRes.statistics_week)
    val monthModeText = stringResource(StringRes.statistics_month)
    val yearModeText = stringResource(StringRes.statistics_year)

    val errorLoadingText = stringResource(StringRes.statistics_error_loading)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Drawer Menu",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = brandColor
            ),
            windowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
        )

        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth(),
            containerColor = brandColor,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = Color.White,
                    height = 3.dp
                )
            }
        ) {
            val mainTabs = listOf(
                StatisticsTab.OVERVIEW to overviewTabText,
                StatisticsTab.EXPENSE to expenseTabText,
                StatisticsTab.INCOME to incomeTabText
            )
            mainTabs.forEach { (tab, title) ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.changeTab(tab) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        TabRow(
            selectedTabIndex = timeMode.ordinal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp)
                .clip(RoundedCornerShape(8.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = brandColor,
            indicator = { Box(Modifier) }
        ) {
            StatisticsTimeMode.values().forEach { mode ->
                val isModeSelected = timeMode == mode
                val modeText = when (mode) {
                    StatisticsTimeMode.DAY -> dayModeText
                    StatisticsTimeMode.WEEK -> weekModeText
                    StatisticsTimeMode.MONTH -> monthModeText
                    StatisticsTimeMode.YEAR -> yearModeText
                }

                Tab(
                    selected = isModeSelected,
                    onClick = { viewModel.changeTimeMode(mode) },
                    text = {
                        Text(
                            text = modeText,
                            fontSize = 13.sp,
                            fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isModeSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .height(38.dp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isModeSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                )
            }
        }

        when (val state = uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = brandColor)
                }
            }

            is StatisticsUiState.Success -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TimeNavigationBar(
                            displayText = state.displayTimeText,
                            onPrevious = { viewModel.movePrevious() },
                            onNext = { viewModel.moveNext() },
                            isNextEnabled = viewModel.isNextEnabled(),
                            brandColor = brandColor
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            when (selectedTab) {
                                StatisticsTab.OVERVIEW -> {
                                    OverviewTabContent(
                                        statistics = state,
                                        navController = navController,
                                        viewModel = viewModel
                                    )
                                }
                                StatisticsTab.EXPENSE -> {
                                    ExpenseTabContent(
                                        statistics = state,
                                        navController = navController,
                                        viewModel = viewModel
                                    )
                                }
                                StatisticsTab.INCOME -> {
                                    IncomeTabContent(
                                        statistics = state,
                                        navController = navController,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }

                    // ✅ THÊM: Loading indicator nhỏ khi đang refresh
                    if (state.isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = brandColor,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }

            is StatisticsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = errorLoadingText,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeNavigationBar(
    displayText: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean,
    brandColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.width(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Period",
                tint = brandColor
            )
        }

        Text(
            text = displayText,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
            onClick = onNext,
            enabled = isNextEnabled,
            modifier = Modifier.width(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Period",
                tint = if (isNextEnabled) brandColor else MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
