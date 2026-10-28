package br.com.joaoduartemariucio.rodaro.app

import android.app.Activity
import android.os.Bundle
import br.com.joaoduartemariucio.rodaro.data.PreviewData
import kotlinx.coroutines.runBlocking

class SeedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking { PreviewData.seed((application as RodaroApplication).dependencies.database) }
        finish()
    }
}
