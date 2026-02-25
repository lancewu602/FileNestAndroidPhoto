# AGENTS.md

## Build Commands

### General Build
```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Running Tests
Note: No test directory exists yet. When tests are added, run:
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :app:test

# Run a single test class
./gradlew :app:test --tests "com.filenest.photo.ExampleTest"

# Run a single test method
./gradlew :app:test --tests "com.filenest.photo.ExampleTest.testSpecificMethod"
```

### Lint & Formatting
```bash
# Run lint
./gradlew lint

# Check for Kotlin code style violations
./gradlew ktlintCheck

# Format code
./gradlew ktlintFormat
```

## Code Style Guidelines

### Package Structure
Base package: `com.filenest.photo`

Subpackages:
- `ui` - Jetpack Compose screens
- `viewmodel` - ViewModels
- `data` - DataStore, database
- `data.api` - Retrofit API services
- `data.dao` - Room DAO interfaces
- `data.entity` - Room entities
- `theme` - Compose theming

### Naming Conventions

**Classes**: PascalCase
```kotlin
class MainActivity : ComponentActivity()
class MainViewModel : ViewModel()
data class LogEntity(val id: Long)
```

**Functions/methods**: camelCase
```kotlin
fun getUsername(context: Context): Flow<String>
suspend fun save(entity: LogEntity): Long
```

**Composable functions**: camelCase with descriptive names
```kotlin
@Composable
fun MainScreen() { }
@Composable
fun BrowseScreen() { }
```

**Variables**: camelCase
```kotlin
val navController = rememberNavController()
val startDestination = "login"
```

**Constants**: UPPER_SNAKE_CASE
```kotlin
private val LightColorScheme = lightColorScheme()
val DATABASE_NAME = "db_001"
```

**Database entities**: PascalCase table names in code, lowercase in annotation
```kotlin
@Entity(tableName = "log")
data class LogEntity(val message: String)
```

**Database columns**: lowercase snake_case
```kotlin
@ColumnInfo(name = "message")
val message: String
```

**Database files**: descriptive with prefix/numbers
- Database: `db_XXX` (e.g., "db_001")
- DataStore: `app_prefs_XXX` (e.g., "app_prefs_001")

### Import Organization
```kotlin
// Standard library
import kotlinx.coroutines.flow.Flow

// Android framework
import android.content.Context

// AndroidX
import androidx.lifecycle.ViewModel
import androidx.room.Dao

// Jetpack Compose
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Material
import androidx.compose.material3.Text

// Third-party libraries
import dagger.hilt.android.AndroidEntryPoint
import io.coil-kt.coil.compose.AsyncImage

// Project imports
import com.filenest.photo.data.api.ApiService
import com.filenest.photo.ui.theme.MainTheme
```

### Dependency Injection (Hilt)

**Application class**:
```kotlin
@HiltAndroidApp
class MainApplication : Application()
```

**ViewModels**:
```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel()
```

**Modules**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase { }

    @Provides
    fun provideLogDao(db: AppDatabase): LogDao { }
}
```

**Activities/Fragments**:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

### Architecture Patterns

**MVVM with Compose**:
- Activities/Compose screens observe ViewModel state
- ViewModels injected with @HiltViewModel
- Coroutines Flows for reactive data streams
- suspend functions for async operations

**Database (Room)**:
- @Entity for data classes
- @Dao interfaces with Flow returns
- suspend for insert/update/delete operations
- @TypeConverters for complex types

**Preferences (DataStore)**:
- Extension property on Context for DataStore instance
- object class for preference keys with get/set functions
- Flow for reading preferences
- suspend for writing preferences

**API (Retrofit)**:
- Interface for API endpoints
- Generic Ret<T> wrapper for responses
- Helper functions: isRetOk(), retMsg()

### Error Handling

```kotlin
try {
    resetApiService(domain, token)
} catch (e: Exception) {
    startDestination = "login"
}
```

Use try-catch for operations that may fail. Provide graceful fallbacks.

### Jetpack Compose Patterns

**Composable structure**:
```kotlin
@Composable
fun ScreenName() {
    val navController = rememberNavController()
    val viewModel: ScreenViewModel = hiltViewModel()

    ScreenContent(
        state = viewModel.state.collectAsState(),
        onClick = { viewModel.onEvent(EventType) }
    )
}
```

**Modifiers always come first**:
```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
)
```

### Navigation

Use sealed classes for type-safe routes:
```kotlin
sealed class Screen(val route: String, val title: String) {
    data object Browse : Screen("browse", "浏览")
    data object Sync : Screen("sync", "同步")
}
```

### Kotlin Style

- Kotlin code style: `official` (set in gradle.properties)
- JVM Target: Java 11
- Use `data class` for immutable data models
- Use `val` by default, `var` only when mutable state is needed
- Prefer expression functions for simple logic

### Git Ignore

.gradle/, .idea/, /build, /captures, local.properties, .DS_Store, *.iml

## Tech Stack

- **UI**: Jetpack Compose + Material Design 3
- **DI**: Hilt
- **Database**: Room
- **Preferences**: DataStore
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Navigation**: Navigation Compose
- **Coroutines**: kotlinx.coroutines
