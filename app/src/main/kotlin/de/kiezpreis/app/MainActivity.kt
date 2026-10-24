package de.kiezpreis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kiezpreis.ui.AppController
import de.kiezpreis.ui.KiezApp

/** Keeps the controller (search text, navigation, filters) across rotation and theme changes. */
class AppViewModel(app: KiezApplication) : ViewModel() {
    val controller = AppController(app.offers, app.userData, viewModelScope)
}

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application as KiezApplication) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val platform = AndroidPlatform(BuildConfig.VERSION_NAME)
        setContent { KiezApp(vm.controller, platform) }
    }

    override fun onResume() {
        super.onResume()
        // Refreshes only sources whose data is older than their interval.
        vm.controller.refresh(force = false)
    }
}
