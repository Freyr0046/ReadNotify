# Android Architecture Layer Responsibilities

Strict layering patterns following Google's Guide to App Architecture.

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           UI Layer (Presentation)        │
│  Activities, Fragments, Composables     │
│  ViewModels, UI State                   │
└─────────────────┬───────────────────────┘
                  │ observes state
                  │ dispatches events
┌─────────────────▼───────────────────────┐
│         Domain Layer (Optional)          │
│  Use Cases, Business Logic              │
│  Domain Models                          │
└─────────────────┬───────────────────────┘
                  │ coordinates
                  │ transforms
┌─────────────────▼───────────────────────┐
│           Data Layer                     │
│  Repositories, Data Sources             │
│  Network, Database, Cache               │
└─────────────────────────────────────────┘
```

## UI Layer Responsibilities

### What UI Layer SHOULD Do

```kotlin
// ✅ CORRECT - UI layer handles presentation only
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Initialize views
        setupToolbar()
        setupRecyclerView()

        // ✅ Observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }

        // ✅ Dispatch user events
        binding.saveButton.setOnClickListener {
            viewModel.onSaveClick(
                name = binding.nameEdit.text.toString(),
                email = binding.emailEdit.text.toString()
            )
        }

        // ✅ Handle navigation events
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.GoToDetail -> navigateToDetail(event.id)
                    is NavigationEvent.GoBack -> findNavController().popBackStack()
                }
            }
        }
    }

    // ✅ Pure rendering logic
    private fun renderState(state: UserUiState) {
        when (state) {
            is UserUiState.Loading -> showLoading()
            is UserUiState.Success -> showUser(state.user)
            is UserUiState.Error -> showError(state.message)
        }
    }

    private fun showUser(user: UserUiModel) {
        binding.nameText.text = user.displayName
        binding.emailText.text = user.email
        binding.avatarImage.load(user.avatarUrl)
    }
}
```

### What UI Layer SHOULD NOT Do

```kotlin
// ❌ WRONG - Business logic in UI
class UserFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.saveButton.setOnClickListener {
            val name = binding.nameEdit.text.toString()
            val email = binding.emailEdit.text.toString()
            
            // ❌ Validation logic in UI
            if (name.isBlank() || name.length < 3) {
                showError("Name must be at least 3 characters")
                return@setOnClickListener
            }
            
            // ❌ Email validation in UI
            if (!email.contains("@") || !email.contains(".")) {
                showError("Invalid email format")
                return@setOnClickListener
            }
            
            // ❌ Direct repository access
            lifecycleScope.launch {
                try {
                    userRepository.saveUser(User(name, email))
                    showSuccess()
                } catch (e: Exception) {
                    showError(e.message ?: "Error")
                }
            }
        }
    }
}

// ❌ WRONG - Data transformation in UI
class UserFragment : Fragment() {
    private fun displayUsers(users: List<UserEntity>) {
        // ❌ Converting database entities to UI models in Fragment
        val uiModels = users.map { entity ->
            UserUiModel(
                id = entity.id,
                displayName = "${entity.firstName} ${entity.lastName}",
                email = entity.email,
                avatarUrl = entity.profileImageUrl ?: DEFAULT_AVATAR,
                memberSince = formatDate(entity.createdAt)
            )
        }
        adapter.submitList(uiModels)
    }
}
```

## ViewModel Layer Responsibilities

### What ViewModel SHOULD Do

```kotlin
// ✅ CORRECT - ViewModel manages UI state and business logic coordination
class UserViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val saveUserUseCase: SaveUserUseCase,
    private val validator: UserValidator
) : ViewModel() {
    
    // ✅ Expose immutable state
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
    
    // ✅ One-shot events
    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()
    
    // ✅ Coordinate business logic
    fun onSaveClick(name: String, email: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Saving
            
            // ✅ Validate using domain layer
            val validationResult = validator.validate(name, email)
            if (validationResult is ValidationResult.Invalid) {
                _uiState.value = UserUiState.Error(validationResult.message)
                return@launch
            }
            
            // ✅ Execute use case
            when (val result = saveUserUseCase(name, email)) {
                is Result.Success -> {
                    _uiState.value = UserUiState.Success(result.data.toUiModel())
                    _navigationEvents.send(NavigationEvent.GoBack)
                }
                is Result.Error -> {
                    _uiState.value = UserUiState.Error(result.message)
                }
            }
        }
    }
    
    // ✅ Transform domain models to UI models
    private fun User.toUiModel() = UserUiModel(
        id = id,
        displayName = "$firstName $lastName",
        email = email,
        avatarUrl = profileImageUrl ?: DEFAULT_AVATAR,
        memberSince = formatDate(createdAt)
    )
}

// ✅ UI State sealed class
sealed class UserUiState {
    object Loading : UserUiState()
    object Saving : UserUiState()
    data class Success(val user: UserUiModel) : UserUiState()
    data class Error(val message: String) : UserUiState()
}
```

### What ViewModel SHOULD NOT Do

```kotlin
// ❌ WRONG - ViewModel with context and direct data access
class UserViewModel(
    private val context: Context, // ❌ Context leak
    private val database: AppDatabase // ❌ Direct database access
) : ViewModel() {
    
    fun saveUser(name: String, email: String) {
        // ❌ Main thread blocking
        val user = database.userDao().insert(User(name, email))
        
        // ❌ Direct Android framework access
        Toast.makeText(context, "User saved", Toast.LENGTH_SHORT).show()
        
        // ❌ Direct navigation
        val intent = Intent(context, DetailActivity::class.java)
        context.startActivity(intent)
    }
    
    // ❌ UI logic in ViewModel
    fun getButtonText(): String {
        return context.getString(R.string.save_button)
    }
}
```

## Domain Layer (Use Case) Responsibilities

### What Domain Layer SHOULD Do

```kotlin
// ✅ CORRECT - Pure business logic
class SaveUserUseCase(
    private val userRepository: UserRepository,
    private val validator: UserValidator,
    private val analytics: AnalyticsService
) {
    suspend operator fun invoke(name: String, email: String): Result<User> {
        // ✅ Business rules validation
        val validationResult = validator.validate(name, email)
        if (validationResult is ValidationResult.Invalid) {
            return Result.Error(validationResult.message)
        }
        
        return try {
            // ✅ Coordinate repository operations
            val user = userRepository.saveUser(
                User(
                    firstName = name.split(" ").first(),
                    lastName = name.split(" ").drop(1).joinToString(" "),
                    email = email.lowercase()
                )
            )
            
            // ✅ Side effects (analytics, logging)
            analytics.logEvent("user_saved", mapOf("user_id" to user.id))
            
            Result.Success(user)
        } catch (e: NetworkException) {
            Result.Error("Network error: Check your connection")
        } catch (e: Exception) {
            Result.Error("Failed to save user: ${e.message}")
        }
    }
}

// ✅ Domain model
data class User(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String,
    val email: String,
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ✅ Business validation
class UserValidator {
    fun validate(name: String, email: String): ValidationResult {
        if (name.isBlank() || name.length < 3) {
            return ValidationResult.Invalid("Name must be at least 3 characters")
        }
        
        if (!email.matches(EMAIL_REGEX)) {
            return ValidationResult.Invalid("Invalid email format")
        }
        
        return ValidationResult.Valid
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}
```

### What Domain Layer SHOULD NOT Do

```kotlin
// ❌ WRONG - Platform dependencies in domain layer
class SaveUserUseCase(
    private val context: Context, // ❌ Android dependency
    private val database: AppDatabase // ❌ Direct DB access
) {
    suspend operator fun invoke(name: String, email: String): Result<User> {
        // ❌ Direct database access
        val user = database.userDao().insert(UserEntity(name, email))
        
        // ❌ UI concerns
        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        
        // ❌ Direct network call
        val response = api.uploadUser(user)
        
        return Result.Success(user)
    }
}
```

## Data Layer (Repository) Responsibilities

### What Repository SHOULD Do

```kotlin
// ✅ CORRECT - Repository abstracts data sources
class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource,
    private val cacheDataSource: UserCacheDataSource
) : UserRepository {
    
    // ✅ Coordinate multiple data sources
    override suspend fun getUser(userId: String): User {
        // ✅ Try cache first
        cacheDataSource.getUser(userId)?.let { return it }
        
        // ✅ Try local database
        localDataSource.getUser(userId)?.let { cachedUser ->
            cacheDataSource.saveUser(cachedUser)
            return cachedUser
        }
        
        // ✅ Fetch from network
        return try {
            val user = remoteDataSource.getUser(userId)
            
            // ✅ Update local sources
            localDataSource.saveUser(user)
            cacheDataSource.saveUser(user)
            
            user
        } catch (e: Exception) {
            throw NetworkException("Failed to fetch user", e)
        }
    }
    
    // ✅ Transform data layer models to domain models
    override suspend fun saveUser(user: User): User {
        val entity = user.toEntity()
        
        // ✅ Save to all sources
        localDataSource.saveUser(entity.toDomainModel())
        val savedUser = remoteDataSource.saveUser(entity.toDomainModel())
        cacheDataSource.saveUser(savedUser)
        
        return savedUser
    }
}

// ✅ Data source interface
interface UserRemoteDataSource {
    suspend fun getUser(userId: String): User
    suspend fun saveUser(user: User): User
}

// ✅ Implementation with API
class UserRemoteDataSourceImpl(
    private val api: UserApi
) : UserRemoteDataSource {
    
    override suspend fun getUser(userId: String): User {
        return withContext(Dispatchers.IO) {
            val response = api.getUser(userId)
            response.toDomainModel()
        }
    }
    
    override suspend fun saveUser(user: User): User {
        return withContext(Dispatchers.IO) {
            val dto = user.toDto()
            val response = api.saveUser(dto)
            response.toDomainModel()
        }
    }
}
```

### What Repository SHOULD NOT Do

```kotlin
// ❌ WRONG - Business logic in repository
class UserRepositoryImpl(
    private val api: UserApi,
    private val database: AppDatabase
) : UserRepository {
    
    override suspend fun saveUser(user: User): User {
        // ❌ Business validation in repository
        if (user.name.length < 3) {
            throw IllegalArgumentException("Name too short")
        }
        
        // ❌ Business logic
        val formattedName = user.name.split(" ")
            .joinToString(" ") { it.capitalize() }
        
        // ❌ UI concern
        Log.d("UserRepo", "Saving user: $formattedName")
        
        return api.saveUser(user.copy(name = formattedName))
    }
}
```

## Layer Communication Patterns

### Dependency Direction

```kotlin
// ✅ CORRECT - Dependencies point inward
┌──────────────────────┐
│    UI Layer          │
│  (Fragment/Activity) │
└──────────┬───────────┘
│ depends on
▼
┌──────────────────────┐
│   ViewModel          │
└──────────┬───────────┘
│ depends on
▼
┌──────────────────────┐
│   Use Case/Domain    │
└──────────┬───────────┘
│ depends on
▼
┌──────────────────────┐
│   Repository         │
└──────────────────────┘

// Fragment knows about ViewModel ✅
class UserFragment : Fragment() {
    private val viewModel: UserViewModel by viewModels()
}

// ViewModel knows about Use Case ✅
class UserViewModel(
    private val getUserUseCase: GetUserUseCase
) : ViewModel()

// Use Case knows about Repository ✅
class GetUserUseCase(
    private val userRepository: UserRepository
)

// ❌ WRONG - Upward dependencies
class UserRepository(
    private val viewModel: UserViewModel // ❌ Repository should not know about ViewModel
)

class GetUserUseCase(
    private val fragment: UserFragment // ❌ Use case should not know about UI
)
```

### Interface Segregation

```kotlin
// ✅ CORRECT - Small, focused interfaces
interface UserRepository {
    suspend fun getUser(userId: String): User
    suspend fun saveUser(user: User): User
    suspend fun deleteUser(userId: String)
}

interface UserAnalytics {
    fun logUserViewed(userId: String)
    fun logUserSaved(userId: String)
}

class GetUserUseCase(
    private val userRepository: UserRepository,
    private val analytics: UserAnalytics
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return try {
            val user = userRepository.getUser(userId)
            analytics.logUserViewed(userId)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}

// ❌ WRONG - God interface
interface UserService {
    suspend fun getUser(userId: String): User
    suspend fun saveUser(user: User): User
    suspend fun deleteUser(userId: String)
    suspend fun sendEmail(userId: String, message: String)
    suspend fun uploadAvatar(userId: String, bitmap: Bitmap)
    suspend fun generateReport(userId: String): Report
    fun logAnalytics(event: String)
    fun showNotification(message: String)
    // Too many responsibilities!
}
```

### Model Transformation Layers

```kotlin
// ✅ CORRECT - Separate models per layer

// Data layer (Network/DB)
data class UserDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("email_address")
    val emailAddress: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val createdAt: Long
)

// Domain layer
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val createdAt: Long
)

// UI layer
data class UserUiModel(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String,
    val memberSince: String
)

// Transformation extensions
fun UserDto.toDomainModel() = User(
    id = userId,
    firstName = fullName.split(" ").first(),
    lastName = fullName.split(" ").drop(1).joinToString(" "),
    email = emailAddress,
    createdAt = System.currentTimeMillis()
)

fun User.toUiModel() = UserUiModel(
    id = id,
    displayName = "$firstName $lastName",
    email = email,
    avatarUrl = DEFAULT_AVATAR,
    memberSince = formatDate(createdAt)
)

// ❌ WRONG - Single model across all layers
data class User(
    @SerializedName("user_id")
    val id: String,
    var displayName: String? = null, // UI concern
    @Embedded val address: Address? = null, // DB concern
    // Mixed responsibilities
)
```