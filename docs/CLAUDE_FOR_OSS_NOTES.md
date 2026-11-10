# Claude for Open Source — truthful project notes (draft, not submitted)

**Project:** EyeBridge Assist
**Phase:** Ongoing, experimental Android accessibility research prototype
**Project site:** GitHub https://github.com/Huzaifa23-cop/Eye-dector
**License:** MIT
**Maturity:** First source release; not medically validated or proven with real device pairs
**Reach and impact:** No verified download counts, dependents, external contributors, or users yet. Do not fabricate adoption statistics.

## Problem and motivation

EyeBridge Assist explores low-cost, camera-based communication for individuals with severe mobility and speech limitations who may retain certain voluntary eyelid movements. Two Android phones use local eye-state inference, deliberate gesture decoding, and caregiver-facing request notifications. The project explicitly requires human-centered participatory design, informed consent, measured safety/reliability and alternative communication channels.

## What has been implemented so far

- Android Java source for a combined patient and caregiver app.
- Local on-device ML Kit eye-open probability, open/closed calibration prompts and opt-in labeled probability export.
- Experimental eye-closure gesture parser with two-step urgent confirmation.
- Wi-Fi UDP prototype with app-level acknowledgments and duplicate filtering.
- Local gesture and transport checks; GitHub Actions Java unit tests and debug APK build **passed on 2026-11-10**; physical two-phone testing has **not** been verified.

## What is NOT done / not claimed

No deployed medical product, no clinical efficacy, no demonstrated real-world caregiver reliability, no external adoption, no published participant data, no guaranteed emergency calling, no automated remote notification while sleeping or outside a trusted LAN, and no eligibility guarantee for any grant or subscription.

## How Claude access could help, if independently approved

Code-review critical Android lifecycle and networking paths; develop accessibility-first UI and tests; document bias/lighting failure conditions; improve open-source contributor onboarding; maintain precise technical documentation; assist static analysis and CI; help plan careful participatory research and privacy reviews. Any outputs would be reviewed by maintainers and qualified domain experts.

## Before applying

1. Publish the repository with real code, license, and honest status.
2. Pass the Android CI build and reproduce a two-phone demo; record evidence without identifying people.
3. Obtain independent review from AAC/disability community contributors and document authentic issues and merged PRs.
4. Publish verifiable adoption/impact metrics, and evaluate current eligibility at https://claude.com/contact-sales/claude-for-oss.
5. Use actual evidence in the application form. A newly published prototype alone is unlikely to qualify under the currently stated thresholds.
