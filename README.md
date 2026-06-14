# HealthyAPP 💧

An Android health tracking application built with Jetpack Compose, Firebase Firestore, and mobile sensors. Track your water intake, view health metrics, and share tips with the community.

## Features

| Feature | Description |
|---------|-------------|
| 💧 **Water Tracking** | Log water intake locally (Room) and sync to server |
| 📊 **Health Dashboard** | Steps, heart rate, sleep quality, calories burned |
| 👤 **Profile Management** | Edit name, height, weight, daily water goal |
| 🔥 **Community Feed** | Real-time sharing via Firebase Firestore (`community_posts`) |
| 📱 **Shake to Add Water** | Accelerometer detects a shake → adds 100ml automatically |
| 💡 **Health Tips** | Random tips on the Tips page |
| 📋 **History** | Local + server-side water intake history |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin** |
| UI | **Jetpack Compose + Material 3** |
| Architecture | **Single-Activity, MVVM** (ViewModel + Repository) |
| Local Storage | **Room** (SQLite) |
| Cloud - Community | **Firebase Firestore** (collection `community_posts`) |
| Cloud - Profile/Water | **PHP REST API** (Retrofit + Gson) |
| Sensor | **Accelerometer** (shake detection) |
| Navigation | **Navigation Compose** |

## 📱 Shake to Add Water

The app listens to the **accelerometer** on every screen.

```
Acceleration = sqrt(x² + y² + z²)
If Acceleration > 9.8 m/s² (gravity) + 15 m/s²
     AND last shake > 2 seconds ago
  → addWater(100ml)
```

- Global listener: works on any page
- 2-second debounce to prevent spam
- Registers in `onResume()`, unregisters in `onPause()`

## 🔥 Firebase Setup

- **Project ID**: `lab0-6e2fb084`
- **Firestore collection**: `community_posts`
- Fields: `studentId`, `tipText`, `source`, `createdAt` (server timestamp)
- `google-services.json` is bundled in `app/`
- First write automatically creates the collection

## Project Structure

```
app/src/main/java/com/example/healthyapp/
├── firebase/
│   ├── CommunityPost.kt         # Firestore document data class
│   └── FirestoreService.kt      # Real-time listen + write
├── network/
│   ├── ApiModels.kt             # Retrofit response models
│   ├── HealthyApiClient.kt      # Retrofit singleton
│   └── HealthyApiService.kt     # API endpoint definitions
├── ui/theme/                    # Material 3 theming
└── MainActivity.kt              # Activity + ViewModel + all composables
```

## Build & Run

1. Open in **Android Studio Ladybug+** (or newer)
2. Sync Gradle (JDK 17 required)
3. Connect a **physical device** (emulator has no accelerometer!)
4. Run `app`

```
minSdk: 24
targetSdk: 35
compileSdk: 35
```

## PHP API Backend

Profile and water logging use a PHP server at:

```
http://lrgs.ftsm.ukm.my/users/a207349/healthyapp/api.php
```

Endpoints: `get_profile`, `save_profile`, `add_water`, `get_water_history`, `get_dashboard`

## License

Project for educational purposes — UKM WANGBINYU
