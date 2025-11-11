# 🏎️ F1 Tracker - Android App

A modern Android application for tracking Formula 1 races, results, and telemetry data using Kotlin and Jetpack Compose.

## 🚀 Features

- **Real-time Race Data**: Live race results and timing
- **Season Schedule**: Complete F1 calendar with all race events
- **Telemetry Visualization**: Detailed lap-by-lap telemetry data with charts
- **Driver Standings**: Championship standings and points
- **Material Design 3**: Modern, beautiful UI with F1 branding

## 🏗️ Architecture

This app follows **Clean Architecture** principles with **MVVM** pattern:

```
📦 com.f1tracker
├── 📂 data
│   ├── 📂 local (Room Database)
│   │   ├── 📂 dao
│   │   └── 📂 entity
│   ├── 📂 remote (Retrofit API)
│   │   └── 📂 dto
│   └── 📂 repository
├── 📂 domain
│   └── 📂 model
├── 📂 di (Hilt Dependency Injection)
├── 📂 ui
│   ├── 📂 screens
│   └── 📂 theme
└── 📂 viewmodel
```

## 🛠️ Tech Stack

### Core
- **Language**: Kotlin 1.9.20
- **UI**: Jetpack Compose (Material 3)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

### Architecture & DI
- **Architecture Pattern**: MVVM + Repository
- **Dependency Injection**: Hilt/Dagger
- **Navigation**: Jetpack Navigation Compose

### Networking
- **HTTP Client**: Retrofit 2.9.0
- **JSON Parsing**: Gson
- **Network Logging**: OkHttp Interceptor

### Database
- **Local Cache**: Room Database
- **Coroutines**: Kotlin Coroutines for async operations

### Visualization
- **Charts**: Vico Charts Library (for telemetry)
- **Image Loading**: Coil

## 🌐 Backend API

**Base URL**: `https://5n9b86y4sb.execute-api.ap-south-1.amazonaws.com`

### Available Endpoints

1. **Health Check**
   ```
   GET /health
   ```

2. **Season Schedule**
   ```
   GET /schedule?year=2024
   ```

3. **Session Results**
   ```
   GET /session-results?year=2024&gp=Bahrain&session=R
   ```
   - Session types: `R` (Race), `Q` (Qualifying), `FP1`, `FP2`, `FP3`, `S` (Sprint)

4. **Lap Telemetry**
   ```
   GET /lap-telemetry?year=2024&gp=Bahrain&session=R&driver=VER&lap=1
   ```

## 📱 Getting Started

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 17
- Android SDK with API 34

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd "BOXBOXBOX APP"
   ```

2. **Open in Android Studio**
   - File → Open → Select project directory

3. **Sync Gradle**
   - Android Studio will automatically sync dependencies

4. **Run the app**
   - Select emulator or physical device
   - Click Run (▶️) or `Shift + F10`

## 📂 Project Structure

```
BOXBOXBOX APP/
├── app/
│   ├── build.gradle.kts           # App-level Gradle config
│   ├── proguard-rules.pro         # ProGuard configuration
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/f1tracker/
│           │   ├── MainActivity.kt
│           │   ├── F1TrackerApplication.kt
│           │   ├── data/
│           │   │   ├── local/
│           │   │   │   ├── F1Database.kt
│           │   │   │   ├── dao/
│           │   │   │   │   ├── RaceEventDao.kt
│           │   │   │   │   └── SessionResultDao.kt
│           │   │   │   └── entity/
│           │   │   │       ├── RaceEventEntity.kt
│           │   │   │       └── SessionResultEntity.kt
│           │   │   ├── remote/
│           │   │   │   ├── FastF1Api.kt
│           │   │   │   ├── ApiClient.kt
│           │   │   │   └── dto/
│           │   │   │       └── ApiResponses.kt
│           │   │   └── repository/
│           │   │       └── F1Repository.kt
│           │   ├── domain/
│           │   │   └── model/
│           │   │       ├── ApiResult.kt
│           │   │       ├── RaceEvent.kt
│           │   │       ├── DriverResult.kt
│           │   │       └── TelemetryData.kt
│           │   ├── di/
│           │   │   └── AppModule.kt
│           │   ├── ui/
│           │   │   ├── screens/
│           │   │   │   └── HomeScreen.kt
│           │   │   └── theme/
│           │   │       ├── Color.kt
│           │   │       ├── Theme.kt
│           │   │       └── Type.kt
│           │   └── viewmodel/
│           │       └── MainViewModel.kt
│           └── res/
│               ├── values/
│               │   ├── strings.xml
│               │   ├── colors.xml
│               │   └── themes.xml
│               └── xml/
│                   ├── backup_rules.xml
│                   └── data_extraction_rules.xml
├── build.gradle.kts              # Project-level Gradle config
├── settings.gradle.kts           # Gradle settings
├── gradle.properties             # Gradle properties
├── .gitignore
└── README.md
```

## 🎨 Design System

### F1 Brand Colors

- **Primary Red**: `#E10600`
- **Dark Red**: `#9C0000`
- **F1 Black**: `#15151E`

### Team Colors

All 10 F1 teams have their official colors defined in `Color.kt`:
- Red Bull Racing: `#3671C6`
- Ferrari: `#E8002D`
- Mercedes: `#27F4D2`
- McLaren: `#FF8000`
- And more...

## 🔒 Permissions

The app requires the following permissions:
- `INTERNET` - For API calls
- `ACCESS_NETWORK_STATE` - To check network connectivity

## 📦 Dependencies

### Key Libraries

```kotlin
// Compose & Material 3
implementation("androidx.compose.material3:material3:1.1.2")

// Retrofit & OkHttp
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Hilt Dependency Injection
implementation("com.google.dagger:hilt-android:2.48")
ksp("com.google.dagger:hilt-compiler:2.48")

// Vico Charts
implementation("com.patrykandpatrick.vico:compose:1.13.1")

// Coil Image Loading
implementation("io.coil-kt:coil-compose:2.5.0")
```

## 🚧 Development Roadmap

### ✅ Phase 1: Basic Setup (Completed)
- [x] Project structure
- [x] MVVM architecture
- [x] Retrofit API integration
- [x] Room database setup
- [x] Hilt dependency injection
- [x] Material 3 theming
- [x] Basic "Hello World" screen

### 📝 Phase 2: Core Features (Next)
- [ ] Race calendar screen
- [ ] Live race results
- [ ] Driver standings
- [ ] Constructor standings
- [ ] Session details screen

### 🎯 Phase 3: Advanced Features
- [ ] Telemetry visualization with charts
- [ ] Lap comparison
- [ ] Real-time timing
- [ ] Weather data integration
- [ ] Pit stop analysis

### 🌟 Phase 4: Premium Features
- [ ] Offline mode with full caching
- [ ] Push notifications for race starts
- [ ] Dark/Light theme toggle
- [ ] Driver profiles
- [ ] Historical data and records

## 📝 Notes

- The app uses **Hilt** for dependency injection
- API responses are cached in **Room** database
- Network calls are made using **Kotlin Coroutines**
- UI is built with **Jetpack Compose** (no XML layouts)
- Follows **Material Design 3** guidelines

## 🤝 Contributing

This is a personal project. Feel free to fork and customize for your own use!

## 📄 License

This project is for educational purposes.

## 🏁 Ready to Race!

The app is now set up and ready for feature development. Open in Android Studio and start building! 🏎️💨

---

**Built with ❤️ for F1 fans**

