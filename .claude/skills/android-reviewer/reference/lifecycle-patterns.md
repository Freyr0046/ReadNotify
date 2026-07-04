# Android Lifecycle Management Patterns

Comprehensive patterns and anti-patterns for Android lifecycle management.

## Activity Lifecycle

### State Restoration Pattern

```kotlin
// ✅ CORRECT - Proper state management
class MainActivity : AppCompatActivity() {
    private var userQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore state
        userQuery = savedInstanceState?.getString(KEY_QUERY) ?: ""

        setContentView(binding.root)
        setupViews()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_QUERY, userQuery)
    }

    companion object {
        private const val KEY_QUERY = "user_query"
    }
}

// ❌ WRONG - No state restoration
class MainActivity : AppCompatActivity() {
    private var userQuery: String = "" // Lost on process death

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // savedInstanceState ignored
    }
}
```

### Configuration Change Handling

```kotlin
// ✅ CORRECT - Use ViewModel for config changes
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        // ViewModel survives config changes
        viewModel.userData.observe(this) { data ->
            updateUI(data)
        }
    }
}

// ❌ WRONG - Storing data in Activity
class MainActivity : AppCompatActivity() {
    private var userData: UserData? = null // Lost on rotation
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadUserData() // Reloads unnecessarily on rotation
    }
}
```

## Fragment Lifecycle

### ViewLifecycleOwner Pattern

```kotlin
// ✅ CORRECT - Using viewLifecycleOwner
class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ Use viewLifecycleOwner for view-related observers
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.textView.text = state.message
                }
            }
        }
        
        // ✅ View-related listeners tied to view lifecycle
        binding.button.setOnClickListener {
            viewModel.onButtonClick()
        }
    }
    
    override fun onDestroyView() {
        _binding = null // ✅ CRITICAL: Prevent memory leak
        super.onDestroyView()
    }
}

// ❌ WRONG - Using fragment lifecycle for view observers
class UserFragment : Fragment() {
    private lateinit var binding: FragmentUserBinding
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ❌ Uses fragment lifecycle, not view lifecycle
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Crash! binding.textView accessed after onDestroyView
                binding.textView.text = state.message
            }
        }
    }
}
```

### Fragment Result API

```kotlin
// ✅ CORRECT - Modern Fragment Result API
class ListFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set result listener
        setFragmentResultListener("selection_key") { _, bundle ->
            val result = bundle.getString("selected_item")
            handleSelection(result)
        }
        
        binding.button.setOnClickListener {
            findNavController().navigate(R.id.action_to_picker)
        }
    }
}

class PickerFragment : Fragment() {
    private fun onItemSelected(item: String) {
        setFragmentResult("selection_key", bundleOf("selected_item" to item))
        findNavController().popBackStack()
    }
}

// ❌ WRONG - Deprecated setTargetFragment
class ListFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.button.setOnClickListener {
            val picker = PickerFragment()
            picker.setTargetFragment(this, REQUEST_CODE) // Deprecated!
            picker.show(parentFragmentManager, "picker")
        }
    }
}
```

### Fragment Argument Validation

```kotlin
// ✅ CORRECT - Defensive argument handling
class DetailFragment : Fragment() {
    private val args: DetailFragmentArgs by navArgs()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = args.userId.takeIf { it.isNotBlank() } ?: run {
            Log.e(TAG, "Invalid userId argument")
            showError("Invalid user ID")
            findNavController().popBackStack()
            return
        }
        
        loadUser(userId)
    }
}

// ❌ WRONG - No validation
class DetailFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = arguments?.getString("USER_ID")!!
        loadUser(userId) // Crashes if missing or empty
    }
}
```

## ViewModel Lifecycle

### Context-Free ViewModel

```kotlin
// ✅ CORRECT - No context references
class UserViewModel(
    private val repository: UserRepository,
    private val analytics: AnalyticsService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            try {
                val user = repository.getUser(userId)
                _uiState.value = UiState.Success(user)
                analytics.logEvent("user_loaded")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up if needed
    }
}

// ❌ WRONG - Context leak
class UserViewModel(
    private val context: Context // NEVER!
) : ViewModel() {
    
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // Leaks Activity
    }
}

// ❌ WRONG - Application context still problematic
class UserViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    fun getAppName(): String {
        return getApplication<Application>().getString(R.string.app_name) // Bad practice
    }
}
```

### Proper Resource Access from ViewModel

```kotlin
// ✅ CORRECT - Use ResourceProvider abstraction
interface ResourceProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}

class AndroidResourceProvider(
    private val context: Context
) : ResourceProvider {
    override fun getString(resId: Int): String = context.getString(resId)
    override fun getString(resId: Int, vararg formatArgs: Any): String = 
        context.getString(resId, *formatArgs)
}

class UserViewModel(
    private val repository: UserRepository,
    private val resources: ResourceProvider
) : ViewModel() {
    
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    
    fun loadUser() {
        viewModelScope.launch {
            try {
                val user = repository.getUser()
                _message.value = resources.getString(R.string.welcome, user.name)
            } catch (e: Exception) {
                _message.value = resources.getString(R.string.error_loading_user)
            }
        }
    }
}
```

### Scoped ViewModel Usage

```kotlin
// ✅ CORRECT - Scoped to navigation graph
class StepOneFragment : Fragment() {
    // Shared across all fragments in this nav graph
    private val sharedViewModel: CheckoutViewModel by navGraphViewModels(R.id.nav_checkout)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.nextButton.setOnClickListener {
            sharedViewModel.updateStep1Data(binding.editText.text.toString())
            findNavController().navigate(R.id.action_to_step2)
        }
    }
}

class StepTwoFragment : Fragment() {
    // Same instance as StepOneFragment
    private val sharedViewModel: CheckoutViewModel by navGraphViewModels(R.id.nav_checkout)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Access data from step 1
        val step1Data = sharedViewModel.step1Data.value
    }
}

// ❌ WRONG - Using activity scope unnecessarily
class StepOneFragment : Fragment() {
    private val viewModel: CheckoutViewModel by activityViewModels() // Too broad!
    // Lives for entire activity, not just checkout flow
}
```

## Coroutine Lifecycle Integration

### Flow Collection Patterns

```kotlin
// ✅ CORRECT - repeatOnLifecycle
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
}

// ✅ CORRECT - flowWithLifecycle
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        viewModel.uiState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { state -> updateUI(state) }
            .launchIn(lifecycleScope)
    }
}

// ❌ WRONG - Collects even when backgrounded
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state) // Updates UI even in background!
            }
        }
    }
}
```

### One-Shot Events Pattern

```kotlin
// ✅ CORRECT - Channel for one-shot events
class UserViewModel : ViewModel() {
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    sealed class Event {
        data class ShowToast(val message: String) : Event()
        data class Navigate(val route: String) : Event()
    }
    
    fun onSaveClick() {
        viewModelScope.launch {
            try {
                repository.save()
                _events.send(Event.ShowToast("Saved successfully"))
                _events.send(Event.Navigate("home"))
            } catch (e: Exception) {
                _events.send(Event.ShowToast("Error: ${e.message}"))
            }
        }
    }
}

class UserFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is Event.ShowToast -> showToast(event.message)
                    is Event.Navigate -> findNavController().navigate(event.route)
                }
            }
        }
    }
}

// ❌ WRONG - Using StateFlow for events (sticky)
class UserViewModel : ViewModel() {
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()
    
    fun onSaveClick() {
        _event.value = Event.ShowToast("Saved") // Re-emitted on config change!
    }
}
```

## Memory Leak Prevention

### Listener Management

```kotlin
// ✅ CORRECT - Lifecycle-aware listeners
class LocationFragment : Fragment() {
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            handleLocation(result.lastLocation)
        }
    }
    
    override fun onStart() {
        super.onStart()
        locationManager.requestLocationUpdates(locationCallback)
    }
    
    override fun onStop() {
        locationManager.removeLocationUpdates(locationCallback)
        super.onStop()
    }
}

// ❌ WRONG - Listener never removed
class LocationFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        locationManager.requestLocationUpdates { location ->
            handleLocation(location)
        }
        // Never removed - memory leak!
    }
}
```

### Callback References

```kotlin
// ✅ CORRECT - Weak references for callbacks
class ImageLoader {
    private var callbackRef: WeakReference<Callback>? = null
    
    fun loadImage(url: String, callback: Callback) {
        callbackRef = WeakReference(callback)
        
        fetchImage(url) { bitmap ->
            callbackRef?.get()?.onImageLoaded(bitmap)
        }
    }
}

// ❌ WRONG - Strong reference to Activity
class ImageLoader {
    private var callback: Callback? = null
    
    fun loadImage(url: String, callback: Callback) {
        this.callback = callback // Leaks Activity if ImageLoader lives longer
        
        fetchImage(url) { bitmap ->
            callback?.onImageLoaded(bitmap)
        }
    }
}
```

### Resource Cleanup

```kotlin
// ✅ CORRECT - Proper resource management
class DataFragment : Fragment() {
    private var cursor: Cursor? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cursor = database.query(...)
        displayData(cursor)
    }

    override fun onDestroyView() {
        cursor?.close()
        cursor = null
        super.onDestroyView()
    }
}

// ❌ WRONG - Resource leak
class DataFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cursor = database.query(...) // Never closed!
        displayData(cursor)
    }
}
```