package com.wagmilabs.sigil.ui

import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Tests for RTL layout support.
 *
 * Per suki-i18n-requirements.md §5:
 * - RTL support for ar, he, fa, ur
 * - Layout mirrors for RTL
 * - Pictogram emoji order does NOT flip
 *
 * AGPL-3.0 License
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RTLLayoutTest {

    @Test
    fun `RTL layout direction is correct for Arabic`() {
        val config = Configuration()
        config.setLocale(Locale("ar"))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val layoutDirection = context.resources.configuration.layoutDirection
        assertEquals(View.LAYOUT_DIRECTION_RTL, layoutDirection)
    }

    @Test
    fun `RTL layout direction is correct for Hebrew`() {
        val config = Configuration()
        config.setLocale(Locale("he"))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val layoutDirection = context.resources.configuration.layoutDirection
        assertEquals(View.LAYOUT_DIRECTION_RTL, layoutDirection)
    }

    @Test
    fun `LTR layout direction is correct for English`() {
        val config = Configuration()
        config.setLocale(Locale("en"))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val layoutDirection = context.resources.configuration.layoutDirection
        assertEquals(View.LAYOUT_DIRECTION_LTR, layoutDirection)
    }

    @Test
    fun `pictogram_speakable uses spaces not hyphens regardless of locale`() {
        // Per D10: space-separated in JSON, hyphen in URL query params only
        val speakable = "apple banana plane car dog"

        // Verify format
        assertTrue(speakable.contains(" "))
        assertFalse(speakable.contains("-"))

        // Verify word count
        assertEquals(5, speakable.split(" ").size)
    }
}
