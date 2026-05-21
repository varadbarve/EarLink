# EarLink — Bluetooth Earbud Custom Automation & Control

<p align="center">
  <b>Turn any Bluetooth TWS earbuds into programmable cross-platform smart controllers for your phone, desktop, and AI assistant flows — without modifying firmware.</b>
</p>

---

## 🚀 Product Vision

Current Bluetooth TWS earbuds have rigid, hardcoded interaction schemes (e.g., Double Tap $\rightarrow$ Next Track, Long Press $\rightarrow$ Native Assistant). **EarLink** bypasses these limitations by intercepting Android media key events and translating them into user-defined reactive workflows across both your phone and Windows desktop.

Whether you want to mute Discord from your earbud double-tap, compile a VS Code project during a long press, or invoke a voice-activated Gemini AI assistant loop, EarLink makes it possible.

---

## 🛠 How It Works

EarLink uses a decoupled, event-driven architecture to intercept input and dispatch actions asynchronously.

```
+------------------+      +---------------------+      +------------------------+
|   TWS Earbud     | ---> | Android Media Event | ---> | EarLink Input Service  |
|  (Tap Gesture)   |      |   (MediaSession)    |      | (Foreground Service)   |
+------------------+      +---------------------+      +------------------------+
                                                                   |
                                                                   v
+------------------+      +---------------------+      +------------------------+
|    OS Actions    | <--- |   Action Dispatcher | <--- |   Gesture Resolver     |
|   (Mobile/PC)    |      |    & Rule Engine    |      | (Kotlin Event Bus / Flow)|
+------------------+      +---------------------+      +------------------------+
```

1. **Tap Gesture**: The user performs a tap gesture on their TWS earbud.
2. **Android Media Event**: The earbud sends standard Bluetooth commands (e.g. `MEDIA_NEXT`, `VOLUME_UP`), which are captured by Android's media routing.
3. **EarLink Input Service**: A persistent Android foreground service intercepts these raw keycodes.
4. **Gesture Resolver**: Translates raw input sequences into abstract, device-agnostic gestures (`SINGLE_TAP`, `LEFT_DOUBLE_TAP`, etc.).
5. **Rule Engine**: Evaluates the active profile's database mappings and executes actions (or forwards them to the Desktop service).

---

## 🌟 Key Features

### 📱 Android Application
* **Reactive Event Bus:** Uses Kotlin Coroutines and `SharedFlow` to decouple raw inputs from the execution engine.
* **Smart Volume Interception:** Toggleable "Advanced Gesture Capture" allows you to choose whether to intercept volume buttons for remapping or preserve normal system volume controls.
* **Profiles & Contexts:** Swap between profiles (e.g., *Gaming*, *Study*, *Music*) with unique gesture-to-action mapping structures stored in a local SQLite Room database.
* **AI Voice Assistant Loop:** Press a button to run a voice loop (TTS "Listening" -> Record audio -> Query Gemini API -> TTS Reads response aloud) fully isolated from the main app thread.

### 💻 Windows Desktop Companion
* **WebSocket Integration:** Connects instantly with the mobile application to trigger desktop actions wirelessly.
* **Safe Script Registry:** Features a secure script validation engine. Only predefined commands and scripts inside `config/scripts.json` can be executed by path/identifier to prevent arbitrary remote command execution.
* **System Automation:** Execute keyboard shortcuts, launch software, control media streams, or run complex developer automation scripts.

---

## 📂 Repository Structure

```
EarLink/
├── app/                      # Android Mobile Application (Jetpack Compose)
│   ├── src/main/java/com/example/earlink/
│   │   ├── actions/          # Android action dispatching & isolated Gemini Assistant
│   │   ├── database/         # Room Database schemas (Profiles, Mappings, Logs)
│   │   ├── event/            # Kotlin SharedFlow Event Bus and InputEvent models
│   │   ├── rules/            # Gesture Resolver and Rule Engine mappings
│   │   ├── service/          # MediaSession foreground event capture service
│   │   └── ui/               # Modern M3 Compose UI (Home, Mapping, Profile, Logs)
│
└── earlink-desktop/          # Python Desktop Companion Service
    ├── actions/              # OS execution scripts (Discord, Spotify, System)
    ├── config/               # Script execution registry configs (scripts.json)
    ├── service/              # WebSocket Server and safe action executor
    └── main.py               # Desktop server entrypoint
```

---

## 🔧 Installation & Setup

### 1. Android Application
1. Open the `/` root directory in Android Studio.
2. Build and run the `:app` module on a device running Android 8.0 (API 26) or higher.
3. Grant **Notification Access** and **Microphone** permissions when prompted.
4. Go to **Settings** and configure your desktop companion's WebSocket IP address.

### 2. Desktop Companion
1. Navigate to the desktop directory:
   ```bash
   cd earlink-desktop
   ```
2. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Register your scripts in `config/scripts.json`:
   ```json
   {
     "mock_script": "echo 'Hello from EarLink Desktop'",
     "build_project": "python C:/path/to/build_script.py"
   }
   ```
4. Start the companion server:
   ```bash
   python main.py
   ```

---

## 🛡 Security & Verification

EarLink separates gesture capture from arbitrary command execution:
* **Safe List Registry:** To protect your host PC, the desktop service validates all command requests against `config/scripts.json` and rejects any unlisted executable paths.
* **Room Compiler Migration:** Native compilation is managed using **KSP** (Kotlin Symbol Processing) and **Room 2.8.0**, ensuring full compatibility with Kotlin `2.3.x` compilation targets under AGP 9.0+.
