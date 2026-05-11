# Stores Tab — Phase 1 Roadmap

## Confirmed Behaviour
- Finishing shopping (per-store or global) uses a **single source of truth**: marking items
  as finished removes them from the shopping list for ALL stores — there is no per-store cart state.
- "Change to store view" in the ViewGroceries sub-screen **replaces** the existing
  Switch-to-Store-View toggle (the toggle is hidden inside the Stores tab).
- Phase 2 (separate task): remove the Shopping List tab entirely; Stores tab takes over.

---

## Screen Architecture

```
StoresScreen
├── Landing          — two vertical buttons
├── ViewGroceries    — shopping-list-style view (no store toggle, change-to-store-view navigates to SelectStore)
├── SelectStore      — grid of store cards (only stores with active shopping-list items)
└── StoreDetail      — filtered shopping list for one store + exit button
```

Managed by a sealed class inside `StoresScreen.kt`:
```kotlin
sealed class StoresSubScreen {
    object Landing        : StoresSubScreen()
    object ViewGroceries  : StoresSubScreen()
    object SelectStore    : StoresSubScreen()
    data class StoreDetail(val storeId: String) : StoresSubScreen()
}
```

---

## Step-by-Step Implementation Plan

### Step 1 — Drawable icon for Stores tab
- Add `ic_store.xml` vector drawable to `res/drawable/`
  (simple shop/storefront outline icon)

### Step 2 — String resources
New strings to add to `strings.xml` (+ translations):
- `nav_stores`                 — "Stores"
- `view_groceries`             — "View Groceries"
- `select_a_store`             — "Select a Store"
- `change_to_store_view`       — "Change to Store View"
- `items_to_buy`               — "Items to Buy: %d"
- `unique_items`               — "Unique: %d"
- `no_stores_with_items`       — "No stores have items in the shopping list"
- `exit`                       — "Exit"

### Step 3 — BottomNavigationBar
- Add a third `NavigationItem` with route `"stores"`, label `nav_stores`, icon `ic_store`

### Step 4 — MainActivity routing
- Add `"stores"` to the `when (currentRoute)` block, rendering `StoresScreen()`

### Step 5 — StoresScreen.kt (new file)
Full screen composable managing `StoresSubScreen` state via `remember { mutableStateOf(...) }`.

#### 5a. Landing sub-screen
- Two full-width `Button` composables, stacked vertically, centred on screen:
  - "View Groceries" → `subScreen = ViewGroceries`
  - "Select a Store" → `subScreen = SelectStore`

#### 5b. ViewGroceries sub-screen
Reuses the **same components** as ShoppingListScreen (category view only):
- Header row: BurgerMenu, current date Text, create-grocery FAB
- Search bar + Collapse All button
- "Change to Store View" button → `subScreen = SelectStore`
- `HierarchicalCategoryDisplay` of items where `inShoppingList=true && !isBought`
  (isShoppingList=true so item rows show bought/remove actions)
- "Already Bought" collapsible section
- Finish Shopping button (same logic as ShoppingListScreen — full list finish)

`ShoppingListScreen` is **not modified**. ViewGroceries is a self-contained composable
inside `StoresScreen.kt` that calls the same child components directly.

#### 5c. SelectStore sub-screen
Data logic:
```
activeStores = stores where:
  ∃ grocery: grocery.inShoppingList && storeId ∈ grocery.storeIds

For each store:
  itemCount   = groceries where inShoppingList && storeId ∈ storeIds
  uniqueCount = groceries where inShoppingList && storeIds == [storeId]  (linked to ONLY this store)
```

UI:
- Back arrow / header "Select a Store"
- `LazyVerticalGrid(columns = GridCells.Fixed(2))` of `StoreSelectionCard` composables
- Each card:
  - Store name (bold, centred)
  - "Items: N" text
  - "Unique: N" text
  - Black thin border, rounded corners, same card style as grocery cards
- Tap → `subScreen = StoreDetail(store.uuid)`
- If `activeStores` is empty: show "No stores have items in the shopping list" message

#### 5d. StoreDetail sub-screen
Parameters: `storeId: String`

Data logic:
```
storeItems    = groceries where inShoppingList && storeId ∈ storeIds
toBuy         = storeItems where !isBought
alreadyBought = storeItems where isBought
```

UI:
- Header: Exit button (→ `subScreen = SelectStore`), store name, (no create/burger needed)
- `HierarchicalCategoryDisplay` filtered to `toBuy` items
- "Already Bought" collapsible section for `alreadyBought`
- Finish Shopping button:
  - Same logic as existing `confirmFinishShopping()` in DataManagerObject
  - Applied only to `storeItems` (not the full shopping list)
  - After finish: navigate back to `SelectStore`

---

## Files to Create / Modify

| File | Change |
|------|--------|
| `res/drawable/ic_store.xml` | **CREATE** — store vector icon |
| `res/values/strings.xml` | **MODIFY** — add new strings |
| `ui/components/BottomNavigationBar.kt` | **MODIFY** — add Stores tab |
| `MainActivity.kt` | **MODIFY** — add "stores" route |
| `ui/screens/StoresScreen.kt` | **CREATE** — full Stores screen |

`ShoppingListScreen.kt` and `HierarchicalCategoryDisplay.kt` are **not touched**.

---

## Implementation Order
1. `ic_store.xml`
2. `strings.xml`
3. `BottomNavigationBar.kt`
4. `MainActivity.kt`
5. `StoresScreen.kt` — Landing
6. `StoresScreen.kt` — ViewGroceries
7. `StoresScreen.kt` — SelectStore (data helpers + UI)
8. `StoresScreen.kt` — StoreDetail (filtered display + finish shopping)
