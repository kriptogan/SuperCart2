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

```bash
bash tools/check-size-rules.sh               # size rules from .claude/coding-rules.md (must print OK)
```

- ~570 unit tests, full run ≈ 5–10 min. Every test class runs in its own JVM (`forkEvery = 1` in
  `app/build.gradle.kts`): the app state lives in process-wide singletons that otherwise leak between classes and
  make Compose tests hang with `AppNotIdleException`. Don't remove it.
- `testOptions.unitTests.isReturnDefaultValues = true`, so `android.util.Log` is a no-op in plain JVM tests.
- Robolectric: `@RunWith(RobolectricTestRunner::class)`, `@Config(sdk = [34])`. Compose UI tests use
  `createComposeRule()` and English strings; tall screens/dialogs need `qualifiers = "h1200dp"` (a width qualifier
  hangs). Reset state with `TestData.reset()` in `@Before`.
- Firebase: mock `FirebaseFirestore.getInstance()` with `mockkStatic` **before** `FirebaseManager` is first touched,
  then stub with `mockkObject`; see `data/FirebaseManagerFakes.kt`.
- Windows quirks: DataStore must stay ≥ 1.1 (1.0.0 can't overwrite its file); `FileProvider.getUriForFile` needs
  stubbing; Coil may keep image files open, so don't assert deletion right after display.
- `app/src/androidTest` only contains the template test (all UI tests run on the JVM via Robolectric).
- Tests marked "Pinned" or "(bug)" assert current behaviour that looks like a bug (see Known issues). If you fix
  the bug, update the test in the same change, deliberately.

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
  (drops emptied sub-categories/categories).
- Gson for persistence/backups comes from `gsonBuilder()` in `data/JsonConfig.kt`.
- Shared dialog pieces (title, text-field colors, cancel/confirm buttons) live in `ui/components/common/DialogKit.kt`.
- Large files are split by concern with the source name as prefix (e.g. `StoresScreenStoreDetail.kt`,
  `DataManagerObjectStores.kt`, `FirebaseManagerMappers.kt`); screens keep UI state in `*State.kt` holders.
- Strings in `res/values{,-iw,-ru,-bg}/strings.xml`; keep all four in sync.
- Theming: `ui/theme` (`SuperCartColors`, palettes selectable in Settings).

## Simplification backlog
- Duplicated composables across screens: search + expand/collapse row, finish-shopping confirmation,
  bought-grocery card, top bar with burger menu.
- `EditCategoryDialog` ignores its `onCategoryUpdated`/`onSubCategory*` callbacks; `BottomNavigationBar.selectedItem`
  is written but never read; `searchQuery` is accepted but unused by the category/store display composables.

## Sync and persistence rules (established while fixing bugs)
- Entities read from JSON (DataStore, backups) go through `normalized()` (`data/JsonConfig.kt`): Gson bypasses
  Kotlin defaults, so missing lists/strings would otherwise be `null`.
- Backup restore is atomic: parse, normalize and validate everything before replacing in-memory data. Backups
  include `storeSubCategoryOrders`.
- Firestore: `storeCategoryOrders/orders` holds `orders` (category orders) and `subOrders` (sub-category orders).
  Malformed documents are skipped (logged); download drops orphaned sub-categories/groceries and deleted stores.
  Only images of fully visible groceries are synced.
- Reordering (`swapCategoryOrder` / `swapStoreOrder`) renumbers visible items 1..n so ties always move.
- Any mutation that must sync has to stamp `lastUpdate`.

## Known issues (intentionally not changed)
- Hidden stores are only hidden in the shopping list's store view; the Stores tab still lists them (design choice).
- Backups use the deprecated `Environment.getExternalStoragePublicDirectory` (works; migrating to MediaStore/SAF is
  a larger change).
- `CreateCategoryDialog` counts deleted categories when choosing the next `viewOrder` (harmless gaps).
