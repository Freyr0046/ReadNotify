package com.freyr.readmynotify.ui.main

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * doc/phase4-tasks.md Task 14 — happy-path Compose UI 測試，直接對
 * [MainScreenContent]（stateless）餵入假的 [MainUiState]/[MainViewIntent]，
 * 不透過真正的 Hilt ViewModel。
 */
class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionDenied_blocksMainConfigUi_andSettingsButtonDispatchesIntent() {
        var dispatched: MainViewIntent? = null
        composeTestRule.setContent {
            MainScreenContent(
                uiState = MainUiState.PermissionDenied,
                onIntent = { dispatched = it },
            )
        }

        // PERMISSION_DENIED 阻擋畫面下，白名單清單與測試通知按鈕都不應存在。
        composeTestRule.onAllNodesWithText("發送測試通知").assertCountEquals(0)

        composeTestRule.onNodeWithText("前往開啟權限").assertIsDisplayed().performClick()

        assertEquals(MainViewIntent.OnPermissionSettingsClicked, dispatched)
    }

    @Test
    fun idleConfig_togglingWhitelistRow_dispatchesOnAppWhitelistToggled() {
        var dispatched: MainViewIntent? = null
        val items = listOf(AppWhitelistItem(packageName = "com.line", appLabel = "Line", isChecked = false))

        composeTestRule.setContent {
            MainScreenContent(
                uiState = MainUiState.IdleConfig(installedApps = items),
                onIntent = { dispatched = it },
            )
        }

        composeTestRule.onNodeWithText("Line").assertIsDisplayed().performClick()

        val intent = dispatched
        assertTrue(intent is MainViewIntent.OnAppWhitelistToggled)
        intent as MainViewIntent.OnAppWhitelistToggled
        assertEquals("com.line", intent.packageName)
        assertTrue(intent.checked)
    }

    @Test
    fun idleConfig_searchFieldFiltersWhitelistRowsByAppLabel() {
        val items =
            listOf(
                AppWhitelistItem(packageName = "com.line", appLabel = "Line", isChecked = false),
                AppWhitelistItem(packageName = "com.facebook.katana", appLabel = "Facebook", isChecked = false),
            )

        composeTestRule.setContent {
            MainScreenContent(
                uiState = MainUiState.IdleConfig(installedApps = items),
                onIntent = {},
            )
        }

        composeTestRule.onNodeWithText("Line").assertIsDisplayed()
        composeTestRule.onNodeWithText("Facebook").assertIsDisplayed()

        composeTestRule.onNodeWithText("搜尋 App 名稱").performTextInput("Lin")

        composeTestRule.onNodeWithText("Line").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Facebook").assertCountEquals(0)
    }

    @Test
    fun idleConfig_selectAllButton_checksEveryFilteredAppThenLabelBecomesDeselectAll() {
        val dispatched = mutableListOf<MainViewIntent>()
        val items =
            listOf(
                AppWhitelistItem(packageName = "com.line", appLabel = "Line", isChecked = false),
                AppWhitelistItem(packageName = "com.facebook.katana", appLabel = "Facebook", isChecked = false),
            )

        composeTestRule.setContent {
            MainScreenContent(
                uiState = MainUiState.IdleConfig(installedApps = items),
                onIntent = { dispatched += it },
            )
        }

        composeTestRule.onNodeWithText("全選").assertIsDisplayed().performClick()

        assertEquals(
            setOf("com.line" to true, "com.facebook.katana" to true),
            dispatched.filterIsInstance<MainViewIntent.OnAppWhitelistToggled>()
                .map { it.packageName to it.checked }
                .toSet(),
        )
    }

    @Test
    fun idleConfig_selectAllButton_showsDeselectAllWhenEverythingAlreadyChecked() {
        val dispatched = mutableListOf<MainViewIntent>()
        val items =
            listOf(
                AppWhitelistItem(packageName = "com.line", appLabel = "Line", isChecked = true),
                AppWhitelistItem(packageName = "com.facebook.katana", appLabel = "Facebook", isChecked = true),
            )

        composeTestRule.setContent {
            MainScreenContent(
                uiState = MainUiState.IdleConfig(installedApps = items),
                onIntent = { dispatched += it },
            )
        }

        composeTestRule.onNodeWithText("全部取消").assertIsDisplayed().performClick()

        assertEquals(
            setOf("com.line" to false, "com.facebook.katana" to false),
            dispatched.filterIsInstance<MainViewIntent.OnAppWhitelistToggled>()
                .map { it.packageName to it.checked }
                .toSet(),
        )
    }

    @Test
    fun ttsPlaying_showsSpeakingBanner() {
        composeTestRule.setContent {
            MainScreenContent(
                uiState =
                    MainUiState.TtsPlaying(
                        speakingFromLabel = "正在播報：來自 Line 的通知...",
                        installedApps = emptyList(),
                    ),
                onIntent = {},
            )
        }

        composeTestRule.onNodeWithText("正在播報：來自 Line 的通知...").assertIsDisplayed()
    }
}
