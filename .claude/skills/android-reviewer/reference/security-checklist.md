# Android Security Audit Checklist

Comprehensive security patterns and vulnerabilities to check during code review.

## Data Storage Security

### Sensitive Data Encryption

```kotlin
// ✅ CORRECT - Encrypted SharedPreferences
class SecurePreferences(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        sharedPreferences.edit {
            putString(KEY_AUTH_TOKEN, token)
        }
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }
}

// ❌ WRONG - Plain text sensitive data
class Preferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        sharedPreferences.edit {
            putString("auth_token", token) // ❌ Stored in plain text
        }
    }

    fun savePassword(password: String) {
        sharedPreferences.edit {
            putString("password", password) // ❌ CRITICAL: Never store passwords
        }
    }
}
```

### Secure File Storage

```kotlin
// ✅ CORRECT - Encrypted file storage
class SecureFileManager(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    fun writeSecureFile(fileName: String, data: ByteArray) {
        val file = File(context.filesDir, fileName)
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(data)
        }
    }
    
    fun readSecureFile(fileName: String): ByteArray {
        val file = File(context.filesDir, fileName)
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        return encryptedFile.openFileInput().use { it.readBytes() }
    }
}

// ❌ WRONG - External storage with sensitive data
class FileManager {
    fun saveSensitiveData(data: String) {
        val file = File(
            Environment.getExternalStorageDirectory(), // ❌ External storage
            "sensitive.txt"
        )
        file.writeText(data) // ❌ Unencrypted, world-readable
    }
}
```

### Database Encryption

```kotlin
// ✅ CORRECT - Encrypted Room database
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    
    companion object {
        fun create(context: Context): AppDatabase {
            val passphrase = SQLiteDatabase.getBytes(
                getPassphrase(context).toCharArray()
            )
            val factory = SupportFactory(passphrase)
            
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "app_database"
            )
                .openHelperFactory(factory)
                .build()
        }
        
        private fun getPassphrase(context: Context): String {
            // Retrieve from KeyStore or generate securely
            return KeyStoreManager.getOrCreateDatabaseKey(context)
        }
    }
}

// ❌ WRONG - Unencrypted database with sensitive data
@Database(entities = [User::class, CreditCard::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "app_database"
            ).build() // ❌ No encryption for sensitive data
        }
    }
}
```

## Network Security

### Certificate Pinning

```kotlin
// ✅ CORRECT - Certificate pinning
class NetworkModule {
    fun provideOkHttpClient(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("api.example.com", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .build()
        
        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()
    }
}

// Network security config (res/xml/network_security_config.xml)
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.example.com</domain>
        <pin-set>
            <pin digest="SHA-256">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=</pin>
            <pin digest="SHA-256">BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=</pin>
        </pin-set>
    </domain-config>
</network-security-config>

// ❌ WRONG - No certificate validation
class NetworkModule {
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .hostnameVerifier { _, _ -> true } // ❌ Accepts all certificates
            .build()
    }
}
```

### HTTPS Enforcement

```kotlin
// ✅ CORRECT - HTTPS only
class ApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/") // ✅ HTTPS
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    if (request.url.scheme != "https") {
                        throw SecurityException("Only HTTPS connections allowed")
                    }
                    chain.proceed(request)
                }
                .build()
        )
        .build()
}

// AndroidManifest.xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false">

// ❌ WRONG - Cleartext traffic allowed
class ApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://api.example.com/") // ❌ HTTP
        .build()
}

// AndroidManifest.xml
<application
    android:usesCleartextTraffic="true"> // ❌ Allows HTTP
```

### API Key Security

```kotlin
// ✅ CORRECT - API keys from native code or backend
class ApiKeyProvider {
    external fun getApiKey(): String
    
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}

// native-lib.cpp
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_ApiKeyProvider_getApiKey(JNIEnv* env, jobject) {
    // Even this can be reverse-engineered, but better than plain text
    const char* apiKey = "your-api-key";
    return env->NewStringUTF(apiKey);
}

// ✅ BETTER - Backend proxy for API keys
class ApiClient {
    suspend fun searchPlaces(query: String): List<Place> {
        // Backend adds API key
        return backendApi.searchPlaces(query)
    }
}

// ❌ WRONG - Hardcoded API keys
class ApiClient {
    companion object {
        const val API_KEY = "sk-1234567890abcdef" // ❌ CRITICAL: Exposed in APK
    }
    
    fun search(query: String): List<Result> {
        return api.search(query, API_KEY)
    }
}

// ❌ WRONG - API key in strings.xml
<string name="google_maps_api_key">AIzaSyDxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx</string>
```

## Component Security

### Exported Component Protection

```kotlin
// ✅ CORRECT - Properly secured exported component
// AndroidManifest.xml
<activity
    android:name=".DeepLinkActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="example.com"
            android:pathPrefix="/app" />
    </intent-filter>
</activity>

class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uri = intent?.data ?: run {
            finish()
            return
        }
        
        // ✅ Validate deep link origin
        if (!isValidDeepLink(uri)) {
            Log.w(TAG, "Invalid deep link: $uri")
            finish()
            return
        }
        
        // ✅ Sanitize and validate parameters
        val userId = uri.getQueryParameter("user_id")
            ?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{1,64}$")) }
            ?: run {
                Log.e(TAG, "Invalid user_id parameter")
                finish()
                return
            }
        
        handleDeepLink(userId)
    }
    
    private fun isValidDeepLink(uri: Uri): Boolean {
        return uri.scheme == "https" &&
               uri.host == "example.com" &&
               uri.path?.startsWith("/app/") == true
    }
}

// ❌ WRONG - Insecure exported component
<activity
    android:name=".MainActivity"
    android:exported="true"> // ❌ Exported without protection
</activity>

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ❌ No validation of external input
        val userId = intent.getStringExtra("USER_ID")
        loadUserData(userId)
    }
}
```

### Broadcast Receiver Security

```kotlin
// ✅ CORRECT - Secured broadcast receiver
// AndroidManifest.xml
<receiver
    android:name=".SecureReceiver"
    android:permission="com.example.app.CUSTOM_PERMISSION"
    android:exported="false">
    <intent-filter>
        <action android:name="com.example.app.ACTION_UPDATE" />
    </intent-filter>
</receiver>

<permission
    android:name="com.example.app.CUSTOM_PERMISSION"
    android:protectionLevel="signature" />

class SecureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ✅ Validate intent action
        if (intent.action != "com.example.app.ACTION_UPDATE") {
            return
        }
        
        // ✅ Validate sender (if possible)
        val senderId = intent.getStringExtra("sender_id")
        if (!isValidSender(senderId)) {
            Log.w(TAG, "Invalid sender: $senderId")
            return
        }
        
        handleUpdate(intent)
    }
}

// ❌ WRONG - Insecure broadcast receiver
<receiver
    android:name=".UpdateReceiver"
    android:exported="true"> // ❌ Anyone can send broadcasts
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ❌ No validation
        val command = intent.getStringExtra("command")
        executeCommand(command) // ❌ Arbitrary command execution
    }
}
```

## Input Validation

### Intent Extras Validation

```kotlin
// ✅ CORRECT - Comprehensive intent validation
class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent?.getStringExtra(EXTRA_USER_ID)
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it.matches(Regex("^[a-zA-Z0-9_-]{1,64}$")) }
            ?: run {
                Log.e(TAG, "Invalid or missing user ID")
                finish()
                return
            }
        
        val isAdmin = intent?.getBooleanExtra(EXTRA_IS_ADMIN, false) ?: false
        
        // ✅ Validate against expected range
        val age = intent?.getIntExtra(EXTRA_AGE, -1)
            ?.takeIf { it in 0..150 }
            ?: run {
                Log.e(TAG, "Invalid age value")
                finish()
                return
            }
        
        loadUser(userId, isAdmin, age)
    }
    
    companion object {
        private const val EXTRA_USER_ID = "user_id"
        private const val EXTRA_IS_ADMIN = "is_admin"
        private const val EXTRA_AGE = "age"
    }
}

// ❌ WRONG - No validation
class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra("user_id")!! // ❌ Force unwrap
        val query = intent.getStringExtra("query") // ❌ No SQL injection check
        
        // ❌ Direct database query with user input
        database.rawQuery("SELECT * FROM users WHERE name = '$query'", null)
    }
}
```

### WebView Security

```kotlin
// ✅ CORRECT - Secure WebView configuration
class SecureWebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = false // ✅ Disable unless required
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                setGeolocationEnabled(false)
                databaseEnabled = false
                domStorageEnabled = false
            }
            
            // ✅ Whitelist allowed domains
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val url = request.url.toString()
                    return if (isAllowedUrl(url)) {
                        false
                    } else {
                        Log.w(TAG, "Blocked URL: $url")
                        true
                    }
                }
            }
        }
        
        // ✅ Load only HTTPS URLs
        val url = intent?.getStringExtra(EXTRA_URL)
            ?.takeIf { it.startsWith("https://") }
            ?: run {
                finish()
                return
            }
        
        webView.loadUrl(url)
    }
    
    private fun isAllowedUrl(url: String): Boolean {
        val allowedDomains = listOf("example.com", "trusted-site.com")
        val uri = Uri.parse(url)
        return allowedDomains.any { uri.host?.endsWith(it) == true }
    }
}

// ❌ WRONG - Insecure WebView
class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true // ❌ Enabled by default
                allowFileAccess = true // ❌ Allows file:// access
                allowFileAccessFromFileURLs = true // ❌ XSS risk
            }
            
            // ❌ JavaScript interface exposed
            addJavascriptInterface(object {
                @JavascriptInterface
                fun deleteAllData() {
                    // ❌ Critical operation exposed to JS
                }
            }, "Android")
        }
        
        // ❌ No URL validation
        val url = intent.getStringExtra("url")
        webView.loadUrl(url!!)
    }
}
```

## Logging & Information Disclosure

### Safe Logging Practices

```kotlin
// ✅ CORRECT - Safe logging
class AuthRepository {
    suspend fun login(email: String, password: String): Result<User> {
        Log.d(TAG, "Login attempt for email: ${email.take(3)}***") // ✅ Partial info only
        
        return try {
            val response = api.login(email, password)
            Log.d(TAG, "Login successful for user: ${response.userId}")
            Result.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e::class.simpleName}") // ✅ No sensitive details
            Result.Error("Authentication failed")
        }
    }
}

// Build config for debug logging
object Logger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        } else {
            // Send to crash reporting (with sanitization)
            Crashlytics.log("Error in $tag")
        }
    }
}

// ❌ WRONG - Sensitive data in logs
class AuthRepository {
    suspend fun login(email: String, password: String): Result<User> {
        Log.d(TAG, "Login: email=$email, password=$password") // ❌ CRITICAL: Password logged
        
        try {
            val response = api.login(email, password)
            Log.d(TAG, "Auth token: ${response.authToken}") // ❌ Token in logs
            Log.d(TAG, "User data: $response") // ❌ Full user object
            return Result.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e) // ❌ Full stack trace with sensitive data
            return Result.Error(e.message ?: "Error")
        }
    }
}
```

### Analytics Security

```kotlin
// ✅ CORRECT - Sanitized analytics
class AnalyticsService {
    fun logScreenView(screenName: String) {
        analytics.logEvent("screen_view") {
            param("screen_name", screenName)
        }
    }

    fun logUserAction(action: String, userId: String) {
        analytics.logEvent(action) {
            // ✅ Hash user ID
            param("user_id_hash", hashUserId(userId))
            param("timestamp", System.currentTimeMillis())
        }
    }

    private fun hashUserId(userId: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

// ❌ WRONG - PII in analytics
class AnalyticsService {
    fun logPurchase(userId: String, email: String, amount: Double, cardNumber: String) {
        analytics.logEvent("purchase") {
            param("user_id", userId) // ❌ Direct user ID
            param("email", email) // ❌ PII
            param("amount", amount)
            param("card", cardNumber) // ❌ CRITICAL: Payment data
        }
    }
}
```

## Security Checklist

Use this checklist during code review:

### Data Storage
- [ ] Sensitive data encrypted (EncryptedSharedPreferences, EncryptedFile)
- [ ] No passwords stored locally
- [ ] Database encrypted if containing sensitive data
- [ ] No sensitive data in external storage
- [ ] Proper file permissions set

### Network
- [ ] HTTPS enforced (no cleartext traffic)
- [ ] Certificate pinning implemented for critical APIs
- [ ] API keys not hardcoded
- [ ] Network security config properly configured
- [ ] SSL/TLS validation not disabled

### Components
- [ ] Exported components justified and protected
- [ ] Intent data validated
- [ ] Deep links validated and sanitized
- [ ] Broadcast receivers secured
- [ ] Content providers secured

### Input Validation
- [ ] All external inputs validated
- [ ] SQL injection prevented (use Room/parameterized queries)
- [ ] Path traversal attacks prevented
- [ ] Intent extras boundary-checked
- [ ] WebView URLs whitelisted

### Logging
- [ ] No passwords in logs
- [ ] No auth tokens in logs
- [ ] No PII in logs (or hashed/masked)
- [ ] No credit card data logged
- [ ] Debug logs disabled in release builds

### Code Obfuscation
- [ ] ProGuard/R8 enabled for release
- [ ] Sensitive algorithms obfuscated
- [ ] No hardcoded secrets
- [ ] String encryption for critical strings

### Permissions
- [ ] Only necessary permissions requested
- [ ] Runtime permissions properly handled
- [ ] Permission rationale provided
- [ ] No dangerous permissions without justification