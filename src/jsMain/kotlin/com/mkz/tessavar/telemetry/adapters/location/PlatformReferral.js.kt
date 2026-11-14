package com.mkz.tessavar.telemetry.adapters.location

import kotlinx.browser.window

actual fun takeLinkReferral(): String? {
    val search = window.location.search
    val taken = ReferralQuery.take(search)
    if (taken.query != search) window.history.replaceState(null, "", "${window.location.pathname}${taken.query}${window.location.hash}")
    return taken.referral
}
