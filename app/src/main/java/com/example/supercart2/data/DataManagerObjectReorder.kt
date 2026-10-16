package com.example.supercart2.data

// Reordering helper for DataManagerObject.swapCategoryOrder / swapStoreOrder.

/**
 * Swaps [uuid1] and [uuid2] in [sorted] (uuid to viewOrder, in display order) and numbers every item 1..n,
 * so the move takes effect even when viewOrders are equal. Returns only the entries whose viewOrder changes;
 * empty when either uuid is missing or both are the same.
 */
internal fun changedViewOrders(sorted: List<Pair<String, Int>>, uuid1: String, uuid2: String): Map<String, Int> {
    val ids = sorted.map { it.first }.toMutableList()
    val (index1, index2) = ids.indexOf(uuid1) to ids.indexOf(uuid2)
    if (index1 == -1 || index2 == -1 || index1 == index2) return emptyMap()
    java.util.Collections.swap(ids, index1, index2)
    val current = sorted.toMap()
    return ids.withIndex().associate { (index, id) -> id to index + 1 }.filter { (id, order) -> current[id] != order }
}
