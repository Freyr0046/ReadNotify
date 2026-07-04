package com.freyr.readmynotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.freyr.readmynotify.ui.main.MainScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host（取代 SplashActivity + Jetpack Navigation
 * Component，見 doc/phase1-spec.md Assumption #2/#4）。冷啟動視覺改用
 * AndroidX Core SplashScreen（見 Theme.ReadNotify.Starting）。
 *
 * Android 15+（targetSdk 35+）強制 edge-to-edge，無法選擇退出；
 * enableEdgeToEdge() 讓 minSdk 26–34 的裝置也採用相同行為，避免同一支
 * App 在不同 Android 版本上呈現不一致的系統列樣式。背景在此填滿整個視窗
 * （真正 edge-to-edge），實際內容的安全區域留白改在
 * MainScreen 的 windowInsetsPadding 處理（見 doc/phase5-edge-to-edge.md）。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.MAIN) {
                        composable(Routes.MAIN) {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }

    private object Routes {
        const val MAIN = "main"
    }
}
