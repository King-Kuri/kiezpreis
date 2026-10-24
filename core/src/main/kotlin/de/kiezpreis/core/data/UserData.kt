package de.kiezpreis.core.data

import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.region.PostalCodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Hell"), DARK("Dunkel") }

@Serializable
data class UserPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false,
    /** Stronger, less transparent glass for readability. */
    val reduceTransparency: Boolean = false,
    val postalCode: String? = null,
    val preferredRetailers: Set<Retailer> = emptySet(),
    val enabledSources: Set<String> = setOf(SourceCatalog.sample.id, SourceCatalog.openPrices.id),
    val onboardingDone: Boolean = false,
)

/** A saved product: identified by its product key and the words used to find it again. */
@Serializable
data class Favorite(
    val productKey: String,
    val title: String,
    val brand: String? = null,
    val searchQuery: String,
    val addedAt: Long,
)

/**
 * Planned price alert. Stored, but only evaluated by a background job once a regularly checkable
 * source is available. [isActive] is never true while no such source is enabled.
 */
@Serializable
data class PriceAlert(
    val id: String,
    val query: String,
    val maxPrice: Cents?,
    val createdAt: Long,
)

class UserDataRepository(private val store: JsonFileStore) {

    private val _prefs = MutableStateFlow(store.read(KEY_PREFS, UserPrefs.serializer()) ?: UserPrefs())
    val prefs: StateFlow<UserPrefs> = _prefs.asStateFlow()

    private val _favorites = MutableStateFlow(store.read(KEY_FAVORITES, ListSerializer(Favorite.serializer())) ?: emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites.asStateFlow()

    private val _recent = MutableStateFlow(store.read(KEY_RECENT, ListSerializer(kotlinx.serialization.serializer<String>())) ?: emptyList())
    val recentSearches: StateFlow<List<String>> = _recent.asStateFlow()

    fun updatePrefs(transform: (UserPrefs) -> UserPrefs) {
        _prefs.update(transform)
        store.write(KEY_PREFS, UserPrefs.serializer(), _prefs.value)
    }

    /** Accepts German postal codes; returns false otherwise. Empty input clears the code. */
    fun setPostalCode(input: String): Boolean {
        val code = input.trim()
        if (code.isEmpty()) {
            updatePrefs { it.copy(postalCode = null) }
            return true
        }
        if (!PostalCodes.isValid(code)) return false
        updatePrefs { it.copy(postalCode = code) }
        return true
    }

    fun isFavorite(productKey: String) = _favorites.value.any { it.productKey == productKey }

    fun toggleFavorite(favorite: Favorite) {
        _favorites.update { list ->
            if (list.any { it.productKey == favorite.productKey }) list.filterNot { it.productKey == favorite.productKey }
            else listOf(favorite) + list
        }
        store.write(KEY_FAVORITES, ListSerializer(Favorite.serializer()), _favorites.value)
    }

    fun removeFavorite(productKey: String) {
        _favorites.update { list -> list.filterNot { it.productKey == productKey } }
        store.write(KEY_FAVORITES, ListSerializer(Favorite.serializer()), _favorites.value)
    }

    fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        _recent.update { list -> (listOf(q) + list.filterNot { it.equals(q, ignoreCase = true) }).take(MAX_RECENT) }
        store.write(KEY_RECENT, ListSerializer(kotlinx.serialization.serializer<String>()), _recent.value)
    }

    fun clearRecentSearches() {
        _recent.value = emptyList()
        store.delete(KEY_RECENT)
    }

    companion object {
        const val MAX_RECENT = 8
        private const val KEY_PREFS = "prefs"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENT = "recent_searches"
    }
}
