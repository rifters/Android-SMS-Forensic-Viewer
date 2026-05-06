# Android SMS Forensic Viewer

An Android app that reads SMS and MMS messages (including Google Messages group chats), merges them into a unified timeline, and exports a fully interactive HTML viewer with clickable messages that open their full conversation context.

The output is similar to forensic tools (Cellebrite / Oxygen) but is user-friendly, portable, and completely open-source.

---

## Features

| Feature | Details |
|---|---|
| **SMS extraction** | Reads from `content://sms` |
| **MMS extraction** | Reads from `content://mms` including parts (text, images, audio, video) and group-chat participants |
| **Unified timeline** | All messages merged and sorted by timestamp |
| **Thread pages** | One HTML page per conversation with chat-bubble layout |
| **Click-to-jump** | Clicking a message in the timeline opens the thread page and highlights that message |
| **Search** | Live search bar filters messages by text, sender, or thread name |
| **Date jump** | Date picker scrolls timeline to the first message on a chosen date |
| **Contact names** | Phone numbers resolved to display names via Android Contacts |
| **No root needed** | Uses standard Android `ContentResolver` APIs |

---

## Project Structure

```
Android-SMS-Forensic-Viewer/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/timelineexporter/
│   │   │   │   ├── MainActivity.kt        ← UI, permission handling, orchestration
│   │   │   │   ├── MessageModel.kt        ← Unified Message / Thread data classes
│   │   │   │   ├── SmsReader.kt           ← Reads content://sms
│   │   │   │   ├── MmsReader.kt           ← Reads content://mms (parts + participants)
│   │   │   │   ├── HtmlGenerator.kt       ← Builds timeline.html + thread pages
│   │   │   │   ├── FileExporter.kt        ← Writes HTML + assets to storage
│   │   │   │   └── Utils.kt               ← Shared helpers (escaping, contacts, grouping)
│   │   │   ├── assets/assets/             ← CSS + JS bundled into the APK
│   │   │   │   ├── style.css
│   │   │   │   ├── timeline.css
│   │   │   │   ├── thread.css
│   │   │   │   ├── script.js
│   │   │   │   └── thread.js
│   │   │   ├── res/
│   │   │   │   ├── layout/activity_main.xml
│   │   │   │   ├── values/strings.xml
│   │   │   │   ├── values/colors.xml
│   │   │   │   └── xml/file_paths.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/                          ← JVM unit tests (no emulator needed)
│   └── build.gradle
│
├── export-template/                       ← Reference templates / design guide
│   ├── timeline.html
│   ├── thread_template.html
│   └── assets/
│       ├── style.css
│       ├── timeline.css
│       ├── thread.css
│       ├── script.js
│       └── thread.js
│
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## How to Use

1. **Install** the APK on your Android device (API 21+).
2. Open the app and tap **"📖 Generate Timeline"** — grant SMS and Contacts permissions when prompted.
3. Wait for the message count summary to appear.
4. Tap **"📄 Export HTML"** — all HTML files are written to:
   ```
   <External Storage>/Android/data/com.example.timelineexporter/files/Documents/TimelineExport/
   ```
5. Tap **"📂 Open Output Folder"** to open `timeline.html` directly in a browser, or navigate to the folder with a file manager.

---

## Output Files

| File | Description |
|---|---|
| `timeline.html` | Merged, scrollable, searchable timeline of all messages |
| `thread_<id>.html` | Per-conversation chat view (one per thread) |
| `assets/style.css` | Shared styles |
| `assets/timeline.css` | Timeline-specific styles |
| `assets/thread.css` | Thread page styles with bubble layout |
| `assets/script.js` | Timeline search, date-jump, and navigation |
| `assets/thread.js` | Highlight + scroll to anchor on thread pages |

---

## Building

```bash
# From the repo root
./gradlew assembleDebug
```

### Running unit tests (JVM, no emulator)

```bash
./gradlew :app:test
```

---

## Permissions

| Permission | Purpose |
|---|---|
| `READ_SMS` | Read SMS and MMS messages |
| `READ_CONTACTS` | Resolve phone numbers to contact display names |
| `WRITE_EXTERNAL_STORAGE` | (API ≤ 28 only) Write export files |

On API 29+, the app writes to its own `getExternalFilesDir()` which requires no storage permission.

---

## Architecture

```
MainActivity
  └─► SmsReader  ──┐
  └─► MmsReader  ──┴─► Utils.groupIntoThreads()
                              │
                    HtmlGenerator.buildTimeline()
                    HtmlGenerator.buildThreadPage() × N
                              │
                    FileExporter.exportAll()
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.
