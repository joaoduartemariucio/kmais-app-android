package br.com.joaoduartemariucio.kmais_app_android.app

import android.app.Application

class KmaisApplication : Application() {
    val dependencies: AppDependencies by lazy { AppDependencies.live(this) }
}
