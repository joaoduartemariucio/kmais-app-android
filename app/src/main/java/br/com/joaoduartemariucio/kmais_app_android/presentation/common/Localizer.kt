package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

interface Localizer {
    val locale: Locale

    fun string(
        @StringRes id: Int,
        vararg args: Any,
    ): String

    fun plural(
        @PluralsRes id: Int,
        count: Int,
        vararg args: Any,
    ): String
}

class ResourcesLocalizer(
    private val resources: Resources,
) : Localizer {
    override val locale: Locale get() = resources.configuration.locales[0] ?: Locale.getDefault()

    override fun string(
        @StringRes id: Int,
        vararg args: Any,
    ): String = if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args)

    override fun plural(
        @PluralsRes id: Int,
        count: Int,
        vararg args: Any,
    ): String = resources.getQuantityString(id, count, *args)
}

@Composable
fun rememberLocalizer(): Localizer {
    val resources = LocalResources.current
    return remember(resources) { ResourcesLocalizer(resources) }
}
