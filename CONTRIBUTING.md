# Contributing to EyeBridge Assist

EyeBridge Assist is an ongoing **non-clinical** accessibility research prototype. Open issues and PRs are welcome.

## Contributions we value
- User-centered accessibility and UX research proposals from consenting participants
- Tests and failure-mode analysis, especially accidental blink triggers and tracking loss
- Robust yet privacy-preserving device pairing, transport, encryption and acknowledgements
- Device reproducibility guides, localization and documentation

## Safety and privacy
- **No patient videos, images, medical histories or personal data** in GitHub issues/PRs.
- Do not describe prototype detection as medical-grade or a guaranteed emergency channel.
- Keep a tested independent call-for-help mechanism during any supervised evaluation.
- Obtain meaningful informed consent before collecting or publishing any research data.
- Use synthetic test signals whenever possible.

## Developer setup
Run `python -m unittest discover -s tests -v` from the repository root. The Android project lives in `android/`; use Android Studio with JDK 17. Open a focused PR with a description, a test plan and any relevant accessibility/safety concerns.
