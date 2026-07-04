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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.freyr.readmynotify.ui.main.AppWhitelistItem

/**
 * App 圖示不放進 [AppWhitelistItem]（Drawable 不可比較/不穩定，見
 * doc/phase1-spec.md Screen Inventory 設計備註），改由這裡依 packageName
 * 自行以 remember{} 快取載入，避免不必要的 recomposition 成本。
 */
@Composable
fun AppWhitelistRow(
    item: AppWhitelistItem,
    onToggle: (packageName: String, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon =
        remember(item.packageName) {
            runCatching { context.packageManager.getApplicationIcon(item.packageName) }.getOrNull()
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onToggle(item.packageName, !item.isChecked) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
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
