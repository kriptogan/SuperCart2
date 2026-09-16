package com.example.supercart2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The English strings.xml and its Hebrew, Russian and Bulgarian translations must stay in sync. */
class StringResourcesTest {

    private val resDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private val translations = listOf("values-iw", "values-ru", "values-bg")

    private fun strings(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resDir, "$folder/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private fun placeholders(text: String): List<String> =
        Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[sdf]").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `every English key is translated to Hebrew, Russian and Bulgarian`() {
        val english = strings("values").keys
        translations.forEach { folder ->
            val missing = english - strings(folder).keys
            assertTrue("$folder is missing: $missing", missing.isEmpty())
        }
    }

    @Test
    fun `no translation has keys that English does not have`() {
        val english = strings("values").keys
        translations.forEach { folder ->
            val extra = strings(folder).keys - english
            assertTrue("$folder has extra keys: $extra", extra.isEmpty())
        }
    }

    @Test
    fun `translations keep the English format placeholders`() {
        val english = strings("values")
        translations.forEach { folder ->
            strings(folder).filterKeys { it in english }.forEach { (key, text) ->
                assertEquals("$folder/$key", placeholders(english.getValue(key)), placeholders(text))
            }
        }
    }

    @Test
    fun `no translation is empty`() {
        translations.forEach { folder ->
            val empty = strings(folder).filterValues { it.isBlank() }.keys
            assertTrue("$folder has empty strings: $empty", empty.isEmpty())
        }
    }
}
