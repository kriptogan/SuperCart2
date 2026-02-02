# Store-Specific Category Ordering - Implementation Roadmap

## Overview
Add the ability to customize the order of categories displayed under each store in the shopping list store view. Each store can have its own category ordering, independent of the global category order.

## Requirements Summary
- **UI Location**: Store Management Dialog
- **Reordering Method**: Up/Down arrows (like stores)
- **Scope**: Only affects Shopping List store view
- **Empty Categories**: Hide categories with no items in that store
- **Default Behavior**: Use global category order (viewOrder) if no custom order exists
- **Data Structure**: Separate mapping (storeId → List<categoryId>)
- **Cloud Sync**: Include in upload/download functionality

---

## Phase 1: Data Model & Storage

### 1.1 Data Structure
**File**: `app/src/main/java/com/example/supercart2/data/DataManagerObject.kt`

- Add property to store category order mapping:
  ```kotlin
  var storeCategoryOrders: MutableMap<String, List<String>> = mutableMapOf()
  ```
  - Key: Store UUID
  - Value: List of Category UUIDs in desired order

### 1.2 Data Persistence
**File**: `app/src/main/java/com/example/supercart2/data/DataStoreManager.kt`

- Add new preference key:
  ```kotlin
  private val STORE_CATEGORY_ORDERS_KEY = stringPreferencesKey("store_category_orders")
  ```

- Add save function:
  ```kotlin
  suspend fun saveStoreCategoryOrders(orders: Map<String, List<String>>)
  ```
  - Serialize Map<String, List<String>> to JSON string
  - Save to DataStore

- Add load function:
  ```kotlin
  suspend fun loadStoreCategoryOrders(): Map<String, List<String>>
  ```
  - Load from DataStore
  - Deserialize JSON string to Map<String, List<String>>
  - Return empty map if not found

- Update `loadData()` to load store category orders
- Update `saveDataGlobally()` to save store category orders

### 1.3 Cloud Sync
**File**: `app/src/main/java/com/example/supercart2/data/FirebaseManager.kt`

- Add to upload function:
  - Include `storeCategoryOrders` in the data structure uploaded to Firebase
  - Key: `"storeCategoryOrders"` → Map<String, List<String>>

- Add to download function:
  - Read `storeCategoryOrders` from Firebase data
  - Deserialize and populate `DataManagerObject.storeCategoryOrders`
  - Handle missing data gracefully (default to empty map)

---

## Phase 2: Data Management Logic

### 2.1 Helper Functions
**File**: `app/src/main/java/com/example/supercart2/data/DataManagerObject.kt`

- Add function to get/store category order:
  ```kotlin
  fun getStoreCategoryOrder(storeId: String): List<String>?
  ```
  - Returns custom order if exists, null otherwise

- Add function to set store category order:
  ```kotlin
  fun setStoreCategoryOrder(storeId: String, categoryIds: List<String>)
  ```
  - Updates `storeCategoryOrders` map
  - Increments version to trigger recomposition

- Add function to swap category order for a store:
  ```kotlin
  fun swapStoreCategoryOrder(storeId: String, categoryId1: String, categoryId2: String)
  ```
  - Swaps positions of two categories in the store's custom order
  - Creates order if it doesn't exist
  - Increments version

- Modify `groupGroceriesByStore()`:
  - After grouping categories by store, check if custom order exists
  - If custom order exists, sort categories according to it
  - If not, use global category order (viewOrder)
  - Filter out categories with no items (already done, but ensure it's maintained)

---

## Phase 3: UI Components

### 3.1 Store Category Order Dialog
**File**: `app/src/main/java/com/example/supercart2/ui/components/StoreCategoryOrderDialog.kt` (NEW)

Create new dialog component:
- **Parameters**:
  - `store: Store`
  - `onDismiss: () -> Unit`
  - `onOrderUpdated: () -> Unit`

- **Functionality**:
  - Display list of categories that have items in this store
  - Show categories in current order (custom or default)
  - Up/Down arrows for each category (similar to StoreCard in StoresManagementDialog)
  - Save button to persist changes
  - Cancel button to dismiss without saving

- **UI Structure**:
  ```
  AlertDialog
    title: "Reorder Categories for [Store Name]"
    text:
      LazyColumn
        items: categories (filtered to only those with items in store)
          CategoryOrderCard
            - Category name
            - Up arrow (disabled if first)
            - Down arrow (disabled if last)
    confirmButton:
      - Cancel
      - Save
  ```

### 3.2 Category Order Card Component
**File**: `app/src/main/java/com/example/supercart2/ui/components/StoreCategoryOrderDialog.kt` (within same file)

- **Parameters**:
  - `category: Category`
  - `canMoveUp: Boolean`
  - `canMoveDown: Boolean`
  - `onMoveUp: () -> Unit`
  - `onMoveDown: () -> Unit`

- **UI**: Similar to StoreCard in StoresManagementDialog

### 3.3 Integration with Store Management
**File**: `app/src/main/java/com/example/supercart2/ui/components/StoresManagementDialog.kt`

- Add "Reorder Categories" button/icon to StoreCard
- On click, open StoreCategoryOrderDialog
- Pass store instance and callback to refresh list

---

## Phase 4: String Resources & i18n

### 4.1 Add String Resources
**Files**: 
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-iw/strings.xml`
- `app/src/main/res/values-ru/strings.xml`
- `app/src/main/res/values-bg/strings.xml`

Add translations:
- `reorder_categories` - "Reorder Categories"
- `reorder_categories_for_store` - "Reorder Categories for %s" (with %s placeholder)
- `save_order` - "Save Order"
- `category_order_saved` - "Category order saved" (optional success message)

---

## Phase 5: Testing & Edge Cases

### 5.1 Test Scenarios
1. **New Store**: Create store, verify default order (global viewOrder)
2. **Custom Order**: Set custom order, verify it's used in shopping list
3. **Empty Store**: Store with no items - verify no categories shown
4. **Partial Items**: Store with items in some categories - verify only those categories appear
5. **Order Persistence**: Set order, close app, reopen - verify order persists
6. **Cloud Sync**: Upload, download - verify order syncs correctly
7. **Category Deletion**: Delete category - verify it's removed from custom orders
8. **Store Deletion**: Delete store - verify custom order is cleaned up (optional)

### 5.2 Edge Cases to Handle
- Store with no custom order → use global order
- Category removed from store (no more items) → remove from order if present
- Category deleted globally → remove from all store orders
- Store deleted → remove order entry (optional cleanup)
- Empty order list → fallback to global order
- Invalid category UUIDs in order → filter out, use global order for missing

---

## Implementation Order

1. **Phase 1.1 & 1.2**: Data structure and local persistence
2. **Phase 2.1**: Helper functions in DataManagerObject
3. **Phase 2.1 (modify groupGroceriesByStore)**: Apply custom ordering logic
4. **Phase 1.3**: Cloud sync integration
5. **Phase 4.1**: String resources
6. **Phase 3.1 & 3.2**: Create StoreCategoryOrderDialog component
7. **Phase 3.3**: Integrate into StoresManagementDialog
8. **Phase 5**: Testing and edge case handling

---

## Files to Modify

### New Files
- `app/src/main/java/com/example/supercart2/ui/components/StoreCategoryOrderDialog.kt`

### Modified Files
- `app/src/main/java/com/example/supercart2/data/DataManagerObject.kt`
- `app/src/main/java/com/example/supercart2/data/DataStoreManager.kt`
- `app/src/main/java/com/example/supercart2/data/FirebaseManager.kt`
- `app/src/main/java/com/example/supercart2/ui/components/StoresManagementDialog.kt`
- `app/src/main/res/values/strings.xml` (and locale variants)

---

## Notes

- The custom order only affects the shopping list store view, not the home page or category view
- Categories with no items in a store are automatically hidden (already implemented)
- If a store has no custom order, it falls back to global category order seamlessly
- The order mapping is separate from the Store model to keep it flexible and avoid model changes
- Cloud sync ensures the custom orders are shared across devices when using group sharing
