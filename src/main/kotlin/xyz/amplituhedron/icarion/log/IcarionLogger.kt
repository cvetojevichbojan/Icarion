package xyz.amplituhedron.icarion.log

/**
 * Simple logger interface to ease the bridge with Logging frameworks without introducing a dependency
 */
// TODO Maybe switch to a widely used logger libs such as slf4j ?
interface IcarionLogger {

    /** Log a debug message with optional format args. */
    fun d(message: String?)

    /** Log an info message with optional format args. */
    fun i(message: String?)

    /** Log an error exception and a message with optional format args. */
    fun e(t: Throwable?, message: String?)

    /** Log an error exception. */
    fun e(t: Throwable?)
}

internal object IcarionLoggerAdapter: IcarionLogger {
    private val LOCK = Any()
    private var instance: IcarionLogger? = null

    fun init(adapter: IcarionLogger) {
        synchronized(LOCK) {
            if (instance == null) {
                instance = adapter
            }
        }
    }

    /**
     * Allows tests to replace the default implementations.
     */
    fun swap(adapter: IcarionLogger) {
        instance = adapter
    }
    override fun d(message: String?) {
        instance?.d(message)
    }

    override fun i(message: String?) {
        instance?.i(message)
    }
    override fun e(t: Throwable?, message: String?) {
        instance?.e(t, message)
    }

    override fun e(t: Throwable?) {
        instance?.e(t)
    }
}
