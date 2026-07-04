package com.freyr.readmynotify.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * PRD 3. UI 狀態機 PERMISSION_DENIED：全螢幕阻擋，僅顯示一個「前往開啟權限」
 * 核心按鈕，不允許進入主介面的其他操作。
 */
@Composable
fun PermissionDeniedOverlay(
    onPermissionSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "需要通知存取權限",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "請前往系統設定開啟通知存取權限，才能使用語音播報功能。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onPermissionSettingsClicked) {
                Text("前往開啟權限")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionDeniedOverlayPreview() {
    PermissionDeniedOverlay(onPermissionSettingsClicked = {})
}
