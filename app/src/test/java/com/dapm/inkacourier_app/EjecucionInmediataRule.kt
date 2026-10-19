package com.dapm.inkacourier_app

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Ejecuta las actualizaciones de LiveData de inmediato, sin hilo principal de Android. */
class EjecucionInmediataRule : TestWatcher() {
    override fun starting(description: Description) {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
    }

    override fun finished(description: Description) {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}
