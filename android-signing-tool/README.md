# Nomad POS Android Signing Tool

Offline Android app to generate exportable password-encrypted RSA-4096 PKCS12 signing keys, save with Android's document picker, inspect SHA-256 certificate fingerprints, and compare installed Nomad app certificates.

**Separate signing utility only — not a Nomad POS application update.** Signing with a new key cannot upgrade an APK signed by a different key. Keep the old POS app installed until verified data and activation migration.

No INTERNET or storage permissions. Generated keys must never be uploaded to a public repository. Build using JDK 17, Gradle 8.9 and Android SDK 35.
