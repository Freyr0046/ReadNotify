---
name: android-api-scaffold
description: >
  從 API Spec 自動產生 Android 整合程式碼，涵蓋 Model data class、MonsterApiService 
  endpoint、以及 Repository 方法。當使用者貼上 API Spec（含 endpoint path、request 
  參數、response 欄位）並要求完成 apiService 或 repository 時，立即使用此 skill。
  觸發關鍵字：「替我完成 apiService 和 repository」、「根據 spec 新增 API」、
  「幫我接這支 API」、「新增 endpoint」、「API Spec 實作」。
---

# Android API Integration Skill

根據使用者提供的 API Spec，自動完成三個層面的整合：Model、ApiService、Repository。

---

## Step 1 — 解析 API Spec

從使用者訊息中提取：

- **Endpoint path**（例：`/member/frontendWinningInvoiceList`）
- **HTTP method**（幾乎都是 POST）
- **Request parameters**：欄位名稱、型別、是否必填
- **Response data**：欄位名稱、型別、巢狀結構

---

## Step 2 — 探索專案 pattern

在動手寫任何程式碼之前，先讀懂專案慣例。

### 2a. 找 Model 目錄與現有範例
```
app/src/main/java/friendo/mtel/loyalty/model/
```
讀一個同領域的現有 Model（例如 `winning_receive/GetWinningInvoiceList.kt`），觀察：
- 是否使用 `@Parcelize` + `Parcelable`
- `@SerializedName` 的風格
- 是否有 helper function

### 2b. 確認 MonsterApiService 的 endpoint 格式
搜尋 MonsterApiService.kt 中與目標功能相近的 endpoint，確認：
- URL prefix（通常是 `v2/`）
- 函式簽名格式：`suspend fun xxx(@HeaderMap header: Map<String, String>, @Body body: JsonObject): Response<MonsterApiResponse<T>>`
- 是否已有同名但型別錯誤的 stub（若有，直接修正）

### 2c. 找對應的 Repository 與 apiManager 用法
讀取目標 Repository（通常是 `InvoiceRepository.kt` 或 `HomeRepository.kt`），確認：
- `apiManager.requestOnIoThread` 的呼叫方式
- body 建構：`JsonObject().apply { addProperty(...) }.toEncryptedRequestObject()`
- `apiManager.accessToken` 與 `memberId` 的使用時機

---

## Step 3 — 確認檔案路徑（動手前必做）

在寫任何程式碼之前，使用 `AskUserQuestion` 工具向使用者確認三件事。每個問題都要提供幾個你根據 API Spec domain 與專案現有結構推測的選項，加上一個「自行輸入」選項。

**3a. Model 檔案路徑**
根據 endpoint domain（例如 `/member/` → `model/member/`、`/invoice/` → `model/invoice/`）推算 2–3 個候選路徑與檔名，詢問使用者要在哪裡建立：

範例問法：
> Model 檔案要建立在哪裡？
> 1. `model/coupon/MemberCouponListInfo.kt`（依 endpoint domain 推測）
> 2. `model/member/MemberCouponListInfo.kt`
> 3. 其他（請輸入路徑與檔名）

**3b. Repository 方法要加在哪個 Repository**
列出專案中現有的 Repository 作為選項（通常是 `InvoiceRepository`、`HomeRepository` 等），詢問要加到哪一個：

範例問法：
> 這個方法要加到哪個 Repository？
> 1. `InvoiceRepository.kt`
> 2. `HomeRepository.kt`
> 3. 其他（請輸入）

若 Step 2 探索後發現 Model 檔案已存在（需填入空殼）或 ApiService 已有同名 stub，跳過對應的詢問，直接告知使用者「已找到 XXX，將直接修改」。

---

## Step 4 — 建立 Model

在使用者確認的路徑下建立或填入 data class：

**判斷是否需要 `@Parcelize`**：
- 若 response 物件會被放進 `SavedStateHandle`、Bundle、或透過 Safe Args 傳遞 → 加 `@Parcelize : Parcelable`
- 若僅在 ViewModel / Repository 內流通（StateFlow、SharedFlow）→ 純 `data class` 即可

**欄位型別對照**：

| API Spec 型別 | Kotlin 型別 |
|---|---|
| `int64` | `Long` |
| `int` | `Int` |
| `string` | `String` |
| `boolean` | `Boolean` |
| `array` | `List<T>` |

**範例（無需 Parcelable）**：
```kotlin
data class FooBarInfo(
    @SerializedName("item_list")
    val itemList: List<FooBarItem>
)

data class FooBarItem(
    @SerializedName("item_id")
    val itemId: Long,

    @SerializedName("item_name")
    val itemName: String
)
```

---

## Step 5 — 更新 MonsterApiService

在 `MonsterApiService` interface 中：

1. **若已有同名 stub 但型別錯誤** → 直接修正 return type，不要新增重複函式
2. **若尚無此 endpoint** → 在最後一個 `@POST` 之後、`companion object` 之前新增

```kotlin
@POST("v2/member/fooBarEndpoint")
suspend fun fooBarEndpoint(@HeaderMap header: Map<String, String>, @Body body: JsonObject): Response<MonsterApiResponse<FooBarInfo>>
```

3. 在 import 區塊加上新 Model 的 import（按字母順序插入）

---

## Step 6 — 新增 Repository 方法

在對應的 Repository 末尾（`}` 之前）新增 suspend fun：

```kotlin
// <中文說明，與其他方法一致>
suspend fun getFooBarInfo(invPeriod: String): FooBarInfo {
    return apiManager.requestOnIoThread({
        val body = JsonObject().apply {
            addProperty("member_id", memberId)
            addProperty("inv_period", invPeriod)
            addProperty("access_token", apiManager.accessToken)
        }.toEncryptedRequestObject()
        service.fooBarEndpoint(apiManager.getApiKeyHeaders(), body)
    })
}
```

**參數命名原則**：
- 使用 API Spec 原始欄位名稱作為 `addProperty` 的 key（不要自行轉換）
- 若 Spec 有 `access_token` → 用 `apiManager.accessToken`
- 若 Spec 有 `member_id` → 用 `memberId`（已由 constructor 注入）

---

## Step 7 — 收尾確認

完成後回報：
1. **Model 檔案**：路徑 + 欄位清單
2. **ApiService**：新增或修正的函式簽名
3. **Repository**：新增的方法名稱與參數

若發現既有 stub 型別錯誤（如本次 `GetPlayableWheel` 應為 `FrontendWinningInvoiceListInfo`），主動說明「修正了 XXX 的 return type」。
