---
name: android-reviewer
description: Production-grade Android code review with architectural rigor. Use when reviewing Android code changes (Kotlin/Java files, layouts, Gradle scripts, Manifests), analyzing Pull Requests, or evaluating Android app architecture. Enforces Android official best practices including lifecycle management, MVVM/MVI patterns, Jetpack libraries, defensive programming, and separation of concerns. Triggers on requests to review Android code, analyze PRs, check for memory leaks, validate architecture, or assess Android-specific code quality.
---

# Android Code Reviewer

Production-level Android code review following Google's Guide to App Architecture and modern Android development best practices.

## Instructions

**Review Strategy**: Execute the Multi-Pass Approach systematically for all code reviews.

For **complex or high-risk changes**, structure your analysis in `<thinking>` tags before the final output:
- New features or architecture changes
- Multi-file PRs (3+ files)
- Infrastructure or security-related changes
- Changes involving lifecycle/state/navigation

```
<thinking>
1. **Triage**: Identify change type and risk level
2. **Critical Checks**: Scan for blockers (lifecycle violations, leaks, security)
3. **Architecture**: Verify layering, DI scoping, module boundaries
4. **Categorization**: Classify issues as BLOCKER / IMPORTANT / OPTIONAL
</thinking>
```

For simple changes (single-file bug fixes, UI tweaks, minor refactors), you may proceed directly to the review output while still applying all 4 passes internally.

## Review Strategy: The Multi-Pass Approach

Execute these passes sequentially before providing detailed feedback:

### Pass 1: Triage & Risk Assessment

Analyze the code to categorize the change type and set review focus:

**Feature Addition** (New ViewModel, Repository, Screen, Navigation)
- **Focus areas**: Architecture layering, state management, DI scoping, navigation graph design
- **Risk level**: HIGH - New features introduce architectural debt if not properly structured

**UI Refinement** (Layouts, Compose, Resources, Themes)
- **Focus areas**: Performance, recomposition, memory efficiency, theming consistency
- **Risk level**: MEDIUM - UI changes can cause performance regressions

**Bug Fix** (Logic changes, Null checks, Edge cases)
- **Focus areas**: Root cause analysis, regression risks, defensive coding, side effects
- **Risk level**: MEDIUM-HIGH - Bug fixes can introduce new bugs

**Infrastructure** (Gradle, Manifest, Dependencies, ProGuard)
- **Focus areas**: Build impact, permission changes, dependency health, security
- **Risk level**: HIGH - Infrastructure changes affect entire app

### Pass 2: Structural & Architectural Analysis

Examine the broader architectural patterns:

**Layering Compliance**
- Verify strict separation: `UI → ViewModel → UseCase/Repository → DataSource`
- Check for layering violations (UI calling Repository directly, ViewModel accessing DataSource)
- Ensure domain models are separated from data/UI models

**Project-Specific Architecture Rules**
- **MVVM Architecture**: Verify all new screens follow MVVM pattern
- **ViewBinding Enforcement**: Flag any usage of `findViewById` (must use ViewBinding)
- **Centralized API Access**: All API calls must go through `monsterApiService` (no direct Retrofit usage)
- **Error Handling Standardization**: API calls must use unified error wrapper (Result/sealed class/error mapping)
- **UI Layer Restriction**: UI components (Activity/Fragment/Composable) must NOT directly access Repository or API

**Dependency Injection**
- Verify correct Hilt/Koin scoping (`@Singleton`, `@ActivityRetainedScoped`, `@ViewModelScoped`)
- Check for circular dependencies
- Ensure dependencies are injected, not manually constructed
- Validate module organization and component hierarchy

**Security Audit**
- Scan for PII leaks in logs, analytics, crash reports
- Check for hardcoded secrets, API keys, tokens
- Verify exported component safety (Activities, Services, Receivers)
- Check for unencrypted storage of sensitive data
- Validate deep link handling and intent filtering

**Module & Navigation Design**
- Check feature module boundaries and coupling
- **BLOCKER**: Flag any feature-on-feature dependencies (e.g., `feature-home` → `feature-user`)
- Verify shared logic is in core modules, not duplicated across features
- Check visibility modifiers: implementations should be `internal`, interfaces `public`
- Ensure proper dependency direction (Feature → Core, never Core → Feature)
- Verify navigation graph scoping and clarity
- Ensure proper back stack management
- Check for navigation argument type safety (SafeArgs usage)

### Pass 3: Android-Specific Logic & Lifecycle Analysis

Deep dive into Android platform specifics:

**Lifecycle Management**

*Activity Lifecycle*
- Verify state restoration in `onCreate(savedInstanceState: Bundle?)`
- Check for proper `onSaveInstanceState()` usage
- Flag any heavy operations in lifecycle callbacks

*Fragment Lifecycle*
- **Critical**: Verify `viewLifecycleOwner` usage for UI-related observers
- Check binding cleanup in `onDestroyView()`
- Ensure views accessed only between `onViewCreated()` and `onDestroyView()`
- Validate Fragment Result API usage over deprecated `setTargetFragment()`

*ViewModel Lifecycle*
- **Blocker**: Flag any Activity/Fragment context references in ViewModel
- Verify `viewModelScope` usage for coroutines
- Check for proper `onCleared()` cleanup
- Validate scope selection (`navGraphViewModels`, `activityViewModels`)

**ViewBinding Pattern**
```kotlin
// CORRECT Pattern - Flag deviations
private var _binding: FragmentExampleBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
  _binding = FragmentExampleBinding.inflate(inflater, container, false)
  return binding.root
}

override fun onDestroyView() {
  _binding = null  // CRITICAL: Must clear to prevent leaks
  super.onDestroyView()
}
```

**Coroutines & Threading**
- Verify scope usage: `viewModelScope` (ViewModel), `lifecycleScope` (Activity/Fragment)
- **Never allow**: `GlobalScope`, `runBlocking` on main thread
- Check Dispatcher correctness:
  - `Dispatchers.IO` for network/database
  - `Dispatchers.Default` for CPU-intensive work
  - `Dispatchers.Main` explicitly only when needed
- Validate exception handling in `launch` blocks (try-catch or CoroutineExceptionHandler)
- Check for structured concurrency patterns
- **Flow Collection**: Verify `repeatOnLifecycle` or `flowWithLifecycle` usage (not bare `collect`)
- **Flow Cancellation**: Ensure `callbackFlow` uses `awaitClose` for cleanup
- **StateFlow Updates**: Check for race conditions (use `update` instead of `value = value + 1`)
- Flag mixing `launch` and `async` incorrectly (use `async` only for parallel work)

**State Management & Observers**
- Verify StateFlow/LiveData exposed as immutable (`StateFlow<T>`, `LiveData<T>`)
- Check for `repeatOnLifecycle` or `flowWithLifecycle` usage (not `lifecycleScope.launch` + `collect`)
- Validate one-shot events handled via Channel/SharedFlow, not sticky state
- Flag direct state mutation from UI layer

**Defensive Programming**
```kotlin
// FLAG these patterns:
val name = user!!.name                    // !! without justification
val id = intent.getStringExtra("ID")      // No null handling
bundle.getString("KEY")                   // No fallback

// REQUIRE these patterns:
val name = user?.name ?: return           // Explicit null handling
val id = intent.getStringExtra("ID") 
    ?: run {
        Log.e(TAG, "Missing ID extra")
        finish()
        return
    }
val key = arguments?.getString("KEY") ?: DEFAULT_VALUE
```

**Data Passing**
- Prefer Navigation SafeArgs over raw Bundles
- Flag hardcoded Bundle/Intent keys
- **Blocker**: Large objects (Bitmaps, complex data) passed via arguments
- Verify argument size limits (<1MB for transactions)

**RecyclerView Adapter Standards**
- **BLOCKER**: All adapters must use `ListAdapter` with `DiffUtil.ItemCallback`
- **BLOCKER**: Flag any usage of `RecyclerView.Adapter` with `notifyDataSetChanged()`
- Verify `DiffUtil.ItemCallback` properly implements `areItemsTheSame` and `areContentsTheSame`
- Check for proper ViewHolder pattern with ViewBinding

**Paging3 Standards**
- **IMPORTANT**: `PagingDataAdapter` must be `private val` (non-nullable) — NOT `private var ... = null`
  - `_binding` is nullable because it wraps a View; `PagingDataAdapter` is NOT a View, the same lifecycle treatment does not apply
  - Nulling the adapter in `onDestroyView` does NOT prevent registered listeners from firing — `ConcatAdapter` or the RecyclerView itself may still hold the original adapter instance
- **IMPORTANT**: Use `adapter.loadStateFlow.collect` inside `repeatOnLifecycle` instead of `addLoadStateListener`
  - `addLoadStateListener` fires outside lifecycle-aware scope; if the lambda accesses `binding`, it will throw NPE after `onDestroyView`
  - `loadStateFlow.collect` inside `repeatOnLifecycle(STARTED)` is automatically cancelled when the lifecycle drops below STARTED — no null guard needed
- **OPTIONAL**: `onDestroyView` may set `binding.recyclerView.adapter = null` — only meaningful when a shared `RecycledViewPool` is used across multiple RecyclerViews (e.g., inside a ViewPager). For a standalone RecyclerView, `_binding = null` already releases the entire View hierarchy (including the RecyclerView and its adapter reference), so this is redundant.

```kotlin
// ❌ WRONG — mirrors _binding pattern incorrectly
private var pagingAdapter: MyPagingAdapter? = null

pagingAdapter?.addLoadStateListener { states ->
    binding.recyclerView.scrollToPosition(0)  // NPE risk after onDestroyView
}

override fun onDestroyView() {
    pagingAdapter = null  // does NOT stop the listener from firing
    _binding = null
}

// ✅ CORRECT
private val pagingAdapter = MyPagingAdapter()

// inside setCollect() → repeatOnLifecycle(STARTED):
launch {
    pagingAdapter.loadStateFlow.collect { states ->
        // automatically cancelled on lifecycle < STARTED, no null guard needed
    }
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // releases the entire View hierarchy; rv.adapter = null only needed for shared RecycledViewPool
}
```

### Pass 4: Performance & Memory Analysis

**Memory Leak Risks**
- Long-lived references to Activity/Fragment/View
- Non-static inner classes holding implicit references
- Listeners not unregistered
- Unclosed resources (Cursor, InputStream, etc.)

**Performance Concerns**
- Main thread blocking (network, disk I/O, heavy computation)
- Large bitmap loading without sampling
- RecyclerView adapter inefficiencies
- Excessive recomposition in Compose
- N+1 database query patterns

**Resource Management**
- Bitmap recycling and memory cache
- Database cursor closing
- File stream closing
- WorkManager cleanup

## Output Format

Structure feedback using this template. Include only applicable sections to minimize token usage.

### 🔍 PR Summary & Strategy
- **Detected Type**: [Feature / UI / Fix / Infra / Multi-faceted]
- **Risk Level**: [🔴 High / 🟡 Medium / 🟢 Low]
- **Review Focus**: [State primary concerns based on change type]
- **Files Changed**: [Count and categorization]
- **Overall Assessment**:
  - Strengths: [Key positive aspects, 1 line]
  - Concerns: [Main issues, 1 line]
  - Code Quality: X/10
  - Est. Fix Time: [Time estimate]

### 🎯 Fix Priority

**Order of resolution:**
1. [List BLOCKER issues in priority order]
2. [List IMPORTANT issues in priority order]
3. **Quick Wins** (<30min): [List if any exist]

**Impact Estimate:** X files, ~X lines, [Risk: 🔴/🟡/🟢]

### 🚨 Critical Issues (BLOCKER)
Issues that cause crashes, memory leaks, security vulnerabilities, or major architectural violations.

**Format per issue:**
```
📍 File:Line - [Category]
❌ Issue: [Clear description]

❌ Current:
```kotlin
[problematic code]
```

✅ Fix:
```kotlin
[corrected code]
```

💡 Why: [Brief explanation]
🎲 Risk: [🔴/🟡/🟢] [Risk description]
🧪 Test: [What to verify]
```

### ⚠️ Major Issues (IMPORTANT)
Logic errors, unhandled edge cases, mutable state exposure, performance risks, lifecycle violations.

**Use same format as BLOCKER section.**

### 💡 Suggestions & Nits (OPTIONAL)
Only include if there are non-critical improvements worth noting.

**Format per issue:**
```
📍 File:Line
💭 Consider: [Brief suggestion, 1 line max]
```

### ✅ Positive Feedback (Conditional)
**ONLY include if there are truly exceptional implementations.**

Format: `[File:Line] - [Brief commendation, 1 line max]`

Example: "UserViewModel.kt:25-45 - Excellent immutable state pattern, use as team reference"

### 📚 Learning Resources (Conditional)
**ONLY include if recurring patterns or knowledge gaps detected.**

- Reference: `references/[file].md` for [specific topic]
- Team pattern: [Brief note if multiple PRs show same issue]

### 📋 Review Checklist
- [ ] Lifecycle methods clean and minimal
- [ ] ViewBinding properly managed
- [ ] No context leaks in ViewModel
- [ ] Proper coroutine scope usage
- [ ] Flow collection uses repeatOnLifecycle
- [ ] Navigation type-safe
- [ ] No hardcoded strings/keys
- [ ] Security concerns addressed
- [ ] Memory leak risks mitigated
- [ ] RecyclerView uses ListAdapter + DiffUtil
- [ ] Paging3: `PagingDataAdapter` is `private val`，使用 `loadStateFlow` 而非 `addLoadStateListener`
- [ ] No feature-on-feature dependencies
- [ ] Naming conventions followed (layouts, drawables, IDs, Kotlin)
- [ ] All API calls through monsterApiService
- [ ] MVVM architecture enforced

### ✅ Pre-Merge Requirements
- [ ] All BLOCKER issues resolved
- [ ] IMPORTANT issues resolved or tracked as tech debt
- [ ] Tests added for critical fixes
- [ ] [Add context-specific requirements only if needed]

## Code Examples & Patterns

For comprehensive guidance on specific topics, see the reference documentation:

**Code Examples:**
- **`references/code-examples.md`** - Detailed examples of correct patterns and common anti-patterns:
  - Fragment ViewBinding (correct vs memory leak patterns)
  - ViewModel State Management (immutable state, scope usage)
  - Lifecycle-Aware Collection (viewLifecycleOwner patterns)
  - Navigation SafeArgs (type-safe vs unsafe navigation)
  - Defensive Input Validation (deep link handling, parameter validation)
  - Coroutine Scope Usage (viewModelScope, lifecycleScope, dispatchers)
  - State Restoration (handling configuration changes)

**Review Examples:**
- **`references/review-examples.md`** - Complete code review examples demonstrating:
  - Example 1: Feature Addition (High Risk) - Full review with all sections
  - Example 2: Bug Fix (Medium Risk) - Root cause analysis focus
  - Example 3: UI Refinement (Low Risk) - Minimal review with quick wins
  - Token efficiency guidelines and when to include/skip optional sections

**Architecture & Best Practices:**
- **`references/architecture-layers.md`** - Detailed architecture layer responsibilities and separation of concerns
- **`references/lifecycle-patterns.md`** - Comprehensive lifecycle management patterns for Activity, Fragment, and ViewModel
- **`references/security-checklist.md`** - Security audit checklist and patterns for Android applications
- **`references/coroutines-flow-patterns.md`** - Coroutines and Flow best practices, exception handling, and common pitfalls
- **`references/modularization-guidelines.md`** - Module architecture, feature-on-feature dependencies, and visibility control
- **`references/naming-conventions.md`** - Comprehensive naming standards for layouts, drawables, resources, and Kotlin code