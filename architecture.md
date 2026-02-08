# Demo App - Architecture Documentation

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Application Setup](#application-setup)
3. [MVVM Pattern](#mvvm-pattern)
4. [Data Flow](#data-flow)
5. [Navigation System](#navigation-system)
6. [AppPreferences (DataStore)](#apppreferences-datastore)
7. [Dependency Injection](#dependency-injection)
8. [Feature Structure](#feature-structure)

---

## Architecture Overview

Demo app follows **Clean Architecture** principles with a **feature-based** structure and **MVVM** pattern.

### Three-Layer Architecture

```
┌─────────────────────────────────────────┐
│      Presentation Layer (UI)            │
│  • Composable Screens                   │
│  • ViewModels                           │
│  • UI States (Sealed Classes)           │
│  • UI Components                        │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│      Domain Layer (Business Logic)      │
│  • Use Cases                            │
│  • Repository Interfaces                │
│  • Domain Models                        │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│      Data Layer (Data Sources)          │
│  • Repository Implementations           │
│  • API Services (Retrofit)              │
│  • DTOs (Data Transfer Objects)         │
│  • Local Storage (Room/DataStore)       │
└─────────────────────────────────────────┘
```

### Key Principles

- **Separation of Concerns**: Each layer has a single responsibility
- **Dependency Inversion**: Domain layer doesn't depend on data layer
- **Unidirectional Data Flow**: Data flows from Data → Domain → Presentation
- **Reactive State Management**: StateFlow for reactive UI updates
- **Type Safety**: Sealed classes for exhaustive state handling
- **Error Handling**: Result pattern for consistent error management

---

## Application Setup

### 1. DemoApplication - Application Class

**Purpose**: Initialize app-wide configurations, analytics, and services

```kotlin
@HiltAndroidApp
class DemoApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var environmentConfig: EnvironmentConfig

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        // Register CleverTap lifecycle callbacks
        ActivityLifecycleCallback.register(this)
        CleverTapAPI.setNotificationHandler(
            PushTemplateNotificationHandler() as NotificationHandler
        )

        super.onCreate()
        
        // Initialize Timber for logging (debug builds only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize services
        initializeAppCheck()
        initializeAnalytics()
        initializeClarity()
        initializeLocale()
        
        // Configure StrictMode for development
        if (BuildConfig.DEBUG) {
            configureStrictMode()
        }
    }

    private fun initializeAppCheck() {
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            // Debug provider for local/dev builds
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // Play Integrity for production builds
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }

    private fun initializeClarity() {
        val config = ClarityConfig(
            projectId = environmentConfig.clarityProject,
            logLevel = LogLevel.None
        )
        Clarity.initialize(applicationContext, config)
    }

    private fun initializeAnalytics() {
        try {
            analyticsManager.initialize()
            Timber.d("Analytics initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize analytics")
        }
    }

    private fun initializeLocale() {
        applicationScope.launch {
            try {
                // Get saved language from SharedPreferences
                val savedLanguage = appPreferences.getAppLocalData(
                    AppPreferences.LANGUAGE_REFERRER
                )
                
                // Get current locale from AppCompatDelegate
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                val currentLocaleTag = if (currentLocales.isEmpty) {
                    null
                } else {
                    currentLocales.get(0)?.toLanguageTag()
                }
                
                Timber.d("Current locale: $currentLocaleTag")
                Timber.d("Saved locale: $savedLanguage")
                
                if (!savedLanguage.isNullOrBlank()) {
                    val expectedLocaleTag = LocaleManager.languageToLocaleTag(savedLanguage)
                    
                    // Fix locale mismatch (handles device-specific persistence issues)
                    if (currentLocaleTag != expectedLocaleTag) {
                        Timber.w("Locale mismatch! Fixing: $currentLocaleTag -> $expectedLocaleTag")
                        LocaleManager.applyLocale(savedLanguage)
                        
                        // Track for analytics
                        AnalyticsManager.trackEventStatic(
                            "locale_mismatch_fixed",
                            mapOf(
                                "device_manufacturer" to Build.MANUFACTURER,
                                "device_model" to Build.MODEL,
                                "android_version" to Build.VERSION.SDK_INT,
                                "expected_locale" to expectedLocaleTag,
                                "current_locale" to (currentLocaleTag ?: "none")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize locale")
            }
        }
    }

    private fun configureStrictMode() {
        Choreographer.getInstance().postFrameCallback {
            // VM Policy - detect memory/resource leaks
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )

            // Thread Policy - detect network calls on main thread
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
```

**Key Points:**
- `@HiltAndroidApp` enables Hilt dependency injection
- Initialize app-wide services (Firebase, Analytics, Clarity)
- Handle locale persistence across app restarts
- Configure StrictMode for development debugging
- Use Timber for structured logging

### 2. MainActivity - Entry Point

**Purpose**: Handle app entry, splash screen, navigation setup, and deep links

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), CTPushNotificationListener {
    
    private val mainViewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var appPreferences: AppPreferences

    private val intentAnalyzer = IntentAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        // Setup CleverTap push notification listener
        CleverTapAPI.getDefaultInstance(applicationContext)?.apply {
            ctPushNotificationListener = this@MainActivity
        }
        
        // Verify locale on activity creation
        verifyLocale()

        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            when (val currentState = uiState) {
                is MainUiState.Loading -> {
                    keepSplashScreen = true
                }
                is MainUiState.Success -> {
                    LaunchedEffect(Unit) {
                        keepSplashScreen = false
                    }
                    AppNavigation(
                        startDestination = currentState.startDestination
                    )
                }
            }
            
            // Handle intent (deep links, notifications)
            LaunchedEffect(Unit) {
                handleIntent(intent, coldStart = true)
            }
            
            // Track app open times
            LaunchedEffect(Unit) {
                mainViewModel.setAppOpenTimes()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        // Handle CleverTap notification click (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            CleverTapAPI.getDefaultInstance(applicationContext)
                ?.pushNotificationClickedEvent(intent.extras)
        }
        
        setIntent(intent)
        handleIntent(intent, coldStart = false)
    }

    private fun handleIntent(intent: Intent, coldStart: Boolean) {
        val analysis = intentAnalyzer.analyzeIntent(intent)
        Timber.d("Intent extras: ${analysis.extras}")
        Timber.d("Intent URI: ${analysis.uri}")
        
        mainViewModel.handleNotificationClick(analysis.extras)

        when (analysis.type) {
            is IntentAnalyzer.IntentType.DeepLink -> {
                handleDeepLink(analysis.uri!!, coldStart)
            }
            is IntentAnalyzer.IntentType.NormalAppLaunch -> {
                AnalyticsManager.trackEventStatic(
                    eventName = "app_open",
                    mapOf("type" to "direct")
                )
            }
        }
        
        mainViewModel.viewModelScope.launch {
            mainViewModel.saveDeepLink(analysis.uri, "deep_link_click")
            if (coldStart) {
                mainViewModel.fetchAndStoreDeferredFBAppLinkData()
            }
            mainViewModel.checkAuthStatus(coldStart)
        }
    }

    private fun handleDeepLink(uri: String, coldStart: Boolean) {
        AnalyticsManager.trackEventStatic(
            eventName = "app_open",
            mapOf("type" to "deep_link")
        )
    }

    override fun onNotificationClickedPayloadReceived(
        payload: HashMap<String?, in Any>?
    ) {
        try {
            Timber.d("Notification clicked: $payload")
            val title = payload?.get("nt")?.toString() ?: ""
            val body = payload?.get("nm")?.toString() ?: ""

            AnalyticsManager.trackEventStatic(
                eventName = "notification_clicked_ct",
                properties = mapOf(
                    "action_type" to "notification_clicked",
                    "notification_title" to title,
                    "notification_description" to body
                )
            )
        } catch (t: Throwable) {
            Timber.e(t, "Error handling notification click")
        }
    }
    
    private fun verifyLocale() {
        lifecycleScope.launch {
            try {
                val savedLanguage = appPreferences.getAppLocalData(
                    AppPreferences.LANGUAGE_REFERRER
                )
                if (!savedLanguage.isNullOrBlank()) {
                    val currentLocales = AppCompatDelegate.getApplicationLocales()
                    val currentLocaleTag = currentLocales.get(0)?.toLanguageTag()
                    val expectedLocaleTag = LocaleManager.languageToLocaleTag(savedLanguage)
                    
                    if (currentLocaleTag != expectedLocaleTag) {
                        Timber.w("Locale mismatch in MainActivity! Fixing...")
                        LocaleManager.applyLocale(savedLanguage)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to verify locale")
            }
        }
    }
}
```

**Key Points:**
- `@AndroidEntryPoint` enables Hilt injection in Activity
- Splash screen with conditional display
- Deep link and notification handling
- Locale verification on activity creation
- Navigation event observation from ViewModel
- Analytics tracking for app opens

### 3. EnvironmentConfig - Multi-Environment Setup

**Purpose**: Manage different configurations for development, staging, and production

```kotlin
enum class AppEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

data class EnvironmentConfig(
    val environment: AppEnvironment,
    val baseUrl: String,
    val amplitudeApiKey: String,
    val clarityProject: String,
    val appHash: String,
    val mixpanel: String
) {
    val isDevelopment: Boolean get() = environment == AppEnvironment.DEVELOPMENT
    val isStaging: Boolean get() = environment == AppEnvironment.STAGING
    val isProduction: Boolean get() = environment == AppEnvironment.PRODUCTION

    companion object {
        // Automatically select environment based on build type
        val activeEnvironment: AppEnvironment =
            if (BuildConfig.DEBUG) AppEnvironment.DEVELOPMENT 
            else AppEnvironment.PRODUCTION

        fun forEnvironment(env: AppEnvironment): EnvironmentConfig {
            return when (env) {
                AppEnvironment.DEVELOPMENT -> EnvironmentConfig(
                    environment = env,
                    baseUrl = "https://api.dev.Demopapp.in/api/",
                    amplitudeApiKey = "cfa3b327228ec690c70380624bea89d4",
                    clarityProject = "t3v9hqrjae",
                    appHash = "VYR6SEkNRBd",
                    mixpanel = "fdddb6e901ad224f720802b4b2d82f20"
                )
                AppEnvironment.STAGING -> EnvironmentConfig(
                    environment = env,
                    baseUrl = "https://staging-api.Demopapp.in/api/",
                    amplitudeApiKey = "cfa3b327228ec690c70380624bea89d4",
                    clarityProject = "t3v9hqrjae",
                    appHash = "VYR6SEkNRBd",
                    mixpanel = "fdddb6e901ad224f720802b4b2d82f20"
                )
                AppEnvironment.PRODUCTION -> EnvironmentConfig(
                    environment = env,
                    baseUrl = "https://api.Demopapp.in/api/",
                    amplitudeApiKey = "c90cf1319e441df2fa99662e8cb847ee",
                    clarityProject = "swqiod2bly",
                    appHash = "is6J0XIXpNf",
                    mixpanel = "5066c33aeb0a32783c0fa135f1e08bde"
                )
            }
        }

        // Current active configuration
        val current: EnvironmentConfig = forEnvironment(activeEnvironment)
    }
}
```

**Dependency Injection Setup:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideEnvironmentConfig(): EnvironmentConfig =
        EnvironmentConfig.current
}
```

**Usage in Repository/Service:**

```kotlin
@Singleton
class ApiClient @Inject constructor(
    private val environmentConfig: EnvironmentConfig
) {
    val baseUrl: String = environmentConfig.baseUrl
    val isProduction: Boolean = environmentConfig.isProduction
}
```

**Key Points:**
- Automatic environment selection based on `BuildConfig.DEBUG`
- Centralized configuration for all environments
- Type-safe access to environment-specific values
- Easy to add new environments or configuration values
- Injected as singleton throughout the app

---

## MVVM Pattern

### ViewModel Structure

ViewModels manage UI state using **StateFlow** and handle business logic through **Use Cases**.

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questUseCase: QuestUseCase,
    private val dispatcherProvider: DispatcherProvider,
    private val appPreferences: AppPreferences
) : ViewModel() {

    // Private mutable state (internal)
    private val _homeUiState = MutableStateFlow(HomeUiState())
    
    // Public immutable state (exposed to UI)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            // Set loading state
            _homeUiState.value = _homeUiState.value.copy(
                homePageState = HomePageState.Loading
            )

            // Call use case
            when (val result = questUseCase.getHomeData()) {
                is Result.Success -> {
                    _homeUiState.value = _homeUiState.value.copy(
                        homePageState = HomePageState.Success(result.data)
                    )
                }
                is Result.Error -> {
                    _homeUiState.value = _homeUiState.value.copy(
                        homePageState = HomePageState.Error(
                            result.throwable.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }
}
```

### State Management with Sealed Classes

Sealed classes provide **type-safe state handling** with exhaustive pattern matching.

```kotlin
// Sealed class for different UI states
sealed class HomePageState {
    object Loading : HomePageState()
    data class Success(val data: HomePageModel) : HomePageState()
    data class Error(val message: String) : HomePageState()
}

// Data class for overall UI state
data class HomeUiState(
    val homePageState: HomePageState = HomePageState.Loading,
    val userName: String? = null,
    val isSubscribed: Boolean? = null,
    val isProfileLoading: Boolean = false
)
```

### Composable Screen

Screens observe ViewModel state and render UI accordingly.

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.homeUiState.collectAsState()

    when (val state = uiState.homePageState) {
        is HomePageState.Loading -> {
            LoadingComponent()
        }
        is HomePageState.Success -> {
            HomeContent(
                data = state.data,
                onItemClick = { questId ->
                    onNavigateToDetail(questId)
                }
            )
        }
        is HomePageState.Error -> {
            ErrorComponent(
                message = state.message,
                onRetry = { viewModel.loadHomeData() }
            )
        }
    }
}
```

---

## Data Flow

### Complete Data Flow Example

```
User Action (UI)
    ↓
ViewModel.loadHomeData()
    ↓
QuestUseCase.getHomeData()
    ↓
QuestRepository.getHomeData()
    ↓
QuestApiService.getHomeData()
    ↓
API Response (HomePageResponseDto) or SQLite -> Room
    ↓
Transform DTO → Domain Model (HomePageModel) or Transform Entity (Local Storage)
    ↓
Result.Success(HomePageModel)
    ↓
Update StateFlow in ViewModel
    ↓
UI Recomposes with new state
```

### 1. API Service Layer

**Purpose**: Define API endpoints using Retrofit

```kotlin
interface QuestApiService {
    @GET("v1/home-page/get-home-data")
    suspend fun getHomeData(): HomePageResponseDto

    @GET("v1/quests/{quest_id}")
    suspend fun getQuestById(
        @Path("quest_id") questId: String
    ): QuestDto

    @POST("v1/user-actions/quest-started")
    suspend fun questStarted(
        @Body request: QuestStartedRequestDto
    ): QuestStartedResponseDto
}
```

### 2. DTOs (Data Transfer Objects)

**Purpose**: Represent API response structure with serialization

```kotlin
@Serializable
data class HomePageResponseDto(
    @SerialName("joy_audio_url")
    val joyAudioUrl: String? = null,
    
    @SerialName("subtitle_text")
    val subtitleText: String? = null,
    
    @SerialName("progress")
    val progress: List<HomeItemDto>,
    
    @SerialName("no_progress")
    val noProgress: List<HomeItemDto>
)

@Serializable
data class HomeItemDto(
    @SerialName("type")
    val type: String, // "dc" or "mc"
    
    @SerialName("dc")
    val dc: DailyChallengeDto? = null,
    
    @SerialName("mc")
    val mc: MicroCourseDto? = null
)
```

**Key Points:**
- Use `@Serializable` for Kotlinx Serialization
- Use `@SerialName` to map JSON keys to Kotlin properties
- Nullable fields with default values for optional API fields

### 3. Domain Models

**Purpose**: Business logic representation, independent of API structure

```kotlin
@Serializable
data class HomePageModel(
    val homeThread: HomeThreadModel?,
    val joyAudioUrl: String,
    val subtitleText: String,
    val progress: List<HomeItemModel>,
    val noProgress: List<HomeItemModel>
) {
    companion object {
        fun fromDto(dto: HomePageResponseDto): HomePageModel {
            return HomePageModel(
                homeThread = HomeThreadModel.fromDto(dto.homeThread),
                joyAudioUrl = dto.joyAudioUrl ?: "",
                subtitleText = dto.subtitleText ?: "",
                progress = dto.progress.map { HomeItemModel.fromDto(it) },
                noProgress = dto.noProgress.map { HomeItemModel.fromDto(it) }
            )
        }
    }
}

@Serializable
data class HomeItemModel(
    val type: String,
    val dailyChallenge: ChallengeModel? = null,
    val masteryCourse: ChallengeModel? = null,
    val progress: ProgressModel? = null
) {
    companion object {
        fun fromDto(dto: HomeItemDto): HomeItemModel {
            return HomeItemModel(
                type = dto.type,
                dailyChallenge = dto.dc?.let { ChallengeModel.fromDto(it) },
                masteryCourse = dto.mc?.let { ChallengeModel.fromDto(it) },
                progress = dto.progress?.let { ProgressModel.fromDto(it) }
            )
        }
    }
}
```

**Key Points:**
- Domain models are independent of API structure
- Use companion object `fromDto()` for transformation
- Non-nullable fields with sensible defaults

### 4. Result Pattern

**Purpose**: Type-safe error handling without exceptions

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val throwable: Throwable) : Result<Nothing>()

    suspend fun fold(
        onSuccess: suspend (T) -> Unit,
        onFailure: suspend (Throwable) -> Unit
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onFailure(throwable)
        }
    }

    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            is Error -> null
        }
    }
}
```

### 5. Repository Layer

**Repository Interface (Domain Layer):**

```kotlin
interface QuestRepository {
    suspend fun getHomeData(): Result<HomePageModel>
    suspend fun getQuestById(questId: String?): Result<QuestModel?>
    suspend fun questStarted(questId: String): Result<QuestStartedModel>
}
```

**Repository Implementation (Data Layer):**

```kotlin
@Singleton
class QuestRepositoryImpl @Inject constructor(
    private val questApiService: QuestApiService,
    private val errorHandler: ErrorHandler
) : QuestRepository {

    override suspend fun getHomeData(): Result<HomePageModel> {
        return try {
            // 1. Make API call
            val response = questApiService.getHomeData()
            
            // 2. Transform DTO to Domain Model
            val homePageModel = HomePageModel.fromDto(response)
            
            // 3. Return success
            Result.Success(homePageModel)
        } catch (e: Exception) {
            // 4. Handle errors
            Timber.e(e, "Failed to fetch home data")
            val userFriendlyMessage = errorHandler.handleErrorWithFallback(e)
            Result.Error(Exception(userFriendlyMessage))
        }
    }

    override suspend fun getQuestById(questId: String?): Result<QuestModel?> {
        return try {
            val response = questApiService.getQuestById(questId)
            val questModel = QuestModel.fromDTO(response)
            Result.Success(questModel)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch quest: $questId")
            Result.Error(e)
        }
    }
}
```

**Key Points:**
- Repository implements interface from domain layer
- Handles API calls and error handling
- Transforms DTOs to domain models
- Returns `Result<T>` for type-safe error handling
- Uses `@Singleton` for single instance

### 6. Use Case Layer

**Use Case Interface (Domain Layer):**

```kotlin
interface QuestUseCase {
    suspend fun getHomeData(): Result<HomePageModel>
    suspend fun getQuestById(questId: String?): Result<QuestModel?>
    suspend fun questStarted(questId: String): Result<QuestStartedModel>
}
```

**Use Case Implementation (Domain Layer):**

```kotlin
class QuestUseCaseImpl @Inject constructor(
    private val questRepository: QuestRepository,
    private val dispatcherProvider: DispatcherProvider
) : QuestUseCase {

    override suspend fun getHomeData(): Result<HomePageModel> {
        return withIOContext(dispatcherProvider) {
            questRepository.getHomeData()
        }
    }

    override suspend fun getQuestById(questId: String?): Result<QuestModel?> {
        return withIOContext(dispatcherProvider) {
            questRepository.getQuestById(questId)
        }
    }

    override suspend fun questStarted(questId: String): Result<QuestStartedModel> {
        return withIOContext(dispatcherProvider) {
            questRepository.questStarted(questId)
        }
    }
}
```

**Key Points:**
- Use cases contain business logic
- Switch to IO dispatcher for background operations
- Can combine multiple repository calls
- Inject repository interface, not implementation

---

## Navigation System

This app uses **Navigation 3** - Google's latest navigation library built specifically for Jetpack Compose. Navigation 3 gives you full control over the back stack and treats navigation as simple list manipulation.

### Why Navigation 3?

| Feature | Navigation 3 |
|---------|--------------|
| Back stack ownership | You own it (just a `List`) |
| Type safety | ✅ Compile-time checks |
| Adaptive layouts | Built-in support |
| Compose-native | Designed for Compose |
| Complex objects | Direct passing via keys |

### NavigationRoutes.kt - Route Definitions

All navigation keys are defined in a separate file to keep data separate from logic:

```kotlin
// NavigationRoutes.kt
package com.example.demo.navigation

// Simple destinations
data object Login
data object Home
data object Profile

// Destinations with arguments
data class QuestDetail(val questId: String)

data class ChallengeEngagement(
    val dcId: String,
    val dcIndex: Int = 0,
    val dcSection: String = ""
)

// Settings sub-graph
sealed interface SettingsRoute {
    data object Root : SettingsRoute
    data object Account : SettingsRoute
    data object Notifications : SettingsRoute
}
```

### MainActivity.kt - Entry Point

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val mainViewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            when (val currentState = uiState) {
                is MainUiState.Loading -> {
                    keepSplashScreen = true
                }
                is MainUiState.Success -> {
                    LaunchedEffect(Unit) {
                        keepSplashScreen = false
                    }
                    AppNavigation(startDestination = currentState.startDestination)
                }
            }
        }
    }
}
```

### AppNavigation.kt - Navigation Logic

All navigation logic is centralized in a single file:

```kotlin
// AppNavigation.kt
package com.example.demo.navigation

@Composable
fun AppNavigation(startDestination: Any = Login) {
    // You own the back stack - it's just a list!
    val backStack = remember { mutableStateListOf<Any>(startDestination) }
    
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            // Login
            entry<Login> {
                LoginScreen(
                    onLoginSuccess = {
                        backStack.clear()
                        backStack.add(Home)
                    }
                )
            }
            
            // Home
            entry<Home> {
                HomeScreen(
                    onNavigateToQuest = { questId ->
                        backStack.add(QuestDetail(questId))
                    },
                    onNavigateToProfile = {
                        backStack.add(Profile)
                    },
                    onNavigateToSettings = {
                        backStack.add(SettingsRoute.Root)
                    }
                )
            }
            
            // Profile
            entry<Profile> {
                ProfileScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            
            // Quest Detail - with required argument
            entry<QuestDetail> { key ->
                QuestDetailScreen(
                    questId = key.questId,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToChallenge = { dcId, index, section ->
                        backStack.add(ChallengeEngagement(dcId, index, section))
                    }
                )
            }
            
            // Challenge Engagement - with optional arguments
            entry<ChallengeEngagement> { key ->
                ChallengeEngagementScreen(
                    dcId = key.dcId,
                    dcIndex = key.dcIndex,
                    dcSection = key.dcSection,
                    onBack = { backStack.removeLastOrNull() },
                    onComplete = {
                        // Pop back to Home
                        backStack.clear()
                        backStack.add(Home)
                    }
                )
            }
            
            // Settings sub-graph
            entry<SettingsRoute.Root> {
                SettingsRootScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToAccount = { backStack.add(SettingsRoute.Account) },
                    onNavigateToNotifications = { backStack.add(SettingsRoute.Notifications) }
                )
            }
            
            entry<SettingsRoute.Account> {
                AccountSettingsScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            
            entry<SettingsRoute.Notifications> {
                NotificationSettingsScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
```

### Navigation Patterns

**Navigate forward:**
```kotlin
backStack.add(QuestDetail(questId = "quest_123"))
```

**Navigate back:**
```kotlin
backStack.removeLastOrNull()
```

**Clear and navigate (e.g., after login):**
```kotlin
backStack.clear()
backStack.add(Home)
```

**Pop to specific destination:**
```kotlin
while (backStack.lastOrNull() !is Home && backStack.isNotEmpty()) {
    backStack.removeLastOrNull()
}
```

**Replace current destination:**
```kotlin
backStack.removeLastOrNull()
backStack.add(NewDestination)
```

### Passing Complex Objects

Navigation 3 allows passing complex objects directly as key properties:

```kotlin
// In NavigationRoutes.kt - define key with complex object
data class QuestScreen(
    val quest: QuestModel,
    val userProgress: ProgressModel?
)

// Navigate with complex data
backStack.add(
    QuestScreen(
        quest = questModel,
        userProgress = progressModel
    )
)

// Access in entry
entry<QuestScreen> { key ->
    QuestScreenContent(
        quest = key.quest,
        progress = key.userProgress
    )
}
```

### Feature Events Pattern - Bidirectional Communication

Events are defined in `{feature}/presentation/events/` and flow in two directions:

| Event Type | Direction | Purpose |
|------------|-----------|---------|
| `{Feature}EventFromUi` | UI → ViewModel | User actions (clicks, inputs) |
| `{Feature}EventToUi` | ViewModel → UI | Navigation, feedback (snackbar, toast) |

**HomeEventFromUi.kt** - Events triggered from UI, handled by ViewModel:

```kotlin
// features/home/presentation/events/HomeEventFromUi.kt
package com.example.demo.features.home.presentation.events

sealed interface HomeEventFromUi {
    
    // Navigation requests from UI
    sealed interface NavigationEvent : HomeEventFromUi {
        data class OnQuestClicked(val questId: String) : NavigationEvent
        data class OnChallengeClicked(val dcId: String) : NavigationEvent
        data object OnProfileClicked : NavigationEvent
        data object OnBackClicked : NavigationEvent
    }
    
    // Other UI actions
    sealed interface ActionEvent : HomeEventFromUi {
        data class OnSearchQueryChanged(val query: String) : ActionEvent
        data object OnRefreshRequested : ActionEvent
        data class OnQuestBookmarked(val questId: String) : ActionEvent
    }
}
```

**HomeEventToUi.kt** - Events triggered from ViewModel, handled by UI:

```kotlin
// features/home/presentation/events/HomeEventToUi.kt
package com.example.demo.features.home.presentation.events

sealed interface HomeEventToUi {
    
    // Navigation commands to UI
    sealed interface NavigationEvent : HomeEventToUi {
        data class GoToQuestDetail(val questId: String) : NavigationEvent
        data class GoToChallenge(val dcId: String, val index: Int = 0) : NavigationEvent
        data object GoToProfile : NavigationEvent
        data object GoBack : NavigationEvent
    }
    
    // Feedback events for UI
    sealed interface FeedbackEvent : HomeEventToUi {
        data class ShowSnackbar(val message: String) : FeedbackEvent
        data class ShowToast(val message: String) : FeedbackEvent
        data object ShowRatingDialog : FeedbackEvent
    }
}
```

**ViewModel - Receives EventFromUi, Emits EventToUi:**

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questUseCase: QuestUseCase
) : ViewModel() {

    // Events TO UI (ViewModel → UI)
    private val _eventToUi = MutableSharedFlow<HomeEventToUi>()
    val eventToUi = _eventToUi.asSharedFlow()

    // Handle events FROM UI (UI → ViewModel)
    fun onEvent(event: HomeEventFromUi) {
        when (event) {
            // Navigation events from UI
            is HomeEventFromUi.NavigationEvent.OnQuestClicked -> {
                viewModelScope.launch {
                    _eventToUi.emit(HomeEventToUi.NavigationEvent.GoToQuestDetail(event.questId))
                }
            }
            is HomeEventFromUi.NavigationEvent.OnChallengeClicked -> {
                viewModelScope.launch {
                    _eventToUi.emit(HomeEventToUi.NavigationEvent.GoToChallenge(event.dcId))
                }
            }
            HomeEventFromUi.NavigationEvent.OnProfileClicked -> {
                viewModelScope.launch {
                    _eventToUi.emit(HomeEventToUi.NavigationEvent.GoToProfile)
                }
            }
            HomeEventFromUi.NavigationEvent.OnBackClicked -> {
                viewModelScope.launch {
                    _eventToUi.emit(HomeEventToUi.NavigationEvent.GoBack)
                }
            }
            
            // Action events from UI
            is HomeEventFromUi.ActionEvent.OnSearchQueryChanged -> {
                // Update search state
            }
            HomeEventFromUi.ActionEvent.OnRefreshRequested -> {
                loadHomeData()
            }
            is HomeEventFromUi.ActionEvent.OnQuestBookmarked -> {
                bookmarkQuest(event.questId)
            }
        }
    }
    
    private fun bookmarkQuest(questId: String) {
        viewModelScope.launch {
            when (val result = questUseCase.bookmarkQuest(questId)) {
                is Result.Success -> {
                    _eventToUi.emit(HomeEventToUi.FeedbackEvent.ShowSnackbar("Quest bookmarked!"))
                }
                is Result.Error -> {
                    _eventToUi.emit(HomeEventToUi.FeedbackEvent.ShowToast("Failed to bookmark"))
                }
            }
        }
    }
}
```

**UI - Sends EventFromUi, Listens to EventToUi:**

```kotlin
entry<Home> {
    val viewModel: HomeViewModel = hiltViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Listen to events FROM ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventToUi.collect { event ->
            when (event) {
                // Handle navigation
                is HomeEventToUi.NavigationEvent.GoToQuestDetail -> {
                    backStack.add(QuestDetail(event.questId))
                }
                is HomeEventToUi.NavigationEvent.GoToChallenge -> {
                    backStack.add(ChallengeEngagement(event.dcId, event.index))
                }
                HomeEventToUi.NavigationEvent.GoToProfile -> {
                    backStack.add(Profile)
                }
                HomeEventToUi.NavigationEvent.GoBack -> {
                    backStack.removeLastOrNull()
                }
                
                // Handle feedback
                is HomeEventToUi.FeedbackEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is HomeEventToUi.FeedbackEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                HomeEventToUi.FeedbackEvent.ShowRatingDialog -> {
                    // Show rating dialog
                }
            }
        }
    }
    
    // Pass event handler to screen
    HomeScreen(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent  // UI sends events via this
    )
}
```

**HomeScreen - Triggers EventFromUi:**

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onEvent: (HomeEventFromUi) -> Unit
) {
    val uiState by viewModel.homeUiState.collectAsState()
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(uiState.quests) { quest ->
                QuestCard(
                    quest = quest,
                    onClick = { 
                        onEvent(HomeEventFromUi.NavigationEvent.OnQuestClicked(quest.id))
                    },
                    onBookmark = {
                        onEvent(HomeEventFromUi.ActionEvent.OnQuestBookmarked(quest.id))
                    }
                )
            }
        }
    }
}
```

### Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                           UI Layer                               │
│  ┌─────────────┐                           ┌─────────────────┐  │
│  │ HomeScreen  │──EventFromUi.OnClick────→│ AppNavigation   │  │
│  │             │←──EventToUi.Navigate─────│ (LaunchedEffect)│  │
│  └─────────────┘                           └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↑ ↓
                    EventFromUi │ │ EventToUi
                              ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                        ViewModel Layer                           │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ HomeViewModel                                            │    │
│  │   fun onEvent(event: HomeEventFromUi)  ← receives       │    │
│  │   val eventToUi: SharedFlow            → emits          │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```
```

### Adaptive Layouts (Tablet/Desktop)

Navigation 3 has built-in support for multi-pane layouts:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    sceneStrategy = TwoPaneSceneStrategy(
        paneStrategy = { entry ->
            when (entry.key) {
                is Home -> PaneStrategy.Main
                is QuestDetail -> PaneStrategy.Detail
                else -> PaneStrategy.Main
            }
        }
    ),
    entryProvider = { ... }
)
```

### Deep Link Handling

```kotlin
// In MainActivity
private fun handleDeepLink(uri: Uri) {
    val destination = when {
        uri.pathSegments.contains("quest") -> {
            val questId = uri.lastPathSegment ?: return
            QuestDetail(questId)
        }
        uri.pathSegments.contains("profile") -> Profile
        else -> Home
    }
    
    mainViewModel.navigateTo(destination)
}
```

### Navigation DI Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideStartDestination(
        appPreferences: AppPreferences
    ): Any {
        return runBlocking {
            val isLoggedIn = appPreferences.getAppLocalData(
                AppPreferences.AUTH_TOKEN
            ) != null
            if (isLoggedIn) Home else Login
        }
    }
}
```

---

## AppPreferences (DataStore)

### Implementation

**Purpose**: Persistent key-value storage using Jetpack DataStore

```kotlin
@Singleton
class AppPreferences @Inject constructor(
    @Named("app_pref") private val appDataStore: DataStore<Preferences>,
    private val dispatcherProvider: DispatcherProvider,
    private val json: Json
) {
    companion object PreferencesKeys {
        const val DEFERRED_DEEP_LINK = "deferred_deeplink"
        const val USER_NAME_REFERRER = "user_name_referrer"
        const val LANGUAGE_REFERRER = "language_referrer"
        const val ONBOARDING_DATA = "onboarding_data"
        const val APP_OPEN_TIMES_REFERRER = "app_open_times_referrer"
    }

    // Get string data
    suspend fun getAppLocalData(key: String): String? {
        return withIOContext(dispatcherProvider) {
            val preferenceKey = stringPreferencesKey(key)
            appDataStore.data
                .catch { emit(emptyPreferences()) }
                .map { preferences -> preferences[preferenceKey] }
                .first()
        }
    }

    // Set string data
    suspend fun setAppLocalData(key: String, value: String) {
        return withIOContext(dispatcherProvider) {
            val preferenceKey = stringPreferencesKey(key)
            appDataStore.edit { preferences ->
                preferences[preferenceKey] = value
            }
        }
    }

    // Store complex objects using JSON serialization
    suspend fun saveOnboardingData(data: OnboardingDataModel) {
        return withIOContext(dispatcherProvider) {
            val jsonString = json.encodeToString(data)
            setAppLocalData(ONBOARDING_DATA, jsonString)
        }
    }

    suspend fun getOnboardingData(): OnboardingDataModel? {
        return withIOContext(dispatcherProvider) {
            try {
                val jsonString = getAppLocalData(ONBOARDING_DATA)
                jsonString?.let { json.decodeFromString<OnboardingDataModel>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Clear specific key
    suspend fun clearSpecificAppLocalData(key: String): Boolean {
        return withIOContext(dispatcherProvider) {
            val preferenceKey = stringPreferencesKey(key)
            var wasFound = false
            appDataStore.edit { preferences ->
                if (preferences.contains(preferenceKey)) {
                    preferences.remove(preferenceKey)
                    wasFound = true
                }
            }
            wasFound
        }
    }

    // Clear all data
    suspend fun clearAllAppLocalData() {
        return withIOContext(dispatcherProvider) {
            appDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
}
```

### DataStore DI Setup

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataSourcePrefsModule {

    @Provides
    @Singleton
    @Named("app_pref")
    fun provideAppDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("app_preferences")
        }
    }

    @Provides
    @Singleton
    @Named("auth_pref")
    fun provideAuthDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("auth_preferences")
        }
    }
}
```

### Usage in ViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    fun saveUserName(name: String) {
        viewModelScope.launch {
            appPreferences.setAppLocalData(
                AppPreferences.USER_NAME_REFERRER,
                name
            )
        }
    }

    fun getUserName() {
        viewModelScope.launch {
            val name = appPreferences.getAppLocalData(
                AppPreferences.USER_NAME_REFERRER
            )
            // Use name
        }
    }
}
```

---

## Dependency Injection

### Application Class

```kotlin
@HiltAndroidApp
class DemoApplication : Application() {
    
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        initializeAnalytics()
        initializeLocale()
    }
}
```

### Feature DI Module

**Provides Module (for concrete classes):**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    @Singleton
    fun provideQuestApiService(retrofit: Retrofit): QuestApiService {
        return retrofit.create(QuestApiService::class.java)
    }
}
```

**Binds Module (for interfaces):**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeBindsModule {

    @Binds
    @Singleton
    abstract fun bindQuestRepository(
        impl: QuestRepositoryImpl
    ): QuestRepository

    @Binds
    @Singleton
    abstract fun bindQuestUseCase(
        impl: QuestUseCaseImpl
    ): QuestUseCase
}
```

### ViewModel Injection

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questUseCase: QuestUseCase,
    private val dispatcherProvider: DispatcherProvider,
    private val appPreferences: AppPreferences
) : ViewModel() {
    // ViewModel implementation
}
```

### Activity/Fragment Injection

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationCache: NavigationCache

    @Inject
    lateinit var appPreferences: AppPreferences

    // Activity implementation
}
```

---

## Feature Structure

### Complete Feature Structure

```
features/{feature_name}/
├── data/
│   ├── api/
│   │   ├── {feature}_api_service.kt
│   │   └── dto/
│   │       ├── {feature}_request_dto.kt
│   │       └── {feature}_response_dto.kt
│   ├── local/                    # Optional: if feature needs local storage
│   │   ├── {feature}_dao.kt
│   │   └── {feature}_entity.kt
│   └── repository/
│       └── {feature}_repository_impl.kt
├── domain/
│   ├── model/
│   │   └── {feature}_model.kt
│   ├── repository/
│   │   └── {feature}_repository.kt
│   └── usecase/
│       ├── {feature}_usecase.kt
│       └── {feature}_usecase_impl.kt
├── presentation/
│   ├── events/
│   │   ├── {Feature}EventFromUi.kt   # UI → ViewModel events
│   │   └── {Feature}EventToUi.kt     # ViewModel → UI events
│   ├── components/
│   │   └── {feature}_components.kt
│   ├── screen/
│   │   └── {feature}_screen.kt
│   ├── states/
│   │   └── {feature}_state.kt
│   └── viewmodel/
│       └── {feature}_viewmodel.kt
└── di/
    └── {feature}_module.kt
```

### Example: Home Feature

```
features/home/
├── data/
│   ├── api/
│   │   ├── QuestDatabase.kt
│   │   ├── QuestApiService.kt
│   │   └── dto/
│   │       ├── HomePageResponseDto.kt
│   │       ├── HomeItemDto.kt
│   │       └── QuestDto.kt
│   ├── local/                    # Optional: if feature needs local storage
│   │   ├── QuestDao.kt
│   │   └── QuestEntity.kt
│   └── repository/
│       └── QuestRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── HomePageModel.kt
│   │   ├── HomeItemModel.kt
│   │   └── QuestModel.kt
│   ├── repository/
│   │   └── QuestRepository.kt
│   └── usecase/
│       ├── QuestUseCase.kt
│       └── QuestUseCaseImpl.kt
├── presentation/
│   ├── events/
│   │   ├── HomeEventFromUi.kt    # OnQuestClicked, OnRefresh, etc.
│   │   └── HomeEventToUi.kt      # GoToQuestDetail, ShowSnackbar, etc.
│   ├── components/
│   │   ├── HomeCard.kt
│   │   └── QuestItem.kt
│   ├── screen/
│   │   ├── HomeScreen.kt
│   │   └── QuestDetailScreen.kt
│   ├── states/
│   │   └── home_states.kt
│   └── viewmodel/
│       └── HomeViewModel.kt
└── di/
    └── HomeModule.kt
```

---

## Best Practices

### 1. State Management
- Use sealed classes for all UI states
- Always emit loading state before async operations
- Provide meaningful error messages
- Use immutable data classes

### 2. Error Handling
- Use Result pattern for all fallible operations
- Wrap all async operations in try-catch
- Log errors with Timber
- Provide user-friendly error messages

### 3. Navigation
- Define all route keys in `NavigationRoutes.kt`
- Keep navigation logic in `AppNavigation.kt`
- Use `{Feature}EventFromUi` for UI → ViewModel events (user actions)
- Use `{Feature}EventToUi` for ViewModel → UI events (navigation, feedback)
- Define events in `{feature}/presentation/events/`
- Pass complex objects directly via route key properties

### 4. Dependency Injection
- Use constructor injection
- Mark repositories with @Singleton
- Use @Binds for interfaces
- Use @Provides for concrete classes

### 5. Data Layer
- Transform DTOs to domain models in repository
- Use @Serializable for JSON serialization
- Handle null values with defaults
- Separate API structure from domain structure

### 6. File Organization
- Put each DTO in its own file: `data/dto/{DtoName}.kt`
- Put each domain model in its own file: `domain/model/{ModelName}.kt`
- Put each UI component in its own file: `presentation/components/{ComponentName}.kt`
- Avoid combining multiple classes/data classes in a single file

### 7. Testing
- Mock dependencies in tests
- Test ViewModels with Turbine
- Test repositories with mock API services
- Test state transitions

---

## Summary

This architecture ensures:

✅ **Maintainability**: Clear separation of concerns  
✅ **Scalability**: Feature-based structure  
✅ **Testability**: Constructor injection and interfaces  
✅ **Type Safety**: Sealed classes and Result pattern  
✅ **Reactive UI**: StateFlow for unidirectional data flow  
✅ **Error Handling**: Consistent error management  
✅ **Code Reusability**: Composable components and use cases
