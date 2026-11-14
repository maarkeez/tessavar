package com.mkz.tessavar.telemetry.adapters.location

import com.mkz.tessavar.telemetry.adapters.location.ReferralQuery.Taken
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ReferralQueryTest {
    @Test
    fun `ReferralQuery should return the referral and the query without it, keeping the other parameters, when the link has one`() {
        // When
        val taken = ReferralQuery.take("?story=a&ref=bluesky&lang=es")
        // Then
        taken shouldBe Taken(referral = "bluesky", query = "?story=a&lang=es")
    }

    @Test
    fun `ReferralQuery should return no referral and the same query when the link has none`() {
        // When
        val taken = ReferralQuery.take("?lang=es")
        // Then
        taken shouldBe Taken(referral = null, query = "?lang=es")
    }

    @Test
    fun `ReferralQuery should return no query when ref was its only parameter`() {
        // When
        val taken = ReferralQuery.take("?ref=reddit-webgames")
        // Then
        taken shouldBe Taken(referral = "reddit-webgames", query = "")
    }

    @Test
    fun `ReferralQuery should decode the referral when it is URL-encoded`() {
        // When
        val taken = ReferralQuery.take("?ref=Hello%20World")
        // Then
        taken shouldBe Taken(referral = "Hello World", query = "")
    }
}
