# SuperCart2 - Project Overview

## 📋 **Project Summary**
SuperCart2 is an Android grocery management app built with Jetpack Compose. It provides a hierarchical organization system for groceries with categories, sub-categories, and shopping list functionality.

## 🏗️ **Architecture**

### **State Management**
- **Shared Objects (Simple Global State)** approach
- `DataManagerObject` - Central data management using `SnapshotStateList`
- `DataStoreManager` - Persistent storage using DataStore
- Reactive state binding with `derivedStateOf`

### **Data Flow**
```
DataManagerObject.categories (SnapshotStateList)
    ↓ (observed by)
derivedStateOf in UI components
    ↓ (triggers recomposition)
UI updates automatically
```

## 📊 **Data Models**

### **Grocery**
```kotlin
data class Grocery(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String,
    val subCategoryId: String,
    val expirationDate: LocalDate? = null,
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false,
    val buyEvents: List<LocalDate> = emptyList(),
    val averageBuyDays: Int? = null,
    val imageUUID: String? = null
)
```

### **Category**
```kotlin
data class Category(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val viewOrder: Int
)
```

### **SubCategory**
```kotlin
data class SubCategory(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String,
    val viewOrder: Int
)
```

### **Data Structure**
```kotlin
data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: MutableList<SubCategoryWithGroceries>
)

data class SubCategoryWithGroceries(
    val subCategory: SubCategory,
    val groceries: MutableList<Grocery>
)
```

## ✨ **Current Features**

### **1. Category Management**
- ✅ Create new categories
- ✅ Edit existing categories
- ✅ Delete categories (with viewOrder updates)
- ✅ Sort categories by viewOrder
- ✅ Protect first category from deletion

### **2. Sub-Category Management**
- ✅ Create sub-categories within categories
- ✅ Edit sub-categories
- ✅ Delete sub-categories
- ✅ Manage sub-categories in category edit screen
- ✅ Protect first sub-category from deletion

### **3. Grocery Management**
- ✅ Create new groceries
- ✅ Edit existing groceries
- ✅ Delete groceries with confirmation
- ✅ Assign groceries to categories and sub-categories
- ✅ Optional expiration date field
- ✅ Buy history tracking with last 4 events
- ✅ Average buy days calculation
- ✅ Hierarchical display with expand/collapse
- ✅ Bulk import with "Other/General" fallback
- ✅ Alert system for:
  - Expiring/expired items
  - Items due for purchase based on buy history

### **4. Shopping List Management**
- ✅ Add/remove groceries from shopping list (cart icon toggle)
- ✅ Two shopping lists:
  - **"To Buy Next Time"** - `inShoppingList = true, isBought = false`
  - **"Bought This Time"** - `inShoppingList = true, isBought = true`
- ✅ Toggle bought status in shopping list screen
- ✅ Real-time updates between home and shopping list
- ✅ "Finish Shopping" functionality:
  - Confirmation dialog
  - Adds current date to `buyEvents` for bought items
  - Calculates `averageBuyDays` based on last 4 events
  - Resets `inShoppingList` and `isBought` to false

### **5. Search & Navigation**
- ✅ Search functionality across all groceries
- ✅ Expand/collapse all categories
- ✅ Bottom navigation (Home, Shopping List)
- ✅ Hierarchical category display

### **6. Data Persistence**
- ✅ DataStore integration for local storage
- ✅ Automatic data saving
- ✅ Data loading on app startup
- ✅ Default data initialization

## 🎨 **UI Components**

### **Screens**
- `HomeScreen` - Main grocery management interface
- `ShoppingListScreen` - Shopping list management

### **Reusable Components**
- `HierarchicalCategoryDisplay` - Category/grocery tree view with:
  - Clickable title rows for expand/collapse
  - Buy history dialog
  - Delete confirmation
- `CategoriesManagementDialog` - Category management
- `GroceryCreationDialog` - Grocery creation/editing
- `BurgerMenu` - Navigation menu with import option
- `ImportGroceriesDialog` - Bulk grocery import
- `BottomNavigationBar` - Bottom navigation
- Various selection dialogs for categories/sub-categories

### **Theme**
- `SuperCartColors` - Consistent color scheme
- `SuperCartSpacing` - Standard spacing values
- Material 3 design system

## 🔧 **Technical Implementation**

### **Reactive State Management**
- Uses `SnapshotStateList` for automatic UI updates
- `derivedStateOf` for computed state
- Proper state observation to prevent stale closures

### **Data Operations**
- CRUD operations for all entities
- Automatic viewOrder management
- UUID-based entity identification
- Hierarchical data relationships
- Single source of truth through DataManagerObject
- Centralized data manipulation functions
- Automatic UI updates through SnapshotStateList
- Buy history tracking and calculations

### **Performance**
- Efficient list rendering with Compose
- Optimized state updates
- Minimal recomposition through proper state binding

## 📱 **User Experience**

### **Workflow**
1. **Setup** - Create categories and sub-categories
2. **Manage** - Add groceries manually or through bulk import
3. **Shop** - Use cart icons to add items to shopping list
4. **Track** - Mark items as bought and finish shopping
5. **Monitor** - Track expiration dates and buying patterns
6. **Organize** - Search and filter groceries as needed

### **Key Benefits**
- Hierarchical organization system
- Real-time shopping list management
- Persistent data storage
- Intuitive UI with Material Design
- Efficient state management
- Smart alerts for expiring items
- Buy pattern tracking and suggestions
- Bulk import capabilities
- Comprehensive buy history

## 🚀 **Current Status**
The app is fully functional with all core features implemented. The shopping list management system is working with proper reactive state binding, ensuring real-time updates across all screens.

## 📝 **Notes**
- Follows KISS principle (Keep It Simple, Stupid)
- Uses existing working patterns
- Maintains single source of truth for data
- Implements proper reactive state binding
- No over-engineering - focuses on core functionality
