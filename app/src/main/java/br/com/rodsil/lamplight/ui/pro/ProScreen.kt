package br.com.rodsil.lamplight.ui.pro

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.rodsil.lamplight.R
import br.com.rodsil.lamplight.analytics.Analytics
import br.com.rodsil.lamplight.analytics.paywallViewed
import br.com.rodsil.lamplight.pro.OfferPrice
import br.com.rodsil.lamplight.pro.ProOffer
import br.com.rodsil.lamplight.pro.ProStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

private val OFFER_LABELS = mapOf(ProOffer.LIFETIME to R.string.pro_lifetime, ProOffer.MONTHLY to R.string.pro_monthly)
private val PRO_BENEFITS = listOf(R.string.pro_benefit_no_ads, R.string.pro_benefit_all_scenes, R.string.pro_benefit_unlimited_mixes)

@HiltViewModel
class ProViewModel @Inject constructor(private val proStore: ProStore, analytics: Analytics) : ViewModel() {
  val isPro: StateFlow<Boolean> = proStore.isPro
  val prices: StateFlow<List<OfferPrice>> = proStore.prices

  init {
    analytics.log(paywallViewed())
  }

  fun buy(activity: Activity, offer: ProOffer) = proStore.buy(activity, offer)
}

@Composable
fun ProScreen(modifier: Modifier = Modifier, viewModel: ProViewModel = hiltViewModel()) {
  val isPro by viewModel.isPro.collectAsStateWithLifecycle()
  val prices by viewModel.prices.collectAsStateWithLifecycle()
  val activity = LocalActivity.current
  ProContent(isPro = isPro, prices = prices, onBuy = { offer -> activity?.let { viewModel.buy(it, offer) } }, modifier = modifier)
}

@Composable
private fun ProContent(isPro: Boolean, prices: List<OfferPrice>, onBuy: (ProOffer) -> Unit, modifier: Modifier = Modifier) {
  Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.displaySmall)
    PRO_BENEFITS.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
    if (isPro) {
      Text(stringResource(R.string.pro_owned), style = MaterialTheme.typography.titleMedium)
      return@Column
    }
    if (prices.isEmpty()) Text(stringResource(R.string.pro_unavailable), style = MaterialTheme.typography.bodyMedium)
    prices.forEach { price ->
      Button(onClick = { onBuy(price.offer) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(OFFER_LABELS.getValue(price.offer), price.formattedPrice))
      }
    }
  }
}
