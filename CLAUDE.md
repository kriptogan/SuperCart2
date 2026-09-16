# SuperCart2

## Description
SuperCart2 is an Android app for managing household groceries. It keeps a catalogue of groceries organised
in categories and sub-categories, with expiration dates, photos, linked stores and purchase history, and
turns it into a shopping list that can be viewed per category or per store.

## Purpose
Make grocery shopping easier to plan and share:
- **Know what to buy** — alerts for items that are expiring or due to be bought again (based on the average
  interval between past purchases).
- **Plan the trip** — see the shopping list per store, hide stores you won't visit, and walk each store in
  your own category/sub-category order.
- **Shop together** — share the same data with family members through a group code synced via Firebase.
- **Never lose data** — local persistence plus backup/restore to a ZIP file.
- **Everyone can use it** — English, Hebrew (RTL), Russian and Bulgarian.

## Coding rules
@.claude/coding-rules.md

## Tech overview
Single-module Kotlin + Jetpack Compose (Material3), no ViewModels, no DI, no Navigation library. Local
persistence via DataStore (JSON strings through Gson); optional sharing through Firebase Firestore + Storage,
scoped by a "group code" (UUID).

## Build & test

No `java` on PATH — use Android Studio's JBR:

```bash
export JAVA_HOME="/d/Android/Android Studio/jbr"
./gradlew testDebugUnitTest                  # JVM + Robolectric unit tests
./gradlew createDebugUnitTestCoverageReport  # JaCoCo → app/build/reports/coverage/test/debug
./gradlew assembleDebug
```

- `testOptions.unitTests.isReturnDefaultValues = true`, so `android.util.Log` is a no-op in plain JVM tests.
- Tests touching `Context`/DataStore/files use Robolectric (`@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [34])`).
- `DataManagerObject` is a global singleton: every test must reset it (see `TestData.reset()` in `app/src/test`).
- Anything that touches `FirebaseManager` initialises `FirebaseFirestore.getInstance()` in the object
  initialiser and cannot run in unit tests. `FirebaseStorageManager` likewise. They are untested.
- `app/src/androidTest` only contains the template test; there are no Compose UI tests.
- DataStore must stay ≥ 1.1: 1.0.0 cannot overwrite its file on Windows, which breaks Robolectric tests there.
- Tests marked "Pinned" assert current behaviour that looks like a bug (see Known issues). If you fix the bug,
  update the test in the same change, deliberately.

## Architecture

```
MainActivity ── MainApp (route state "home" | "stores", BottomNavigationBar)
   ├─ HomeScreen            full catalogue, search, "alerts" filter, BurgerMenu → management dialogs
   └─ StoresScreen          own sub-screen state machine: landing → select store → store detail (Buying | Edit)
        └─ ShoppingListScreen   reachable only from Stores → "View groceries"

ui/components/*Dialog.kt ──► DataManagerObject (in-memory state, mutations)
                         └─► DataStoreManager.saveDataGlobally() (persist)   ← callers must remember to call this
BurgerMenu / group dialogs ─► FirebaseManager.uploadData()/downloadData(), GroupManager, BackupManager
```

### Data model (`models/`)
`Category` → `SubCategory` (`categoryId`) → `Grocery` (`categoryId`, `subCategoryId`, `storeIds`).
`Store` is independent; groceries link to stores by `storeIds`. Every entity has `uuid`, `lastUpdate`,
`deleted`.

- **Soft delete everywhere.** Deletes set `deleted = true`; items stay in memory/DataStore/Firebase so
  deletions can sync. UI reads through `getSortedCategories()` / `getSortedStores()`, which filter them out.
  Reading `DataManagerObject.categories` directly includes deleted items.
- **`lastUpdate` drives sync conflict resolution.** `update*` helpers stamp `lastUpdate = now()`.
  `FirebaseManager.uploadData` only writes an item if local `lastUpdate` is strictly after Firebase's.
  Mutations that don't stamp it (see Known issues) may silently not sync.
- `protected` categories/sub-categories ("Other", every default "General") can't be deleted in the UI.
- Default data (`DataInitializer`): Other(1, protected), Vegetables(2), Fruits(3), each with a protected
  "General" sub-category. Created when there are no categories after load. Names are English in data and
  translated at display time (`utils/LocalizedCategoryNames.kt`).

### `data/`
- **`DataManagerObject`** — the app state. `categories: SnapshotStateList<CategoryWithSubCategories>` (nested
  tree), `stores`, `hiddenStoreIds`, `storeCategoryOrders` (storeId → categoryIds),
  `storeSubCategoryOrders` (storeId → categoryId → subCategoryIds). Composables recompose by reading
  `version`, which every mutation bumps via `updateData()`. Business rules live here:
  - `toggleShoppingListStatus` also resets `isBought`.
  - `confirmBoughtItems[ForStore]`: for bought items, append today to `buyEvents` (once per day), clear
    `isBought`/`inShoppingList`, recompute `averageBuyDays` = floor(mean gap between the newest ≤4 events),
    null if <2 events.
  - `getSortedCategoriesForStore` / `groupGroceriesByStore`: custom per-store order first, remaining items
    by global `viewOrder`. Hidden stores are omitted; groceries with no store go into a `store = null` group.
  - `deleteStore`: unlinks from groceries, soft-deletes, removes hidden flag and both per-store orders.
- **`DataConverter`** — nested tree ⇄ flat lists, plus `validateRelationships` (all parent IDs exist).
- **`DataStoreManager`** — Gson (with LocalDate/LocalDateTime ISO adapters) into the `supercart_data`
  Preferences DataStore, one key per flat list. `loadData` validates relationships, drops deleted stores,
  falls back to defaults. Also stores view mode ("STORE"/"CATEGORY") and the group code. Holds
  `globalContext` so non-UI code can `saveDataGlobally()`.
- **`BackupManager`** — ZIP (`data.json` + `images/<uuid>.jpg`) in public Downloads; restore accepts ZIP or
  legacy `.json`. Restore re-stamps every `lastUpdate` to now (so the restore wins the next upload).
- **`FirebaseManager`** — Firestore under `groups/{groupCode}/{categories|subcategories|groceries|stores}`
  plus `storeCategoryOrders/orders`. Upload = per-item last-write-wins; download = **replace all local data**.
- **`FirebaseStorageManager`**, **`ImageManager`** — grocery photos: compressed JPEG in `cacheDir`, synced to
  `grocery_images/{uuid}.jpg`.
- **`GroupManager`** — create (random UUID) / join (validate UUID format, group must have ≥1 category, then
  download) / leave (clears code, keeps data).
- **`SettingsManager`** (`supercart_settings`: show alerts, color palette) and **`utils/LanguageManager`**
  (`language_settings`: en/iw/ru/bg; iw is RTL; applied in `MainActivity.attachBaseContext`).

### UI conventions
- Composables mutate `DataManagerObject` directly, then `scope.launch { DataStoreManager.saveDataGlobally() }`.
- "Alert" = expiration date ≤ tomorrow, or days since last buy ≥ `averageBuyDays` → use `Grocery.hasAlert()`
  (`models/GroceryAlerts.kt`).
- To filter the category tree by a grocery predicate use `List<CategoryWithSubCategories>.filterGroceries { }`
  (drops emptied sub-categories/categories). Several screens still hand-roll this (ShoppingListScreen,
  StoresScreen, HomeScreen search) — see "Simplification backlog".
- Gson for persistence/backups comes from `gsonBuilder()` in `data/JsonConfig.kt`.

## Simplification backlog (UI code has no tests — cover it before changing behaviour)
- Hand-rolled tree filters: `ShoppingListScreen` to-buy/bought, `StoresScreen` per-store to-buy/bought,
  `HomeScreen.filterAndExpandData` (returns `mutableStateListOf` lists, so check before swapping in `filterGroceries`).
- `onGroceryCreated` handlers duplicated across HomeScreen / ShoppingListScreen / StoresScreen, and the
  dialog already saves; HomeScreen rebuilds nested lists by hand instead of `updateGrocery`/`updateGroceryLocation`.
- Duplicated composables: search + expand/collapse card (4×), finish-shopping confirmation (2×), bought-grocery
  card (2×), top bar with burger menu (2×), Create*/Edit* dialog button rows, up/down reorder lambdas.
- `EditCategoryDialog` ignores its `onCategoryUpdated`/`onSubCategory*` callbacks; `HomeScreen.dataRefreshTrigger`
  and `BottomNavigationBar.selectedItem` are written but never read.
- `FirebaseManager` entity ⇄ Firestore map conversion could become pure functions (would make sync testable).
- Strings in `res/values{,-iw,-ru,-bg}/strings.xml`; keep all four in sync.
- Theming: `ui/theme` (`SuperCartColors`, palettes selectable in Settings).

## Known issues (found during onboarding, intentionally not fixed yet)
- **`confirmBoughtItems` / `confirmBoughtItemsForStore` lose updates**: when several sub-categories of the same
  category contain bought items, only the last one is confirmed (each write rebuilds the category from a stale
  snapshot). Pinned by "(bug)" tests in `DataManagerObjectTest`.
- `Store.address` is not written to or read from Firestore (`storeToMap` / `documentToStore`) → lost on sync.
- `storeSubCategoryOrders` is neither in backups nor synced to Firebase.
- `updateGroceryLocation` and `unlinkStoreFromAllGroceries` don't bump `lastUpdate`, so those changes can be
  skipped by upload conflict resolution.
- `downloadData` keeps deleted stores in memory, while `loadData`/restore drop them (display filters them anyway).
- Gson ignores Kotlin default values: JSON missing a field (older saves) yields `null` in non-null properties.
  A backup whose groceries lack `storeIds`/`buyEvents` is rejected entirely by restore (the re-stamping
  `copy()` throws). Pinned in `BackupManagerTest`.
- Grocery saves run 2–3 times per edit (dialog saves, then parent's `onGroceryCreated` saves again).
- Legacy file backups use `Environment.getExternalStoragePublicDirectory` (deprecated; scoped storage).
