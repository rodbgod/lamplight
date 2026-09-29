package br.com.rodsil.lamplight.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProPurchasesTest {
  private val lifetime = OwnedPurchase(listOf("pro_lifetime"), isPurchased = true, isAcknowledged = true, token = "a")
  private val monthly = OwnedPurchase(listOf("pro_monthly"), isPurchased = true, isAcknowledged = false, token = "b")

  @Test
  fun `grants Pro for a paid lifetime or monthly purchase`() {
    assertEquals(listOf(lifetime, monthly), proPurchases(listOf(lifetime, monthly)))
  }

  @Test
  fun `does not grant Pro for a pending purchase`() {
    assertTrue(proPurchases(listOf(lifetime.copy(isPurchased = false))).isEmpty())
  }

  @Test
  fun `does not grant Pro for other products`() {
    assertTrue(proPurchases(listOf(lifetime.copy(productIds = listOf("scene_pack_winter")))).isEmpty())
  }
}
