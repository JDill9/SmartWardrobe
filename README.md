# SmartWardrobe – Android Mobile App

SmartWardrobe is a mobile application designed to help users digitize and manage their clothing using AI-powered image processing and 3D model generation.  
The app is built in **Kotlin**, uses **Jetpack Compose**, and integrates with **Firebase** and **Tripo AI** for AI-based outfit rendering.

This repository contains the full Android Studio project for running, testing, and developing the SmartWardrobe app.

---

## 🚀 Features

- 📸 **Take or upload clothing photos**
- 🤖 **AI-powered 3D model generation (TripoAI integration)**
- 👕 **Outfit builder** (generate outfits from selected clothing items)
- 🧩 **Wardrobe organization & storage**
- 🔐 **Firebase Authentication** (Email + Google Sign-in)
- ☁️ **Firebase Firestore/Storage integration**
- 📱 **Built with Jetpack Compose + MVVM architecture**

---

## 🛠️ Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Android Studio (Giraffe+/Hedgehog+)**
- **Firebase Auth / Firestore / Storage**
- **TripoAI API (image → 3D model generation)**
- **Coil** for image loading
- **Coroutines + Flow**
- **ViewModel / MVVM**

---

## 📦 Project Structure

SmartWardrobe/
│
├── app/
│ ├── src/main/java/com/example/smartwardrobe/
│ │ ├── ai/ # AI & model generation logic
│ │ ├── auth/ # Firebase authentication
│ │ ├── data/ # Repositories & models
│ │ ├── ui/ # Jetpack Compose UI screens
│ │ ├── viewmodel/ # ViewModels (MVVM)
│ │ └── utils/ # Helpers & extensions
│ ├── res/ # Layouts, icons, images
│ ├── AndroidManifest.xml
│ └── build.gradle.kts
│
└── build.gradle.kts # Project-level Gradle

---

## ▶️ How to Run the App (Android Studio)

### **1. Clone the repository**
```bash
git clone https://github.com/JDill9/SmartWardrobe.git
cd SmartWardrobe

2. Open the project in Android Studio

Start Android Studio

Select “Open an existing project”

Choose this folder
Sync Gradle

Android Studio will usually prompt automatically.
If not:

File → Sync Project with Gradle Files

3. Run the app with an emulator or device

Click Run ▶ in Android Studio

Choose a device/emulator such as:

Pixel 6 API 34

Pixel 7 API 33

Any Android 12–14 emulator


