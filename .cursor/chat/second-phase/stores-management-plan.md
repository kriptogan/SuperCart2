# Stores Management #

- Each grocery item should have the option to be linked to stores.
- The user should be able see what stores the item is linked to, and add/edit/remove stores the item is linked to.
- The user should have access to a window that lists all the stores and enables them to edit the names, create new ones, or delete existing
  stores (which will unlink groceries that are connected to that store).
- There should be a way to view groceries by the stores (some kind of group view).
  for example: when the user sets their shopping list, they should have a store-view-mode that organize the shopping list
  by the stores the items linked to. this way, the user can plan what items to buy from which stores.
  in case items are linked to multiple stores, the item will be visible multiple times, in different store each time.
- In the shopping list, while in store-view-mode, the user should have an option to hide stores they not plan to go to.
  for example: I can buy apples in multiple stores, but banana in only 1 of the stores. Logically I will plan to go to the store
  where I can buy both banana and apples. so I would like to hide all the other stores.
  * we need an option to un-hide stores to undo the hide action.
- In each store, the user should view the groceries by categories and sub-categories the same way it is working now.