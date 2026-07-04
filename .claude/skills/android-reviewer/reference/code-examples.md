# Android Code Examples & Patterns

Complete code examples demonstrating correct patterns and common anti-patterns in Android development.

## Table of Contents

1. [Fragment ViewBinding](#fragment-viewbinding)
2. [ViewModel State Management](#viewmodel-state-management)
3. [Lifecycle-Aware Collection](#lifecycle-aware-collection)
4. [Navigation SafeArgs](#navigation-safeargs)
5. [Defensive Input Validation](#defensive-input-validation)
6. [Coroutine Scope Usage](#coroutine-scope-usage)
7. [State Restoration](#state-restoration)
8. [RecyclerView ListAdapter](#recyclerview-listadapter)

---

## Fragment ViewBinding

### ✅ CORRECT Pattern

```kotlin
private var _binding: FragmentExampleBinding? = null
private val binding get() = _binding!!

override fun onCreateView(
    inflater: LayoutInflater, 
    container: ViewGroup?, 
    savedInstanceState: Bundle?
): View {
    _binding = FragmentExampleBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // Safe to use binding here
    binding.textView.text = "Hello World"
    binding.button.setOnClickListener {
        // Handle click
    }
}

override fun onDestroyView() {
    // CRITICAL: Must clear binding to prevent memory leaks
    _binding = null
    super.onDestroyView()
}
```

**Why this works:**
- Nullable `_binding` prevents holding destroyed view reference
- Non-null `binding` getter provides convenient access
- Clearing in `onDestroyView()` prevents memory leaks

### ❌ WRONG Pattern - Memory Leak

```kotlin
// WRONG: Never cleared, holds reference to destroyed view
private lateinit var binding: FragmentExampleBinding

override fun onCreateView(...): View {
    binding = FragmentExampleBinding.inflate(inflater, container, false)
    return binding.root
}

// No onDestroyView cleanup = Memory Leak!
```

**Why this fails:**
- `binding` holds reference to destroyed view after `onDestroyView()`
- Fragment instance may outlive view (e.g., on back stack)
- Causes memory leak and potential crashes

---

## ViewModel State Management

### ✅ CORRECT Pattern

```kotlin
class UserViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    // Public immutable state
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // One-shot events via Channel
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    fun loadUser(id: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val user = repository.getUser(id)
                _uiState.value = UiState.Success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
                _events.send(Event.ShowError(e.message))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
    }
}

// Sealed class for state representation
sealed class UiState {
    object Loading : UiState()
    data class Success(val user: User) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class Event {
    data class ShowError(val message: String?) : Event()
    object NavigateBack : Event()
}
```

**Why this works:**
- Immutable public state prevents external mutation
- `viewModelScope` auto-cancels on `onCleared()`
- Sealed classes provide type-safe state representation
- Channel for one-shot events (not sticky)

### ❌ WRONG Pattern - Multiple Issues

```kotlin
// WRONG: Context leak
class UserViewModel(
    private val context: Context  // NEVER pass Context to ViewModel!
) : ViewModel() {
    
    // WRONG: Exposed mutable state
    val uiState = MutableStateFlow<UiState>(UiState.Loading)
    
    // WRONG: GlobalScope never cancels
    fun loadUser(id: String) {
        GlobalScope.launch {
            val user = repository.getUser(id)
            uiState.value = UiState.Success(user)
            
            // WRONG: Accessing context after ViewModel may be cleared
            Toast.makeText(context, "User loaded", Toast.LENGTH_SHORT).show()
        }
    }
}
```

**Why this fails:**
- Context reference causes memory leak
- External code can mutate `uiState` directly
- `GlobalScope` never cancels, continues after ViewModel cleared
- No error handling

---

## Lifecycle-Aware Collection

### ✅ CORRECT Pattern - Fragment

```kotlin
class UserFragment : Fragment() {
    private val viewModel: UserViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Use viewLifecycleOwner for UI-related observers
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
        
        // Collect one-shot events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }
    
    private fun updateUI(state: UiState) {
        when (state) {
            is UiState.Loading -> showLoading()
            is UiState.Success -> showUser(state.user)
            is UiState.Error -> showError(state.message)
        }
    }
}
```

**Why this works:**
- `viewLifecycleOwner` respects Fragment's view lifecycle
- `repeatOnLifecycle(STARTED)` stops collection when backgrounded
- Prevents UI updates when not visible
- Avoids crashes from accessing destroyed views

### ❌ WRONG Pattern - Lifecycle Issues

```kotlin
class UserFragment : Fragment() {
    private val viewModel: UserViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // WRONG: Uses Fragment lifecycle instead of view lifecycle
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->  // Keeps collecting even after view destroyed
                updateUI(state)  // Crash: accessing destroyed view
            }
        }
    }
}
```

**Why this fails:**
- Fragment lifecycle outlives view lifecycle
- Collection continues after `onDestroyView()`
- Attempting to update destroyed views causes crashes
- Wastes resources collecting while backgrounded

---

## Navigation SafeArgs

### ✅ CORRECT Pattern

```kotlin
// In DetailFragment
class DetailFragment : Fragment() {
    private val args: DetailFragmentArgs by navArgs()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Type-safe argument access
        val userId = args.userId
        val userName = args.userName
        
        loadUserDetails(userId)
    }
    
    private fun navigateToEdit() {
        // Type-safe navigation with compile-time checking
        val action = DetailFragmentDirections.actionDetailToEdit(
            userId = args.userId,
            userName = args.userName
        )
        findNavController().navigate(action)
    }
}

// In navigation graph (nav_graph.xml)
// <argument
//     android:name="userId"
//     app:argType="string" />
// <argument
//     android:name="userName"
//     app:argType="string"
//     app:nullable="true"
//     android:defaultValue="@null" />
```

**Why this works:**
- Compile-time type safety
- Auto-generated argument classes
- IDE autocomplete support
- Refactoring-safe (rename detection)

### ❌ WRONG Pattern - Type-Unsafe

```kotlin
class DetailFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // WRONG: Hardcoded keys, no type safety
        val userId = arguments?.getString("USER_ID") ?: ""  // What if key typo?
        val userName = arguments?.getString("USER_NAME")     // Nullable? Non-nullable?
        
        loadUserDetails(userId)
    }
    
    private fun navigateToEdit() {
        // WRONG: Error-prone Bundle construction
        val bundle = bundleOf(
            "USER_ID" to userId,     // Easy to make typo
            "USER_NAME" to userName  // No compile-time checking
        )
        findNavController().navigate(
            R.id.action_detail_to_edit,
            bundle
        )
    }
}
```

**Why this fails:**
- Typos in keys only discovered at runtime
- No compile-time type checking
- Refactoring doesn't update argument keys
- Unclear which arguments are required vs optional

---

## Defensive Input Validation

### ✅ CORRECT Pattern - Deep Link Handling

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        // Step 1: Validate intent and URI exist
        val uri = intent?.data ?: run {
            Log.w(TAG, "No deep link URI provided")
            return
        }
        
        // Step 2: Extract and validate parameter existence
        val userId = uri.getQueryParameter("user_id")?.takeIf { it.isNotBlank() } ?: run {
            Log.e(TAG, "Missing or empty user_id parameter")
            showError("Invalid link: missing user ID")
            return
        }
        
        // Step 3: Validate parameter format
        if (!userId.matches(Regex("^[a-zA-Z0-9_-]{1,64}$"))) {
            Log.e(TAG, "Invalid user_id format: $userId")
            showError("Invalid user ID format")
            return
        }
        
        // Step 4: Optional parameter with default
        val tab = uri.getQueryParameter("tab")?.takeIf { 
            it in listOf("profile", "posts", "followers") 
        } ?: "profile"
        
        // Step 5: Safe navigation with validated data
        loadUser(userId, tab)
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // Optionally navigate to safe state
        finish()
    }
}
```

**Why this works:**
- Multiple layers of validation
- Clear error logging for debugging
- Explicit format validation (regex)
- Safe fallbacks for optional parameters
- User-friendly error messages

### ❌ WRONG Pattern - No Validation

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        // WRONG: Multiple crash risks
        val userId = intent?.data?.getQueryParameter("user_id")!!  // NPE if null
        loadUser(userId)  // What if userId is empty or malformed?
    }
}
```

**Why this fails:**
- Crashes if `intent.data` is null
- Crashes if `user_id` parameter missing
- No validation of parameter format
- Security risk: injection attacks possible
- No error handling or user feedback

---

## Coroutine Scope Usage

### ✅ CORRECT Pattern

```kotlin
// In ViewModel
class UserViewModel : ViewModel() {
    fun loadUser(id: String) {
        viewModelScope.launch {  // Auto-cancelled when ViewModel cleared
            try {
                val user = withContext(Dispatchers.IO) {
                    repository.getUser(id)  // Network/DB on IO dispatcher
                }
                _uiState.value = UiState.Success(user)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message)
            }
        }
    }
}

// In Fragment
class UserFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {  // Auto-cancelled when view destroyed
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
}

// For CPU-intensive work
fun processImage(bitmap: Bitmap) {
    viewModelScope.launch {
        val processed = withContext(Dispatchers.Default) {
            // Heavy computation on Default dispatcher
            applyFilters(bitmap)
        }
        _processedImage.value = processed
    }
}
```

**Why this works:**
- Appropriate scope for each context
- Auto-cancellation prevents leaks
- Correct dispatchers for different work types
- Structured concurrency

### ❌ WRONG Pattern - Scope Issues

```kotlin
// WRONG: GlobalScope never cancels
class UserViewModel : ViewModel() {
    fun loadUser(id: String) {
        GlobalScope.launch {  // Continues after ViewModel cleared!
            val user = repository.getUser(id)
            _uiState.value = UiState.Success(user)
        }
    }
}

// WRONG: runBlocking on main thread
fun loadUserSync(id: String): User {
    return runBlocking {  // Blocks UI thread!
        repository.getUser(id)
    }
}

// WRONG: No dispatcher specified for blocking I/O
fun saveUser(user: User) {
    viewModelScope.launch {  // Defaults to Main dispatcher
        database.save(user)  // Blocking I/O on main thread!
    }
}
```

**Why this fails:**
- `GlobalScope` creates memory leaks
- `runBlocking` freezes UI
- Wrong dispatcher causes ANR (Application Not Responding)

---

## State Restoration

### ✅ CORRECT Pattern

```kotlin
class MainActivity : AppCompatActivity() {
    private var selectedTab: Int = 0
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Restore state from savedInstanceState
        savedInstanceState?.let {
            selectedTab = it.getInt(KEY_SELECTED_TAB, 0)
            userId = it.getString(KEY_USER_ID)
        }
        
        setupUI()
        
        // Use restored state
        selectTab(selectedTab)
        userId?.let { loadUser(it) }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        // Save state before activity destroyed
        outState.putInt(KEY_SELECTED_TAB, selectedTab)
        userId?.let { outState.putString(KEY_USER_ID, it) }
        super.onSaveInstanceState(outState)
    }
    
    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val KEY_USER_ID = "user_id"
    }
}
```

**Why this works:**
- State survives configuration changes (rotation)
- State survives process death
- Clear key constants prevent typos

### ❌ WRONG Pattern - State Loss

```kotlin
class MainActivity : AppCompatActivity() {
    private var selectedTab: Int = 0
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // WRONG: No state restoration
        setupUI()
        selectTab(0)  // Always resets to 0 on rotation
    }
    
    // WRONG: No onSaveInstanceState implementation
    // State is lost on rotation or process death
}
```

**Why this fails:**
- User loses current state on rotation
- Poor user experience (loses scroll position, form data, etc.)
- State lost on process death (low memory situations)

---

## RecyclerView ListAdapter

### ✅ CORRECT Pattern - ListAdapter with DiffUtil

```kotlin
// Data class
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?
)

// DiffUtil.ItemCallback
class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        // Check if items represent the same entity (by ID)
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        // Check if item contents are identical
        return oldItem == newItem
    }
}

// ListAdapter with ViewBinding
class UserAdapter(
    private val onItemClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onItemClick)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class UserViewHolder(
        private val binding: ItemUserBinding,
        private val onItemClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: User) {
            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email
            
            // Load avatar if available
            user.avatarUrl?.let { url ->
                // Use your image loading library
                // Glide.with(binding.ivAvatar).load(url).into(binding.ivAvatar)
            }
            
            binding.root.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}

// Usage in Fragment
class UserListFragment : Fragment() {
    
    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: UserListViewModel by viewModels()
    
    private val adapter = UserAdapter { user ->
        // Handle item click
        navigateToUserProfile(user.id)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.rvUsers.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { users ->
                    // submitList automatically calculates diff and animates changes
                    adapter.submitList(users)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
```

### ✅ CORRECT - ListAdapter with Payload for Partial Updates

```kotlin
// Enhanced DiffUtil with payload support
class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
    
    // Return payload for partial updates
    override fun getChangePayload(oldItem: User, newItem: User): Any? {
        val changes = mutableListOf<String>()
        
        if (oldItem.name != newItem.name) changes.add(PAYLOAD_NAME)
        if (oldItem.email != newItem.email) changes.add(PAYLOAD_EMAIL)
        if (oldItem.avatarUrl != newItem.avatarUrl) changes.add(PAYLOAD_AVATAR)
        
        return if (changes.isNotEmpty()) changes else null
    }
    
    companion object {
        const val PAYLOAD_NAME = "name"
        const val PAYLOAD_EMAIL = "email"
        const val PAYLOAD_AVATAR = "avatar"
    }
}

class UserAdapter(
    private val onItemClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onItemClick)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Partial update
            val user = getItem(position)
            holder.bindPartial(user, payloads)
        }
    }
    
    class UserViewHolder(
        private val binding: ItemUserBinding,
        private val onItemClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: User) {
            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email
            user.avatarUrl?.let { loadAvatar(it) }
            binding.root.setOnClickListener { onItemClick(user) }
        }
        
        fun bindPartial(user: User, payloads: List<Any>) {
            payloads.forEach { payload ->
                if (payload is List<*>) {
                    payload.forEach { change ->
                        when (change) {
                            UserDiffCallback.PAYLOAD_NAME -> {
                                binding.tvUserName.text = user.name
                            }
                            UserDiffCallback.PAYLOAD_EMAIL -> {
                                binding.tvUserEmail.text = user.email
                            }
                            UserDiffCallback.PAYLOAD_AVATAR -> {
                                user.avatarUrl?.let { loadAvatar(it) }
                            }
                        }
                    }
                }
            }
        }
        
        private fun loadAvatar(url: String) {
            // Load avatar image
        }
    }
}
```

### ❌ INCORRECT Pattern - Old RecyclerView.Adapter

```kotlin
// ❌ BLOCKER - Using old RecyclerView.Adapter with notifyDataSetChanged
class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    
    private var users: List<User> = emptyList()
    
    // ❌ Manual list management
    fun setUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()  // ❌ BLOCKER - Inefficient, no animations
    }
    
    override fun getItemCount(): Int = users.size
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }
    
    class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUserName.text = user.name
            binding.tvUserEmail.text = user.email
        }
    }
}
```

**Why this is wrong:**
- ❌ No automatic diffing - entire list re-rendered
- ❌ No animations for item changes
- ❌ Poor performance with large lists
- ❌ Manual index management prone to errors
- ❌ `notifyDataSetChanged()` is inefficient

### ❌ INCORRECT - Manual DiffUtil without ListAdapter

```kotlin
// ❌ IMPORTANT - Manual DiffUtil calculation (unnecessary complexity)
class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    
    private var users: List<User> = emptyList()
    
    fun setUsers(newUsers: List<User>) {
        val diffCallback = UserDiffCallback(users, newUsers)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        users = newUsers
        diffResult.dispatchUpdatesTo(this)
    }
    
    // ... rest of implementation
}

// ❌ Unnecessary DiffUtil.Callback
class UserDiffCallback(
    private val oldList: List<User>,
    private val newList: List<User>
) : DiffUtil.Callback() {
    
    override fun getOldListSize(): Int = oldList.size
    
    override fun getNewListSize(): Int = newList.size
    
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
    
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
```

**Why use ListAdapter instead:**
- ✅ ListAdapter handles DiffUtil calculation automatically
- ✅ Cleaner API with `submitList()`
- ✅ Built-in background thread calculation
- ✅ Less boilerplate code

### ✅ CORRECT - Multi-ViewType with ListAdapter

```kotlin
// Sealed class for different item types
sealed class ListItem {
    data class UserItem(val user: User) : ListItem()
    data class HeaderItem(val title: String) : ListItem()
    object LoadingItem : ListItem()
}

class MultiTypeAdapter : ListAdapter<ListItem, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return when {
                oldItem is ListItem.UserItem && newItem is ListItem.UserItem ->
                    oldItem.user.id == newItem.user.id
                oldItem is ListItem.HeaderItem && newItem is ListItem.HeaderItem ->
                    oldItem.title == newItem.title
                oldItem is ListItem.LoadingItem && newItem is ListItem.LoadingItem ->
                    true
                else -> false
            }
        }
        
        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }
) {
    
    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_LOADING = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.UserItem -> VIEW_TYPE_USER
            is ListItem.HeaderItem -> VIEW_TYPE_HEADER
            is ListItem.LoadingItem -> VIEW_TYPE_LOADING
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(
                ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                ItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            VIEW_TYPE_LOADING -> LoadingViewHolder(
                ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.UserItem -> (holder as UserViewHolder).bind(item.user)
            is ListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item.title)
            is ListItem.LoadingItem -> { /* No-op for loading */ }
        }
    }
    
    class UserViewHolder(private val binding: ItemUserBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUserName.text = user.name
        }
    }
    
    class HeaderViewHolder(private val binding: ItemHeaderBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.tvHeader.text = title
        }
    }
    
    class LoadingViewHolder(binding: ItemLoadingBinding) : 
        RecyclerView.ViewHolder(binding.root)
}
```

### Review Checklist for RecyclerView

When reviewing RecyclerView adapter code:

- [ ] Uses `ListAdapter` instead of `RecyclerView.Adapter`
- [ ] Implements `DiffUtil.ItemCallback` properly
- [ ] `areItemsTheSame` checks unique identifier (ID)
- [ ] `areContentsTheSame` checks full equality
- [ ] ViewHolder uses ViewBinding (no `findViewById`)
- [ ] No manual `notifyDataSetChanged()` calls
- [ ] Uses `submitList()` to update data
- [ ] Item click listeners passed via constructor, not set in `bind()`
- [ ] No memory leaks from listeners
- [ ] Multi-type lists use proper `getItemViewType()`