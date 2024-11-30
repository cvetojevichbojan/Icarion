package xyz.amplituhedron.icarion.version

import xyz.amplituhedron.icarion.SemanticVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SemanticVersionStringParsingTest {

    @Test
    fun `fromVersion parses from major version correctly`() {
        val version = SemanticVersion.fromVersion("3")
        assertEquals(SemanticVersion(3, 0, 0), version)
    }

    @Test
    fun `fromVersion parses from major and minor versions correctly`() {
        val version = SemanticVersion.fromVersion("3.2")
        assertEquals(SemanticVersion(3, 2, 0), version)
    }

    @Test
    fun `fromVersion parses from major minor and patch versions correctly`() {
        val version = SemanticVersion.fromVersion("3.2.1")
        assertEquals(SemanticVersion(3, 2, 1), version)
    }

    @Test
    fun `fromVersion throws error for empty or blank string`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("")
        }

        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("   ")
        }
    }

    @Test
    fun `fromVersion throws error for non-numeric major version`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("a")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("a.1")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("a.2.3")
        }
    }

    @Test
    fun `fromVersion throws error for non-numeric minor version`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.a")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.a.2")
        }
    }

    @Test
    fun `fromVersion throws error for non-numeric patch version`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.2.a")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.2.1-alpha")
        }
    }

    @Test
    fun `fromVersion throws error for too many version parts`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.2.1.4")
        }
    }

    @Test
    fun `fromVersion throws error for negative major version`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("-1")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("-1.2")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("-1.2.4")
        }
    }

    @Test
    fun `fromVersion throws error for negative minor version`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.-2")
        }
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.-2.1")
        }
    }

    @Test
    fun `fromVersion throws error for negative patch version`() {
        assertFailsWith<IllegalArgumentException> {
            SemanticVersion.fromVersion("3.2.-1")
        }
    }

}