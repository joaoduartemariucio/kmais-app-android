package br.com.joaoduartemariucio.rodaro.app

import android.app.Application

class RodaroApplication : Application() {
    val dependencies: AppDependencies by lazy { AppDependencies.live(this) }

    override fun onCreate() {
        super.onCreate()
        FirebaseSetup.start(this)
    }
}
