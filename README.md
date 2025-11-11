# BOXBOXBOX

Formula 1 tracking application for Android.

## Features

- Live timing with animated position changes
- Race schedules (upcoming and completed)
- Driver standings
- Constructor standings  
- F1 news from ESPN
- Auto-scrolling stats cards

## Screenshots

![Home Screen](screenshots/home.png)

## Data Sources

- [Ergast F1 API](http://ergast.com/mrd/) - Race data, schedules, standings
- [ESPN F1 API](https://site.api.espn.com) - News articles
- [flagcdn.com](https://flagcdn.com) - Country flags

## Tech Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- Retrofit
- Coil
- Coroutines

## Installation

### From Release
Download the latest APK from [Releases](https://github.com/ashwkun/BOXBOXBOX-F1/releases)

### Build from Source
```bash
git clone https://github.com/ashwkun/BOXBOXBOX-F1.git
cd "BOXBOXBOX APP"
./gradlew assembleDebug
```

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection

## Project Structure

```
app/
├── data/
│   ├── api/          # Retrofit services
│   ├── local/        # Local data providers
│   └── models/       # Data models
├── ui/
│   ├── components/   # Reusable UI components
│   ├── screens/      # App screens
│   └── viewmodels/   # ViewModels
└── res/
    ├── drawable/     # Team logos, circuit maps
    └── font/         # Custom fonts
```

## License

Educational purposes only.
