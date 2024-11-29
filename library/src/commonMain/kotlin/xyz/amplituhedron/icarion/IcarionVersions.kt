package xyz.amplituhedron.icarion

/**
 * Integer based versions for your application. For ex. IntVersion(1), IntVersion(7), ....
 */
data class IntVersion(val value: Int) : Comparable<IntVersion> {
    init {
        require(value >= 0) { "Version numbers must be non-negative." }
    }

    override fun compareTo(other: IntVersion): Int = value.compareTo(other.value)
}

/**
 * Simple semantic based versions (e.g., 1.0.0, 2.1.3).
 *
 * Usage:
 *
 * ```
 * val v123 = SemanticVersion(1, 2, 3)
 *
 * val 104 = SemanticVersion.fromVersion("1.0.4")
 * val 110 = SemanticVersion.fromVersion("1.1.0")
 * val 700 = SemanticVersion.fromVersion("7")
 * ```
 *
 * Note: This implementation does not cover the whole SemVer specification, such as pre-release identifiers or build metadata.
 * You can easily integrate a 3rd party SemVer with Icarion.
 *
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {

    companion object {
        private const val INVALID_STRING_VERSION_MSG = "\"Invalid version format. Supported formats are [major.minor.patch] (ex. 1.2.3), [major.minor] (ex 1.4) or [major] (ex. 7)"

        /**
         * Builds a SemanticVersion from String. Only basic SemVer is supported.
         *
         * Supported formats are [major.minor.patch] (ex. 1.2.3), [major.minor] (ex 1.4) or [major] (ex. 7)
         */
        fun fromVersion(version: String): SemanticVersion {
            require(version.isNotBlank()) { INVALID_STRING_VERSION_MSG }

            val parts = version.split(".")

            // Validate the number of parts and ensure they match the expected format
            return when (parts.size) {
                1 -> {
                    // Format: [major]
                    val major = parts[0].toIntOrNull()
                    require(major != null) { INVALID_STRING_VERSION_MSG }
                    SemanticVersion(major, 0, 0)
                }
                2 -> {
                    // Format: [major.minor]
                    val major = parts[0].toIntOrNull()
                    val minor = parts[1].toIntOrNull()
                    require(major != null && minor != null) { INVALID_STRING_VERSION_MSG }
                    SemanticVersion(major, minor, 0)
                }
                3 -> {
                    // Format: [major.minor.patch]
                    val major = parts[0].toIntOrNull()
                    val minor = parts[1].toIntOrNull()
                    val patch = parts[2].toIntOrNull()
                    require(major != null && minor != null && patch != null) { INVALID_STRING_VERSION_MSG }
                    SemanticVersion(major, minor, patch)
                }
                else -> {
                    // If there are more than 3 parts, it's an invalid version format
                    require(false) { INVALID_STRING_VERSION_MSG }
                    SemanticVersion(0, 0, 0)  // This won't be reached because of the require above
                }
            }
        }
    }

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
