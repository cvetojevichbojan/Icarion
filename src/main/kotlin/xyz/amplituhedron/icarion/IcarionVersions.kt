package xyz.amplituhedron.icarion

/**
 * Integer based versions for your application. For ex. IntVersion(1), IntVersion(7), ....
 */
data class IntVersion(val value: Int) : Comparable<IntVersion> {
    override fun compareTo(other: IntVersion): Int = value.compareTo(other.value)
}

/**
 * Simple semantic based versions (e.g., 1.0.0, 2.1.3).
 *
 * Note: This implementation does not cover the whole SemVer specification, such as pre-release identifiers or build metadata.
 * You can easily integrate a 3rd party SemVer with Icarion, there are working examples in the sample project.
 *
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Version numbers must be non-negative." }
    }

    override fun compareTo(other: SemanticVersion): Int {
        return compareValuesBy(
            a = this,
            b = other,
            { it.major },
            { it.minor },
            { it.patch }
        )
    }

    override fun toString(): String = "$major.$minor.$patch"
}
