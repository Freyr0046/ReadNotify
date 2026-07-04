package com.freyr.readmynotify.ui.main

import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freyr.readmynotify.ui.main.components.AppWhitelistRow
import com.freyr.readmynotify.ui.main.components.EngineErrorDialog
import com.freyr.readmynotify.ui.main.components.PermissionDeniedOverlay
import com.freyr.readmynotify.ui.main.components.TtsPlayingBanner

/**
 * PRD 第 4 節 UI 狀態機的唯一進入點。五種 [MainUiState] 都在這裡渲染，
 * 因為它們描述的是「同一個畫面」隨背景服務狀態改變的呈現方式，而非彼此可
 * 導覽的獨立頁面（doc/phase1-spec.md Screen Inventory 設計備註）。
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 異常矩陣：使用者中途關閉通知權限，回到前景須立即重新檢查。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onIntent(MainViewIntent.OnScreenResumed)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MainScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/** internal（非 private）讓 androidTest 的 Compose UI 測試可直接餵入假的
 *  MainUiState / onIntent，不需要透過真正的 Hilt ViewModel 走一遍。 */
@Composable
internal fun MainScreenContent(
    uiState: MainUiState,
    onIntent: (MainViewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Android 15+ 強制 edge-to-edge，內容會預設畫到系統列（狀態列/導覽列）
    // 底下；safeDrawing 把實際內容（文字、按鈕）從系統列往內推，背景則仍由
    // MainActivity 的 Surface 延伸到整個視窗，維持真正的 edge-to-edge 效果。
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (uiState) {
            MainUiState.InitChecking -> InitCheckingContent()

            MainUiState.PermissionDenied ->
                PermissionDeniedOverlay(
                    onPermissionSettingsClicked = {
                        onIntent(MainViewIntent.OnPermissionSettingsClicked)
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                )

            is MainUiState.IdleConfig ->
                IdleConfigContent(
                    installedApps = uiState.installedApps,
                    onIntent = onIntent,
                )

            is MainUiState.TtsPlaying ->
                Column(modifier = Modifier.fillMaxSize()) {
                    TtsPlayingBanner(speakingFromLabel = uiState.speakingFromLabel)
                    IdleConfigContent(
                        installedApps = uiState.installedApps,
                        onIntent = onIntent,
                        modifier = Modifier.weight(1f),
                    )
                }

            is MainUiState.EngineError ->
                EngineErrorDialog(
                    reason = uiState.reason,
                    onInstallTtsEngineClicked = {
                        onIntent(MainViewIntent.OnInstallTtsEngineClicked)
                        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    },
                    onRetryClicked = { onIntent(MainViewIntent.OnRetryEngineInitClicked) },
                )
        }
    }
}

@Composable
private fun InitCheckingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * IdleConfig 的白名單清單若為空，顯示區塊層級的 empty-hint 文案，
 * 不視為獨立的頂層 UiState（doc/phase1-spec.md Screen Inventory 設計備註）。
 *
 * 搜尋框的輸入文字純粹是畫面顯示用的過濾條件，不影響白名單本身的business
 * 邏輯，因此刻意留在 Compose 本地狀態（不放進 [MainUiState]／不經過
 * [MainViewModel]），符合 UI 層「dumb」的原則。
 */
@Composable
private fun IdleConfigContent(
    installedApps: List<AppWhitelistItem>,
    onIntent: (MainViewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps =
        remember(installedApps, searchQuery) {
            if (searchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter { it.appLabel.contains(searchQuery, ignoreCase = true) }
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜尋 App 名稱") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )

        if (filteredApps.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (installedApps.isEmpty()) {
                        "尚未偵測到可勾選的應用程式"
                    } else {
                        "找不到符合「$searchQuery」的 App"
                    },
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredApps, key = { it.packageName }) { item ->
                    AppWhitelistRow(
                        item = item,
                        onToggle = { packageName, checked ->
                            onIntent(MainViewIntent.OnAppWhitelistToggled(packageName, checked))
                        },
                    )
                }
            }
        }
        Button(
            onClick = { onIntent(MainViewIntent.OnSendTestNotificationClicked) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text("發送測試通知")
        }
    }
}
