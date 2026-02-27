# Jarvis Voice Assistant (Phone Control Prototype)

This repository now contains a runnable Python prototype for a voice-triggered assistant that:

- wakes on the trigger word **"Jarvis"**,
- translates user language into operation plans,
- controls a connected Android phone through **ADB**,
- asks for explicit confirmation before risky actions (send/delete/upload),
- can inspect visible WhatsApp UI text and report whether a requested pattern is present.

## What this implements

### 1) Wake word gate
`JarvisAssistant.is_wake_phrase()` checks for the wake word before any action execution.

### 2) "Intuition" style command understanding
`IntentPlanner` converts natural language text into a strict `ActionPlan` schema.

Examples:
- `Jarvis send message to Alex saying I'll be late`
- `Jarvis check my whatsapp group "Family" and see if message like meeting tomorrow is there`
- `Jarvis search best sushi near me`

### 3) Device operations via ADB
`DeviceController` supports:
- opening apps,
- preparing SMS messages,
- launching browser searches,
- reading currently visible WhatsApp UI text using `uiautomator dump`.

### 4) Final confirmation gates
If the action is risky (send/delete/upload), CLI asks for approval:
- "Should I send this?"
- "Should I delete?"
- "Should I upload these?"

## Quick start

```bash
python -m jarvis_assistant.cli "Jarvis search weather in Mumbai"
python -m jarvis_assistant.cli "Jarvis send message to +123456 saying reached home"
python -m jarvis_assistant.cli "Jarvis check my whatsapp group 'Family' and see if message like invoice is there"
```

> Note: ADB must be installed and a device must be connected (`adb devices`).

## Next production upgrades

- Replace deterministic planner with an LLM tool-calling planner.
- Add real wake-word and STT/TTS engines (Porcupine/Vosk/Whisper etc.).
- Add per-app connectors with accessibility flows (WhatsApp deep navigation, group search, scrolling).
- Add secure auth (voice biometrics + lock-screen confirmation) before sensitive actions.
