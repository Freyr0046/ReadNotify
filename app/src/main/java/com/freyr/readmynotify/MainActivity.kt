package com.freyr.readmynotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface {
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
