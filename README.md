<div align="center">

# 🏎️ BOXBOXBOX
### The Ultimate Formula 1 Companion App

**Real-time F1 tracking, live timing, race schedules, standings, and more — all in one sleek Android app.**

[![Release](https://img.shields.io/github/v/release/ashwkun/BOXBOXBOX-F1?style=for-the-badge&label=Latest%20Release)](https://github.com/ashwkun/BOXBOXBOX-F1/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg?style=for-the-badge&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Educational-lightgrey.svg?style=for-the-badge)](LICENSE)

[📥 **Download Latest APK**](https://github.com/ashwkun/BOXBOXBOX-F1/releases/latest) • [📸 Screenshots](#-screenshots) • [✨ Features](#-features) • [🛠️ Build](#-build-from-source)

---

</div>

## 🚀 Quick Start

<div align="center">

### **Get Started in Seconds**

[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen.svg?style=for-the-badge&logo=android)](https://github.com/ashwkun/BOXBOXBOX-F1/releases/latest)

**Latest Version: v1.2** | Requires Android 8.0+

</div>

---

## ✨ Features

<div align="center">

### **Everything You Need for F1 Weekend** 🏁

</div>

### ⚡ **Real-Time Live Timing**
- 🟢 **Live F1 positions** with SignalR WebSocket integration
- 📊 **Driver positions, lap times, gaps, and pit stops** in real-time
- 🔌 Connection status indicators
- 📱 Terminal-style RAW data view
- ⚡ Automatic updates during live sessions

### 🏎️ **Race Weekend Tracking**
- 📅 **Complete session schedules** (Practice, Qualifying, Sprint, Race)
- ✅ **Session completion markers** with visual indicators
- ⏱️ **Real-time countdown timers** for upcoming sessions
- 🌤️ **Weather forecasts** for each session
- 🗺️ **Circuit track layouts** with country flag gradients

### 📊 **Standings & Results**
- 🥇 **Driver standings** with team colors
- 🏭 **Constructor standings**
- 🏆 **Last race results** with top 3 podium
- 🎯 **Completed qualifying and sprint results** during race weekends

### 📰 **F1 Content Hub**
- 📰 **F1 news** from ESPN
- ▶️ **YouTube highlights** with in-app video player
- 🎧 **F1 podcasts** with in-app audio player
  - The Fast and the Curious
  - The F1 Podcast
  - Autosport F1 Podcast

### 🎨 **Beautiful Design**
- 🌙 **Dark theme** with sharp, modern aesthetic
- ✨ **Smooth animations** and transitions
- 📱 **Auto-scrolling stats cards**
- 🔤 **Custom fonts** (Brigends Expanded, Michroma)
- 🎯 **Material Design 3** principles

---

## 📸 Screenshots



---

## 🛠️ Tech Stack

<div align="center">

Built with modern Android technologies

</div>

| Category | Technology |
|----------|-----------|
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin) |
| **UI Framework** | ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5-blue?logo=android) |
| **Networking** | ![Retrofit](https://img.shields.io/badge/Retrofit-2.9-red) • ![OkHttp](https://img.shields.io/badge/OkHttp-4.12-green) |
| **Media** | ![ExoPlayer](https://img.shields.io/badge/ExoPlayer-3.19-orange) • ![Coil](https://img.shields.io/badge/Coil-2.5-purple) |
| **Async** | ![Coroutines](https://img.shields.io/badge/Coroutines-1.7-green) • ![Flow](https://img.shields.io/badge/StateFlow-1.7-blue) |

**Key Libraries:**
- **Retrofit** - REST API client
- **OkHttp** - WebSocket client (SignalR)
- **SimpleXML** - XML/RSS parsing
- **ExoPlayer (AndroidX Media3)** - Audio/video playback
- **Coil** - Image loading
- **Coroutines & Flow** - Asynchronous programming
- **StateFlow** - Reactive state management

---

## 📡 Data Sources

<div align="center">

Powered by official and reliable F1 data sources

</div>

| Source | Purpose |
|--------|---------|
| [Ergast F1 API](http://ergast.com/mrd/) | Race data, schedules, standings, results |
| [Planet F1 Live Timing](https://live.planetf1.com) | Real-time live timing via SignalR WebSocket |
| [ESPN F1 API](https://site.api.espn.com) | News articles |
| [Open-Meteo Weather API](https://open-meteo.com) | Weather forecasts for sessions |
| [flagcdn.com](https://flagcdn.com) | Country flags |
| YouTube RSS | Video highlights |
| Podcast RSS | F1 podcasts |

---

## 📥 Installation

### **Option 1: Download APK (Recommended)**

<div align="center">

[![Download Latest APK](https://img.shields.io/badge/Download-v1.2-APK-brightgreen.svg?style=for-the-badge&logo=android)](https://github.com/ashwkun/BOXBOXBOX-F1/releases/latest)

1. Download the latest APK from [Releases](https://github.com/ashwkun/BOXBOXBOX-F1/releases/latest)
2. Enable "Install from Unknown Sources" on your Android device
3. Open the APK file and install
4. Enjoy! 🎉

</div>

### **Option 2: Build from Source**

   ```bash
# Clone the repository
git clone https://github.com/ashwkun/BOXBOXBOX-F1.git
   cd "BOXBOXBOX APP"

# Build debug APK
./gradlew assembleDebug

# Or build release APK
./gradlew assembleRelease
```

The APK will be located at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## ⚙️ Requirements

- **Android 8.0 (API 26)** or higher
- **Internet connection** for live data
- **Storage:** ~50MB for app installation

---

## 📋 Changelog

### 🎉 **v1.2 (Latest)** - Live Timing Update
- ✨ **Real-time F1 Live Timing** with SignalR WebSocket integration
- ✨ **Session completion markers** with green gradient theme
- ✨ **Weather data caching** (no loss on screen off/on)
- 🎨 Redesigned Live Timing page with connection status
- 🎨 Short driver codes (VER, HAM, LEC, etc.)
- 🎨 Fixed card heights for consistent display
- 🎨 Improved countdown with 00 padding
- 🔧 Enhanced state management and error handling

### 🚀 **v1.1** - Initial Release
- Core features and functionality
- Race schedules and standings
- News and content sections

[View full changelog →](https://github.com/ashwkun/BOXBOXBOX-F1/releases)

---

## 📁 Project Structure

```
app/
├── data/
│   ├── api/          # Retrofit services
│   ├── local/        # Local data providers
│   ├── live/         # SignalR WebSocket client
│   └── models/       # Data models
├── ui/
│   ├── components/   # Reusable UI components
│   ├── screens/      # App screens
│   ├── viewmodels/   # ViewModels
│   └── theme/        # Theme configuration
└── res/
    ├── drawable/     # Team logos, circuit maps
    └── font/         # Custom fonts
```

---

## 🤝 Contributing

Contributions are welcome! If you'd like to contribute:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is for **educational purposes only**.

---

<div align="center">

### **Made with ❤️ for F1 Fans**

[⬆ Back to Top](#-boxboxbox)

**⭐ Star this repo if you find it useful!**

</div>
