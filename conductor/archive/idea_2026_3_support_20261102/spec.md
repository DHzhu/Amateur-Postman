# Spec: 升级 IDEA 版本支持至 2026.3

## Background
- Latest stable: IntelliJ IDEA 2026.2.4 (build 262.11879.100, 2026-10-30) — already covered by `pluginUntilBuild = 262.*`.
- Latest EAP/Beta: IntelliJ IDEA 2026.3 (build 263.7307.44, 2026-10-23).
- Current config: `platformVersion = 2026.2`, `pluginSinceBuild = 253`, `pluginUntilBuild = 262.*`, IDE resolved via hard-coded `local(file("/opt/idea/idea-2026.2"))`, IntelliJ Platform Gradle Plugin 2.16.0, Kotlin 2.4.0, JVM toolchain 25, Gradle 9.4.0.
- `pluginVerification` only targets IU 2025.3.

## Goal
Plugin builds, passes tests, and installs/runs on IntelliJ IDEA 2026.3 (263.*) while keeping compatibility with 253–262.

## Technical Strategy
1. `gradle.properties`: `pluginUntilBuild = 263.*`, `platformVersion = 2026.3`.
2. IDE dependency: replace hard-coded local 2026.2 path with a 2026.3 target (local install under `/opt/idea/` or remote `intellijIdea(...)` with EAP/beta channel via `useInstaller = false` if required).
3. Upgrade IntelliJ Platform Gradle Plugin (and Kotlin/Gradle only if 263 requires it).
4. `pluginVerification`: verify against 2025.3, 2026.2 and 2026.3.
5. Fix compile errors / deprecated & internal API usage reported by build and verifier.

## Constraints
- 2026.3 is still Beta; final release build number may differ — keep `263.*` wildcard.
- No change to plugin features; minimal, surgical config edits.
- Quality gates: `./gradlew build`, `./gradlew test`, `./gradlew verifyPlugin`, coverage >80%.

## Research Findings (2026-11-02)
- 2026.3 incompatible changes (module extraction to bundled plugins: VCS/XML/Tasks; JNA off core classloader; OkHttp unbundled; Kotlin UI DSL v1 removed; `ApplicationLoadListener` removed; `LangDataKeys` type changes; `StdModuleTypes` removed): **none used** by this plugin. OkHttp is already bundled via `implementation(libs.okhttp)`.
- Requires Java 25 (already configured). IntelliJ Platform Gradle Plugin upgraded 2.16.0 -> 2.19.0.
- Build IDE: IU 2026.3 Beta (263.7307.44) installed locally at `/opt/idea/idea-2026.3` (sha256 verified).
- Test infrastructure breakage on 263:
  - 2026.3 test framework ships JUnit Platform 6 (`junit-vintage` failed with `NoSuchMethodError: ExecutionRequest.getCancellationToken()`) -> JUnit 5.10.2 -> 6.1.3 (jupiter + launcher aligned via `version.ref`).
  - Mockito 5.11.0 (ByteBuddy 1.14.12) cannot inline-mock classes implementing 263 platform interfaces -> Mockito 5.24.0, mockito-kotlin 6.4.0.
- Pre-existing bug surfaced: `VariableResolver.safeRandomInt` produced negative values for `$randomInt:min,Int.MAX_VALUE` (`toULong().toLong()` is identity) -> replaced with `Random.nextLong(min, max + 1)`; test hardened to 1000 iterations.
