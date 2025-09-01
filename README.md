# SpeechToCall - Android App - Jaro-Winkler

**SpeechToCall** is an Android application that allows users to make phone calls by speaking the contact's name. The app uses speech recognition to convert voice input into text, searches for the closest matching contact using the Jaro-Winkler similarity algorithm, and then initiates a phone call. It also displays the contact list and highlights matching results.

## Features
- Convert speech to text using Android's `RecognizerIntent`.
- Search for contacts based on spoken names using fuzzy matching (Jaro-Winkler similarity).
- Make phone calls directly from the app.
- Display all contacts with names and phone numbers.
- Handles runtime permissions for microphone, contacts, and phone calls.

## Permissions
The app requires the following Android permissions:
- `RECORD_AUDIO` – for speech recognition.
- `READ_CONTACTS` – to access the contact list.
- `CALL_PHONE` – to make phone calls.

## How It Works
1. The user taps the microphone button to start speech recognition.
2. The app converts the speech to text and extracts the contact name.
3. It searches the contacts for the closest match using Jaro-Winkler similarity.
4. If a match is found, the app displays the contact information and automatically makes a phone call if permission is granted.
5. All contacts are displayed in a list on the main screen.

![SpeechToCall](./speechtocall.png)

---
Copyright © 2025, All Rights Reserved For ELKADDI-Solutions
