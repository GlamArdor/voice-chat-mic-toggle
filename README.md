# Voice Chat Mic Toggle

A client-side add-on for [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) that turns the push-to-talk and whisper keys into switches: press once to start talking, press again to stop.

## What it does

Each key has three modes, chosen separately:

- **Press to switch** (default): one press turns it on, the next one turns it off.
- **Tap or hold**: a short tap switches it on until the next tap; hold the key longer than the threshold (300 ms by default) and it works only while held, like before.
- **Only while held**: exactly what Simple Voice Chat does on its own.

Talking and whispering end each other: pressing one key switches the other off. A quiet click tells you the microphone switched (the whisper click is higher). The microphone switches off when you leave a server.

While the chat or any menu is open, keyboard keys switch nothing, so typing the letter the key sits on does not open your microphone mid-sentence. Mouse buttons keep working.

The push-to-talk key only matters with **Push to Talk** chosen as the activation type in Simple Voice Chat; the whisper key works in both modes.

## Settings

Through Mod Menu or `/mictoggle`. With Cloth Config installed you get its screen, otherwise a built-in one; either way the world stays visible behind it and changes apply at once.

## Requirements

- Minecraft 1.21.8, Fabric Loader, Fabric API
- Simple Voice Chat
- Optional: Cloth Config, Mod Menu

Nothing is needed on the server.

## License

LGPL-3.0-or-later.
