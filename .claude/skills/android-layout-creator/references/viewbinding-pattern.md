# ViewBinding Fragment Pattern

## Standard Template

```kotlin
class ExampleFragment : Fragment(R.layout.fragment_example) {

    // Nullable backing field for ViewBinding
    private var _binding: FragmentExampleBinding? = null

    // Non-null accessor — only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExampleBinding.bind(view)

        initViews()
        setupListeners()
    }

    /**
     * Static UI setup: adapter init, default text, toolbar config.
     */
    private fun initViews() {
        binding.apply {
            // Toolbar setup, adapter initialization, static defaults
        }
    }

    /**
     * User interaction handlers: click, focus, scroll.
     */
    private fun setupListeners() {
        binding.apply {
            // Click listeners, focus change, scroll listeners
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Set to null to prevent Memory Leak
        _binding = null
    }
}
```

## Key Rules

1. **Binding lifecycle:** `_binding` is valid only between `onViewCreated` and `onDestroyView`
2. **Leak prevention:** Always null out `_binding` in `onDestroyView()` — non-negotiable
3. **Code structure:** Keep `onViewCreated` clean — delegate to `initViews()` and `setupListeners()`
4. **Readability:** Use `binding.apply { }` blocks to reduce `binding.` prefix repetition

## RecyclerView Adapter Pattern

When the layout includes a RecyclerView, add adapter initialization in `initViews()`:

```kotlin
private lateinit var adapter: ItemAdapter

private fun initViews() {
    adapter = ItemAdapter()
    binding.apply {
        rvItems.adapter = adapter
        rvItems.layoutManager = LinearLayoutManager(requireContext())
    }
}
```

## PopupWindow ViewBinding Pattern

PopupWindow 沒有 Fragment lifecycle，`dismiss()` 等同 `onDestroyView()`。

```kotlin
class ExamplePopupWindow(
    context: Context,
    private var onActionClicked: (() -> Unit)?   // var — 以便 dismiss 後清空
) : PopupWindow() {

    private var _binding: PopupExampleBinding? = null
    private val binding get() = _binding!!
    private var activityContext: Context? = context  // 不與構造參數同名，避免 shadow
    private var window: Window? = null

    init {
        _binding = PopupExampleBinding.inflate(LayoutInflater.from(context))
        contentView = binding.root
        width = MATCH_PARENT
        height = WRAP_CONTENT
        isFocusable = true
        animationStyle = R.style.PopupAnimation
        setBackgroundDrawable(0x00FFFFFF.toDrawable())
        initListeners()
    }

    private fun initListeners() {
        binding.apply {
            ivClose.setOnClickListener { dismiss() }
            tvAction.setOnClickListener {
                onActionClicked?.invoke()
                dismiss()
            }
        }
    }

    // Usage: showAtLocation(requireView(), Gravity.BOTTOM, 0, 0)
    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        window = (activityContext as? Activity)?.window  // as? 防止 ContextWrapper crash
        setWindowParam(0.5f, 0.5f)
    }

    override fun dismiss() {
        super.dismiss()
        setWindowParam(1.0f, 1.0f)   // 先還原 dim，再 null window
        _binding = null              // 等同 onDestroyView()，斷開 view → Activity 參考鏈
        onActionClicked = null       // lambda 可能 capture caller，必須清空
        activityContext = null
        window = null
    }

    private fun setWindowParam(alpha: Float, dimAmount: Float) {
        val win = window ?: return
        win.attributes = win.attributes?.apply {
            this.alpha = alpha
            this.dimAmount = dimAmount
        }
    }
}
```

### PopupWindow Key Rules

1. **context field 不與構造參數同名**：用 `activityContext` 避免 shadow 混淆
2. **callback 宣告為 `var`（nullable）**：lambda 通常 capture caller，dismiss 後必須能清空
3. **`as? Activity`**：`requireContext()` 可能回傳 `ContextThemeWrapper`，強制 `as Activity` 會 crash
4. **dismiss 順序**：`setWindowParam(1.0f, 1.0f)` 必須在 `window = null` 之前呼叫
5. **`params` 不存 field**：`WindowManager.LayoutParams` 不持有 View 參考，用 local `apply {}` 即可

---

## ViewModel Observer Pattern

When integrating with ViewModel, add `observeData()` after `setupListeners()`:

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    _binding = FragmentExampleBinding.bind(view)

    initViews()
    setupListeners()
    observeData()
}

private fun observeData() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                binding.apply {
                    // Update UI from state
                }
            }
        }
    }
}
```
