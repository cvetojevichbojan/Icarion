package xyz.amplituhedron.icarion.version

import xyz.amplituhedron.icarion.IntVersion
import xyz.amplituhedron.icarion.SemanticVersion
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class IcarionVersionsTest {

    @Test
    fun `Valid IntVersions should compare correctly`() {
        val v1 = IntVersion(1)
        val v2 = IntVersion(2)
        val v3 = IntVersion(1)

        assertTrue { v1 < v2 }
        assertTrue { v2 > v1 }
        assertTrue { v2 != v1 }
        assertTrue { v1 == v1 }
        assertTrue { v1 == v3 }
    }

    @Test
    fun `IntVersions with invalid version should throw an exception`() {
        assertFails { IntVersion(-1) }
    }

    @Test
    fun `SemanticVersion with invalid version should throw an exception`() {
        assertFails {
            SemanticVersion(-1, 0, 0)
        }

        assertFails {
            SemanticVersion(1, -1, 0)
        }

        assertFails {
            SemanticVersion(1, 0, -1)
        }
    }

    @Test
    fun `SemanticVersion comparison logic should handle all components - major minor patch`() {
        val v100 = SemanticVersion(1, 0, 0)
        val v101 = SemanticVersion(1, 0, 1)
        val v110 = SemanticVersion(1, 1, 0)
        val v200 = SemanticVersion(2, 0, 0)
        val v201 = SemanticVersion(2, 0, 1)
        val v210 = SemanticVersion(2, 1, 0)
        val v211 = SemanticVersion(2, 1, 1)

        // Comparing by major version
        assertTrue { v100 < v200 }
        assertTrue { v100 < v201 }
        assertTrue { v100 < v210 }
        assertTrue { v100 < v211 }


        // Comparing by minor version
        assertTrue { v100 < v110 }
        assertTrue { v200 < v210 }
        assertTrue { v200 < v211 }

        // Comparing by patch version
        assertTrue { v100 < v101 }
        assertTrue { v200 < v201 }
        assertTrue { v210 < v211 }

        // Equals
        assertTrue { v100 == SemanticVersion(1, 0, 0) }
        assertTrue { v101 == SemanticVersion(1, 0, 1) }
        assertTrue { v210 == SemanticVersion(2, 1, 0) }
        assertTrue { v211 == SemanticVersion(2, 1, 1) }
    }

    @Test
    fun `Valid CustomAppNamedVersion should compare correctly`() {
        val v1 = CustomAppEnumVersion.ACACIA
        val v2 = CustomAppEnumVersion.BIRCH
        val v3 = CustomAppEnumVersion.CEDAR
        val v4 = CustomAppEnumVersion.DOUGLAS_FIR


        assertTrue { v1 < v2 }
        assertTrue { v2 < v3 }
        assertTrue { v3 < v4 }

        assertTrue { v1 != v2 }
        assertTrue { v1 != v4 }
        assertTrue { v1 != v3 }

        assertTrue { v2 > v1 }
        assertTrue { v3 > v2 }
        assertTrue { v4 > v3 }

        assertTrue { v4 != v3 }
        assertTrue { v4 == CustomAppEnumVersion.DOUGLAS_FIR }
    }

}

enum class CustomAppEnumVersion {
    ACACIA,
    BIRCH,
    CEDAR,
    DOUGLAS_FIR,
    OAK,
    PINE,
    SEQUOIA;
}