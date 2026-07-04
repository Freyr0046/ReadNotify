# Naming Conventions

Comprehensive naming standards for Android projects including XML resources, drawables, and Kotlin code.

## Table of Contents

1. [XML Layout Naming](#xml-layout-naming)
2. [Drawable Naming](#drawable-naming)
3. [Resource ID Naming](#resource-id-naming)
4. [Kotlin Code Naming](#kotlin-code-naming)
5. [String Resources](#string-resources)
6. [Common Violations](#common-violations)

---

## XML Layout Naming

### Layout File Naming Pattern

**Format:** `{type}_{name}.xml`

| Type | Pattern | Example |
|------|---------|---------|
| Fragment | `fragment_{name}` | `fragment_user_profile.xml` |
| Activity | `activity_{name}` | `activity_main.xml` |
| Dialog | `dialog_{name}` | `dialog_confirmation.xml` |
| Bottom Sheet | `bottom_sheet_{name}` | `bottom_sheet_filter.xml` |
| Shared Layout | `layout_{name}` | `layout_toolbar.xml` |
| RecyclerView Item | `item_{name}` | `item_user.xml` |
| ViewHolder Layout | `item_{recyclerview_name}` | `item_chat_message.xml` |
| Include/Merge | `include_{name}` | `include_header.xml` |

### ✅ CORRECT Layout Naming

```
res/layout/
├── fragment_home.xml              ✅ Fragment screen
├── fragment_user_profile.xml      ✅ Fragment with compound name
├── activity_main.xml              ✅ Activity screen
├── activity_splash.xml            ✅ Activity screen
├── dialog_logout_confirmation.xml ✅ Dialog with descriptive name
├── bottom_sheet_sort_options.xml  ✅ Bottom sheet
├── layout_toolbar_custom.xml      ✅ Shared custom toolbar
├── layout_empty_state.xml         ✅ Shared empty state view
├── item_user.xml                  ✅ RecyclerView item
├── item_chat_message.xml          ✅ RecyclerView item
├── item_product_grid.xml          ✅ RecyclerView item (grid)
└── include_loading_spinner.xml    ✅ Include/merge layout
```

### ❌ INCORRECT Layout Naming

```
res/layout/
├── home.xml                       ❌ Missing 'fragment_' prefix
├── user_profile_fragment.xml      ❌ Wrong order (should be fragment_user_profile.xml)
├── mainActivity.xml               ❌ camelCase (should be activity_main.xml)
├── dialog.xml                     ❌ Not descriptive
├── custom_toolbar.xml             ❌ Should be layout_toolbar_custom.xml
├── user_item.xml                  ❌ Should be item_user.xml
├── recycler_item_product.xml      ❌ Should be item_product.xml
└── UserListItem.xml               ❌ PascalCase (should be item_user.xml)
```

---

## Drawable Naming

### Drawable File Naming Patterns

#### 1. Shape Backgrounds

**Format:** `shape_bg_{color}_{optional_descriptor}_radius{value}.xml`

```
res/drawable/
├── shape_bg_white_radius8.xml           ✅ White background, 8dp radius
├── shape_bg_primary_radius12.xml        ✅ Primary color, 12dp radius
├── shape_bg_gray_border_radius16.xml    ✅ Gray with border, 16dp radius
├── shape_bg_transparent_stroke_radius4.xml ✅ Transparent with stroke
└── shape_bg_gradient_blue_radius24.xml  ✅ Gradient background
```

**Anti-patterns:**
```
res/drawable/
├── background.xml                       ❌ Not descriptive
├── shape_white.xml                      ❌ Missing 'bg' and radius info
├── bg_radius_8.xml                      ❌ Missing color
├── white_background_8dp.xml             ❌ Should be shape_bg_white_radius8.xml
└── shape_background_white.xml           ❌ Missing radius value
```

**Example content:**
```xml
<!-- shape_bg_white_radius8.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/white" />
    <corners android:radius="8dp" />
</shape>
```

```xml
<!-- shape_bg_primary_border_radius12.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/primary" />
    <stroke
        android:width="1dp"
        android:color="@color/primary_dark" />
    <corners android:radius="12dp" />
</shape>
```

#### 2. Icons

**Format:** `ic_{name}_{size}.xml` or `ic_{name}.xml`

```
res/drawable/
├── ic_home.xml                    ✅ Home icon
├── ic_user.xml                    ✅ User icon
├── ic_arrow_right.xml             ✅ Arrow icon
├── ic_close_24.xml                ✅ Close icon, 24dp
├── ic_menu_24.xml                 ✅ Menu icon, 24dp
├── ic_search_white.xml            ✅ Search icon, white variant
└── ic_notification_badge.xml      ✅ Notification with badge
```

**Anti-patterns:**
```
res/drawable/
├── home.xml                       ❌ Missing 'ic_' prefix
├── icon_home.xml                  ❌ Use 'ic_' not 'icon_'
├── home_icon.xml                  ❌ Wrong order
├── icHome.xml                     ❌ camelCase
└── ic_HomeIcon.xml                ❌ PascalCase
```

#### 3. Image Resources

**Format:** `pic_{name}.{ext}`

```
res/drawable/
├── pic_logo.png                   ✅ Logo image
├── pic_banner_home.png            ✅ Home banner
├── pic_placeholder_avatar.png     ✅ Avatar placeholder
├── pic_empty_state.png            ✅ Empty state illustration
└── pic_onboarding_step1.png       ✅ Onboarding image
```

**Anti-patterns:**
```
res/drawable/
├── logo.png                       ❌ Missing 'pic_' prefix
├── image_logo.png                 ❌ Use 'pic_' not 'image_'
├── img_logo.png                   ❌ Use 'pic_' not 'img_'
└── banner.png                     ❌ Not descriptive enough
```

#### 4. Selectors

**Format:** `selector_{name}.xml`

```
res/drawable/
├── selector_button_primary.xml    ✅ Button state selector
├── selector_tab.xml               ✅ Tab state selector
├── selector_checkbox.xml          ✅ Checkbox state selector
└── selector_ripple_primary.xml    ✅ Ripple effect selector
```

#### 5. Layer Lists

**Format:** `layer_{name}.xml`

```
res/drawable/
├── layer_card_shadow.xml          ✅ Card with shadow
├── layer_button_elevated.xml      ✅ Elevated button
└── layer_divider_gradient.xml     ✅ Gradient divider
```

### Complete Drawable Naming Reference

| Drawable Type | Pattern | Example |
|---------------|---------|---------|
| Shape | `shape_bg_{color}_radius{n}` | `shape_bg_white_radius8.xml` |
| Icon | `ic_{name}` | `ic_home.xml` |
| Icon (sized) | `ic_{name}_{size}` | `ic_close_24.xml` |
| Image | `pic_{name}` | `pic_logo.png` |
| Selector | `selector_{name}` | `selector_button.xml` |
| Layer | `layer_{name}` | `layer_shadow.xml` |
| Vector | `vec_{name}` | `vec_illustration.xml` |
| Ripple | `ripple_{name}` | `ripple_primary.xml` |

---

## Resource ID Naming

### View ID Naming Convention

**Format:** `{viewType}{Description}`

| View Type | Prefix | Example |
|-----------|--------|---------|
| TextView | `tv` | `tvTitle`, `tvUserName` |
| EditText | `et` | `etEmail`, `etPassword` |
| Button | `btn` | `btnSubmit`, `btnCancel` |
| ImageView | `iv` | `ivProfile`, `ivLogo` |
| RecyclerView | `rv` | `rvUsers`, `rvProducts` |
| ConstraintLayout | `cl` | `clContainer`, `clRoot` |
| LinearLayout | `ll` | `llHeader`, `llContent` |
| FrameLayout | `fl` | `flContainer` |
| CardView | `cv` | `cvUser`, `cvProduct` |
| ProgressBar | `pb` | `pbLoading` |
| Switch | `sw` | `swNotifications` |
| CheckBox | `cb` | `cbAccept` |
| RadioButton | `rb` | `rbOption1` |
| Spinner | `sp` | `spCountry` |
| ViewPager2 | `vp` | `vpOnboarding` |
| TabLayout | `tl` | `tlCategories` |
| NestedScrollView | `nsv` | `nsvContent` |
| Toolbar | `toolbar` | `toolbar` (exception, no prefix) |
| AppBarLayout | `appBar` | `appBar` (exception, no prefix) |

### ✅ CORRECT Resource ID Naming

```xml
<!-- fragment_user_profile.xml -->
<ConstraintLayout
    android:id="@+id/clRoot">
    
    <ImageView
        android:id="@+id/ivProfilePhoto" />
    
    <TextView
        android:id="@+id/tvUserName" />
    
    <TextView
        android:id="@+id/tvUserEmail" />
    
    <Button
        android:id="@+id/btnEditProfile" />
    
    <RecyclerView
        android:id="@+id/rvUserPosts" />
        
</ConstraintLayout>
```

### ❌ INCORRECT Resource ID Naming

```xml
<!-- ❌ Bad naming examples -->
<TextView
    android:id="@+id/title" />              ❌ No prefix

<TextView
    android:id="@+id/textViewTitle" />      ❌ Full word instead of prefix

<TextView
    android:id="@+id/tv_title" />           ❌ Snake case

<TextView
    android:id="@+id/TvTitle" />            ❌ PascalCase

<EditText
    android:id="@+id/email" />              ❌ No prefix

<Button
    android:id="@+id/button1" />            ❌ Not descriptive
```

---

## Kotlin Code Naming

### Class Naming

**Format:** `UpperCamelCase` (PascalCase)

```kotlin
// ✅ CORRECT
class UserProfileFragment : Fragment()
class UserViewModel : ViewModel()
class UserRepository
interface ApiService
data class User(val id: String)
sealed class UiState
object Constants
enum class UserRole

// ❌ INCORRECT
class userProfileFragment : Fragment()     ❌ lowerCamelCase
class user_profile_fragment : Fragment()   ❌ snake_case
class USERFRAGMENT : Fragment()            ❌ UPPERCASE
```

### Variable & Property Naming

**Format:** `lowerCamelCase`

```kotlin
// ✅ CORRECT
private var userName: String = ""
private val binding: FragmentBinding
private val viewModel: UserViewModel by viewModels()
val isLoggedIn: Boolean = false
var userAge: Int = 0

// ❌ INCORRECT
private var UserName: String = ""          ❌ PascalCase
private var user_name: String = ""         ❌ snake_case
private var USERNAME: String = ""          ❌ UPPERCASE (not a constant)
```

### Function Naming

**Format:** `lowerCamelCase`

```kotlin
// ✅ CORRECT
fun loadUserData()
fun onLoginButtonClicked()
private fun setupRecyclerView()
suspend fun fetchUserFromApi()

// ❌ INCORRECT
fun LoadUserData()                         ❌ PascalCase
fun load_user_data()                       ❌ snake_case
fun LOAD_USER_DATA()                       ❌ UPPERCASE
```

### Constant Naming

**Format:** `UPPER_SNAKE_CASE`

```kotlin
// ✅ CORRECT
const val MAX_RETRY_COUNT = 3
const val API_BASE_URL = "https://api.example.com"
const val DEFAULT_PAGE_SIZE = 20

companion object {
    private const val TAG = "UserFragment"
    const val REQUEST_CODE_CAMERA = 100
}

// ❌ INCORRECT
const val maxRetryCount = 3                ❌ lowerCamelCase
const val Max_Retry_Count = 3              ❌ Mixed case
const val max-retry-count = 3              ❌ Kebab case
```

### Package Naming

**Format:** `lowercase` (no underscores)

```kotlin
// ✅ CORRECT
package com.example.userprofile
package com.example.core.data
package com.example.feature.home

// ❌ INCORRECT
package com.example.userProfile           ❌ camelCase
package com.example.user_profile          ❌ snake_case
package com.example.User.Profile          ❌ PascalCase
```

### File Naming

**Format:** Match class name (PascalCase)

```
// ✅ CORRECT
UserProfileFragment.kt
UserViewModel.kt
UserRepository.kt
StringExtensions.kt
Constants.kt

// ❌ INCORRECT
userProfileFragment.kt                    ❌ lowerCamelCase
user_profile_fragment.kt                  ❌ snake_case
fragment_user_profile.kt                  ❌ File should match class
```

---

## String Resources

### String Resource Naming

**Format:** `{screen}_{element}_{description}`

```xml
<!-- ✅ CORRECT -->
<resources>
    <!-- Fragment/Screen specific -->
    <string name="home_title">Home</string>
    <string name="home_subtitle">Welcome back</string>
    <string name="home_btn_logout">Log Out</string>
    
    <string name="login_title">Login</string>
    <string name="login_et_email_hint">Email</string>
    <string name="login_et_password_hint">Password</string>
    <string name="login_btn_submit">Sign In</string>
    <string name="login_error_invalid_email">Invalid email address</string>
    
    <!-- Global/Shared strings -->
    <string name="app_name">My App</string>
    <string name="common_ok">OK</string>
    <string name="common_cancel">Cancel</string>
    <string name="common_retry">Retry</string>
    <string name="error_network">Network error</string>
    <string name="error_unknown">Unknown error</string>
</resources>
```

```xml
<!-- ❌ INCORRECT -->
<resources>
    <string name="title">Home</string>                    ❌ Not specific
    <string name="button_text">Submit</string>            ❌ Not descriptive
    <string name="email">Email</string>                   ❌ Missing context
    <string name="LoginEmailHint">Email</string>          ❌ PascalCase
    <string name="login-email-hint">Email</string>        ❌ Kebab case
</resources>
```

### String Format Naming

```xml
<!-- ✅ CORRECT - Parameterized strings -->
<string name="user_greeting">Hello, %1$s!</string>
<string name="items_count">%1$d items</string>
<string name="user_profile_subtitle">%1$s · %2$d posts</string>

<!-- Usage in Kotlin -->
val greeting = getString(R.string.user_greeting, userName)
val count = getString(R.string.items_count, itemCount)
```

---

## Common Violations

### Violation 1: Inconsistent Prefixes

```xml
<!-- ❌ INCORRECT - Mixed naming -->
<TextView android:id="@+id/titleText" />        ❌ Should be @+id/tvTitle
<Button android:id="@+id/submit" />             ❌ Should be @+id/btnSubmit
<EditText android:id="@+id/emailField" />       ❌ Should be @+id/etEmail
```

### Violation 2: Non-Descriptive Names

```kotlin
// ❌ INCORRECT
class Fragment1 : Fragment()                    ❌ Use UserProfileFragment
fun func1() { }                                 ❌ Use loadUserData()
val data = ""                                   ❌ Use userName
```

### Violation 3: Wrong Case Convention

```kotlin
// ❌ INCORRECT
class user_viewmodel : ViewModel()              ❌ Use UserViewModel
fun Load_Data() { }                             ❌ Use loadData()
val User_Name = ""                              ❌ Use userName
const val maxRetryCount = 3                     ❌ Use MAX_RETRY_COUNT
```

### Violation 4: Abbreviations

```kotlin
// ❌ INCORRECT - Unclear abbreviations
class UsrPrflFrgmnt : Fragment()               ❌ Use UserProfileFragment
fun ldUsrDt() { }                              ❌ Use loadUserData()

// ✅ CORRECT - Acceptable abbreviations
class HttpClient                               ✅ HTTP is well-known
class ApiService                               ✅ API is well-known
val urlString                                  ✅ URL is well-known
```

---

## Review Checklist

When reviewing naming conventions:

### XML Layouts
- [ ] Fragment layouts use `fragment_{name}.xml`
- [ ] Activity layouts use `activity_{name}.xml`
- [ ] Dialog layouts use `dialog_{name}.xml`
- [ ] RecyclerView items use `item_{name}.xml`
- [ ] Shared layouts use `layout_{name}.xml`

### Drawables
- [ ] Shapes use `shape_bg_{color}_radius{n}.xml`
- [ ] Icons use `ic_{name}.xml`
- [ ] Images use `pic_{name}.{ext}`
- [ ] No generic names like `background.xml`, `icon.xml`

### Resource IDs
- [ ] TextView uses `tv` prefix
- [ ] EditText uses `et` prefix
- [ ] Button uses `btn` prefix
- [ ] ImageView uses `iv` prefix
- [ ] RecyclerView uses `rv` prefix
- [ ] All IDs use lowerCamelCase

### Kotlin Code
- [ ] Classes use PascalCase
- [ ] Functions use lowerCamelCase
- [ ] Variables use lowerCamelCase
- [ ] Constants use UPPER_SNAKE_CASE
- [ ] Packages use lowercase (no underscores)
- [ ] No abbreviations unless well-known (HTTP, API, URL)

### Strings
- [ ] Format: `{screen}_{element}_{description}`
- [ ] Global strings prefixed with `common_` or `error_`
- [ ] Parameterized strings use proper format specifiers

---

## Violation Reporting Template

When reporting naming violations in code reviews:

```markdown
### 🏷️ Naming Convention Violations

📍 File: fragment_user_profile.xml, Line: 15
❌ Current: `<TextView android:id="@+id/title" />`
✅ Should be: `<TextView android:id="@+id/tvTitle" />`
💡 Reason: TextView must use 'tv' prefix per naming conventions

📍 File: res/drawable/background.xml
❌ Current: `background.xml`
✅ Should be: `shape_bg_white_radius8.xml`
💡 Reason: Shape drawables must include color and radius information

📍 File: UserViewModel.kt, Line: 25
❌ Current: `const val maxRetryCount = 3`
✅ Should be: `const val MAX_RETRY_COUNT = 3`
💡 Reason: Constants must use UPPER_SNAKE_CASE

📍 File: res/layout/home.xml
❌ Current: `home.xml`
✅ Should be: `fragment_home.xml`
💡 Reason: Fragment layouts must use 'fragment_' prefix
```
