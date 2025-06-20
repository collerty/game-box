# 🎮 GamHub

**GameHub** is an all-in-one mobile app for Android, built in modern **Kotlin** using **Jetpack Compose**, that lets you play multiple mini-games—both singleplayer and multiplayer!  
With a stylish unified menu and support for Firebase-backed multiplayer, GameHub is designed as a fun, extensible playground for your favorite classic and new games.

---

## ✨ Features

- **Multiple Games in One App**
- **Multiplayer Support:**  
  Play with friends in real time using Firebase Firestore.
- **Singleplayer Modes:**  
  Challenge yourself in local games (where available).
- **Modern Android Design:**
    - Written 100% in **Kotlin**
    - Built with **Jetpack Compose** for a smooth, declarative UI
    - No legacy XML layouts—everything is code
- **Easy to Extend:**  
  The codebase is organized for adding new games, menus, and screens.

## 🛠️ Tech Stack

- **Kotlin** (primary language)
- **Jetpack Compose** (modern Android UI toolkit)
- **Firebase Firestore** (for multiplayer state sync)
- **Android Studio** (recommended IDE)
- Coil (for image/GIF loading)
- Material3 (modern Material Design)
- Other modern Android libraries

---

## 🚀 How to Run GameHub

You can **run GameHub in an emulator or on a real Android device**, or just sideload the provided APK.  
These instructions are beginner-friendly and detailed for both devs and casual testers.

---

### 1. 📥 Clone the Repository

Open a terminal (or use GitHub Desktop) and run:

```sh
git clone https://github.com/collerty/game-box.git
cd game-box
2. 🛠️ Open the Project in Android Studio
Download and install Android Studio (latest stable is best).

In Android Studio, choose “Open an Existing Project”.

Select the game-box folder you just cloned.

Android Studio will sync Gradle and download dependencies automatically.

This may take a few minutes on first launch.

Accept all prompts to update plugins or install SDK components.

3. ⚙️ Set Up the Android SDK & Emulator/Device
Android SDK:

GameHub targets Android 13+ (API level 33+).

Make sure you have this installed:
Tools > SDK Manager in Android Studio.

Emulator:

Go to Tools > Device Manager

Create a new Virtual Device (e.g. Pixel 6, API 33+).

Start the emulator.

Physical Device:

On your Android phone, enable Developer Options and USB Debugging:
Google’s guide here

Plug in your device via USB and accept prompts.

4. 🏗️ Build and Run the App
In Android Studio, click the green "Run" arrow at the top (or press Shift+F10).

Choose your device or emulator as the target.

The app will build and install automatically.

You’ll see the GameHub menu. Select any game to start!

Troubleshooting Build Issues
If you get build errors, try Build > Clean Project and then Build > Rebuild Project.

Make sure all your plugins and the Android SDK are up to date.

Delete .gradle and .idea folders and re-sync if you run into strange issues.

5. 📱 Install the APK Directly (No Android Studio Needed)
If you just want to try GameHub on your phone:

Find the latest APK in the repository

A. Install via file manager:

Copy the APK to your phone (USB, Google Drive, email, etc).

Open it using your phone’s file browser.

If prompted, allow “Install unknown apps”.

Tap “Install”. The app will appear in your launcher.

💡 About GameHub
GameHub is a modern, modular Android game launcher and playground built for fun and learning.

Code is Kotlin-only, no legacy Java or XML layouts.

Built to make it easy to add new games, features, and UI improvements.

Multiplayer and real-time features use Firebase (you may need your own Firebase credentials for production/distribution).

🧑‍💻 Contributing
Pull requests, issues, and forks are welcome!

Please follow Kotlin/Jetpack Compose idioms and Android best practices.

Want to add your own mini-game?
Check the project structure and see how Battleships and TriviaToe are organized.

🙏 Credits
Jetpack Compose

Firebase

Coil

Material3
