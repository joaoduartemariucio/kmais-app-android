package br.com.joaoduartemariucio.kmais_app_android.app

import android.app.Activity
import android.os.Bundle
import br.com.joaoduartemariucio.kmais_app_android.data.PreviewData
import kotlinx.coroutines.runBlocking

class SeedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking { PreviewData.seed((application as KmaisApplication).dependencies.database) }
        finish()
    }
}
