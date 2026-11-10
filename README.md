# EyeBridge Assist — experimental eye-gesture communication

> **Ongoing open-source accessibility prototype (v0.1.0 alpha). Not a medical device or emergency service.** Never rely on it as someone's only way to request help. Maintain an independent reliable call method and reachable caregiver.

EyeBridge Assist explores voluntary eye-closure gestures as a way to send simple messages for people with limited movement. **One Android APK is installed on two phones:** one in *patient* camera mode and the other in *caregiver* receiver mode. The two phones communicate directly over trusted local Wi-Fi, with no server or raw video transmission.

## System

Patient Android phone: CameraX front camera → on-device Google ML Kit eye-open probabilities → guided eye-open / eye-closed calibration → deliberate closure duration → local UDP message.

Caregiver Android phone: UDP listener → on-screen request and alert sound/vibration → network acknowledgment. **An ACK proves only that the caregiver app received the message—not that a person noticed or responded.** Both apps must stay active, with the caregiver screen visible.

## Gesture mapping (experimental, not validated)

| Deliberate closure of both eyes, followed by reopening | Request |
|---|---|
| 0.9–1.9 seconds | I feel cold |
| 2.1–3.7 seconds | I need assistance |
| 4–7 seconds, then repeat the same hold within 12 seconds | Urgent assistance requested (two-step confirmation) |

Short natural blinks and lost face tracking should not create commands. Individuals may not be able to perform these gestures. Calibration, timing, face/eye probability detection and delivery reliability require careful supervised testing.

## Implemented source

- Android patient/caregiver screens in a single app; guided 3-second OPEN and 3-second CLOSED calibration.
- On-device ML Kit Face Detection; face visibility checks; opt-in labeled probability CSV export (not face videos).
- Deterministic Java gesture parser and tests; emergency request requires a second deliberate hold.
- Direct private-LAN UDP port **9876**, six-digit code, short retries, duplicate filtering, and app-level receipt ACK.
- Caregiver visible alerts plus audio/vibration while the caregiver mode is on screen.
- GitHub Actions workflow for JDK 17, Android SDK 35, Java tests and debug APK compilation.

## Verified build

**2026-11-10:** GitHub Actions Android test and debug APK build **passed**, and the APK artifact was uploaded. This verifies compilation and tests only, **not** camera accuracy, network reliability, usability or clinical safety.

**[Download the debug APK from the successful build](https://github.com/Huzaifa23-cop/Eye-dector/actions/runs/42758656503)** → scroll to *Artifacts* → download **EyeBridge-Assist-Android-Debug-APK** → extract **app-debug.apk**. Install on two Android phones if you trust this experimental source/build. This is not a production-signed app.

Alternatively open the **android/** folder in Android Studio (JDK 17) and build locally.

## Safe supervised test walkthrough

1. Connect two Android phones to the same **trusted private Wi-Fi**; keep both screens on and apps visible. Disable battery saver for the supervised demo.
2. On the caregiver phone, select **Caregiver receiver**, copy its local Wi-Fi IPv4 and six-digit code, and tap **START LISTENING**.
3. On the patient phone, select **Patient camera** and allow camera permission. Calibrate OPEN and CLOSED eye states separately, for 3 seconds each.
4. Enter the caregiver phone IPv4 and code. Tap **TEST connection**, then **TEST caregiver notification** while someone is watching the caregiver phone.
5. In a supervised experiment only, try deliberate closures and compare the intended message with what appears on the caregiver phone.
6. If a request isn't confirmed, use another reliable communication method. **Do not treat this prototype as suitable for unattended care or an emergency.**

## Limitations and privacy

- **No independent validation:** false positives, missed requests, poor lighting, glasses, fatigue, occlusion, involuntary closures and device movement can cause errors.
- The six-digit code is **not strong authentication**, and UDP traffic is **unencrypted**; use only a trusted LAN. Never expose UDP 9876 to the Internet.
- The app has no remote/cloud routing, background notifications, escalation, human acknowledgment, independent fault monitoring or guaranteed power/network availability.
- The camera image stays on the patient phone. No images or face video are transmitted; labeled eye-open probability samples can be exported only by user action. Never publish identifiable participant material.
- Participation in any usability research requires informed consent and appropriate safeguarding.

See [protocol](docs/LAN_PROTOCOL.md), [research and safety notes](docs/RESEARCH_AND_SAFETY.md), and [ongoing project status](PROJECT_STATUS.md).

## Roadmap — ongoing

- [x] First open-source Android source, UI, calibration and gesture parser
- [x] Local caregiver alert protocol and Java unit tests
- [x] Successful debug APK build on GitHub Actions (2026-11-10)
- [ ] Two-device supervised end-to-end test on real phones
- [ ] Gesture error and delivery-latency measurements, including failure cases
- [ ] Personalized accessible gesture calibration with community participation
- [ ] Human caregiver acknowledgment and reliable independent fallback design
- [ ] Authenticated encrypted transport and robust lifecycle handling
- [ ] Accessibility audits, translated interface, privacy and ethics review

## Contributing and Claude for Open Source

This is an **early prototype, not an established project with proven usage**. Contributions from people with lived disability experience, caregivers, accessibility practitioners, and Android engineers are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md) and [the draft project notes for Claude OSS](docs/CLAUDE_FOR_OSS_NOTES.md). Do not claim clinical success, fabricated adoption, or guaranteed Claude program acceptance.

**License:** [MIT](LICENSE). ML Kit and Android CameraX are third-party dependencies with separate licenses/terms. There are no patient photos or medical records in this source repository.