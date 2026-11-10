# Experimental LAN protocol v0.1

**Not encrypted, not secure authentication, not medical-grade or reliable emergency transport.** Only use trusted private Wi-Fi; keep both phones active.

- Caregiver listens UDP **9876** on a private IPv4 interface.
- Patient transmits UTF-8 text: `EB1|<6-digit-code>|<UUID>|<PING|TEST|COLD|HELP|EMERGENCY>`.
- Caregiver replies to each correctly paired, valid message: `ACK|<code>|<UUID>` to the sender UDP port.
- Patient sends up to nine attempts, waiting up to 400 milliseconds per attempt. The same message UUID is used for retries; caregiver de-duplicates recent UUIDs.
- `PING` means test connectivity; `TEST` means test visible caregiver notification (not a patient request).
- A network ACK confirms only that the receiving *application* replied, not caregiver awareness or action.
- No secret storage, backend, cloud, location, identification, images, microphone, or raw face data is transmitted.

## Known failure modes

Spoofing, message observation, guessed codes, replay beyond small de-duplication window, dropped UDP datagrams, device sleep, battery saver, firewall/client isolation, disabled Wi-Fi, noise, incorrect IP and caregiver inattention. Production-grade design would require encrypted authentication, acknowledged human workflow, supported foreground/background service policy, independent fallback pathways, availability/fault monitoring and formal usability review.
