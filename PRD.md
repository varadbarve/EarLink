# EarLink — Product Requirements Document (PRD)

Version: v1.0  
Status: MVP Planning  
Platform: Android + Windows Desktop  
Author: Varad Barve

---

# Product Vision

Transform ordinary Bluetooth TWS earbuds into programmable cross-platform controllers without modifying firmware.

EarLink allows users to intercept earbud gestures and convert them into custom actions across mobile and desktop environments.

---

# Problem Statement

Current TWS earbuds have fixed interactions:

- Single Tap → Pause
- Double Tap → Next Track
- Long Press → Assistant

Users cannot:

- Remap controls
- Trigger desktop shortcuts
- Build workflows
- Integrate AI systems
- Create context-aware behavior

Firmware modification is difficult and often impossible for cheap earbuds.

EarLink solves this by intercepting Android media events and translating them into user-defined actions.

---

# Product Goals

## Primary Goals

- Detect Bluetooth media events from TWS devices
- Allow custom gesture remapping
- Support Android and desktop
- Enable profile-based behavior
- Run continuously in background

---

## Secondary Goals

- AI assistant integration
- Context-aware actions
- Multi-device synchronization
- Community profile sharing

---

# Non Goals (v1)

The system will NOT:

- Modify TWS firmware
- Flash earbuds
- Reverse engineer Bluetooth chips
- Require root access
- Run local LLMs

---

# User Types

## Productivity Users

Needs:

- Quick actions
- App launching
- Shortcuts

Examples:

Double tap → Open Notion

Long press → Start recording

---

## Gamers

Needs:

- Discord controls
- Push-to-talk
- Gaming shortcuts

Examples:

Double tap → Mute Discord

Long press → Launch game

---

## Developers / Power Users

Needs:

- Script execution
- Automation
- System shortcuts

Examples:

Double tap → Run Python script

Long press → Build VS Code project

---

## AI Users

Needs:

- Assistant triggers

Examples:

Long press

↓

Record speech

↓

Send to AI

↓

Read response aloud

---

# Core Product Flow

```text
TecSox Gesture
        ↓
Android Media Event
        ↓
EarLink Input Service
        ↓
Rule Engine
        ↓
Action Dispatcher
        ↓
Communication Layer
        ↓
Desktop Service
        ↓
OS Actions
```

Example:

```text
Double Tap
      ↓
MEDIA_NEXT
      ↓
Rule Engine
      ↓
Mute Discord
      ↓
Desktop Service
      ↓
Ctrl + Shift + M
```

---

# Functional Requirements

## FR1 — Input Detection

Capture:

- KEYCODE_MEDIA_PLAY_PAUSE
- KEYCODE_MEDIA_NEXT
- KEYCODE_MEDIA_PREVIOUS
- KEYCODE_VOICE_ASSIST

Output:

```kotlin
Event(
    deviceName="TecSox",
    eventType="MEDIA_NEXT",
    timestamp=...
)
```

---

## FR2 — Rule Engine

Map:

```text
Input → Action
```

Examples:

```text
MEDIA_NEXT
      ↓
OPEN_SPOTIFY
```

---

## FR3 — Action Execution System

### Android Actions

- Open application
- Toggle DND
- Start recording
- Launch assistant
- Send notification

### Desktop Actions

- Execute keyboard shortcuts
- Launch apps
- Run scripts
- Control media
- System actions

---

## FR4 — Profiles

### Gaming Profile

```text
PLAY_PAUSE
        ↓
Mute Discord
```

### Study Profile

```text
PLAY_PAUSE
        ↓
Start recording
```

### Music Profile

```text
NEXT
        ↓
Next Track
```

---

## FR5 — Desktop Companion

Receives:

```json
{
    "command":"mute_discord"
}
```

Executes:

```text
Ctrl + Shift + M
```

---

# Non Functional Requirements

| Requirement | Target |
|-------------|---------|
| Input latency | <100ms |
| Startup time | <2 sec |
| Memory usage | <150 MB |
| Battery impact | Low |
| Desktop communication latency | <200ms |
| Crash rate | <1% |

---

# Database Schema

## Profiles

```text
Profile
---------
id
name
active
```

## Mappings

```text
Mapping
---------
id
profileId
eventType
actionType
```

## Logs

```text
EventLog
---------
id
event
timestamp
```

---

# Android Project Folder Structure

```text
EarLink/
│
├── app/
│   │
│   ├── ui/
│   │   │
│   │   ├── screens/
│   │   │   │
│   │   │   ├── HomeScreen.kt
│   │   │   ├── MappingScreen.kt
│   │   │   ├── ProfileScreen.kt
│   │   │   ├── DeviceScreen.kt
│   │   │   └── LogsScreen.kt
│   │   │
│   │   ├── components/
│   │   │   │
│   │   │   ├── EventCard.kt
│   │   │   ├── ProfileCard.kt
│   │   │   └── ActionSelector.kt
│   │   │
│   │   └── theme/
│   │
│   ├── bluetooth/
│   │   │
│   │   ├── MediaReceiver.kt
│   │   ├── BluetoothManager.kt
│   │   └── DeviceMonitor.kt
│   │
│   ├── event/
│   │   │
│   │   ├── Event.kt
│   │   ├── EventStore.kt
│   │   └── EventLogger.kt
│   │
│   ├── rules/
│   │   │
│   │   ├── Rule.kt
│   │   ├── RuleEngine.kt
│   │   └── RuleRepository.kt
│   │
│   ├── actions/
│   │   │
│   │   ├── Action.kt
│   │   ├── ActionManager.kt
│   │   ├── AndroidActions.kt
│   │   └── DesktopActions.kt
│   │
│   ├── network/
│   │   │
│   │   ├── WebSocketClient.kt
│   │   └── MessageModels.kt
│   │
│   ├── database/
│   │   │
│   │   ├── AppDatabase.kt
│   │   ├── ProfileEntity.kt
│   │   └── MappingEntity.kt
│   │
│   ├── service/
│   │   │
│   │   └── EarLinkForegroundService.kt
│   │
│   └── MainActivity.kt
```

---

# Desktop Folder Structure

```text
earlink-desktop/
│
├── service/
│   ├── websocket_server.py
│   ├── action_executor.py
│   └── shortcut_handler.py
│
├── actions/
│   ├── discord.py
│   ├── spotify.py
│   └── system.py
│
├── config/
│   └── mappings.json
│
└── main.py
```

---

# Development Milestones

## Milestone 1 — Input Inspector

Goal:

```text
Earbud
    ↓
Capture Events
    ↓
Display Logs
```

Deliverables:

- MediaReceiver
- EventStore
- LogsScreen

---

## Milestone 2 — Rule Engine

Goal:

```text
MEDIA_NEXT
      ↓
OPEN_SPOTIFY
```

Deliverables:

- Rule model
- Mapping UI
- Storage system

---

## Milestone 3 — Android Actions

Goal:

```text
Double Tap
      ↓
Launch Application
```

Deliverables:

- Action manager
- Android shortcuts

---

## Milestone 4 — Desktop Integration

Goal:

```text
Double Tap
      ↓
Mute Discord
```

Deliverables:

- WebSocket communication
- Desktop service

---

## Milestone 5 — Profiles

Goal:

- Gaming profile
- Study profile
- Music profile

---

## Milestone 6 — AI Integration

Goal:

```text
Long Press
      ↓
Record speech
      ↓
AI processing
      ↓
TTS response
```

---

# Success Metrics

| Metric | Goal |
|----------|-------|
| Event detection success | >95% |
| Action execution success | >90% |
| Average latency | <100ms |
| Crash rate | <1% |

---

# One-line Product Pitch

EarLink turns any Bluetooth earbuds — even cheap generic TWS devices — into programmable smart controllers for phones, laptops, and AI workflows.
