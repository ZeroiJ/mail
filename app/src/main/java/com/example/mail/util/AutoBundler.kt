package com.example.mail.util

/**
 * Stateless auto-bundler that classifies incoming emails into semantic groups
 * based on sender domain patterns and subject-line heuristics.
 *
 * Bundle types match the `bundle_type` field in the Room entity:
 * - RECEIPT: payment confirmations, invoices, order updates
 * - NEWSLETTER: mailing lists, digest emails, marketing
 * - LOGISTICS: shipping notifications, delivery updates
 * - SOCIAL: social network activity, mentions, comments
 * - OTP: one-time passwords and verification codes
 * - PERSONAL: direct human-to-human emails (default fallback)
 */
enum class BundleType(val label: String) {
    RECEIPT("receipt"),
    NEWSLETTER("newsletter"),
    LOGISTICS("logistics"),
    SOCIAL("social"),
    OTP("otp"),
    PERSONAL("personal")
}

/**
 * Stateless classifier. Safe to call from any coroutine context.
 */
object AutoBundler {

    // -------------------------------------------------------------------
    // Sender-domain patterns
    // -------------------------------------------------------------------

    private val receiptDomains = listOf(
        "paypal.com", "stripe.com", "square.com", "shopify.com",
        "amazon.com", "ebay.com", "walmart.com", "target.com",
        "bestbuy.com", "apple.com/store", "store.steampowered.com",
        "uber.com", "lyft.com", "doordash.com", "grubhub.com",
        "venmo.com", "cash.app", "wise.com", "revolut.com",
        "bank", "chase.com", "wellsfargo.com", "citi.com",
        "goldmansachs.com", "robinhood.com", "coinbase.com"
    )

    private val newsletterDomains = listOf(
        "mailchimp.com", "sendgrid.net", "substack.com", "beehiiv.com",
        "convertkit.com", "medium.com", "github.com", "stackoverflow.com",
        "dev.to", "hashnode.dev", "newsletter", "digest", "weekly",
        "buttondown.email", "tinyletter.com", "ghost.io"
    )

    private val logisticsDomains = listOf(
        "ups.com", "fedex.com", "usps.com", "dhl.com",
        "amazon.com/shipping", "shopify.com/shipping",
        "stubhub.com", "eventbrite.com", "airbnb.com",
        "booking.com", "expedia.com", "kayak.com"
    )

    private val socialDomains = listOf(
        "facebook.com", "twitter.com", "x.com", "linkedin.com",
        "instagram.com", "tiktok.com", "reddit.com", "quora.com",
        "pinterest.com", "threads.net", "mastodon"
    )

    // -------------------------------------------------------------------
    // Subject-line keyword patterns
    // -------------------------------------------------------------------

    private val receiptKeywords = listOf(
        "receipt", "invoice", "order confirmation", "payment received",
        "your order", "purchase confirmation", "billing statement",
        "transaction", "refund", "charge", "subscription renewal"
    )

    private val newsletterKeywords = listOf(
        "newsletter", "digest", "weekly update", "monthly roundup",
        "unsubscribe", "view in browser", "you received this email",
        "no-reply", "donotreply", "marketing", "promotional"
    )

    private val logisticsKeywords = listOf(
        "shipped", "delivery", "tracking", "package", "order status",
        "your item", "out for delivery", "has been delivered",
        "estimated delivery", "shipping update", "transit"
    )

    private val socialKeywords = listOf(
        "commented on", "mentioned you", "sent you a message",
        "accepted your", "wants to connect", "liked your",
        "shared your", "replied to", "new follower", " friend request"
    )

    private val otpKeywords = listOf(
        "verification code", "one-time code", "otp", "your code is",
        "security code", "authentication code", "verify your",
        "enter the following", "login code"
    )

    // -------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------

    /**
     * Classify an email by sender domain and subject. Returns the best-fit
     * [BundleType]. Order: OTP > RECEIPT > LOGISTICS > SOCIAL > NEWSLETTER > PERSONAL.
     */
    fun classify(sender: String, subject: String): BundleType {
        val senderLower = sender.lowercase()
        val subjectLower = subject.lowercase()

        // OTP is highest priority — display as ephemeral widget.
        if (matchesAny(subjectLower, otpKeywords) || matchesAny(senderLower, listOf("noreply@", "no-reply@"))) {
            // Only flag as OTP if subject explicitly mentions codes
            if (matchesAny(subjectLower, otpKeywords)) return BundleType.OTP
        }

        if (matchesAny(senderLower, receiptDomains) || matchesAny(subjectLower, receiptKeywords)) {
            return BundleType.RECEIPT
        }

        if (matchesAny(senderLower, logisticsDomains) || matchesAny(subjectLower, logisticsKeywords)) {
            return BundleType.LOGISTICS
        }

        if (matchesAny(senderLower, socialDomains) || matchesAny(subjectLower, socialKeywords)) {
            return BundleType.SOCIAL
        }

        if (matchesAny(senderLower, newsletterDomains) || matchesAny(subjectLower, newsletterKeywords)) {
            return BundleType.NEWSLETTER
        }

        return BundleType.PERSONAL
    }

    /**
     * Return a human-readable label for the bundle type.
     */
    fun labelFor(type: BundleType): String = type.label

    // -------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------

    private fun matchesAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
}
