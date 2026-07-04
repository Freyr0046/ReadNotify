# Android UI Patterns & Best Practices

## Core Principle

**Always prefer the simplest solution that meets requirements.** Use single Views with attributes over nested layouts whenever possible.

---

## Pattern 1: Icon + Text

### ✅ PREFERRED: TextView with Compound Drawable

```xml
<TextView
    android:id="@+id/tvLabel"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/label"
    android:drawableStart="@drawable/ic_icon"
    android:drawablePadding="8dp"
    android:gravity="center_vertical" />
```

**Use cases:**
- Buttons with icons
- List items with leading icons
- Status indicators with text
- Menu items

**Drawable positions:**
- `drawableStart` — left side (supports RTL)
- `drawableEnd` — right side (supports RTL)
- `drawableTop` — above text
- `drawableBottom` — below text

**Key attributes:**
- `android:drawablePadding` — spacing between icon and text
- `android:drawableTint` — tint color for the drawable (API 23+)

### ❌ AVOID: Separate ImageView + TextView

Only use when:
- Icon and text need independent animations
- Icon needs click handling separate from text
- Complex alignment requirements beyond TextView capabilities

---

## Pattern 2: Rounded Background

### ✅ PREFERRED: Shape Drawable

Create `res/drawable/bg_rounded_primary.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/primary" />
    <corners android:radius="8dp" />
</shape>
```

Use in layout:

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_rounded_primary"
    android:padding="16dp"
    android:text="@string/label" />
```

**Common variations:**
- **Stroke only:** Replace `<solid>` with `<stroke android:width="1dp" android:color="@color/border" />`
- **Gradient:** Replace `<solid>` with `<gradient android:startColor="@color/start" android:endColor="@color/end" />`
- **Selective corners:** Use `android:topLeftRadius`, `android:topRightRadius`, etc.

### ❌ AVOID: PNG/WebP images for simple shapes

Only use images when:
- Complex gradients or shadows required
- Design uses non-standard visual effects
- Performance profiling shows drawable inflation is bottleneck

---

## Pattern 3: Divider / Separator Line

### ✅ PREFERRED: View with Background

```xml
<View
    android:id="@+id/divider"
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:background="@color/divider" />
```

**Use `View`, not `ImageView`.** A divider is decorative, not content.

**Key attributes:**
- Horizontal divider: `height="1dp"` or `height="@dimen/divider_height"`
- Vertical divider: `width="1dp"`

---

## Pattern 4: Circular Avatar / Icon

### ✅ PREFERRED: ImageView with Shape Drawable Clip

Create `res/drawable/bg_circle.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/avatar_background" />
</shape>
```

Use in layout:

```xml
<ImageView
    android:id="@+id/ivAvatar"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:background="@drawable/bg_circle"
    android:scaleType="centerCrop"
    android:contentDescription="@string/avatar"
    tools:src="@drawable/sample_avatar" />
```

**Alternative for API 21+:** Use `android:clipToOutline="true"` with `ViewOutlineProvider`

### For images loaded at runtime:

Use libraries like **Glide** or **Coil** with `circleCrop()` transformation.

---

## Pattern 5: Badge / Notification Count

### ✅ PREFERRED: TextView with Circular Background

Create `res/drawable/bg_badge.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/error" />
</shape>
```

Use in layout:

```xml
<TextView
    android:id="@+id/tvBadge"
    android:layout_width="20dp"
    android:layout_height="20dp"
    android:background="@drawable/bg_badge"
    android:gravity="center"
    android:textColor="@color/white"
    android:textSize="12sp"
    android:visibility="gone"
    tools:text="9"
    tools:visibility="visible" />
```

**Key attributes:**
- Fixed width/height for circular appearance
- `gravity="center"` for centered text
- Default `visibility="gone"` — only show when count > 0

---

## Pattern 6: Multi-line Text with Icon

When text can wrap but icon should stay aligned to **first line**:

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <ImageView
        android:id="@+id/ivIcon"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@drawable/ic_info"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tvMessage"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:text="@string/message"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@id/ivIcon"
        app:layout_constraintTop_toTopOf="@id/ivIcon" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

**Why separate Views here:**
- TextView with `drawableStart` aligns icon to text baseline, not top
- Multi-line text would push icon down
- This pattern keeps icon fixed at top

---

## Pattern 7: Button States

### ✅ PREFERRED: Selector Drawable

Create `res/drawable/bg_button_primary.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_enabled="false">
        <shape android:shape="rectangle">
            <solid android:color="@color/button_disabled" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/button_pressed" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/button_normal" />
            <corners android:radius="8dp" />
        </shape>
    </item>
</selector>
```

Use in layout:

```xml
<Button
    android:id="@+id/btnSubmit"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_button_primary"
    android:text="@string/submit" />
```

**State order matters:** More specific states (disabled, pressed) must come before default state.

---

## Pattern 8: Overlay / Scrim

For darkening backgrounds behind dialogs or highlighting content:

```xml
<View
    android:id="@+id/scrim"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#80000000"
    android:clickable="true"
    android:focusable="true"
    android:visibility="gone" />
```

**Key attributes:**
- `#80000000` — 50% black overlay (80 = 50% opacity in hex)
- `clickable="true"` — intercepts touch events to prevent interaction with content below
- `focusable="true"` — captures focus

---

## Pattern 9: Empty State / Placeholder

### ✅ PREFERRED: Vertical LinearLayout or ConstraintLayout

```xml
<LinearLayout
    android:id="@+id/llEmptyState"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:gravity="center_horizontal"
    android:orientation="vertical"
    android:visibility="gone"
    tools:visibility="visible">

    <ImageView
        android:layout_width="120dp"
        android:layout_height="120dp"
        android:src="@drawable/ic_empty"
        android:contentDescription="@null"
        android:importantForAccessibility="no" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/empty_state_title"
        android:textSize="18sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="@string/empty_state_message"
        android:textColor="@color/text_secondary" />

</LinearLayout>
```

**Default visibility:** `gone` — toggle programmatically based on data state.

---

## Pattern 10: Loading State with Spinner

### ✅ PREFERRED: AVLoadingIndicatorView (if using library)

```xml
<com.wang.avi.AVLoadingIndicatorView
    android:id="@+id/aviLoading"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:visibility="gone"
    app:indicatorColor="@color/primary"
    app:indicatorName="BallPulseIndicator"
    tools:visibility="visible" />
```

### Alternative: Native ProgressBar

```xml
<ProgressBar
    android:id="@+id/progressBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:indeterminateTint="@color/primary"
    android:visibility="gone"
    tools:visibility="visible" />
```

**Key attributes:**
- Default `visibility="gone"`
- Use `tools:visibility="visible"` for preview
- `indeterminateTint` for color customization (API 21+)

---

## Pattern 11: Card with Shadow

### ✅ PREFERRED: MaterialCardView

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/cvItem"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp"
    app:cardUseCompatPadding="true">

    <!-- Card content here -->

</com.google.android.material.card.MaterialCardView>
```

**Key attributes:**
- `cardCornerRadius` — rounded corners
- `cardElevation` — shadow depth
- `cardUseCompatPadding` — ensures consistent shadow rendering across API levels

### Alternative: CardView (AndroidX)

```xml
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp">

    <!-- Card content here -->

</androidx.cardview.widget.CardView>
```

---

## Decision Matrix

| Pattern | Single View | Nested Layout | Reason |
|---------|-------------|---------------|--------|
| Icon + Single-line Text | ✅ TextView + drawable | ❌ | Simpler, fewer Views |
| Icon + Multi-line Text | ❌ | ✅ ConstraintLayout | Icon needs top alignment |
| Rounded Background | ✅ Shape drawable | ❌ | No image assets needed |
| Divider | ✅ View | ❌ | Semantic correctness |
| Button States | ✅ Selector drawable | ❌ | Android handles states |
| Empty State | ❌ | ✅ LinearLayout/ConstraintLayout | Multiple elements needed |

---

## Anti-Patterns to Avoid

### ❌ Using ImageView for Decorative Backgrounds

```xml
<!-- BAD -->
<ImageView
    android:src="@drawable/background"
    android:scaleType="fitXY" />
```

**Why bad:** ImageView is for content, not decoration. Use `android:background` on container instead.

### ❌ Hardcoding Sizes for Text

```xml
<!-- BAD -->
<TextView
    android:layout_width="200dp"
    android:layout_height="50dp" />
```

**Why bad:** Text should use `wrap_content` and let constraints define max width. Fixed sizes break with different text lengths or font scaling.

### ❌ Nested LinearLayouts for Grid Layouts

```xml
<!-- BAD -->
<LinearLayout android:orientation="vertical">
    <LinearLayout android:orientation="horizontal">
        <View />
        <View />
    </LinearLayout>
    <LinearLayout android:orientation="horizontal">
        <View />
        <View />
    </LinearLayout>
</LinearLayout>
```

**Why bad:** Use ConstraintLayout with Chains or RecyclerView with GridLayoutManager instead. Deep nesting causes performance issues.

### ❌ Using `match_parent` on Children of ConstraintLayout

```xml
<!-- BAD -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

**Why bad:** In ConstraintLayout, use `0dp` (match_constraint) with constraints instead. `match_parent` ignores constraints.

**Correct approach:**

```xml
<!-- GOOD -->
<TextView
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

---

## Performance Considerations

1. **Reduce View hierarchy depth** — Flat layouts render faster
2. **Reuse drawables** — Share shape drawables across Views with different tints
3. **Avoid overdraw** — Don't set backgrounds on parent and child when not needed
4. **Use `tools:` attributes** — Enables preview without runtime overhead

