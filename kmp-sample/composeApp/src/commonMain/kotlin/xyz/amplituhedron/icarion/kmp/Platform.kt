package xyz.amplituhedron.icarion.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform