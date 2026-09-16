package com.example.supercart2

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** User-visible texts must come from string resources, not from literals in Kotlin sources. */
class HardCodedTextsTest {

    private val sourceDir = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }

    private val sources: List<Pair<File, List<String>>> =
        sourceDir.walkTopDown().filter { it.extension == "kt" }.map { it to it.readLines() }.toList()

    /** Lines (outside comments and log calls) that match [pattern]. */
    private fun offending(pattern: Regex): List<String> = sources.flatMap { (file, lines) ->
        lines.withIndex()
            .filter { (_, line) -> pattern.containsMatchIn(line) }
            .filterNot { (_, line) -> line.trimStart().let { it.startsWith("//") || it.startsWith("*") } }
            .filterNot { (_, line) -> line.trimStart().startsWith("/*") }
            .filterNot { (_, line) -> "Log." in line }
            .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
    }

    @Test
    fun `Text, contentDescription and Toast do not use string literals`() {
        val pattern = Regex("""(\bText\(|\btext = |contentDescription = |Toast\.makeText\([^,]+, )"[^"$]*[A-Za-z]""")
        val found = offending(pattern)
        assertTrue("Hard-coded texts:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun `previously hard-coded texts are not in the sources anymore`() {
        val texts = listOf(
            "Settings", "Show Alerts", "Backup & Restore", "Backup", "Restore", "Create Backup", "Restore Backup",
            "Close", "Cancel", "Select Category", "Select Sub-Category", "New Category", "New Sub-Category",
            "Save Changes", "Create Grocery", "Select Date", "No date selected", "Invalid date", "Alerts",
            "Add New Grocery", "Search", "Collapse All", "Expand All", "Collapse", "Expand", "Move Up", "Move Down",
            "No categories yet", "Not Linked to Any Store", "Unknown", "Unknown error", "More options",
            "Mark as bought", "Mark as not bought", "Add to shopping list", "Remove from shopping list",
            "No purchase history yet", "Group code copied!", "Group not found or invalid code",
            "Select Backup File", "No backup files found in Downloads folder.", "Failed to create backup",
            "Backup restored successfully!", "Failed to restore backup. Please check the file format.",
            "Camera permission required to take photos",
            "Display alert bell icon and highlight items with expiring dates or purchase reminders",
            "⚠️ Warning: Your local data will be replaced with the group's data.",
        )
        val found = texts.flatMap { text -> offending(Regex(Regex.escape("\"$text\""))) }
        assertTrue("Hard-coded texts:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun `interpolated user-visible messages are not built in Kotlin`() {
        val messages = "Failed to join: |Failed to create group: |Backup created: |Buy History - |Clear search for "
        val pattern = Regex(""""($messages|\$\w+ items")""")
        val found = offending(pattern)
        assertTrue("Hard-coded texts:\n" + found.joinToString("\n"), found.isEmpty())
    }
}
