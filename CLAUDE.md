# heartandbrain

## What this app does
A personal Android app for self-reflection. The user records a vlog on their phone and uploads it. 
AI transcribes it with timestamps, segments it into distinct key ideas, and labels each with a short title. 
A daily notification surfaces one random clip from the archive and plays it back inline — no scrolling, 
no friction. The experience is: open app, watch a moment from your past self.

This app is personal and private — not built for distribution. Simplicity and actually getting built 
take priority over polish and scalability.

## Tech stack
- Language: Kotlin
- Platform: Native Android (no React Native, no cross-platform)
- Transcription: OpenAI Whisper API (audio extracted from video, sent to cloud)
- Segmentation + labeling: Claude API (timestamped transcript in, labeled segments out)
- Video playback: ExoPlayer
- Local storage: Room database (stores clip metadata, timestamps, titles)
- Notifications: Android WorkManager (daily scheduled notification)

## Architecture

### Pipeline (on upload)
1. User picks a video from their phone
2. App extracts audio and sends to Whisper API → receives timestamped transcript
3. Transcript sent to Claude API → receives list of segments, each with:
   - start_time, end_time
   - short title (the "key idea")
4. Segments stored in Room database alongside reference to original video file
5. Original video stays on device (not uploaded)

### Daily notification flow
1. WorkManager triggers once per day
2. Picks a random clip from the Room database
3. Notification appears → user taps → app opens and seeks ExoPlayer to that clip's timestamps
4. Plays just that segment, then stops

### Screens
- Home: single button to upload a new vlog. Nothing else.
- Player: fullscreen video player that plays the selected clip
- (Future) Archive: browse all clips — not in MVP

## Patterns we use
[update as conventions emerge]

## Patterns we avoid
- No scrollable feeds or lists in the core experience — low friction is a core design constraint
- No on-device ML — cloud APIs only, this is a vibe-coded personal project
- No unnecessary screens — if it's not in the MVP, it doesn't exist yet

## Agent roles

### Architect
Receives a plain-English feature request. Outputs a technical spec — files affected, data flow, 
API shapes, edge cases. Does NOT write code.

### Coder
Receives a spec from Architect. Implements it in Kotlin exactly as described. Notes ambiguities 
as inline comments but still implements best guess. Outputs code only.

### Reviewer
Reads Coder output. Flags bugs, bad patterns, spec deviations, and anything that will hurt later. 
Ranks issues by severity. Is blunt.

### CLAUDE.md updater
After any meaningful decision, updates this file to reflect the current state of the project.

## Current state
- [ ] Android Studio installed
- [ ] Project scaffolded
- [ ] Single screen with video upload button
- [ ] Whisper API integration
- [ ] Claude API segmentation
- [ ] Room database setup
- [ ] ExoPlayer playback
- [ ] WorkManager daily notification
