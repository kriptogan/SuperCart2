I want to start a new complicated task. This will require the building of a roadmap first.

For this task, we need two phases.

= phase one =
First, we need to create a new navigation page called "stores". 
When the user navigates to this page they will have to select an option:
- view groceries
- select a store

if the user selects "view groceries" they will see a page similar to the current shopping list page:
- burger button, current date, create button.
- search bar and collapse all button.
- change to store view (this will send the user to the "select a store" option).
- list of things to buy.
- already bought list.
- finish shopping button.

if the user selects "select a store":
- A list of stores will appear (similar to the cards of groceries from the home page).
- In each store card:
-- show the name of store
-- show the number of items in the shopping list that are linked to this store.
-- show the number of unique items in that store (items from the shopping list that do not exist in the other stores).
- clicking the store will show the list of things to buy, already bought list, and finish shopping for the items from the shopping list that are linked to that store. Also, add an exit button that will return to the store selection page.