package de.kiezpreis.app

import android.app.Application
import android.util.Log
import de.kiezpreis.core.data.JsonFileStore
import de.kiezpreis.core.data.OfferRepository
import de.kiezpreis.core.data.OkHttpClientAdapter
import de.kiezpreis.core.data.OpenPricesSource
import de.kiezpreis.core.data.SampleOfferSource
import de.kiezpreis.core.data.UserDataRepository
import java.io.File

/** Manual dependency wiring; small enough that a DI framework would add nothing. */
class KiezApplication : Application() {
    lateinit var offers: OfferRepository
        private set
    lateinit var userData: UserDataRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val store = JsonFileStore(File(filesDir, "kiezpreis"))
        userData = UserDataRepository(store)
        offers = OfferRepository(
            sources = listOf(SampleOfferSource(), OpenPricesSource(OkHttpClientAdapter())),
            store = store,
            // Only technical facts (source id, attempt, reason) are logged, never user input.
            log = { Log.i("Kiezpreis", it) },
        )
    }
}
