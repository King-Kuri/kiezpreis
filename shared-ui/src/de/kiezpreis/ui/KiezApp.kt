package de.kiezpreis.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import de.kiezpreis.core.data.ThemeMode
import de.kiezpreis.ui.components.AuroraBackground
import de.kiezpreis.ui.components.GlassIconButton
import de.kiezpreis.ui.components.GlassNavBar
import de.kiezpreis.ui.screens.DetailContent
import de.kiezpreis.ui.screens.DetailTopBar
import de.kiezpreis.ui.screens.DetailTopBarHeight
import de.kiezpreis.ui.screens.FavoritesContent
import de.kiezpreis.ui.screens.FilterSheet
import de.kiezpreis.ui.screens.HomeContent
import de.kiezpreis.ui.screens.SearchContent
import de.kiezpreis.ui.screens.SearchTopBar
import de.kiezpreis.ui.screens.SearchTopBarHeight
import de.kiezpreis.ui.screens.SettingsContent
import de.kiezpreis.ui.screens.SourcesContent
import de.kiezpreis.ui.screens.rememberSearchResult
import de.kiezpreis.ui.theme.GlassSettings
import de.kiezpreis.ui.theme.Kiez
import de.kiezpreis.ui.theme.KiezTheme

/** What fills the screen right now. Detail and the sources page lie above the tabs. */
private sealed interface Route {
    data class TabRoute(val tab: Tab) : Route
    data class Detail(val route: DetailRoute, val depth: Int) : Route
    data object Sources : Route
}

private val NavBarSpace = 112.dp

@Composable
fun KiezApp(controller: AppController, platform: Platform) {
    val prefs by controller.user.prefs.collectAsState()
    val dark = when (prefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    CompositionLocalProvider(LocalPlatform provides platform) {
        KiezTheme(dark = dark, reduceMotion = prefs.reduceMotion, glass = GlassSettings(platform.blurAvailable, prefs.reduceTransparency)) {
            AppScaffold(controller, platform)
        }
    }
}

@Composable
private fun AppScaffold(controller: AppController, platform: Platform) {
    val data by controller.offers.state.collectAsState()
    val prefs by controller.user.prefs.collectAsState()
    val favorites by controller.user.favorites.collectAsState()
    val recent by controller.user.recentSearches.collectAsState()
    val bars = platform.systemBarsPadding()
    val dir = LocalLayoutDirection.current
    val top = bars.calculateTopPadding()
    val bottom = bars.calculateBottomPadding()
    val motion = Kiez.motion

    val route: Route = when {
        controller.detailStack.isNotEmpty() -> Route.Detail(controller.detailStack.last(), controller.detailStack.size)
        controller.sourcesScreenOpen -> Route.Sources
        else -> Route.TabRoute(controller.tab)
    }

    platform.BackHandler(enabled = route != Route.TabRoute(Tab.HOME) || controller.filterSheetOpen) { controller.back() }

    fun padFor(r: Route) = PaddingValues(
        start = bars.calculateStartPadding(dir),
        end = bars.calculateEndPadding(dir),
        top = top + when (r) {
            is Route.TabRoute -> if (r.tab == Tab.SEARCH) SearchTopBarHeight + 8.dp else 12.dp
            is Route.Detail, Route.Sources -> DetailTopBarHeight + 8.dp
        },
        bottom = bottom + when (r) {
            is Route.TabRoute -> NavBarSpace
            else -> 24.dp
        },
    )

    val forward: (Route, Route) -> Boolean = { from, to ->
        depth(to) >= depth(from)
    }

    Box(Modifier.fillMaxSize()) {
        // Layer 1: background and scrolling content. This is what the glass layers blur.
        Box(Modifier.fillMaxSize().let { with(platform) { it.backdropSource() } }) {
            AuroraBackground()
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    if (motion.reduced) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    else if (initialState is Route.TabRoute && targetState is Route.TabRoute) {
                        fadeIn(tween(motion.medium)) + scaleIn(tween(motion.medium), initialScale = 0.985f) togetherWith fadeOut(tween(motion.short))
                    } else if (forward(initialState, targetState)) {
                        (slideInHorizontally(tween(motion.medium)) { it / 6 } + fadeIn(tween(motion.medium))) togetherWith fadeOut(tween(motion.short))
                    } else {
                        fadeIn(tween(motion.medium)) togetherWith (slideOutHorizontally(tween(motion.medium)) { it / 6 } + fadeOut(tween(motion.short)))
                    }
                },
                label = "content",
            ) { r ->
                val p = padFor(r)
                when (r) {
                    is Route.TabRoute -> when (r.tab) {
                        Tab.HOME -> HomeContent(controller, data, prefs, favorites, recent, p)
                        Tab.SEARCH -> SearchContent(controller, data, recent, p)
                        Tab.FAVORITES -> FavoritesContent(controller, data, favorites, p)
                        Tab.SETTINGS -> SettingsContent(controller, prefs, data, p)
                    }
                    is Route.Detail -> DetailContent(controller, r.route, data, p)
                    Route.Sources -> SourcesContent(controller, data, p)
                }
            }
        }

        // Layer 2: floating glass bars.
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn(tween(motion.short)) togetherWith fadeOut(tween(motion.short)) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = top),
            label = "topbar",
        ) { r ->
            when (r) {
                is Route.TabRoute -> if (r.tab == Tab.SEARCH) SearchTopBar(controller, prefs) else Box(Modifier)
                is Route.Detail -> {
                    val group = controller.resolveGroup(r.route.productKey, data.offers)
                    DetailTopBar(controller, r.route, favorites.any { it.productKey == r.route.productKey }, group)
                }
                Route.Sources -> Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GlassIconButton(Icons.Rounded.ArrowBack, "Zurück") { controller.back() }
                }
            }
        }

        AnimatedVisibility(
            visible = route is Route.TabRoute,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 14.dp),
            enter = slideInVertically(tween(motion.medium)) { it * 2 } + fadeIn(tween(motion.medium)),
            exit = slideOutVertically(tween(motion.short)) { it * 2 } + fadeOut(tween(motion.short)),
        ) {
            GlassNavBar(controller.tab, onSelect = { controller.selectTab(it) })
        }

        // Layer 3: filter sheet above everything.
        if (route == Route.TabRoute(Tab.SEARCH) || controller.filterSheetOpen) {
            val result = rememberSearchResult(controller, data)
            FilterSheet(controller, controller.filterSheetOpen, PaddingValues(bottom = bottom), result.hits.size)
        }
    }
}

private fun depth(r: Route): Int = when (r) {
    is Route.TabRoute -> 0
    Route.Sources -> 1
    is Route.Detail -> 1 + r.depth
}
