# AGENTS.md — Shrinkflation Calculator (Android)

## Project
- Path: `/home/stas/dev/android/shr_calc/`
- Package: `com.team.itexperts.shrinkflation.calc`
- Language: Kotlin + Jetpack Compose (Material3)
- Build: Gradle 8.2, AGP 8.2.0, Kotlin 1.9.0

## Environment (Linux only, NO Win_Home)
- JDK: `/home/stas/dev_tools/jdk-17.0.13+11`
- Gradle: `/home/stas/dev_tools/gradle` (GRADLE_USER_HOME)
- Android SDK: `/home/stas/dev_tools/AndroidSDK` (ANDROID_HOME)
- Emulator: `/home/stas/dev_tools/AndroidSDK/emulator/emulator` (AVD `shr_calc`, Android 34 x86_64)
- adb: `/home/stas/dev_tools/AndroidSDK/platform-tools/adb`

Required env vars before any build:
```bash
export JAVA_HOME=/home/stas/dev_tools/jdk-17.0.13+11
export GRADLE_USER_HOME=/home/stas/dev_tools/gradle
export ANDROID_HOME=/home/stas/dev_tools/AndroidSDK
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH
```

## Hard rules
- NEVER use `/home/stas/Win_Home` — Windows mount, noexec, unstable. All tooling must live under `/home/stas/dev_tools/`.
- NEVER modify `happ` or `tinyproxy`.
- If a resource cannot be reached, provide the link to the user instead of guessing.
- Android Studio install path (if needed): `/home/stas/dev_tools/android-studio`.

## Build / test
```bash
cd /home/stas/dev/android/shr_calc
sh ./gradlew :app:assembleDebug
sh ./gradlew :app:testDebugUnitTest
sh ./gradlew :app:connectedDebugAndroidTest
```

## Run on emulator
```bash
$ANDROID_HOME/emulator/emulator -avd shr_calc -no-window -gpu swiftshader_indirect -no-snapshot
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Note: after `connectedDebugAndroidTest` the main APK can be removed from the device — reinstall it before launching the app manually.

## Architecture
- `model/` — enums and data classes (UnitType, MassUnit, VolumeUnit, Currency, PackageInput, results, AppLanguage, ThemeMode)
- `calc/` — pure calculators (UnitConverter, ShrinkflationCalculator, ComparisonCalculator)
- `data/` — Room entities/DAOs/repositories, ProductCard
- `di/` — Hilt modules
- `storage/` — DataStore preferences (Keys: CURRENCY, MASS_UNIT, VOLUME_UNIT, LANGUAGE, THEME, SAVE_PRICES, STORAGE_LIMIT)
- `util/` — LocaleHelper (language switching)
- `ui/` — Compose screens, ViewModels, Labels.kt (localized unit labels), theme/

## DB schema (relational)
- `products` (id, name, unitType)
- `product_variants` (id, productId FK, brand, spec)
- `price_entries` (id, variantId FK, price, quantity, unit, currency, source, createdAt)
- DAO `getLastByVariant` orders by `createdAt DESC` (field is `createdAt`, not `timestamp`)

## UI conventions
- Reusable `ProductFields` composable for name/brand/spec + suggestions
- All screens use `verticalScroll` for overflow safety
- Search is field-specific (name/brand/spec) via `ProductSearch`
- Test tags: price, qty, priceOld, qtyOld, priceNew, qtyNew, calculateBtn
- Measurement units are localized via `ui/Labels.kt`: mass `г/кг`, volume `мл/л`, count `шт`
- Comparison cards use `OutlinedCard`: keep the border, no filled/gray container background
- Error messages must include at least one explanatory line (e.g. product name)

## i18n / localization
- Resources: `res/values/strings.xml` (Russian, default) and `res/values-en/strings.xml` (English). To add a language, add `res/values-<locale>/strings.xml` with the same keys — no code changes needed.
- All user-facing text in screens/navigation goes through `stringResource(...)`; never hardcode Russian text in Compose.
- `AppLanguage` (RU/EN) is stored in DataStore as the locale code; `LocaleHelper.apply()` is called from `MainActivity.attachBaseContext()`.
- Language switching: `SettingsViewModel.setLanguage()` is `suspend` — await the DataStore write, then call `Activity.recreate()`. Recreating before the write completes leaves the old locale active.
- ViewModels inject `@ApplicationContext Context` and use `context.getString(...)` for localized error messages.

## Theme
- `ThemeMode` (SYSTEM, LIGHT, DARK) stored in DataStore key `THEME`.
- `MainActivity` collects `SettingsViewModel.themeMode` and passes `darkTheme` into `ShrinkflationTheme`; LIGHT/DARK apply immediately, SYSTEM uses `isSystemInDarkTheme()`.
- Theme dropdown lives on the Settings screen.

## Conventions
- Offline-first; no network dependency at runtime.
- Room DB version bumped on schema change; provide migration.
- Prices stored as strings (BigDecimal) to avoid float errors.
- Unit tests for calculators must stay ≥90% coverage.
- Prefill latest price/qty when product identity fields (name/brand/spec) are filled and price/qty are blank; never overwrite user-entered values.
- On calculation, save a new price entry unless a recent duplicate (within 5 min) exists.
