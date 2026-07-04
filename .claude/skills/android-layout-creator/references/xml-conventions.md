# XML Layout Conventions

## ID Naming

Use semantic, prefixed naming:

| View Type              | Prefix | Example          |
|------------------------|--------|------------------|
| TextView               | `tv`   | `tvTitle`        |
| Button                 | `btn`  | `btnSubmit`      |
| ImageView              | `iv`   | `ivAvatar`       |
| EditText               | `et`   | `etEmail`        |
| RecyclerView           | `rv`   | `rvItems`        |
| ConstraintLayout       | `cl`   | `clContainer`    |
| LinearLayout           | `ll`   | `llActions`      |
| CardView               | `cv`   | `cvProfile`      |
| AVLoadingIndicatorView | `avi`  | `aviLoading`     |
| Switch                 | `sw`   | `swNotification` |
| Toolbar                | `tb`   | `tbMain`         |

**Never** use generic names like `textView1`, `button2`.

## Typography Weight Mapping

| Figma Weight | Android Attribute                              |
|-------------|------------------------------------------------|
| 400         | `android:textStyle="normal"` (or omit)         |
| 500         | `android:fontFamily="sans-serif-medium"` (omit textStyle) |
| 700         | `android:textStyle="bold"`                     |

## Preview Attributes (Mandatory)

All dynamic content must use `tools:` namespace for Android Studio preview:

```xml
<!-- tools namespace declaration in root -->
xmlns:tools="http://schemas.android.com/tools"

<!-- Text preview -->
<TextView
    android:id="@+id/tvUsername"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    tools:text="John Doe" />

<!-- Image preview -->
<ImageView
    android:id="@+id/ivProfile"
    android:layout_width="48dp"
    android:layout_height="48dp"
    tools:src="@drawable/ic_placeholder" />

<!-- Loading state preview -->
<com.wang.avi.AVLoadingIndicatorView
    android:id="@+id/aviLoading"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone"
    tools:visibility="visible" />

<!-- RecyclerView item count preview -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rvItems"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    tools:listitem="@layout/item_card"
    tools:itemCount="3" />
```

## Strict Prohibitions

1. **No hex codes in layouts** — Use `@color/` references only
2. **No hardcoded strings** — Use `@string/` references only
3. **No hardcoded dimensions** — Use `@dimen/` references for reusable spacing
4. **No deep nesting** — Use ConstraintLayout Chains/Guidelines instead of nested LinearLayouts

## Accessibility

- Image buttons: always set `android:contentDescription`
- Decorative images: use `android:contentDescription="@null"` with `android:importantForAccessibility="no"`

## Spacing

Adhere to **4dp/8dp grid system**. Common values:

| Token     | Value |
|-----------|-------|
| Tiny      | 4dp   |
| Small     | 8dp   |
| Medium    | 16dp  |
| Large     | 24dp  |
| XLarge    | 32dp  |

Flag any irregular spacing to the user before proceeding.

## Hierarchy Optimization

Prefer flat layouts:

```xml
<!-- BAD: Nested LinearLayouts -->
<LinearLayout android:orientation="vertical">
    <LinearLayout android:orientation="horizontal">
        <TextView ... />
        <TextView ... />
    </LinearLayout>
</LinearLayout>

<!-- GOOD: Flat ConstraintLayout with Chain -->
<androidx.constraintlayout.widget.ConstraintLayout>
    <TextView
        android:id="@+id/tvLabel"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
    <TextView
        android:id="@+id/tvValue"
        app:layout_constraintStart_toEndOf="@id/tvLabel"
        app:layout_constraintTop_toTopOf="@id/tvLabel"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_chainStyle="spread" />
</androidx.constraintlayout.widget.ConstraintLayout>
```
