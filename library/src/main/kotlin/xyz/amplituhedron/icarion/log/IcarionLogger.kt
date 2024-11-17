package xyz.amplituhedron.icarion.log

/**
 * Simple logger interface to ease the bridge with Logging frameworks without introducing a dependency
 */
// TODO Maybe switch to a widely used logger libs such as slf4j ?
interface IcarionLogger {

    /** Log a debug message with optional format args. */
    fun d(message: String)

    /** Log an info message with optional format args. */
    fun i(message: String)

    /** Log an error exception and a message with optional format args. */
    fun e(t: Throwable, message: String)

    /** Log an error exception. */
    fun e(t: Throwable)
}

/**
 * You can set your own logger implementation of [IcarionLogger] via [init]
 */
object IcarionLoggerAdapter: IcarionLogger {
    private val LOCK = Any()
    private var instance: IcarionLogger? = null

    /**
     * Sets the logger instance for this singleton adapter.
     *
     * Invoke once per your app setup if you want to see migration logs.
     */
    fun init(logger: IcarionLogger) {
        synchronized(LOCK) {
            if (instance == null) {
                instance = logger
            }
        }
    }

    /**
     * Allows tests to replace the default implementations.
     */
    internal fun swap(adapter: IcarionLogger) {
        instance = adapter
    }
    override fun d(message: String) {
        instance?.d(message)
    }

    override fun i(message: String) {
        instance?.i(message)
    }
    override fun e(t: Throwable, message: String) {
        instance?.e(t, message)
    }

    override fun e(t: Throwable) {
        instance?.e(t)
    }
}
