package com.example.mail.util

/**
 * Stateless utility to detect and neutralize 1×1 transparent tracking pixels
 * embedded in email HTML. Strips invisible image tags that email marketers use
 * for open tracking, replacing them with nothing.
 *
 * Common patterns detected:
 * - 1×1 px transparent GIF/PNG image tags
 * - Dimensions set via `width="1"` / `height="1"` or CSS
 * - Tracker domains: pixelsdm, sendspark, litmus, mailchimp, etc.
 */
object TrackerStripper {

    // -------------------------------------------------------------------
    // Tracking pixel patterns
    // -------------------------------------------------------------------

    /**
     * Matches <img> tags with 1×1 dimensions or known tracker domains.
     * Grouped so we can match inline styles too.
     */
    private val imgTagPattern = Regex(
        """<img[^>]*?>""",
        RegexOption.IGNORE_CASE
    )

    /** Detects explicit 1×1 dimension attributes on an <img> tag. */
    private val oneByOnePattern = Regex(
        """(width\s*=\s*["']?1["']?|height\s*=\s*["']?1["']?)""",
        RegexOption.IGNORE_CASE
    )

    /** Matches known tracker domains in image src URLs. */
    private val trackerDomainPattern = Regex(
        """(pixel\.[a-z]+|track[a-z]*\.[a-z]+|\.track\.|pixels[a-z]*\.|sendspark|"""
            + """litmus\.com|mailchimp\.com|sendgrid\.net|hubspot\.com|"""
            + """intercom\.io|customer\.io|postmarkapp\.com)""",
        RegexOption.IGNORE_CASE
    )

    /** Inline style width:1px height:1px patterns. */
    private val inlineStyleOneByOne = Regex(
        """(?:width|height)\s*:\s*1(?:px)?""",
        RegexOption.IGNORE_CASE
    )

    // -------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------

    /**
     * Strip all 1×1 tracking pixels from [html] and return the cleaned HTML.
     * This is a pure, stateless operation — safe to call from any dispatcher.
     */
    fun strip(html: String): String {
        return imgTagPattern.replace(html) { match ->
            val tag = match.value
            if (isTrackingPixel(tag)) "" else tag
        }
    }

    /**
     * Count how many tracking pixels were found in [html] (for UI metrics).
     */
    fun countTrackers(html: String): Int {
        return imgTagPattern.findAll(html).count { isTrackingPixel(it.value) }
    }

    // -------------------------------------------------------------------
    // Internal detection
    // -------------------------------------------------------------------

    private fun isTrackingPixel(imgTag: String): Boolean {
        // 1. Explicit 1×1 dimension attributes
        if (oneByOnePattern.containsMatchIn(imgTag)) return true

        // 2. Inline CSS style with 1px dimensions
        if (inlineStyleOneByOne.containsMatchIn(imgTag)) return true

        // 3. Known tracker domain in src
        if (trackerDomainPattern.containsMatchIn(imgTag)) return true

        return false
    }
}
