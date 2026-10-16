package com.example.supercart2.data

import com.example.supercart2.TestData.category
import com.example.supercart2.TestData.grocery
import com.example.supercart2.TestData.node
import com.example.supercart2.TestData.sub
import com.example.supercart2.TestData.subNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataConverterTest {

    private val tree = listOf(
        node(
            category("c1"),
            subNode(sub("s1", "c1"), grocery("g1", "c1", "s1"), grocery("g2", "c1", "s1", deleted = true)),
            subNode(sub("s2", "c1", deleted = true), grocery("g3", "c1", "s2")),
        ),
        node(category("c2", deleted = true), subNode(sub("s3", "c2"), grocery("g4", "c2", "s3"))),
    )

    @Test
    fun `flatten includes deleted items by default`() {
        val (c, s, g) = DataConverter.flattenCategories(tree)
        assertEquals(listOf("c1", "c2"), c.map { it.uuid })
        assertEquals(listOf("s1", "s2", "s3"), s.map { it.uuid })
        assertEquals(listOf("g1", "g2", "g3", "g4"), g.map { it.uuid })
    }

    @Test
    fun `flatten without deleted also drops children of deleted parents`() {
        val (c, s, g) = DataConverter.flattenCategories(tree, includeDeleted = false)
        assertEquals(listOf("c1"), c.map { it.uuid })
        assertEquals(listOf("s1"), s.map { it.uuid })
        assertEquals(listOf("g1"), g.map { it.uuid })
    }

    @Test
    fun `build nests by ids and hides deleted by default`() {
        val (c, s, g) = DataConverter.flattenCategories(tree)
        val nested = DataConverter.buildNestedStructure(c, s, g)
        assertEquals(listOf("c1"), nested.map { it.category.uuid })
        assertEquals(listOf("s1"), nested[0].subCategories.map { it.subCategory.uuid })
        assertEquals(listOf("g1"), nested[0].subCategories[0].groceries.map { it.uuid })
    }

    @Test
    fun `build with includeDeleted round-trips flatten`() {
        val (c, s, g) = DataConverter.flattenCategories(tree)
        assertEquals(tree, DataConverter.buildNestedStructure(c, s, g, includeDeleted = true))
    }

    @Test
    fun `build requires grocery categoryId to match its sub-category's category`() {
        val nested = DataConverter.buildNestedStructure(
            listOf(category("c1"), category("c2")),
            listOf(sub("s1", "c1")),
            listOf(grocery("g1", "c2", "s1")),
        )
        assertTrue(nested.flatMap { it.subCategories }.flatMap { it.groceries }.isEmpty())
    }

    @Test
    fun `build returns mutable lists`() {
        val nested = DataConverter.buildNestedStructure(listOf(category("c1")), listOf(sub("s1", "c1")), emptyList())
        nested[0].subCategories[0].groceries.add(grocery("g", "c1", "s1"))
        nested[0].subCategories.add(subNode(sub("s2", "c1")))
        assertEquals(2, nested[0].subCategories.size)
    }

    @Test
    fun `validateRelationships`() {
        val cats = listOf(category("c1"))
        val subs = listOf(sub("s1", "c1"))
        assertTrue(DataConverter.validateRelationships(cats, subs, listOf(grocery("g", "c1", "s1"))))
        assertTrue(DataConverter.validateRelationships(emptyList(), emptyList(), emptyList()))
        assertFalse(DataConverter.validateRelationships(cats, listOf(sub("s1", "missing")), emptyList()))
        assertFalse(DataConverter.validateRelationships(cats, subs, listOf(grocery("g", "missing", "s1"))))
        assertFalse(DataConverter.validateRelationships(cats, subs, listOf(grocery("g", "c1", "missing"))))
        // does not check that the grocery's sub-category belongs to its category
        assertTrue(
            DataConverter.validateRelationships(
                listOf(category("c1"), category("c2")), subs, listOf(grocery("g", "c2", "s1"))
            )
        )
    }
}
