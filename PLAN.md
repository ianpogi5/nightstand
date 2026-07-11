# Nightstand — Plan

An open-source, ad-free, tracker-free StandBy mode for Android. Charge your
phone in landscape, get a glanceable nightstand display: clock + today's
calendar. Nothing else running, nothing phoning home.

Born out of frustration with "StandBy Mode Pro"-style apps: paywalls, ads on
the charger, 33 permissions, third-party data sharing. Nightstand's contract
is the opposite:

- **No ads, no IAP, no analytics, no network access at all** (no `INTERNET`
  permission — the strongest privacy statement an Android app can make).
- **Minimal permissions**, each one user-visible and optional where possible.
- Target device: Samsung Galaxy S23+ (One UI 7 / Android 15), but plain AOSP
  APIs only — no Samsung SDK dependencies.

## MVP (v0.1)

1. **StandBy activity**: full-screen, black background (OLED), landscape.
   - Large clock (analog + digital styles, start with one good digital face).
   - Today's calendar events (next 3–5) beside the clock, iOS-StandBy layout.
   - Battery/charging indicator, subtle.
   - `FLAG_KEEP_SCREEN_ON`; brightness follows a dim "night" level with a
     manual toggle.
   - Pixel-shift every minute to avoid OLED burn-in.
2. **Auto-launch when charging + landscape**:
   - Charging detection: `ACTION_POWER_CONNECTED` is **not** on the implicit
     broadcast exceptions list, so a manifest receiver won't fire on modern
     Android. Verify at implementation time; expected approach is a small
     foreground service (started on boot + when app opened) with a
     runtime-registered receiver, OR launch-on-unlock via
     `ACTION_LOCKED_BOOT_COMPLETED`/WorkManager charging constraint as a
     fallback. Keep the service dumb and battery-free (no polling).
   - Landscape detection via sensor while charging.
   - Launching an activity from the background requires the
     `SYSTEM_ALERT_WINDOW` ("Display over other apps") permission — same as
     the commercial apps. Request it in onboarding with a plain-language
     explanation.
   - Document the One UI gotcha in-app: user must set battery →
     "Unrestricted" or One UI will kill the trigger service.
3. **Calendar**: `READ_CALENDAR` runtime permission, `CalendarContract`
   query for today's instances. App works without it (clock-only) if denied.
4. **Settings**: clock style, night dim level, auto-launch on/off,
   landscape-only on/off, which calendars to show.

## Later (v0.2+)

- More clock faces; photo frame mode (local photos only); weather via
  user-supplied provider *only if* we ever accept a network permission —
  default answer is no, keep it offline.
- Notification peek (needs notification listener — big permission, keep
  opt-in and off by default).
- Widgets host (RemoteViews) like iOS StandBy widget stacks.
- F-Droid release (no-network build makes inclusion easy).

## Tech stack

- Kotlin + Jetpack Compose, single-module Gradle project (Kotlin DSL).
- `minSdk 29`, `targetSdk` latest stable.
- No third-party runtime deps beyond AndroidX/Compose BOM. No Firebase,
  no crash reporting, no analytics SDKs — crash logs stay on device
  (optionally exportable by the user).
- Plain `Activity` + Compose; DataStore for settings; no DI framework
  (project is too small to justify one).

## Permissions budget (hard cap)

| Permission | Why | Optional? |
|---|---|---|
| `READ_CALENDAR` | today's events | yes — clock works without |
| `SYSTEM_ALERT_WINDOW` | auto-launch from background | yes — manual launch works without |
| `RECEIVE_BOOT_COMPLETED` | restart trigger service after reboot | yes |
| `FOREGROUND_SERVICE` (+ typed variant) | charging-trigger service | needed for auto-launch only |
| `POST_NOTIFICATIONS` | the FGS notification Android requires | comes with the above |

Anything beyond this list needs a written justification added here first.
**Never**: `INTERNET`, location, contacts, storage.

## Milestones

- [x] M1: Gradle scaffold builds; empty Compose activity runs on the S23+.
- [x] M2: Clock face + keep-screen-on + dim + burn-in shift (manual launch).
- [x] M3: Calendar column.
- [x] M4: Auto-launch service (charging + landscape), onboarding for
      permissions, One UI battery-exemption guidance.
- [ ] M5: Settings screen; polish; v0.1 tag + APK via GitHub Actions.

Status 2026-07-11: M1–M4 implemented and building; **none of it verified
on the S23+ yet** (no adb device was attached). CI builds a debug APK.
Remaining for v0.1: on-device verification pass, dim-level/clock-style
settings, calendar picker, tag + release.

Implementation notes vs. the original plan:
- SharedPreferences instead of DataStore — BootReceiver needs a
  synchronous read, and the settings surface is a few booleans.
- Landscape detection reads the accelerometer directly (gravity on the
  x-axis) rather than OrientationEventListener, and only between
  power-connected and launch/disconnect.

## Verification

Each milestone gets verified on the actual S23+ over `adb` (wireless
debugging). M4 specifically: plug in → rotate → app appears; unplug →
app exits; survives reboot; survives One UI "sleeping apps" once exempted.
