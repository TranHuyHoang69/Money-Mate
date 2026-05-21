package com.example.moneymate

import AppNavigation
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // SỬA Ở ĐÂY: Thay BuildConfig.DEBUG bằng ứng dụng thực tế để tránh lỗi Unresolved reference
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder() // Thêm ThreadPolicy ở đây để sửa lỗi 'Policy'
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog() // Hoặc .penaltyDeath() nếu muốn app tự sập để tìm chỗ lỗi nhanh
                    .build()
            )
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}