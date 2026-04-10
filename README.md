# Music AI - Android Music Search Application

A modern Android application for searching and discovering music using the iTunes Search API, built with Jetpack Compose and following clean architecture principles.

## 📱 Project Preview

Explore the application's interface and functionality across different form factors.

### **Demo Videos**

| Mobile Device | Tablet Device |
| :---: | :---: |
| ![Phone Demo](assets/videos/screen_recording_phone.webm) | ![Tablet Demo](assets/videos/screen_recording_tablet.webm) |

### **Screenshots & Verification**

#### **Unit Test Coverage**
The project includes comprehensive unit tests for ViewModels and Repositories to ensure business logic reliability.

![Unit Testing Results](assets/images/Unit%20Tests.png)

---

## ✨ Features

- **Music Discovery**: Seamless search through millions of tracks via the iTunes Search API.
- **Offline-First Experience**: Fully integrated with Room Database to cache search results and recently played songs, allowing users to browse music even without an internet connection.
- **Advanced Playback**:
    - Full-screen playback controls with adaptive UI (Tablet vs Phone).
    - Background playback support using Media3 (ExoPlayer + MediaSession).
    - Repeat and Shuffle functionalities.
- **Dynamic UI**:
    - Material 3 Glassmorphism aesthetics.
    - Shimmer loading effects for a premium feel.
    - Responsive layouts using `BoxWithConstraints` and `LocalWindowInfo`.
- **Navigation**: Structured navigation graph supporting complex transitions and deep linking between Home, Song Playback, and Album details.

## 🏗 Architecture & Technologies

The project follows a modularized **MVVM (Model-View-ViewModel)** pattern combined with Clean Architecture principles:

- **UI**: Jetpack Compose (100% Declarative UI)
- **Dependency Injection**: Hilt (Dagger)
- **Networking**: Retrofit 2 & GSON for seamless API interaction.
- **Persistence**: Room Database for local caching and user data.
- **Media**: Android Media3 (ExoPlayer) with `MediaSessionService` for robust background audio playback.
- **Images**: Coil for optimized image loading and caching.
- **Concurrency**: Kotlin Coroutines & Flow (SharedFlow/StateFlow) for reactive state management.
- **Connectivity**: Real-time network monitoring using `ConnectivityManager` and `Flow`.

## 🛠 Development Environment & Requirements

This project is developed and optimized for the following environment:

### **IDE & System**
- **Android Studio**: Android Studio Otter | 2025.2.1
- **Build**: #AI-252.25557.131.2521.14344949 (Oct 28, 2025)
- **Operating System**: Windows 11.0

### **Languages & SDKs**
- **Java (JAVA_HOME)**: 21.0.10
- **Compile SDK**: 36
- **Target SDK**: 36
- **Minimum SDK**: 24
- **Kotlin**: 2.0.21 (K2 enabled)

## 🚀 Getting Started

### **1. Prerequisites**
Ensure you have the correct Java version installed (JDK 21) and that your `JAVA_HOME` environment variable points to it.

### **2. Cloning the project**
```bash
git clone https://github.com/valterh4ck3r/music_ai
cd music-ai
```

### **3. Opening in Android Studio**
1. Open Android Studio.
2. Select **Open** and navigate to the project root directory.
3. Wait for the Gradle sync to finish.

### **4. Running the App**
- Connect an Android device (Physical or Emulator) with API 24 or higher.
- Click the **Run** button (Green arrow) in the toolbar.

### **5. Running Tests**
The application implements robust testing with MockK and Coroutines Test.
```bash
./gradlew test
```

## 📄 License
This project is for development and challenge purposes.
