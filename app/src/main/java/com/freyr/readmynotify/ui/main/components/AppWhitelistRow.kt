package com.freyr.readmynotify.ui.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.freyr.readmynotify.ui.main.AppWhitelistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App 圖示不放進 [AppWhitelistItem]（Drawable 不可比較/不穩定，見
 * doc/phase1-spec.md Screen Inventory 設計備註），改由這裡依 packageName
 * 自行載入。實機測試過的裝置有 300+ 已安裝套件，`getApplicationIcon` +
 * `toBitmap()` 是同步的 binder/解碼呼叫，用 [produceState] 移到
 * [Dispatchers.IO] 執行，避免 LazyColumn 捲動時卡在主執行緒。
 */
@Composable
fun AppWhitelistRow(
    item: AppWhitelistItem,
    onToggle: (packageName: String, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, item.packageName) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(item.packageName).toBitmap().asImageBitmap()
                }.getOrNull()
            }
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onToggle(item.packageName, !item.isChecked) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val loadedIcon = icon
        if (loadedIcon != null) {
            Image(
                bitmap = loadedIcon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.appLabel,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f, fill = true),
        )
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { checked -> onToggle(item.packageName, checked) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppWhitelistRowPreview() {
    AppWhitelistRow(
        item = AppWhitelistItem(packageName = "com.example.line", appLabel = "Line", isChecked = true),
        onToggle = { _, _ -> },
    )
}
