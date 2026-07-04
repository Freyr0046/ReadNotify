package com.freyr.readmynotify.ui.main.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.freyr.readmynotify.domain.model.EngineErrorReason

/**
 * PRD 3. UI 狀態機 ENGINE_ERROR：彈出錯誤對話框，提示修復 TTS 引擎。
 * 不可透過點擊外部關閉（onDismissRequest 為空動作）——引擎未修復前不允許
 * 回到主介面。
 */
@Composable
fun EngineErrorDialog(
    reason: EngineErrorReason,
    onInstallTtsEngineClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, message) =
        when (reason) {
            EngineErrorReason.TTS_ENGINE_NOT_INSTALLED ->
                "尚未安裝語音引擎" to "請先安裝 Google 文字轉語音引擎才能使用播報功能。"
            EngineErrorReason.LANGUAGE_UNSUPPORTED ->
                "語系不支援" to "目前的文字轉語音語系不支援中文，請至系統設定切換語系後重試。"
            EngineErrorReason.INIT_FAILED ->
                "語音引擎初始化失敗" to "請稍後再試，或檢查裝置的文字轉語音設定。"
        }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            if (reason == EngineErrorReason.TTS_ENGINE_NOT_INSTALLED) {
                TextButton(onClick = onInstallTtsEngineClicked) { Text("前往下載") }
            } else {
                TextButton(onClick = onRetryClicked) { Text("重試") }
            }
        },
        dismissButton = {
            if (reason == EngineErrorReason.TTS_ENGINE_NOT_INSTALLED) {
                TextButton(onClick = onRetryClicked) { Text("重試") }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun EngineErrorDialogPreview() {
    EngineErrorDialog(
        reason = EngineErrorReason.TTS_ENGINE_NOT_INSTALLED,
        onInstallTtsEngineClicked = {},
        onRetryClicked = {},
    )
}
