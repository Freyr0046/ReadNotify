# Android Code Review Examples

Complete examples demonstrating how to structure code review feedback following the android-reviewer skill guidelines.

## Table of Contents
1. [Example 1: Feature Addition (High Risk)](#example-1-feature-addition-high-risk)
2. [Example 2: Bug Fix (Medium Risk)](#example-2-bug-fix-medium-risk)
3. [Example 3: UI Refinement (Low Risk)](#example-3-ui-refinement-low-risk)

---

## Example 1: Feature Addition (High Risk)

**Scenario:** New user profile feature with ViewModel, Repository, and Fragment implementation.

### 🔍 PR Summary & Strategy
- **Detected Type**: Feature Addition
- **Risk Level**: 🔴 High
- **Review Focus**: Architecture layering, state management, lifecycle handling, memory leaks
- **Files Changed**: 8 files (4 new, 4 modified)
- **Overall Assessment**:
    - Strengths: Clean MVVM separation, good use of sealed classes for state
    - Concerns: Context leak in ViewModel, missing viewLifecycleOwner, no defensive null checks
    - Code Quality: 6/10
    - Est. Fix Time: 2-3 hours

### 🎯 Fix Priority

**Order of resolution:**
1. **BLOCKER #1**: Context leak in UserProfileViewModel (affects all profile screens)
2. **BLOCKER #2**: Fragment lifecycle violation in UserProfileFragment
3. **IMPORTANT #1**: Missing null validation in deep link handler
4. **IMPORTANT #2**: Exposed mutable state in ViewModel
5. **Quick Wins** (<30min):
    - Replace Bundle keys with SafeArgs
    - Extract hardcoded strings to resources

**Impact Estimate:** 4 files, ~80 lines, Risk: 🟡 Medium

### 🚨 Critical Issues (BLOCKER)

#### Issue 1: Context Leak in ViewModel

```
📍 UserProfileViewModel.kt:15 - [Architecture]
❌ Issue: ViewModel holds reference to Application Context

❌ Current:
```kotlin
class UserProfileViewModel(
    private val context: Context,  // BLOCKER: Context leak
    private val repository: UserRepository
) : ViewModel() {
    
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
```

✅ Fix:
```kotlin
class UserProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {
    
    // Use events for one-shot UI actions
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    suspend fun notifyUser(message: String) {
        _events.send(Event.ShowToast(message))
    }
    
    sealed class Event {
        data class ShowToast(val message: String) : Event()
    }
}

// In Fragment:
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.events.collect { event ->
        when (event) {
            is Event.ShowToast -> Toast.makeText(requireContext(), event.message, LENGTH_SHORT).show()
        }
    }
}
```

💡 Why: ViewModels must not hold Context references as they outlive Activity/Fragment lifecycle, causing memory leaks. Use events to communicate UI actions.

🎲 Risk: 🔴 High - Affects all screens using this ViewModel, causes memory leak on every screen rotation
🧪 Test:
- Verify no context references in ViewModel with lint
- Test with LeakCanary
- Rotate device multiple times and check memory
```

#### Issue 2: Fragment Lifecycle Violation

```
📍 UserProfileFragment.kt:67 - [Lifecycle]
❌ Issue: Using Fragment lifecycle instead of view lifecycle for UI observers

❌ Current:
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // BLOCKER: Uses Fragment lifecycle, not view lifecycle
    lifecycleScope.launch {
        viewModel.uiState.collect { state ->
            updateUI(state)  // Crashes after onDestroyView
        }
    }
}
```

✅ Fix:
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // Correct: Use viewLifecycleOwner for UI-related observers
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }
}
```

💡 Why: Fragment lifecycle outlives view lifecycle. Collection continues after onDestroyView, causing crashes when updating destroyed views.

🎲 Risk: 🔴 High - Guaranteed crash when navigating back from this Fragment
🧪 Test:
- Navigate to Fragment → Back → Trigger state update
- Add Fragment to back stack and verify no crashes
- Use Fragment Strict Mode to detect violations
```

### ⚠️ Major Issues (IMPORTANT)

#### Issue 1: Missing Input Validation

```
📍 UserProfileActivity.kt:45 - [Defensive Programming]
⚠️ Issue: No validation of deep link parameters

❌ Current:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val userId = intent.data?.getQueryParameter("user_id")!!
    loadUserProfile(userId)  // Crashes if parameter missing
}
```

✅ Fix:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val uri = intent?.data ?: run {
        Log.w(TAG, "No deep link URI")
        finish()
        return
    }
    
    val userId = uri.getQueryParameter("user_id")?.takeIf { it.isNotBlank() } ?: run {
        Log.e(TAG, "Missing user_id parameter")
        Toast.makeText(this, "Invalid profile link", Toast.LENGTH_LONG).show()
        finish()
        return
    }
    
    // Validate format
    if (!userId.matches(Regex("^[a-zA-Z0-9_-]{1,64}$"))) {
        Log.e(TAG, "Invalid user_id format: $userId")
        Toast.makeText(this, "Invalid user ID", Toast.LENGTH_LONG).show()
        finish()
        return
    }
    
    loadUserProfile(userId)
}
```

💡 Why: Deep links can be crafted by external apps. Always validate all inputs with null checks, format validation, and boundary checks.

🎲 Risk: 🟡 Medium - Crashes from malformed deep links, potential security risk
🧪 Test:
- Test with missing parameters
- Test with malformed user IDs
- Test with XSS/injection attempts in parameters
```

#### Issue 2: Exposed Mutable State

```
📍 UserProfileViewModel.kt:25 - [State Management]
⚠️ Issue: Public mutable state allows external mutation

❌ Current:
```kotlin
val uiState = MutableStateFlow<UiState>(UiState.Loading)
```

✅ Fix:
```kotlin
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

💡 Why: Exposing mutable state allows any caller to change state, breaking single source of truth principle.

🎲 Risk: 🟡 Medium - State consistency issues, harder to debug
🧪 Test: Verify state only changes through ViewModel methods
```

### 💡 Suggestions & Nits (OPTIONAL)

```
📍 UserProfileFragment.kt:120
💭 Consider: Extract string "Profile loaded" to resources (R.string.profile_loaded)

📍 UserProfileRepository.kt:45
💭 Consider: Use Kotlin's `use {}` for automatic stream closing instead of manual try-finally

📍 UserProfileViewModel.kt:80
💭 Consider: Replace `if (user != null)` with `user?.let { }` for more idiomatic Kotlin
```

### ✅ Positive Feedback (Conditional)

UserProfileViewModel.kt:35-50 - Excellent use of sealed class for UiState, provides type-safe state handling

### 📚 Learning Resources (Conditional)

- Reference: `references/lifecycle-patterns.md` for proper Fragment lifecycle handling
- Reference: `references/code-examples.md` section on "Lifecycle-Aware Collection"
- Team pattern: Multiple recent PRs show confusion about `viewLifecycleOwner` vs `lifecycleScope` - recommend team training

### 📋 Review Checklist
- [❌] Lifecycle methods clean and minimal
- [❌] ViewBinding properly managed
- [❌] No context leaks in ViewModel
- [✅] Proper coroutine scope usage
- [❌] Navigation type-safe
- [❌] No hardcoded strings/keys
- [✅] Security concerns addressed
- [❌] Memory leak risks mitigated

### ✅ Pre-Merge Requirements
- [ ] All BLOCKER issues resolved (Context leak, lifecycle violation)
- [ ] IMPORTANT issues resolved or tracked as tech debt
- [ ] Tests added for deep link validation edge cases
- [ ] Verify with LeakCanary - no memory leaks on rotation
- [ ] Update team documentation with lifecycle best practices

---

## Example 2: Bug Fix (Medium Risk)

**Scenario:** Fix crash when user data is null in profile screen.

### 🔍 PR Summary & Strategy
- **Detected Type**: Bug Fix
- **Risk Level**: 🟡 Medium
- **Review Focus**: Root cause analysis, regression risks, defensive coding
- **Files Changed**: 2 files (UserProfileFragment.kt, UserViewModel.kt)
- **Overall Assessment**:
  - Strengths: Proper null checks added, includes error state
  - Concerns: Doesn't address root cause, missing test coverage
  - Code Quality: 7/10
  - Est. Fix Time: 30 minutes

### 🎯 Fix Priority

**Order of resolution:**
1. **IMPORTANT #1**: Root cause not addressed - why is user null?
2. **Quick Wins** (<30min): Add test case for null user scenario

**Impact Estimate:** 2 files, ~15 lines, Risk: 🟢 Low

### ⚠️ Major Issues (IMPORTANT)

```
📍 UserProfileFragment.kt:89 - [Root Cause]
⚠️ Issue: Symptom fixed but root cause not investigated

❌ Current Fix:
```kotlin
private fun updateUI(state: UiState) {
    when (state) {
        is UiState.Success -> {
            // Added null check
            if (state.user != null) {
                binding.nameText.text = state.user.name
            } else {
                showError("User data unavailable")
            }
        }
    }
}
```

✅ Suggested Investigation:
```kotlin
// In ViewModel:
fun loadUser(id: String) {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        try {
            val user = repository.getUser(id)
            
            // ROOT CAUSE: Why would getUser() return null?
            // Option 1: Should it throw exception instead?
            // Option 2: Is the API returning null for deleted users?
            // Option 3: Is there a caching issue?
            
            if (user == null) {
                Log.w(TAG, "Repository returned null user for ID: $id")
                _uiState.value = UiState.Error("User not found")
            } else {
                _uiState.value = UiState.Success(user)
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }
}

// Recommendation: Change repository signature to non-nullable
// and throw UserNotFoundException instead of returning null
```

💡 Why: Fixing symptoms without understanding root cause may hide deeper issues. Consider if null is a valid state or indicates a problem.

🎲 Risk: 🟡 Medium - May mask underlying data integrity issues
🧪 Test: Add test cases for: user not found, deleted user, network error, cache miss
```

### 📋 Review Checklist
- [✅] Lifecycle methods clean and minimal
- [✅] ViewBinding properly managed
- [✅] No context leaks in ViewModel
- [✅] Proper coroutine scope usage
- [✅] Navigation type-safe
- [✅] No hardcoded strings/keys
- [✅] Security concerns addressed
- [✅] Memory leak risks mitigated

### ✅ Pre-Merge Requirements
- [ ] Root cause investigated and documented
- [ ] Test cases added for null user scenario
- [ ] Consider making User non-nullable in repository layer

---

## Example 3: UI Refinement (Low Risk)

**Scenario:** Update profile screen layout with new design.

### 🔍 PR Summary & Strategy
- **Detected Type**: UI Refinement
- **Risk Level**: 🟢 Low
- **Review Focus**: Performance, memory efficiency, theming consistency
- **Files Changed**: 3 files (fragment_profile.xml, styles.xml, ProfileFragment.kt)
- **Overall Assessment**:
  - Strengths: Follows design system, proper use of themes
  - Concerns: Potential overdraw in layout, missing content descriptions
  - Code Quality: 8/10
  - Est. Fix Time: 15 minutes

### 🎯 Fix Priority

**Quick Wins** (<30min):
- Add content descriptions for accessibility
- Remove unnecessary nested layouts (overdraw)

**Impact Estimate:** 2 files, ~10 lines, Risk: 🟢 Low

### 💡 Suggestions & Nits (OPTIONAL)

```
📍 fragment_profile.xml:45
💭 Consider: Remove nested LinearLayout - can be achieved with ConstraintLayout constraints

📍 fragment_profile.xml:67
💭 Consider: Add android:contentDescription for ImageView (accessibility)

📍 ProfileFragment.kt:120
💭 Consider: Use ViewBinding instead of findViewById for type safety
```

### ✅ Positive Feedback (Conditional)

styles.xml:25-40 - Excellent reuse of theme attributes, maintains consistency across app

### 📋 Review Checklist
- [✅] Lifecycle methods clean and minimal
- [✅] ViewBinding properly managed
- [✅] No context leaks in ViewModel
- [✅] Proper coroutine scope usage
- [✅] Navigation type-safe
- [✅] No hardcoded strings/keys
- [N/A] Security concerns addressed
- [✅] Memory leak risks mitigated

### ✅ Pre-Merge Requirements
- [ ] Add accessibility content descriptions
- [ ] Verify no visual regressions on different screen sizes
- [ ] Test with TalkBack enabled

---

## Token Efficiency Notes

### When to Include Optional Sections:

**Positive Feedback:**
- Only include if implementation is truly exceptional
- 1 line max per commendation
- Example: Code that should be team reference

**Learning Resources:**
- Only include if knowledge gaps detected
- Reference specific files, not generic advice
- Note team patterns only if seen across multiple PRs

**Quick Wins:**
- Only include if fixes take < 30 minutes
- Group together to avoid clutter
- Prioritize high-impact, low-effort items

### When to Skip Sections:

- Skip Positive Feedback if nothing exceptional
- Skip Learning Resources if no recurring issues
- Skip Quick Wins if none exist
- Skip Suggestions if only minor nits

### Average Token Usage by PR Type:

- **High-risk Feature**: ~2000-2500 tokens (includes all sections)
- **Medium-risk Bug Fix**: ~1200-1500 tokens (skips some optional sections)
- **Low-risk UI Change**: ~800-1000 tokens (minimal optional sections)