# Nightstand

An ad-free, tracker-free StandBy mode for Android. Charge your phone in
landscape and get a glanceable nightstand display — clock and today's
calendar — with **no ads, no in-app purchases, no analytics, and no network
permission at all**.

Inspired by iOS 17's StandBy. Built because the Play Store alternatives
show you an ad the moment your phone touches the charger.

Status: **pre-release** — clock, calendar, and charger auto-launch are
implemented; on-device verification and v0.1 are in progress. See
[PLAN.md](PLAN.md).

## Building

```sh
./gradlew assembleDebug   # APK in app/build/outputs/apk/debug/
```

Requires only a JDK on PATH and the Android SDK (a `local.properties`
with `sdk.dir=...`); the Gradle toolchain provisions its own JDK 17.

## Principles

- No `INTERNET` permission. The app cannot phone home even if it wanted to.
- Minimal permissions, each optional where the platform allows.
- Plain AOSP APIs, no vendor SDKs. Primary test device: Galaxy S23+.

## License

MIT
