# heartandbrain — Project Brief

## Overview
heartandbrain is a personal, private Android app for self-reflection. The user uploads video vlogs 
from their phone. AI transcribes and segments each vlog into key moments, labeling each as either 
a video clip or a standalone quote. These moments are surfaced daily on a visual card board — 
no scrolling through menus, no hunting. You open the app and your recent self is just there.

This app is built for one user (the developer). Simplicity, low friction, and actually shipping 
take priority over scalability and polish.

---

## Core Experience

### The Board (Home Screen)
The home screen IS the app. It is a Pinterest-style card board with two zones:

**Pinned Zone (top)**
- Cards the user has manually pinned
- These stay at the top indefinitely regardless of age
- Represent the user's most important goals, commitments, and reminders
- Pinned cards persist until the user unpins them

**Recent Zone (below pinned)**
- Automatically populated with clips and quotes from the last 7 days
- Ordered chronologically, newest first

There is no other home screen content. No navigation menu. No stats. Just the board.

### Card Types
There are two types of cards that appear on the board:

**Video Thumbnail Card**
- Shows a thumbnail from the clip
- Short title underneath (the "key idea" label)
- Tap to play just that clip in the player screen
- Used for moments that are emotional, complex, or hard to summarize in a single sentence

**Quote Card**
- Displays a single crisp sentence extracted from the vlog
- Visually distinct — text-forward, no thumbnail
- Double-tap to open the player screen, which seeks to the exact timestamp of that quote
- Used for moments that stand alone as a punchy, memorable line

The AI decides per segment whether it is better represented as a video card or a quote card.

### Player Screen
- Fullscreen ExoPlayer
- Seeks to the clip's start timestamp and plays until end timestamp, then stops
- Minimal UI — just the video and a back button

### Upload Flow
- Floating Action Button (FAB) in the bottom right corner of the home screen
- Tap FAB → opens Android gallery picker
- User selects a video → processing begins in the background
- User can leave the app while processing happens
- When processing is complete, new cards appear on the board

### Daily Notification
- One notification per day, triggered by WorkManager
- Tapping it opens the app to the board
- The board itself is the "reveal" — no special notification content needed beyond a simple prompt

---

## AI Pipeline

When a video is uploaded, the following steps occur:

### Step 1 — Audio Extraction
- Extract audio from the video file on-device
- Send audio to OpenAI Whisper API
- Receive back a timestamped transcript (word-level or sentence-level timestamps)

### Step 2 — Segmentation + Labeling (Claude API)
Send the timestamped transcript to Claude with a prompt that instructs it to:
1. Identify distinct key ideas (not just sentences — coherent thoughts, even if rambling)
2. For each segment, output:
   - `start_time` (seconds)
   - `end_time` (seconds)
   - `title` (short label, 3-7 words)
   - `type`: either `"clip"` or `"quote"`
   - `quote_text` (only if type is `"quote"` — the single best sentence from that segment)
   - `category`: one of `"goal"`, `"commitment"`, `"emotional"`, `"reminder"`, `"reflection"`

The AI should favor `"quote"` type for segments where a single sentence captures the idea fully, 
and `"clip"` type for everything else.

### Step 3 — Storage
- Store all segment metadata in a local Room database
- The original video file stays on the device and is never uploaded
- Each segment stores a reference to the original video file path + its timestamps

---

## Data Model (Room Database)

### Vlog
- `id` (primary key)
- `file_path` (local path to video file)
- `created_at` (timestamp of upload)
- `title` (optional, auto-generated from date if not set)

### Clip
- `id` (primary key)
- `vlog_id` (foreign key)
- `start_time` (float, seconds)
- `end_time` (float, seconds)
- `title` (short label)
- `type` (enum: CLIP or QUOTE)
- `quote_text` (nullable, only for QUOTE type)
- `category` (enum: GOAL, COMMITMENT, EMOTIONAL, REMINDER, REFLECTION)
- `is_pinned` (boolean, default false)
- `created_at`

---

## Tech Stack
- **Language**: Kotlin
- **Platform**: Native Android
- **Minimum SDK**: API 26 (Android 8.0) — safe modern baseline
- **Transcription**: OpenAI Whisper API
- **Segmentation + Labeling**: Claude API (claude-sonnet-4-20250514)
- **Video Playback**: ExoPlayer (Media3)
- **Local Database**: Room
- **Background Processing**: WorkManager
- **UI**: Jetpack Compose

---

## Screen Map
```
Home (Board)
├── Pinned Zone (top)
│   └── [Pinned cards — video thumbnails or quote cards]
├── Recent Zone (last 7 days)
│   └── [Cards — video thumbnails or quote cards]
└── FAB (bottom right) → Gallery Picker → Background Processing

Player Screen
└── Fullscreen ExoPlayer (seeks to clip timestamps, plays segment only)
```

---

## MVP Scope (Build in this order)

1. **Project scaffolding** — Jetpack Compose, Room, dependencies set up
2. **Home screen** — basic card board UI, two zones, FAB
3. **Gallery picker** — FAB opens picker, video selected and stored
4. **Whisper integration** — extract audio, send to API, get transcript back
5. **Claude segmentation** — send transcript, parse segments, store in Room
6. **Card rendering** — display clip cards and quote cards from Room data
7. **Player screen** — ExoPlayer seeking to timestamps
8. **Pin functionality** — long press to pin/unpin a card
9. **WorkManager notification** — daily notification that opens the app

---

## Explicitly Out of Scope (for now)
- In-app video recording (next feature after MVP)
- User accounts or cloud sync
- Sharing clips
- Archive/browse screen beyond the 7-day board
- Any settings screen
- Onboarding flow
