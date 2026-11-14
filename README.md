# Lunex TV

Independent Android TV / Google TV cinema application (Kotlin, Jetpack Compose for TV, Media3).

## Product decisions
- Russian-language, landscape, D-pad-first Cinema interface.
- Unified search and catalog across independently configurable authorized providers.
- Media3 player with subtitles, audio track selection, episodes, resume and error recovery.
- Signed APK update checks with Android system confirmation.
- Offline-friendly metadata/history storage; no video redistribution backend.

## Legal and integration gates
The upstream repositories [KinogoATV](https://github.com/reziarlleh/KinogoATV) and [KinoPub](https://github.com/ia-alpatov/KinoPub) are research references only. No upstream code or assets may be copied without verified permission. Provider integration must respect service terms, content rights, authentication and DRM; no bypassing access controls.

## Initial milestones
1. Verify upstream licenses, dependencies and permitted APIs.
2. Build a clean-room Android TV project and CI.
3. Implement Cinema navigation, focus and catalog UI with test fixtures.
4. Implement common provider interface, metadata merging and search.
5. Implement Media3 player, episode navigation and resume.
6. Implement signed release updates and TV device QA.

## Safety
No VPS deployment is required. Do not alter upstream repositories. Do not publish an APK before build and TV tests. Track release signing credentials outside Git.

## Status
Planning initialized; Android project not yet compiled. Source provider adapters remain blocked pending permission checks.
