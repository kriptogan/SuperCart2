# Stores Management — Implementation Roadmap

## Overview
Introduce **Buying** and **Edit** modes to the store detail page, extend the reorder dialog to support sub-categories per store, wire the stores management dialog to navigate directly into Edit mode, and consolidate the shopping list into the Stores tab by routing "View Groceries" to the existing `ShoppingListScreen` component.

---

## Phase 1 — Data Model

- [x] **1.1** Add per-store sub-category order storage in `DataManagerObject` (`storeSubCategoryOrders`)
- [x] **1.2** Add helper methods: `getStoreSubCategoryOrder`, `setStoreSubCategoryOrders`
- [x] **1.3** Add `getSortedCategoriesForStore` applying both category and sub-category order
- [x] **1.4** Persist `storeSubCategoryOrders` in `DataStoreManager`

---

## Phase 2 — Store Mode

- [x] **2.1** Add `StoreMode` enum (`Buying`, `Edit`)
- [x] **2.2** Update `StoresSubScreen.StoreDetail` to carry a `mode: StoreMode` parameter
- [x] **2.3** Propagate mode down to `StoresDetailView`

---

## Phase 3 — Buying Mode Enhancements

- [x] **3.1** Add current date display (same style as `ShoppingListScreen`)
- [x] **3.2** Add floating FAB (add grocery) — pre-filled with store, store field changeable
- [x] **3.3** Add Edit icon to store header in Buying mode — enters Edit mode

---

## Phase 4 — Edit Mode

- [x] **4.1** Hide the date in Edit mode
- [x] **4.2** No top-right edit icon in Edit mode
- [x] **4.3** Inline edit icon next to store name → text field + confirm button
- [x] **4.4** "Reorder Categories" button (replaces "Things to Buy" label) → opens reorder popup
- [x] **4.5** Edit icon on grocery items (same flow as 3-dots → Edit Item)
- [x] **4.6** Floating FAB (add grocery) in Edit mode — same as Buying mode
- [x] **4.7** Back arrow returns to Select Store screen

---

## Phase 5 — Reorder Categories Popup (Extended)

- [x] **5.1** Extended `StoreCategoryOrderDialog` to show sub-categories under each category
- [x] **5.2** Up/down reorder controls for sub-categories (same pattern as categories)
- [x] **5.3** Save sub-category order per store via `setStoreSubCategoryOrders`
- [x] **5.4** Sub-category order applied when rendering via `getSortedCategoriesForStore`

---

## Phase 6 — Stores Management Dialog Wiring

- [x] **6.1** Edit icon in `StoresManagementDialog` now navigates into store in Edit mode
- [x] **6.2** `onEnterStoreEditMode: (storeId) -> Unit` added to `StoresManagementDialog`
- [x] **6.3** Delete icon added directly to `StoreCard` (replaces EditStoreDialog delete)
- [x] **6.4** Wired in `HomeScreen`, `ShoppingListScreen` (inside Stores sub-screen)
- [x] **6.5** `MainApp` manages `pendingEditStoreId` + switches to Stores tab

---

## Phase 7 — View Groceries → Shopping List Consolidation

- [x] **7.1** "View Groceries" in `StoresLandingView` now opens `ShoppingListScreen` as sub-screen
- [x] **7.2** `StoresViewGroceriesView` removed
- [x] **7.3** Shopping List bottom navigation tab removed from `BottomNavigationBar`
- [x] **7.4** `"shopping_list"` route removed from `MainApp`; `ShoppingListScreen` now
      receives `onBack` and `onNavigateToStoreEdit` callbacks

---

## Phase 8 — Cleanup & Edge Cases

- [x] **8.1** Edit mode redesigned: title shows "Edit Mode", store info card (name + address +
      reorder + confirm) shown as first scrollable item; `address` field added to `Store` model
- [x] **8.2** `EditStoreDialog` no longer opened from `StoresManagementDialog`; delete is
      handled directly in `StoreCard`
- [x] **8.3** Edit mode layout updated: Store Info card scrolls first; search bar moved below it in
      scrollable area (not fixed in header); header only shows back + "Edit Mode" title in edit mode
- [x] **8.4** Confirm button now shows a snackbar ("Done") as visual feedback after saving store info
- [x] **8.5** Back button in Edit mode returns to Buying mode when entered via the edit icon from
      Buying mode (`returnToBuying` flag on `StoreDetail`); from external entry points (e.g.
      StoresManagement), back still goes to SelectStore
- [ ] **8.6** Smoke-test all entry paths (run in Android Studio):
      - Stores tab → Select a Store → select store → Buying mode → tap ✎ → Edit mode → back → Buying mode ✓
      - Stores tab → Select a Store → select store → Buying mode → tap ✎ → Edit mode → Confirm → snackbar ✓
      - Home burger → Manage Stores → tap store edit icon → Edit mode → back → SelectStore ✓

---

## Testing Note

> Compile tests will be run in Android Studio outside of this chat.

---

## Dependencies / Notes

- Sub-category `viewOrder` is a new field — check if any serialization / persistence layer needs migration
- `StoresManagementDialog` is used in `HomeScreen`, `ShoppingListScreen`, and `StoresViewGroceriesView`; the new navigation callback must be handled (or safely ignored with a no-op) in all host screens. After Phase 7, `StoresViewGroceriesView` will be removed so only `HomeScreen` and `ShoppingListScreen` will remain
- `ShoppingListScreen` will become the sole implementation of the groceries list view; `StoresViewGroceriesView` is essentially a duplicate and will be deleted in Phase 7
- Inline store name editing should save on focus-loss or a confirm action — decide before implementing Phase 4.3
