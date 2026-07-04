# Modularization Guidelines

Comprehensive guide for Android app modularization, module architecture, and dependency management.

## Table of Contents

1. [Module Types](#module-types)
2. [Dependency Rules](#dependency-rules)
3. [Feature-on-Feature Dependencies](#feature-on-feature-dependencies)
4. [Visibility Control](#visibility-control)
5. [Module Structure](#module-structure)
6. [Common Patterns](#common-patterns)
7. [Anti-Patterns](#anti-patterns)

---

## Module Types

### Standard Module Hierarchy

```
app (Application module)
├── feature modules (UI + ViewModel)
│   ├── feature-home
│   ├── feature-profile
│   └── feature-settings
├── core modules (Shared functionality)
│   ├── core-ui (Shared UI components)
│   ├── core-data (Repositories, Data sources)
│   ├── core-domain (Use cases, Business logic)
│   ├── core-network (API clients, DTOs)
│   ├── core-database (Room, DAOs)
│   └── core-common (Utils, Extensions)
└── library modules (Reusable libraries)
    ├── lib-analytics
    ├── lib-logger
    └── lib-imageloader
```

### Module Type Definitions

#### 1. App Module
**Purpose:** Application entry point, dependency aggregation, navigation orchestration.

**Can depend on:**
- All feature modules
- All core modules
- Library modules

**Should contain:**
- `Application` class
- Main `Activity`
- App-level DI configuration (Hilt modules)
- Navigation graph (if single-activity)

**Should NOT contain:**
- Business logic
- Feature-specific code
- Data access logic

#### 2. Feature Modules
**Purpose:** Self-contained features with UI and presentation logic.

**Can depend on:**
- Core modules only (NOT other feature modules)
- Library modules

**Should contain:**
- Fragments/Activities/Composables
- ViewModels
- Feature-specific UI components
- Navigation graphs (feature-specific)

**Should NOT contain:**
- Direct network/database access
- Dependencies on other features
- Shared utilities (move to core)

#### 3. Core Modules
**Purpose:** Shared business logic, data access, and utilities.

**Can depend on:**
- Other core modules (with restrictions)
- Library modules

**Should contain:**
- Repositories
- Use cases
- Data sources (API, Database)
- Domain models
- Shared UI components
- Utilities and extensions

**Should NOT contain:**
- Feature-specific logic
- UI screens (Activities/Fragments)

#### 4. Library Modules
**Purpose:** Reusable, framework-agnostic utilities.

**Can depend on:**
- Other library modules
- Android framework (minimal)

**Should contain:**
- Analytics wrappers
- Logging utilities
- Image loading
- Pure utilities

**Should NOT contain:**
- App-specific logic
- Dependencies on app/feature/core modules

---

## Dependency Rules

### ✅ ALLOWED Dependencies

```
┌─────────────────────────────────────────────┐
│                 app module                   │
│  Can depend on: features, core, libraries   │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌───────────────┐     ┌───────────────┐
│ feature-home  │     │ feature-user  │
│ Can depend on:│     │ Can depend on:│
│ - core-*      │     │ - core-*      │
│ - lib-*       │     │ - lib-*       │
└───────┬───────┘     └───────┬───────┘
        │                     │
        └──────────┬──────────┘
                   ▼
        ┌─────────────────────┐
        │   core-data         │
        │   Can depend on:    │
        │   - core-network    │
        │   - core-database   │
        │   - core-common     │
        └──────────┬──────────┘
                   ▼
        ┌─────────────────────┐
        │   lib-analytics     │
        │   No dependencies   │
        └─────────────────────┘
```

### ❌ FORBIDDEN Dependencies

```
❌ feature-home → feature-user  (Feature-on-Feature)
❌ core-data → feature-home     (Core → Feature)
❌ lib-logger → core-network    (Library → Core)
❌ core-ui → core-data          (UI → Data - wrong direction)
```

### Dependency Direction Rules

**Rule 1: Features are independent**
- ✅ `feature-home` → `core-data`
- ❌ `feature-home` → `feature-user`

**Rule 2: Core modules form layers**
- ✅ `core-data` → `core-network`
- ✅ `core-domain` → `core-data`
- ❌ `core-network` → `core-data` (circular)

**Rule 3: Libraries are self-contained**
- ✅ `lib-analytics` → (no dependencies)
- ❌ `lib-logger` → `core-common` (library depending on core)

---

## Feature-on-Feature Dependencies

### ❌ BLOCKER: Direct Feature Dependencies

```kotlin
// ❌ BLOCKER - feature-home depending on feature-user
// feature-home/build.gradle.kts
dependencies {
    implementation(project(":feature-user"))  // ❌ FORBIDDEN
}
```

**Why this is blocked:**
1. Creates tight coupling between features
2. Makes features non-reusable
3. Complicates testing and CI/CD
4. Violates single responsibility principle

### ✅ CORRECT: Shared Logic via Core

```kotlin
// ✅ Move shared logic to core module

// core-domain/src/main/java/User.kt
data class User(
    val id: String,
    val name: String,
    val email: String
)

// core-domain/src/main/java/GetUserUseCase.kt
class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return userRepository.getUser(userId)
    }
}
```

```kotlin
// feature-home uses core-domain
// feature-home/build.gradle.kts
dependencies {
    implementation(project(":core-domain"))  // ✅ ALLOWED
}

class HomeViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase  // From core-domain
) : ViewModel() {
    // Use shared logic
}
```

```kotlin
// feature-profile also uses core-domain
// feature-profile/build.gradle.kts
dependencies {
    implementation(project(":core-domain"))  // ✅ ALLOWED
}

class ProfileViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase  // Same use case
) : ViewModel() {
    // Reuse shared logic
}
```

### ✅ CORRECT: Feature Communication via Navigation

```kotlin
// Features communicate through navigation, not direct dependencies

// app/src/main/res/navigation/nav_graph.xml
<navigation>
    <fragment
        android:id="@+id/homeFragment"
        android:name="com.example.home.HomeFragment">
        <action
            android:id="@+id/action_home_to_profile"
            app:destination="@id/profileFragment">
            <argument
                android:name="userId"
                app:argType="string" />
        </action>
    </fragment>
    
    <fragment
        android:id="@+id/profileFragment"
        android:name="com.example.profile.ProfileFragment">
        <argument
            android:name="userId"
            app:argType="string" />
    </fragment>
</navigation>
```

```kotlin
// feature-home navigates to feature-profile via Safe Args
class HomeFragment : Fragment() {
    
    private fun navigateToProfile(userId: String) {
        val action = HomeFragmentDirections.actionHomeToProfile(userId)
        findNavController().navigate(action)
    }
}
```

### ✅ CORRECT: Shared Events via Event Bus (When Necessary)

```kotlin
// core-common/src/main/java/events/AppEvent.kt
sealed class AppEvent {
    data class UserLoggedIn(val userId: String) : AppEvent()
    object UserLoggedOut : AppEvent()
}

// core-common/src/main/java/events/EventBus.kt
@Singleton
class EventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AppEvent>()
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }
}
```

```kotlin
// feature-home emits event
class HomeViewModel @Inject constructor(
    private val eventBus: EventBus
) : ViewModel() {
    
    fun onLoginSuccess(userId: String) {
        viewModelScope.launch {
            eventBus.emit(AppEvent.UserLoggedIn(userId))
        }
    }
}
```

```kotlin
// feature-profile reacts to event
class ProfileViewModel @Inject constructor(
    private val eventBus: EventBus
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is AppEvent.UserLoggedIn -> loadUserProfile(event.userId)
                    is AppEvent.UserLoggedOut -> clearProfile()
                }
            }
        }
    }
}
```

---

## Visibility Control

### Kotlin Visibility Modifiers

Use visibility modifiers to enforce module boundaries and hide implementation details.

### ✅ CORRECT: Expose Only Public API

```kotlin
// core-data/src/main/java/repository/UserRepository.kt

// ✅ Public interface - exposed to feature modules
interface UserRepository {
    suspend fun getUser(userId: String): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
}

// ✅ Internal implementation - hidden from feature modules
@Singleton
internal class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) : UserRepository {
    
    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val userDto = apiService.getUser(userId)
            val user = userDto.toDomain()
            userDao.insert(user.toEntity())
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        // Implementation
    }
    
    // ✅ Private helper - completely hidden
    private fun UserDto.toDomain(): User {
        return User(id, name, email)
    }
}
```

```kotlin
// core-data/src/main/java/di/DataModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    
    @Binds
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}
```

### ✅ CORRECT: Internal Utilities

```kotlin
// core-common/src/main/java/utils/StringUtils.kt

// ✅ Internal - only accessible within core-common module
internal fun String.toTitleCase(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

// ✅ Public - exposed to all modules
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}
```

### ❌ INCORRECT: Everything Public

```kotlin
// ❌ IMPORTANT - Exposing implementation details
class UserRepositoryImpl @Inject constructor(
    val apiService: ApiService,  // ❌ Public - should be private
    val userDao: UserDao         // ❌ Public - should be private
) : UserRepository {
    
    // ❌ Public helper - should be private
    fun mapDtoToDomain(dto: UserDto): User {
        return User(dto.id, dto.name, dto.email)
    }
}
```

**Fix:**
```kotlin
// ✅ Hide implementation details
internal class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) : UserRepository {
    
    private fun mapDtoToDomain(dto: UserDto): User {
        return User(dto.id, dto.name, dto.email)
    }
}
```

### Visibility Guidelines

| Visibility | Scope | Use For |
|------------|-------|---------|
| `public` | All modules | Public API, interfaces, domain models |
| `internal` | Same module | Implementations, DI modules, utilities |
| `private` | Same file | Helpers, internal state |
| `protected` | Subclasses | Override hooks (rare in Android) |

---

## Module Structure

### Feature Module Structure

```
feature-home/
├── src/
│   ├── main/
│   │   ├── java/com/example/home/
│   │   │   ├── ui/
│   │   │   │   ├── HomeFragment.kt
│   │   │   │   ├── HomeAdapter.kt
│   │   │   │   └── components/
│   │   │   │       └── HomeItemView.kt
│   │   │   ├── viewmodel/
│   │   │   │   └── HomeViewModel.kt
│   │   │   ├── navigation/
│   │   │   │   └── HomeNavigation.kt
│   │   │   └── di/
│   │   │       └── HomeModule.kt
│   │   └── res/
│   │       ├── layout/
│   │       │   ├── fragment_home.xml
│   │       │   └── item_home.xml
│   │       ├── navigation/
│   │       │   └── nav_home.xml
│   │       └── values/
│   │           └── strings.xml
│   └── test/
│       └── java/com/example/home/
│           └── viewmodel/
│               └── HomeViewModelTest.kt
└── build.gradle.kts
```

### Core Module Structure

```
core-data/
├── src/
│   ├── main/
│   │   ├── java/com/example/core/data/
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.kt          (public interface)
│   │   │   │   └── UserRepositoryImpl.kt      (internal implementation)
│   │   │   ├── source/
│   │   │   │   ├── local/
│   │   │   │   │   └── UserLocalDataSource.kt
│   │   │   │   └── remote/
│   │   │   │       └── UserRemoteDataSource.kt
│   │   │   ├── mapper/
│   │   │   │   └── UserMapper.kt              (internal)
│   │   │   └── di/
│   │   │       └── DataModule.kt              (internal)
│   └── test/
│       └── java/com/example/core/data/
│           └── repository/
│               └── UserRepositoryTest.kt
└── build.gradle.kts
```

---

## Common Patterns

### Pattern 1: API Surface Pattern

**Expose minimal public API, hide everything else.**

```kotlin
// core-network/src/main/java/api/ApiService.kt

// ✅ Public interface
interface ApiService {
    suspend fun getUser(userId: String): UserDto
}

// ✅ Internal implementation
@Singleton
internal class ApiServiceImpl @Inject constructor(
    private val retrofit: Retrofit
) : ApiService {
    
    private val api: UserApi = retrofit.create(UserApi::class.java)
    
    override suspend fun getUser(userId: String): UserDto {
        return api.getUser(userId)
    }
}

// ✅ Private Retrofit interface
private interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): UserDto
}
```

### Pattern 2: Aggregate Module Pattern

**Create facade modules to simplify dependencies.**

```kotlin
// core/build.gradle.kts
// Aggregate module that exposes all core functionality

dependencies {
    api(project(":core-data"))
    api(project(":core-domain"))
    api(project(":core-network"))
    api(project(":core-database"))
    api(project(":core-common"))
}
```

```kotlin
// feature-home/build.gradle.kts
// Feature only needs to depend on core aggregate

dependencies {
    implementation(project(":core"))  // Gets all core modules
    implementation(project(":core-ui"))
}
```

### Pattern 3: Shared Resource Module

```kotlin
// core-ui/src/main/res/values/colors.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Shared colors across all features -->
    <color name="primary">#6200EE</color>
    <color name="primary_variant">#3700B3</color>
    <color name="secondary">#03DAC6</color>
</resources>
```

```kotlin
// feature-home can use shared colors
// feature-home/src/main/res/layout/fragment_home.xml
<TextView
    android:textColor="@color/primary" />  <!-- From core-ui -->
```

---

## Anti-Patterns

### ❌ Anti-Pattern 1: God Module

```kotlin
// ❌ core-common contains everything
core-common/
├── network/
├── database/
├── ui/
├── utils/
├── repositories/
└── viewmodels/
```

**Problem:** Single module becomes bloated, slow to build, hard to maintain.

**Fix:** Split into focused modules:
- `core-network`
- `core-database`
- `core-ui`
- `core-data`

### ❌ Anti-Pattern 2: Circular Dependencies

```kotlin
// ❌ BLOCKER - Circular dependency
core-data → core-domain → core-data
```

**Fix:** Introduce proper layering:
```kotlin
// ✅ One-way dependency
core-domain → core-data → core-network
```

### ❌ Anti-Pattern 3: Utility Dumping Ground

```kotlin
// ❌ core-common/Utils.kt - 5000 lines
object Utils {
    fun formatDate() { }
    fun makeApiCall() { }
    fun parseJson() { }
    fun showToast() { }
    fun calculateHash() { }
    // ... 100 more functions
}
```

**Fix:** Split by responsibility:
```kotlin
// ✅ core-common/date/DateUtils.kt
object DateUtils {
    fun formatDate() { }
}

// ✅ core-common/string/StringUtils.kt
object StringUtils {
    fun calculateHash() { }
}

// ✅ core-ui/toast/ToastUtils.kt
object ToastUtils {
    fun showToast() { }
}
```

### ❌ Anti-Pattern 4: Shared ViewModel

```kotlin
// ❌ IMPORTANT - Shared ViewModel in core module
// core-shared/SharedViewModel.kt
class SharedViewModel : ViewModel() {
    // Used by multiple features
}
```

**Problem:** ViewModels are feature-specific, shouldn't be in core.

**Fix:** Use shared state via Repository/UseCase:
```kotlin
// ✅ core-domain/GetSharedDataUseCase.kt
class GetSharedDataUseCase @Inject constructor(
    private val repository: SharedDataRepository
) {
    operator fun invoke(): Flow<SharedData> = repository.getData()
}

// feature-home/HomeViewModel.kt
class HomeViewModel @Inject constructor(
    private val getSharedData: GetSharedDataUseCase
) : ViewModel() {
    val sharedData = getSharedData().stateIn(...)
}

// feature-profile/ProfileViewModel.kt
class ProfileViewModel @Inject constructor(
    private val getSharedData: GetSharedDataUseCase
) : ViewModel() {
    val sharedData = getSharedData().stateIn(...)
}
```

---

## Review Checklist

When reviewing modularization:

- [ ] No feature-on-feature dependencies
- [ ] Core modules don't depend on feature modules
- [ ] Library modules are self-contained
- [ ] Implementations are `internal`, interfaces are `public`
- [ ] No circular dependencies
- [ ] Module structure follows conventions
- [ ] Shared logic in core modules, not duplicated
- [ ] Navigation used for feature communication
- [ ] API surface is minimal and well-defined
- [ ] Build files don't expose unnecessary dependencies (`implementation` vs `api`)
- [ ] ViewModels are in feature modules, not core
- [ ] No "god modules" with too many responsibilities

---

## Build Configuration Best Practices

### Use `implementation` vs `api`

```kotlin
// feature-home/build.gradle.kts

dependencies {
    // ✅ implementation - not exposed to consumers
    implementation(project(":core-data"))
    implementation(libs.kotlinx.coroutines.core)
    
    // ❌ api - exposes dependency to consumers (use sparingly)
    // api(project(":core-data"))  // Don't expose core-data
}
```

```kotlin
// core/build.gradle.kts (aggregate module)

dependencies {
    // ✅ api - expose all core modules to consumers
    api(project(":core-data"))
    api(project(":core-domain"))
    api(project(":core-network"))
}
```

### Dependency Verification Script

```kotlin
// buildSrc/src/main/kotlin/DependencyVerification.kt

object DependencyVerification {
    
    fun verifyNoCyclicDependencies(project: Project) {
        // Check for circular dependencies
    }
    
    fun verifyNoFeatureOnFeature(project: Project) {
        // Ensure features don't depend on each other
    }
    
    fun verifyLayering(project: Project) {
        // Ensure proper dependency direction
    }
}
```
