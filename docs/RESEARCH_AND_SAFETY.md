# Research, participant consent, and safety plan

**Current phase:** technical proof-of-concept, not human validation, no clinical validation, no deployed medical/emergency system.

## Proposed voluntary research activities (not started)

1. Co-design with AAC users, people with severe mobility impairments, their chosen supporters, and accessibility specialists. Check whether blinking and sustained eye closure are voluntary, comfortable, and appropriate. Many people will need a different input method entirely.
2. Explain in accessible language the prototype's limitations and how eye-open probabilities work. Obtain explicit informed consent and suitable ethics oversight before recruiting participants or collecting research data.
3. Provide an always-available independent communication aid and a supervising caregiver during tests. Never ask participants to rely on this app for urgent help.
4. Record **only** expressly authorized anonymized calibration statistics when necessary. The prototype stores data in RAM and exports a CSV only when a user chooses an Android file save location. It does not capture video, health identifiers, or upload data. Prohibit committing participant data to public GitHub.
5. Pre-register test outcomes: accuracy by gesture and by participant, false alarms, missed requests, accessibility burden, discomfort/fatigue, camera positioning robustness, face-loss behavior, phone-to-phone latency and network dropouts.
6. Solicit adverse-event feedback. Stop the study if usability, privacy, or safety concerns arise. Document limitations and abandoned designs publicly without participant identities.

## Limits of eye gesture controls

- Some people cannot voluntarily close both eyes or hold closures for seconds; some may only move pupils or one eyelid.
- Eyes-closed probability output is not a validated measure of voluntary intent.
- An urgent two-stage gesture is a *research-only* control, not a reliable mechanism for obtaining emergency services.
- Wi-Fi reliability and app background restrictions can prevent caregiver notifications; received network ACK does not prove human acknowledgment.
- A future product may require independent emergency call hardware, dedicated accessibility switches, formal human factors work, and regional medical-device/privacy analysis.

## Data handling

- On-device inference and live probability measurements only; the app never saves or sends video or images.
- Manual calibration log exports contain relative timestamps, prompted `OPEN`/`CLOSED` labels, and eye-open probability numbers (personal biometric-related data in context; handle carefully).
- Calibration records are not automatically uploaded or shared, and are cleared on changing modes or destroying the activity. Exported CSV remains at the user-selected location and is their responsibility to protect.
- Do not gather or publish clinical identities or sensitive health context under a public source-code repository.
