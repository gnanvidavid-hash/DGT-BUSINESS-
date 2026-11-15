package com.example.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.firebase.CommissionCalculator
import com.example.data.firebase.FirestoreOrder
import com.example.data.firebase.PaymentMethod
import com.example.data.firebase.formatFcfa
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel
import org.json.JSONObject
import kotlin.math.roundToLong

@Composable
fun CartScreen(
    viewModel: MarketViewModel,
    onExploreShop: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onOpenFedaPayCheckout: (FirestoreOrder) -> Unit,
    onCashOrderCreated: (FirestoreOrder) -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val discountXof by viewModel.cartDiscountXof.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val shippingFee by viewModel.shippingFee.collectAsStateWithLifecycle()
    val couponApplied by viewModel.couponApplied.collectAsStateWithLifecycle()
    val couponError by viewModel.couponError.collectAsStateWithLifecycle()
    val customerName by viewModel.checkoutCustomerName.collectAsStateWithLifecycle()
    val customerEmail by viewModel.checkoutCustomerEmail.collectAsStateWithLifecycle()
    val address by viewModel.checkoutAddress.collectAsStateWithLifecycle()
    val isCreatingOrder by viewModel.isCreatingOrder.collectAsStateWithLifecycle()
    val checkoutError by viewModel.checkoutError.collectAsStateWithLifecycle()

    var couponInput by remember { mutableStateOf("") }

    if (cartItems.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.ShoppingCart,
            title = "Votre panier est vide",
            subtitle = "Ajoutez des produits en FCFA depuis notre catalogue pour commander et payer avec FedaPay ou à la livraison.",
            actionLabel = "Explorer la boutique",
            actionTestTag = "cart_empty_explore_button",
            onAction = onExploreShop
        )
        return
    }

    val totalXof = total.roundToLong()
    val commissionSplit = CommissionCalculator.calculate(totalXof)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cartItems, key = { it.cartItem.id }) { entry ->
                val item = entry.cartItem
                val prod = entry.product
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyBlue.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIconFor(prod.category),
                                contentDescription = prod.name,
                                tint = NavyBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prod.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val variantText = listOfNotNull(item.selectedColor, item.selectedSize)
                                .joinToString(" • ")
                            if (variantText.isNotEmpty()) {
                                Text(
                                    text = variantText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatMoney(prod.price * item.quantity),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity - 1) },
                                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuer", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = item.quantity.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity + 1) },
                                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Augmenter", modifier = Modifier.size(16.dp))
                                }
                            }
                            TextButton(
                                onClick = { viewModel.removeFromCart(item.id) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Retirer", color = AccentRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Code promo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Code promo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = couponInput,
                                onValueChange = { couponInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("Ex: NEXORA10 ou AFRICA20") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold)
                            )
                            Button(
                                onClick = { viewModel.applyCoupon(couponInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Appliquer", color = Gold, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (couponApplied != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Code ${couponApplied?.code} appliqué (-${couponApplied?.discountPercent}%)",
                                    color = AccentGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { viewModel.removeCoupon() }) {
                                    Text("Retirer", color = AccentRed, fontSize = 12.sp)
                                }
                            }
                        }
                        if (couponError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(couponError!!, color = AccentRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Coordonnées client pour FedaPay & Livraison
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Client & Livraison", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            TextButton(onClick = {
                                viewModel.beginCheckout()
                                onNavigateToCheckout()
                            }) {
                                Text("Éditer en détail", color = NavyBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text("Client : $customerName", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("E-mail FedaPay : $customerEmail", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Adresse : $address", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Récapitulatif + Commission
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sous-total", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatMoney(subtotal), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (discountXof > 0L) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remise promo", fontSize = 14.sp, color = AccentGreen)
                                Text("-${formatFcfa(discountXof)}", fontSize = 14.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Frais de livraison", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatMoney(shippingFee), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total à payer", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = formatFcfa(totalXof),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                CommissionBreakdownCard(
                    totalAmountXof = totalXof,
                    platformCommissionXof = commissionSplit.platformCommissionXof,
                    sellerShareXof = commissionSplit.sellerShareXof
                )
            }
        }

        // Zone inférieure de paiement
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!checkoutError.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = checkoutError!!,
                            color = AccentRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.beginCheckout()
                        viewModel.createOrderForPayment(
                            paymentMethod = PaymentMethod.FEDAPAY,
                            onFedaPayReady = onOpenFedaPayCheckout
                        )
                    },
                    enabled = !isCreatingOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue, contentColor = Gold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pay_with_fedapay_button")
                ) {
                    if (isCreatingOrder) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Gold, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Création de la commande...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payer avec FedaPay • ${formatFcfa(totalXof)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mobile Money (MTN, Moov, Orange) et carte bancaire • Mode Sandbox",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.beginCheckout()
                        viewModel.createOrderForPayment(
                            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                            onCashOrderCreated = onCashOrderCreated
                        )
                    },
                    enabled = !isCreatingOrder,
                    border = BorderStroke(1.5.dp, NavyBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pay_cash_on_delivery_button")
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = NavyBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Paiement à la livraison", fontWeight = FontWeight.Bold, color = NavyBlue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit,
    onOpenFedaPayCheckout: (FirestoreOrder) -> Unit,
    onCashOrderCreated: (FirestoreOrder) -> Unit
) {
    val customerName by viewModel.checkoutCustomerName.collectAsStateWithLifecycle()
    val customerEmail by viewModel.checkoutCustomerEmail.collectAsStateWithLifecycle()
    val address by viewModel.checkoutAddress.collectAsStateWithLifecycle()
    val shippingMethod by viewModel.checkoutShippingMethod.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val isCreatingOrder by viewModel.isCreatingOrder.collectAsStateWithLifecycle()
    val checkoutError by viewModel.checkoutError.collectAsStateWithLifecycle()

    val totalXof = total.roundToLong()
    val split = CommissionCalculator.calculate(totalXof)
    val shippingOptions = listOf(
        "Livraison Express à Domicile" to "24-48h à domicile (3 000 FCFA)",
        "Point Relais Sécurisé" to "Retrait en agence Nexora (1 500 FCFA)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finaliser la commande", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Coordonnées du client (requises pour FedaPay)", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    viewModel.checkoutCustomerName.value = it
                    viewModel.clearCheckoutError()
                },
                label = { Text("Nom complet") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NavyBlue) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold)
            )

            OutlinedTextField(
                value = customerEmail,
                onValueChange = {
                    viewModel.checkoutCustomerEmail.value = it
                    viewModel.clearCheckoutError()
                },
                label = { Text("Adresse e-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NavyBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("checkout_email_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold)
            )

            OutlinedTextField(
                value = address,
                onValueChange = {
                    viewModel.checkoutAddress.value = it
                    viewModel.clearCheckoutError()
                },
                label = { Text("Adresse de livraison") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = NavyBlue) },
                modifier = Modifier.fillMaxWidth().testTag("checkout_address_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold)
            )

            Text("Mode de livraison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            shippingOptions.forEach { (title, subtitle) ->
                val selected = shippingMethod == title
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.checkoutShippingMethod.value = title
                            viewModel.shippingFee.value = if (title.contains("Point Relais")) 1500.0 else 3000.0
                        },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Gold else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected, onClick = {
                            viewModel.checkoutShippingMethod.value = title
                            viewModel.shippingFee.value = if (title.contains("Point Relais")) 1500.0 else 3000.0
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            CommissionBreakdownCard(
                totalAmountXof = totalXof,
                platformCommissionXof = split.platformCommissionXof,
                sellerShareXof = split.sellerShareXof
            )

            if (!checkoutError.isNullOrBlank()) {
                Text(checkoutError!!, color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    viewModel.createOrderForPayment(
                        paymentMethod = PaymentMethod.FEDAPAY,
                        onFedaPayReady = onOpenFedaPayCheckout
                    )
                },
                enabled = !isCreatingOrder,
                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue, contentColor = Gold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Payer avec FedaPay • ${formatFcfa(totalXof)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = {
                    viewModel.createOrderForPayment(
                        paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                        onCashOrderCreated = onCashOrderCreated
                    )
                },
                enabled = !isCreatingOrder,
                border = BorderStroke(1.5.dp, NavyBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Paiement à la livraison", fontWeight = FontWeight.Bold, color = NavyBlue)
            }
        }
    }
}

private class FedaPayJsBridge(
    private val onCompleted: () -> Unit,
    private val onErrored: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onComplete(status: String?) {
        mainHandler.post { onCompleted() }
    }

    @JavascriptInterface
    fun onError(message: String?) {
        mainHandler.post { onErrored(message ?: "Erreur widget FedaPay") }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FedaPayCheckoutScreen(
    order: FirestoreOrder,
    onBack: () -> Unit,
    onFinished: (String) -> Unit
) {
    val publicKey = BuildConfig.FEDAPAY_PUBLIC_KEY
    val hasConfiguredPublicKey = publicKey.isNotBlank() && !publicKey.contains("REPLACE_WITH")
    var widgetMessage by remember { mutableStateOf<String?>(null) }

    val nameParts = order.customerName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val firstName = nameParts.firstOrNull() ?: "Client"
    val lastName = nameParts.drop(1).joinToString(" ").ifBlank { "Nexora" }

    val checkoutHtml = remember(order.id, order.totalAmountXof, order.customerEmail, publicKey) {
        val safeKey = JSONObject.quote(publicKey)
        val safeOrderId = JSONObject.quote(order.id)
        val safeDescription = JSONObject.quote("Commande Nexora Market #${order.id}")
        val safeEmail = JSONObject.quote(order.customerEmail)
        val safeFirst = JSONObject.quote(firstName)
        val safeLast = JSONObject.quote(lastName)

        """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Paiement FedaPay - Nexora Market</title>
          <script src="https://cdn.fedapay.com/checkout.js?v=1.1.7"></script>
          <style>
            body {
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background: #0F2D52;
              color: #FFFFFF;
              margin: 0;
              padding: 20px;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 85vh;
              text-align: center;
            }
            .card {
              background: #143A6B;
              border: 1px solid #D4AF37;
              border-radius: 16px;
              padding: 24px;
              max-width: 420px;
              width: 100%;
              box-sizing: border-box;
            }
            h2 { color: #D4AF37; margin-top: 0; }
            .amount { font-size: 26px; font-weight: 800; margin: 12px 0; color: #FFFFFF; }
            .btn {
              background: #D4AF37;
              color: #0F2D52;
              border: none;
              border-radius: 10px;
              padding: 14px 20px;
              font-size: 16px;
              font-weight: 700;
              width: 100%;
              cursor: pointer;
              margin-top: 14px;
            }
            .muted { font-size: 13px; color: #CBD5E1; line-height: 1.5; }
          </style>
        </head>
        <body>
          <div class="card">
            <h2>FedaPay Checkout (Sandbox)</h2>
            <p class="muted">Commande #${order.id}</p>
            <div class="amount">${order.totalAmountXof} FCFA</div>
            <p class="muted">${order.customerName} (${order.customerEmail})</p>
            <button id="pay-btn" class="btn">Ouvrir le guichet FedaPay</button>
            <p id="status-msg" class="muted" style="margin-top: 14px;"></p>
          </div>
          <script>
            (function() {
              var statusEl = document.getElementById("status-msg");
              function launchFedaPay() {
                if (typeof FedaPay === "undefined") {
                  statusEl.textContent = "Impossible de charger Feda Checkout.js (vérifiez votre connexion).";
                  if (window.FedaPayBridge) {
                    window.FedaPayBridge.onError("Feda Checkout.js indisponible");
                  }
                  return;
                }
                try {
                  var widget = FedaPay.init("#pay-btn", {
                    public_key: $safeKey,
                    environment: "sandbox",
                    transaction: {
                      amount: ${order.totalAmountXof},
                      description: $safeDescription,
                      custom_metadata: {
                        order_id: $safeOrderId
                      }
                    },
                    customer: {
                      email: $safeEmail,
                      firstname: $safeFirst,
                      lastname: $safeLast
                    },
                    onComplete: function(resp) {
                      statusEl.textContent = "Paiement soumis. Vérification en cours...";
                      if (window.FedaPayBridge) {
                        window.FedaPayBridge.onComplete(JSON.stringify(resp || {}));
                      }
                    }
                  });
                  if (widget && typeof widget.open === "function") {
                    widget.open();
                  }
                } catch (err) {
                  statusEl.textContent = "Erreur initialisation FedaPay : " + err.message;
                  if (window.FedaPayBridge) {
                    window.FedaPayBridge.onError(err.message);
                  }
                }
              }
              window.addEventListener("load", function() {
                setTimeout(launchFedaPay, 250);
              });
            })();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement FedaPay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyBlue,
                    titleContentColor = Gold,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!hasConfiguredPublicKey) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Gold)
                ) {
                    Text(
                        text = "Clé publique FedaPay sandbox par défaut détectée. Renseignez FEDAPAY_PUBLIC_KEY dans le panneau Secrets d'AI Studio pour effectuer un vrai paiement test.",
                        fontSize = 12.sp,
                        color = NavyBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (widgetMessage != null) {
                Text(
                    text = widgetMessage!!,
                    color = AccentRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("fedapay_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        addJavascriptInterface(
                            FedaPayJsBridge(
                                onCompleted = { onFinished(order.id) },
                                onErrored = { msg -> widgetMessage = msg }
                            ),
                            "FedaPayBridge"
                        )
                        loadDataWithBaseURL(
                            "https://checkout.fedapay.com",
                            checkoutHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )

            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Button(
                        onClick = { onFinished(order.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verify_fedapay_payment_button")
                    ) {
                        Text("J'ai terminé — Vérifier le paiement", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
