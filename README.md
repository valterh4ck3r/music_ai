# Music AI - Android Music Search Application

A modern Android application for searching and discovering music using the iTunes Search API, built with Jetpack Compose and following clean architecture principles.

## 🛠 Development Environment & Requirements

This project is developed and optimized for the following environment:

### **IDE & System**
- **Android Studio**: Android Studio Otter | 2025.2.1
- **Build**: #AI-252.25557.131.2521.14344949 (Oct 28, 2025)
- **Runtime Version**: 21.0.8+-14196175-b1038.72 amd64
- **VM**: OpenJDK 64-Bit Server VM by JetBrains s.r.o.
- **Operating System**: Windows 11.0
- **Hardware Resources**: 
    - **Memory**: 2GB allocated to IDE
    - **Cores**: 24 Cores
- **Toolkit**: sun.awt.windows.WToolkit

### **Languages & SDKs**
- **Java (JAVA_HOME)**: 21.0.10 (`C:\jdk21\jdk-21.0.10`)
- **Android SDK (ANDROID_HOME)**: `C:\Users\valte\AppData\Local\Android\Sdk`
- **Compile SDK**: 36
- **Target SDK**: 36
- **Minimum SDK**: 24
- **Kotlin**: 2.0.21 (K2 enabled)

## 🚀 Getting Started

To get the project up and running on your local machine, follow these steps:

### **1. Prerequisites**
Ensure you have the correct Java version installed (JDK 21) and that your `JAVA_HOME` environment variable points to it.

### **2. Cloning the project**
```bash
git clone <repository-url>
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
To verify the application business logic, run the unit tests:
```bash
./gradlew test
```

## 🏗 Architecture & Technologies

- **UI**: Jetpack Compose
- **Dependency Injection**: Hilt
- **Network**: Retrofit + Gson
- **Local Cache**: Room Database (Offline-first)
- **Asynchronous Operations**: Kotlin Coroutines & Flow
- **Pattern**: MVVM with ResponseState (Success, Loading, Error)

## 📄 License
This project is for development and challenge purposes.
