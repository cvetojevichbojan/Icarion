package xyz.amplituhedron.icarion

import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.*
import kotlin.test.Test

class IcarionVersionsTest {

    @Test
    fun `Valid IntVersions should compare correctly`() {
        val v1 = IntVersion(1)
        val v2 = IntVersion(2)
        val v3 = IntVersion(1)

        expectThat(v1).isLessThan(v2) // 1 < 2
        expectThat(v2).isGreaterThan(v1) // 2 > 1
        expectThat(v1).isEqualTo(v3) // 1 == 1
    }

    @Test
    fun `IntVersions with invalid version should throw an exception`() {
        expectCatching {
            IntVersion(-1)
        }.isFailure()

    }

    @Test
    fun `Valid SemanticVersion should compare correctly`() {
        val v100 = SemanticVersion(1, 0, 0)
        val v110 = SemanticVersion(1, 1, 0)
        val v200 = SemanticVersion(2, 0, 0)

        expectThat(v100).isLessThan(v110) // 1.0.0 < 1.1.0
        expectThat(v110).isGreaterThan(v100) // 1.1.0 > 1.0.0
        expectThat(v100).isEqualTo(SemanticVersion(1, 0, 0)) // 1.0.0 == 1.0.0
        expectThat(v100).isLessThan(v200) // 1.0.0 < 2.0.0
    }

    @Test
    fun `SemanticVersion with invalid version should throw an exception`() {
        expectCatching {
            SemanticVersion(-1, 0, 0)
        }.isFailure()

        expectCatching {
            SemanticVersion(1, -1, 0)
        }.isFailure()

        expectCatching {
            SemanticVersion(1, 0, -1)
        }.isFailure()
    }

    @Test
    fun `SemanticVersion comparison logic should handle all components (major, minor, patch)`() {
        val v100 = SemanticVersion(1, 0, 0)
        val v110 = SemanticVersion(1, 1, 0)
        val v101 = SemanticVersion(1, 0, 1)
        val v200 = SemanticVersion(2, 0, 0)
        val v201 = SemanticVersion(2, 0, 1)
        val v210 = SemanticVersion(2, 1, 0)
        val v211 = SemanticVersion(2, 1, 1)

        // Comparing by major version
        expectThat(v100).isLessThan(v200)
        expectThat(v100).isLessThan(v201)
        expectThat(v100).isLessThan(v210)
        expectThat(v100).isLessThan(v211)

        // Comparing by minor version
        expectThat(v100).isLessThan(v110)
        expectThat(v200).isLessThan(v210)
        expectThat(v200).isLessThan(v211)

        // Comparing by patch version
        expectThat(v100).isLessThan(v101)
        expectThat(v200).isLessThan(v201)
        expectThat(v210).isLessThan(v211)

        // Equals
        expectThat(v100 == SemanticVersion(1, 0, 0)).isTrue()
        expectThat(v101 == SemanticVersion(1, 0, 1)).isTrue()
        expectThat(v210 == SemanticVersion(2, 1, 0)).isTrue()
        expectThat(v211 == SemanticVersion(2, 1, 1)).isTrue()
    }
}