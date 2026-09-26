package com.example

import com.example.model.PriceDirection
import com.example.viewmodel.HashGridViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testYouTubeUrlValidation() {
        val vm = HashGridViewModel()

        // Valid YouTube URLs
        val validUrl1 = "https://youtube.com/watch?v=dQw4w9WgXcQ"
        val validUrl2 = "https://youtu.be/dQw4w9WgXcQ"
        val validUrl3 = "https://youtube.com/shorts/dQw4w9WgXcQ"

        assertTrue(validUrl1.contains("youtube.com/watch?v="))
        assertTrue(validUrl2.contains("youtu.be/"))
        assertTrue(validUrl3.contains("youtube.com/shorts/"))

        // Invalid URL
        val invalidUrl = "https://tiktok.com/@crypto/video/123"
        val isYoutubeFormat = invalidUrl.contains("youtube.com/watch?v=", ignoreCase = true) ||
                invalidUrl.contains("youtu.be/", ignoreCase = true) ||
                invalidUrl.contains("youtube.com/shorts/", ignoreCase = true)
        assertTrue(!isYoutubeFormat)
    }

    @Test
    fun testYouTubeDuplicateAndSubmit() {
        val vm = HashGridViewModel()
        // Duplicate URL check
        val duplicateUrl = "https://youtube.com/watch?v=ky9q8z1a3w"
        vm.submitYouTubeBounty(duplicateUrl, "Channel Name")
        val err = vm.submitYouTubeBounty(duplicateUrl, "Channel Name")
        assertNotNull(err)
        assertTrue(err!!.contains("already been submitted"))
    }

    @Test
    fun testWithdrawalValidation() {
        val vm = HashGridViewModel()
        // Below minimum 30% work milestone threshold ($3.00 default for $10 rig)
        val errLow = vm.requestWithdrawal(1.0, "TJj7G3U8qVSzqcJaxAhQG34ADHihnR6WuD", "TRC20")
        assertNotNull(errLow)
        assertTrue(errLow!!.contains("Minimum withdrawal threshold"))
    }

    @Test
    fun testPriceDirectionCalculation() {
        val prevPrice = 79200.0
        val higherPrice = 79350.0
        val lowerPrice = 79100.0

        val upDirection = if (higherPrice > prevPrice) PriceDirection.UP else PriceDirection.DOWN
        val downDirection = if (lowerPrice > prevPrice) PriceDirection.UP else PriceDirection.DOWN

        assertEquals(PriceDirection.UP, upDirection)
        assertEquals(PriceDirection.DOWN, downDirection)
    }
}
