# EyeBridge Assist — blink-to-caregiver communication (ongoing research)

> **Status: v0.1.0 proof of concept / ongoing open-source project — NOT clinically validated, NOT a medical device, NOT an emergency service.** No patients have been enrolled, no clinical trials or device testing have been performed. **Never use this as the sole way for someone to call for help.** Always provide a proven independent call button or reachable caregiver. 

An accessibility experiment: an Android device watches for **deliberate long eye closures** using an on-device camera model, converts sequences into simple requests, and sends an alert to a trusted local relay on a PC. Caregivers can view requests on the PC or a second phone in a web browser.

### Why

Some people with severe motor limitations may find eye-based communication helpful. Eye-blink recognition is prone to false positives and misses, especially under variable lighting, glasses, ptosis, eye fatigue, and camera movement. This project is a starting point for voluntary, accessible user research—not a promised assistive or medical solution.

## What is implemented

| Component | Working source | Installation |
|---|---|---|
| **Patient Android app** | CameraX + on-device MediaPipe Face Landmarker with `eyeBlinkLeft` / `eyeBlinkRight` blendshapes, long-closure timing, manual test buttons | Android Studio or GitHub Actions APK build (not yet hardware-tested) |
| **Caregiver relay** | Python standard library HTTP server, 8-digit session code, allowlisted requests, bounded 120-event history | Python 3.10+ on a local PC |
| **Caregiver dashboard** | Mobile-responsive HTML, alerts, timestamp history, test tone, optional vibration, ~1.2s polling | Browser on a PC or a second phone |
| **Testing** | Four server integration tests for code verification, alerts, invalid actions and polling cursors | `python -m unittest discover -s tests -v` |

## Communication diagram

```text
 PATIENT PHONE (Android app)       TRUSTED HOME Wi-Fi        PC (relay, Python)
 CameraX front camera                                       +------------------+
 MediaPipe Face Landmarker --> intentional long blinks --> | POST /api/alert  |
 BlinkSequence state engine      HTTP port 8768 / code      | event queue      |
                                  (no raw video)           +--------+---------+
                                                                    |
                                                                    v
                                                  CAREGIVER PHONE / PC BROWSER
                                                   GET /api/events ~1.2 sec
                                                   visible alert + test tone
```

## Blink gesture mapping in v0.1

A **long blink** is a simultaneous-eye closure lasting approximately **0.45–1.7 seconds**. The next 5 seconds comprise a request window after the first long blink. The system counts valid blinks and then sends:

| Intentional closures | Message |
|---|---|
| 1 long blink | **I feel cold** |
| 2 long blinks | **I need assistance** |
| 3 long blinks | **Urgent assistance requested** (not emergency dispatch) |

There is a 9-second cooldown after recognizing a request. Closing eyes for longer than 1.7 seconds resets the sequence. Losing face tracking clears the sequence. Model threshold is currently a fixed 0.58 confidence value—**personal calibration is still TODO**, and people who cannot reliably perform deliberate closures may not be able to use this version. This mapping MUST be validated for each person's abilities before any supervised test.

## Start the relay on a PC

Install Python 3.10 or later, then run from the repository root:

```bash
python relay/server.py
```

The console shows an IP address and random **8-digit access code**. Keep the PC powered on, connect both phones to the **same trusted private Wi-Fi**, and allow incoming TCP port `8768` on the PC firewall for **Private networks** only. Never forward this port on your router.

Open `http://PC-IP:8768/` on the caregiver's phone. Enter the 8-digit code, click **Connect**, then click **Enable sound / Test alarm**. The browser must remain open: mobile browsers can suspend polling/audio in the background, even when connected. **Do not treat a background browser as guaranteed to receive alerts.**

## Android app installation

A native **Android app** is provided in `android/`. The repository includes `.github/workflows/android-apk.yml` to compile a **debug APK** on GitHub's Ubuntu Actions runner.

1. Create a **new public GitHub repository** (suggested name: `EyeBridge-Assist`) and upload this entire source tree, including `.github`.
2. In GitHub open **Actions → Build EyeBridge Android APK → Run workflow**. If Android build succeeds, download the `EyeBridge-Assist-Android-APK` artifact, extract `app-debug.apk`, and sideload it onto your Android phone.
3. On the phone grant **Camera**, enter the PC's LAN IP and the code printed in the PC terminal, tap **CONNECT & START MONITORING**.
4. First test the **TEST COLD/HELP/URGENT** buttons while the caregiver is watching the dashboard. Then try face monitoring under controlled conditions.

The build downloads Google's published Face Landmarker model as a Gradle prebuild dependency; its `.task` bundle is included in the generated APK. The model and SDK are third-party dependencies with their own licenses/terms.

### Known restrictions

- No ready-made APK has been verified yet. The workflow is source code, not a guarantee of a passing build or device functionality. 
- Audio is played by the caregiver web page only while it is open/unlocked. Silent mode, background restrictions and browser autoplay can prevent sound.
- The camera must point steadily at the patient's eyes, with suitable lighting. No adaptive threshold/calibration wizard yet.
- **Not encrypted:** HTTP messages and access codes travel over the local LAN. Use only a trusted private network, not public Wi-Fi; avoid putting it on the Internet. The session code is not strong authentication. No cloud or video upload is included.
- The relay must keep running on a PC, so it is not currently an independent phone-to-phone application. A fully native caregiver Android app and authenticated secure transport are future milestones.
- A network acknowledgement means the relay received an event, **not** that the caregiver saw, heard or acknowledged it. The patient app does not currently show caregiver confirmation.
- This is not appropriate for unsupervised clinical/patient use or medical emergency reliance.

## Roadmap (ongoing — not yet done)

- [x] Open-source project scaffolding and documentation
- [x] Android camera and model integration source
- [x] Long-blink intent parser source with disabled-on-face-loss behavior
- [x] Local caregiver web dashboard + relay
- [x] Relay integration tests
- [ ] Verify Android APK build and real-device camera recognition
- [ ] Perform consenting user-centered needs assessment and supervised usability studies, including disability-community feedback
- [ ] Per-user calibration with explicit sensitivity profiles and a guided training mode
- [ ] Reduce accidental activation with a confirmation flow and caregiver acknowledgement
- [ ] Dedicated Android caregiver app with reliable notifications and secure authenticated transport
- [ ] Independent safety and accessibility evaluation, error/latency measurements, privacy/threat model
- [ ] Multiple languages, configurable custom request cards, large-type and audio prompts

## Contributing

Issues and PRs welcome for low-vision/low-mobility UX, calibration, robust network transport, accessibility testing, tests, documentation and translated interface strings. See [CONTRIBUTING.md](CONTRIBUTING.md). Do not publish patient videos or health data; never make clinical claims based on this prototype.

## Claude for Open Source — transparent project description

EyeBridge Assist is a **new, ongoing open-source accessibility prototype** that explores camera-based intentional eye-gesture communication for individuals with limited movement. As of this initial release, the project has no demonstrated adoption, independent contributors, clinical testing or verified patient outcomes. If accepted into the Claude for OSS program, we'd use developer assistance for tests, Android app review, privacy/threat modeling, documentation, accessibility improvements and careful community contributions. **Anthropic approval cannot be promised by creating a new repository.**

## Attribution

Project code: MIT license in `LICENSE`. Uses Google [MediaPipe Face Landmarker](https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker) and [Android CameraX](https://developer.android.com/media/camera/camerax). This project does not include patient photos, medical datasets or scraped clinical content.
