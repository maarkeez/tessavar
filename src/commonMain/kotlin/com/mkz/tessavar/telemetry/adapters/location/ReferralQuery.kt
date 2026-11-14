package com.mkz.tessavar.telemetry.adapters.location

import korlibs.io.net.URL

object ReferralQuery {
    const val REFERRAL_PARAMETER = "ref"

    private const val PARAMETER_SEPARATOR = "&"

    data class Taken(
        val referral: String?,
        val query: String,
    )

    fun take(search: String): Taken {
        val parameters =
            search
                .removePrefix("?")
                .split(PARAMETER_SEPARATOR)
                .filter { parameter -> parameter.isNotEmpty() }
        val (referrals, others) = parameters.partition { parameter -> parameter.substringBefore("=") == REFERRAL_PARAMETER }
        val referral = referrals.firstOrNull()?.substringAfter("=", "")?.let(::decodeOrNull)
        val query =
            others
                .joinToString(PARAMETER_SEPARATOR)
                .takeIf { it.isNotEmpty() }
                ?.let { "?$it" }
                .orEmpty()
        return Taken(referral = referral, query = query)
    }

    private fun decodeOrNull(value: String): String? = runCatching { URL.decodeComponent(value, formUrlEncoded = true) }.getOrNull()
}
