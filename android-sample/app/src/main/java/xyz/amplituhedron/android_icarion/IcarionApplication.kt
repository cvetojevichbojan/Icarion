package xyz.amplituhedron.android_icarion

import android.app.Application
import android.util.Log
import xyz.amplituhedron.icarion.log.IcarionLogger
import xyz.amplituhedron.icarion.log.IcarionLoggerAdapter

class IcarionApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set if you wanna see migration logs
        IcarionLoggerAdapter.init(createLogcatIcarionAdapter())
    }
}

private fun createLogcatIcarionAdapter() = object : IcarionLogger {
    private val tag = "IcarionLogger"

    override fun d(message: String) {
        Log.d(tag, message)
    }

    override fun i(message: String) {
        Log.i(tag, message)
    }

    override fun e(t: Throwable, message: String) {
        Log.e(tag, message, t)
    }

    override fun e(t: Throwable) {
        Log.e(tag, t.message ?: "An error occurred", t)
    }
}
