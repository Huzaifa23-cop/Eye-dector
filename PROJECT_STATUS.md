# EyeBridge Assist — Ongoing research project

**Repository:** https://github.com/Huzaifa23-cop/Eye-dector  
**Stage:** experimental Android accessibility prototype, v0.1.0 alpha. **Not a medical device or emergency system.**

Implemented source: one Android app with selectable **patient camera** and **caregiver receiver** modes; Android CameraX + on-device ML Kit Face Detection eye-open scoring; guided open/closed calibration; deliberate closure-duration parser and two-step confirmation for urgent requests; a trusted-LAN UDP receiver with app-level receipt ACK; visible/audible caregiver alert screen; opt-in labeled calibration CSV export; Java unit tests and APK build GitHub Actions workflow.

Verified before publication: source inspection and included gesture unit test logic. **Not yet established:** a passing GitHub APK build, device-to-device pairing, end-to-end reliability, background notification delivery, accessibility suitability, encryption, real-user trials, clinical validation, or impact metrics. These claims must be updated only when independent evidence exists.

**Next gates:** (1) CI APK build, (2) two-phone supervised test, (3) latency/error-rate data, (4) participatory accessibility design with informed consent and external review, (5) robust security and caregiver acknowledgment.

Do not upload identifiable participant material or make promises about emergency safety, clinical benefits, active users or Claude for OSS selection.
