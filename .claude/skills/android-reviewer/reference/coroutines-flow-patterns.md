# Coroutines & Flow Patterns

Comprehensive guide for coroutine and Flow usage in Android, covering common pitfalls and best practices.

## Table of Contents

1. [Coroutine Scope Selection](#coroutine-scope-selection)
2. [Dispatcher Rules](#dispatcher-rules)
3. [Exception Handling](#exception-handling)
4. [Flow Collection Patterns](#flow-collection-patterns)
5. [Cold vs Hot Flows](#cold-vs-hot-flows)
6. [Cancellation & Cleanup](#cancellation--cleanup)
7. [Common Pitfalls](#common-pitfalls)

---

## Coroutine Scope Selection

### ✅ CORRECT Scope Usage

```kotlin
// ViewModel - use viewModelScope
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            // Automatically cancelled when ViewModel is cleared
            _uiState.value = UiState.Loading
            try {
                val user = repository.getUser(userId)
                _uiState.value = UiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message)
            }
        }
    }
}
```

```kotlin
// Fragment - use viewLifecycleOwner.lifecycleScope for UI-related work
class UserFragment : Fragment() {
    
    private val viewModel: UserViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORRECT - tied to view lifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }
}
```

```kotlin
// Activity - use lifecycleScope
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect flows here
            }
        }
    }
}
```

### ❌ INCORRECT Scope Usage

```kotlin
// ❌ BLOCKER - Never use GlobalScope
GlobalScope.launch {
    // This leaks - not tied to any lifecycle
    repository.saveData(data)
}

// ❌ BLOCKER - Never use runBlocking on main thread
fun loadData() {
    runBlocking {  // Blocks UI thread!
        repository.getData()
    }
}

// ❌ IMPORTANT - Wrong scope in Fragment
class UserFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ❌ Using Fragment's lifecycleScope instead of viewLifecycleOwner
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // This continues collecting even after view is destroyed!
                binding.textView.text = state.name  // Potential crash
            }
        }
    }
}
```

**Fix:**
```kotlin
// ✅ Use viewLifecycleOwner.lifecycleScope
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            binding.textView.text = state.name
        }
    }
}
```

---

## Dispatcher Rules

### Dispatcher Selection Matrix

| Work Type | Dispatcher | Example |
|-----------|-----------|---------|
| Network I/O | `Dispatchers.IO` | Retrofit calls, file download |
| Database I/O | `Dispatchers.IO` | Room queries, SharedPreferences |
| CPU-intensive | `Dispatchers.Default` | JSON parsing, sorting, filtering |
| UI updates | `Dispatchers.Main` | View manipulation (automatic in most cases) |

### ✅ CORRECT Dispatcher Usage

```kotlin
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    // ✅ CORRECT - Network call on IO dispatcher
    suspend fun fetchUser(userId: String): User = withContext(ioDispatcher) {
        apiService.getUser(userId)
    }
    
    // ✅ CORRECT - Database call on IO dispatcher
    suspend fun saveUser(user: User) = withContext(ioDispatcher) {
        userDao.insert(user)
    }
    
    // ✅ CORRECT - CPU-intensive work on Default dispatcher
    suspend fun processLargeDataset(data: List<RawData>): List<ProcessedData> {
        return withContext(Dispatchers.Default) {
            data.map { raw ->
                // Heavy computation
                processItem(raw)
            }
        }
    }
}
```

### ❌ INCORRECT Dispatcher Usage

```kotlin
// ❌ IMPORTANT - Blocking main thread
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    fun loadUser() {
        viewModelScope.launch {  // Defaults to Dispatchers.Main
            // ❌ Network call on main thread - will crash or ANR
            val user = apiService.getUser("123")
            _uiState.value = UiState.Success(user)
        }
    }
}
```

**Fix:**
```kotlin
// ✅ Repository handles dispatcher switching
fun loadUser() {
    viewModelScope.launch {
        // Repository internally uses Dispatchers.IO
        val user = repository.fetchUser("123")
        _uiState.value = UiState.Success(user)
    }
}
```

### Dispatcher Injection for Testing

```kotlin
// ✅ CORRECT - Inject dispatcher for testability
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetchUser(userId: String): User = withContext(ioDispatcher) {
        apiService.getUser(userId)
    }
}

// Hilt module
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

---

## Exception Handling

### ✅ CORRECT Exception Handling

```kotlin
// Pattern 1: Try-Catch in ViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val user = repository.getUser(userId)
                _uiState.value = UiState.Success(user)
            } catch (e: IOException) {
                _uiState.value = UiState.Error("Network error: ${e.message}")
            } catch (e: HttpException) {
                _uiState.value = UiState.Error("Server error: ${e.code()}")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Unknown error: ${e.message}")
                // Log to crash reporting
                logException(e)
            }
        }
    }
}
```

```kotlin
// Pattern 2: CoroutineExceptionHandler (for unhandled exceptions)
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Coroutine exception", throwable)
        _uiState.value = UiState.Error("Unexpected error")
    }
    
    fun loadUser(userId: String) {
        viewModelScope.launch(exceptionHandler) {
            val user = repository.getUser(userId)
            _uiState.value = UiState.Success(user)
        }
    }
}
```

```kotlin
// Pattern 3: Result wrapper pattern
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUser(userId: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = apiService.getUser(userId)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// ViewModel becomes simpler
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<Result<User>>(Result.Loading)
    val uiState: StateFlow<Result<User>> = _uiState.asStateFlow()
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = Result.Loading
            _uiState.value = repository.getUser(userId)
        }
    }
}
```

### ❌ INCORRECT Exception Handling

```kotlin
// ❌ BLOCKER - Silent failure
fun loadUser(userId: String) {
    viewModelScope.launch {
        try {
            val user = repository.getUser(userId)
            _uiState.value = UiState.Success(user)
        } catch (e: Exception) {
            // ❌ Exception swallowed - user sees nothing
        }
    }
}

// ❌ IMPORTANT - Generic exception hiding specific errors
fun loadUser(userId: String) {
    viewModelScope.launch {
        try {
            val user = repository.getUser(userId)
            _uiState.value = UiState.Success(user)
        } catch (e: Exception) {
            // ❌ All errors shown as generic message
            _uiState.value = UiState.Error("Something went wrong")
        }
    }
}

// ❌ IMPORTANT - No exception handler for launch
fun loadUser(userId: String) {
    viewModelScope.launch {
        // ❌ If this throws, app crashes
        val user = apiService.getUser(userId)
        _uiState.value = UiState.Success(user)
    }
}
```

---

## Flow Collection Patterns

### ✅ CORRECT Flow Collection

```kotlin
// Pattern 1: repeatOnLifecycle (RECOMMENDED for UI)
class UserFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ Collection starts when STARTED, stops when STOPPED
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }
}
```

```kotlin
// Pattern 2: flowWithLifecycle (alternative syntax)
class UserFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    renderUiState(state)
                }
        }
    }
}
```

```kotlin
// Pattern 3: Multiple flow collection
class UserFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ✅ Launch separate coroutines for each flow
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }
                
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }
}
```

### ❌ INCORRECT Flow Collection

```kotlin
// ❌ BLOCKER - Collection continues after view destroyed
class UserFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ❌ No lifecycle awareness
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Continues collecting even when Fragment is in background
                // Can cause crashes when accessing binding after onDestroyView
                binding.textView.text = state.name
            }
        }
    }
}
```

**Fix:**
```kotlin
// ✅ Add repeatOnLifecycle
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            binding.textView.text = state.name
        }
    }
}
```

```kotlin
// ❌ IMPORTANT - Using lifecycleScope instead of viewLifecycleOwner
class UserFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ❌ Fragment lifecycle != View lifecycle
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.textView.text = state.name  // Crash after onDestroyView
                }
            }
        }
    }
}
```

---

## Cold vs Hot Flows

### Cold Flows (Flow)

**Characteristics:**
- Created on-demand when collected
- Each collector gets independent stream
- Use for: API calls, database queries, one-shot operations

```kotlin
// ✅ Cold Flow example
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    // New API call for each collector
    fun getUser(userId: String): Flow<User> = flow {
        val user = apiService.getUser(userId)
        emit(user)
    }
}

// Usage
viewModelScope.launch {
    repository.getUser("123").collect { user ->
        // Each collection triggers new API call
    }
}
```

### Hot Flows (StateFlow, SharedFlow)

**Characteristics:**
- Always active, emit regardless of collectors
- Multiple collectors share same stream
- Use for: UI state, events, broadcast data

```kotlin
// ✅ StateFlow for UI state
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    // Hot Flow - always has a value
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // All collectors get the same current state
}
```

```kotlin
// ✅ SharedFlow for one-shot events
class UserViewModel @Inject constructor() : ViewModel() {
    
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()
    
    fun deleteUser() {
        viewModelScope.launch {
            repository.deleteUser()
            // Event sent to all active collectors
            _events.emit(Event.UserDeleted)
        }
    }
}
```

### StateFlow vs SharedFlow

| Feature | StateFlow | SharedFlow |
|---------|-----------|------------|
| Initial value | Required | Optional |
| Replay | Last value only | Configurable (0+) |
| Conflation | Always (keeps latest) | Configurable |
| Use case | UI state | Events, broadcasts |

```kotlin
// ✅ StateFlow for state
private val _uiState = MutableStateFlow(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// ✅ SharedFlow for events (no replay, no initial value)
private val _events = MutableSharedFlow<Event>(
    replay = 0,  // Don't replay to new collectors
    extraBufferCapacity = 1
)
val events: SharedFlow<Event> = _events.asSharedFlow()
```

---

## Cancellation & Cleanup

### ✅ CORRECT Cancellation Handling

```kotlin
// Pattern 1: Structured concurrency (automatic cleanup)
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    fun loadUsers() {
        viewModelScope.launch {
            // ✅ Automatically cancelled when ViewModel cleared
            val users = repository.getUsers()
            _uiState.value = UiState.Success(users)
        }
    }
    
    // ✅ onCleared called automatically
    override fun onCleared() {
        super.onCleared()
        // viewModelScope automatically cancels all jobs
    }
}
```

```kotlin
// Pattern 2: Manual job cancellation
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    private var loadJob: Job? = null
    
    fun loadUsers() {
        // Cancel previous job if still running
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            val users = repository.getUsers()
            _uiState.value = UiState.Success(users)
        }
    }
}
```

```kotlin
// Pattern 3: Cooperative cancellation
suspend fun processLargeList(items: List<Item>): List<Result> {
    return items.map { item ->
        // ✅ Check for cancellation periodically
        ensureActive()
        processItem(item)
    }
}
```

### ❌ INCORRECT Cancellation Handling

```kotlin
// ❌ BLOCKER - Uncancellable work
suspend fun uploadFile(file: File) = withContext(Dispatchers.IO) {
    // ❌ Long-running work without cancellation checks
    for (i in 0 until 1000000) {
        processChunk(i)  // Can't be cancelled
    }
}
```

**Fix:**
```kotlin
// ✅ Add cancellation checks
suspend fun uploadFile(file: File) = withContext(Dispatchers.IO) {
    for (i in 0 until 1000000) {
        ensureActive()  // Check if coroutine was cancelled
        processChunk(i)
    }
}
```

---

## Common Pitfalls

### 1. Flow Collection Without Lifecycle Awareness

```kotlin
// ❌ BLOCKER
class UserFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect {  // ❌ No repeatOnLifecycle
                updateUI(it)
            }
        }
    }
}
```

**Impact:** Collection continues when Fragment is stopped, wasting resources and potentially crashing.

**Fix:** Always use `repeatOnLifecycle(Lifecycle.State.STARTED)`.

### 2. Mixing launch and async Incorrectly

```kotlin
// ❌ IMPORTANT - Using async when launch is sufficient
fun loadData() {
    viewModelScope.launch {
        val result = async {  // ❌ Unnecessary async
            repository.getData()
        }.await()
        _uiState.value = result
    }
}
```

**Fix:**
```kotlin
// ✅ Use launch for sequential work
fun loadData() {
    viewModelScope.launch {
        val result = repository.getData()
        _uiState.value = result
    }
}

// ✅ Use async for parallel work
fun loadMultipleData() {
    viewModelScope.launch {
        val user = async { repository.getUser() }
        val posts = async { repository.getPosts() }
        
        _uiState.value = UiState.Success(
            user = user.await(),
            posts = posts.await()
        )
    }
}
```

### 3. StateFlow Update Race Conditions

```kotlin
// ❌ IMPORTANT - Race condition
private val _counter = MutableStateFlow(0)

fun increment() {
    viewModelScope.launch {
        _counter.value = _counter.value + 1  // ❌ Read-modify-write race
    }
}
```

**Fix:**
```kotlin
// ✅ Use update for thread-safe modification
fun increment() {
    _counter.update { it + 1 }
}
```

### 4. Converting Callback to Flow Without Cancellation

```kotlin
// ❌ IMPORTANT - Callback not cleaned up
fun observeLocation(): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            trySend(result.lastLocation)
        }
    }
    
    locationClient.requestLocationUpdates(callback)
    // ❌ Missing awaitClose
}
```

**Fix:**
```kotlin
// ✅ Proper cleanup with awaitClose
fun observeLocation(): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            trySend(result.lastLocation)
        }
    }
    
    locationClient.requestLocationUpdates(callback)
    
    awaitClose {
        locationClient.removeLocationUpdates(callback)
    }
}
```

### 5. Using suspend Functions Without Proper Scope

```kotlin
// ❌ BLOCKER - Suspend function called without coroutine scope
class UserRepository {
    suspend fun getUser(): User {
        // This is a suspend function
        return apiService.getUser()
    }
}

class UserViewModel : ViewModel() {
    init {
        getUser()  // ❌ Compilation error - needs coroutine scope
    }
}
```

**Fix:**
```kotlin
class UserViewModel : ViewModel() {
    init {
        viewModelScope.launch {
            val user = repository.getUser()
        }
    }
}
```

---

## Review Checklist

When reviewing coroutine and Flow code, verify:

- [ ] Appropriate scope used (`viewModelScope`, `lifecycleScope`, `viewLifecycleOwner.lifecycleScope`)
- [ ] No `GlobalScope` or `runBlocking` on main thread
- [ ] Correct Dispatcher for work type (IO, Default, Main)
- [ ] Exception handling present (try-catch or CoroutineExceptionHandler)
- [ ] Flow collection uses `repeatOnLifecycle` or `flowWithLifecycle`
- [ ] Fragment uses `viewLifecycleOwner` for view-related collections
- [ ] StateFlow exposed as immutable (`StateFlow<T>`, not `MutableStateFlow`)
- [ ] Callbacks properly cleaned up in `callbackFlow` with `awaitClose`
- [ ] Long-running operations check for cancellation (`ensureActive()`)
- [ ] No race conditions in StateFlow updates (use `update`)
- [ ] `async` used only for parallel work, not sequential work
- [ ] ViewModel doesn't hold Activity/Fragment context references
