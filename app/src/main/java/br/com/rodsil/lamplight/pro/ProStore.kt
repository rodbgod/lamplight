package br.com.rodsil.lamplight.pro

import android.app.Activity
import android.content.Context
import br.com.rodsil.lamplight.analytics.Analytics
import br.com.rodsil.lamplight.analytics.proPurchased
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The two ways to buy Pro, as configured in the Play Console (PRD section 8). */
enum class ProOffer(val productId: String, val productType: String) {
  LIFETIME("pro_lifetime", ProductType.INAPP),
  MONTHLY("pro_monthly", ProductType.SUBS),
}

data class OfferPrice(val offer: ProOffer, val formattedPrice: String)

/** The parts of a Play purchase that decide Pro, kept apart from the Play class so the rule is testable. */
data class OwnedPurchase(val productIds: List<String>, val isPurchased: Boolean, val isAcknowledged: Boolean, val token: String)

private val PRO_PRODUCT_IDS = ProOffer.entries.map { it.productId }.toSet()

/** Paid and for a Pro product. Pending purchases (cash at a store, for example) do not count yet. */
fun proPurchases(owned: List<OwnedPurchase>): List<OwnedPurchase> = owned.filter { it.isPurchased && it.productIds.any(PRO_PRODUCT_IDS::contains) }

/**
 * Knows whether the user owns Pro, and sells it. Play caches purchases on the device, so the answer
 * works offline. Purchases are acknowledged here, since Play refunds any left unacknowledged for 3 days.
 */
// ponytail: trusts Play's client side answer; add server side purchase verification if piracy shows up in revenue.
@Singleton
class ProStore @Inject constructor(@ApplicationContext context: Context, private val analytics: Analytics) : PurchasesUpdatedListener {
  private val scope = MainScope()
  private val client =
    BillingClient.newBuilder(context)
      .setListener(this)
      .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
      .enableAutoServiceReconnection()
      .build()

  private val mutableIsPro = MutableStateFlow(false)
  val isPro: StateFlow<Boolean> = mutableIsPro.asStateFlow()

  private val details = MutableStateFlow<Map<ProOffer, ProductDetails>>(emptyMap())
  private val mutablePrices = MutableStateFlow<List<OfferPrice>>(emptyList())

  /** Empty until Play answers, and stays empty when Play is unavailable or the products are not set up. */
  val prices: StateFlow<List<OfferPrice>> = mutablePrices.asStateFlow()

  fun connect() {
    client.startConnection(
      object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
          if (result.responseCode != BillingResponseCode.OK) return
          scope.launch {
            refreshPurchases()
            loadOffers()
          }
        }

        override fun onBillingServiceDisconnected() = Unit
      }
    )
  }

  fun buy(activity: Activity, offer: ProOffer) {
    val productDetails = details.value[offer] ?: return
    val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails)
    productDetails.subscriptionOfferDetails?.firstOrNull()?.let { params.setOfferToken(it.offerToken) }
    client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build())
  }

  override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
    if (result.responseCode != BillingResponseCode.OK || purchases == null) return
    scope.launch {
      val bought = acknowledge(purchases.map(::owned))
      if (bought.isEmpty()) return@launch
      mutableIsPro.value = true
      bought.flatMap { it.productIds }.filter(PRO_PRODUCT_IDS::contains).forEach { analytics.log(proPurchased(it)) }
    }
  }

  /** Asks Play for everything owned, so an expired or refunded subscription turns Pro off again. */
  private suspend fun refreshPurchases() {
    val owned =
      ProOffer.entries.map { it.productType }.distinct().flatMap { type ->
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()).purchasesList
      }
    mutableIsPro.value = acknowledge(owned.map(::owned)).isNotEmpty()
  }

  private suspend fun acknowledge(owned: List<OwnedPurchase>): List<OwnedPurchase> {
    val pro = proPurchases(owned)
    pro.filterNot { it.isAcknowledged }.forEach {
      client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.token).build())
    }
    return pro
  }

  private suspend fun loadOffers() {
    val products = ProOffer.entries.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(it.productType).build() }
    val found = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(products).build()).productDetailsList.orEmpty()
    details.value = ProOffer.entries.mapNotNull { offer -> found.find { it.productId == offer.productId }?.let { offer to it } }.toMap()
    mutablePrices.value = details.value.mapNotNull { (offer, product) -> formattedPrice(product)?.let { OfferPrice(offer, it) } }
  }
}

private fun owned(purchase: Purchase) =
  OwnedPurchase(
    productIds = purchase.products,
    isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
    isAcknowledged = purchase.isAcknowledged,
    token = purchase.purchaseToken,
  )

// A subscription's last pricing phase is its recurring price, after any intro offer.
private fun formattedPrice(product: ProductDetails): String? =
  product.oneTimePurchaseOfferDetails?.formattedPrice
    ?: product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
