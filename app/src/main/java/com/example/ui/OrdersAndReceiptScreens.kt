package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.FirestoreOrder
import com.example.data.firebase.OrderStatus
import com.example.data.firebase.PaymentMethod
import com.example.data.firebase.formatFcfa
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    viewModel: MarketViewModel,
    orderId: String,
    onBackHome: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenReceipt: (FirestoreOrder) -> Unit,
    onRetryFedaPay: (FirestoreOrder) -> Unit
) {
    LaunchedEffect(orderId) {
        viewModel.observeConfirmationOrder(orderId)
    }

    val order by viewModel.activeConfirmationOrder.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmation de commande", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Accueil")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (order == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Gold)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Chargement du statut en temps réel...", fontWeight = FontWeight.Medium)
                }
            }
            return@Scaffold
        }

        val currentOrder = order!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statut en temps réel (piloté exclusivement par le webhook backend pour FedaPay)
            when (currentOrder.status) {
                OrderStatus.PENDING_PAYMENT -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("payment_verifying_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyBlue),
                        border = BorderStroke(1.5.dp, Gold)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Gold, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Paiement en cours de vérification",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Votre commande #${currentOrder.id} attend la confirmation officielle du webhook FedaPay. Le statut sera mis à jour automatiquement en temps réel.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { onRetryFedaPay(currentOrder) },
                                border = BorderStroke(1.dp, Gold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                            ) {
                                Text("Ouvrir FedaPay à nouveau")
                            }
                        }
                    }
                }
                OrderStatus.PAID -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.14f)),
                        border = BorderStroke(1.5.dp, AccentGreen)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Paiement confirmé • Payée",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Votre paiement FedaPay pour la commande #${currentOrder.id} a été validé par notre serveur.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            if (!currentOrder.fedapayTransactionId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Réf. FedaPay : ${currentOrder.fedapayTransactionId}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBlue
                                )
                            }
                        }
                    }
                }
                OrderStatus.PAYMENT_FAILED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f)),
                        border = BorderStroke(1.5.dp, AccentRed)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Paiement échoué", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "La transaction FedaPay a été refusée ou annulée. Vous pouvez réessayer le paiement.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onRetryFedaPay(currentOrder) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue, contentColor = Gold)
                            ) {
                                Text("Réessayer avec FedaPay", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.16f)),
                        border = BorderStroke(1.5.dp, Gold)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(50.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Commande enregistrée (${currentOrder.status})",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyBlue,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Commande #${currentOrder.id} • Mode : ${currentOrder.paymentMethod}",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Détails commande
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Statut actuel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        OrderStatusBadge(currentOrder.status)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Montant total", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatFcfa(currentOrder.totalAmountXof), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NavyBlue)
                    }
                    Text("Articles : ${currentOrder.itemsSummary}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Adresse : ${currentOrder.shippingAddress}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            CommissionBreakdownCard(
                totalAmountXof = currentOrder.totalAmountXof,
                platformCommissionXof = currentOrder.platformCommissionXof,
                sellerShareXof = currentOrder.sellerShareXof
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onOpenReceipt(currentOrder) },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("view_receipt_button"),
                    border = BorderStroke(1.5.dp, NavyBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NavyBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voir le reçu", fontWeight = FontWeight.Bold, color = NavyBlue)
                }

                Button(
                    onClick = onOpenOrders,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mes commandes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit,
    onOpenConfirmation: (FirestoreOrder) -> Unit,
    onOpenReceipt: (FirestoreOrder) -> Unit,
    onPayOrder: (FirestoreOrder) -> Unit
) {
    val orders by viewModel.userOrders.collectAsStateWithLifecycle()
    val statusFilters = listOf(
        "Toutes",
        OrderStatus.PENDING_PAYMENT,
        OrderStatus.PENDING,
        OrderStatus.PAID,
        OrderStatus.PAYMENT_FAILED,
        OrderStatus.SHIPPED,
        OrderStatus.DELIVERED,
        OrderStatus.CANCELED
    )
    var selectedFilter by remember { mutableStateOf("Toutes") }

    val displayedOrders = remember(orders, selectedFilter) {
        if (selectedFilter == "Toutes") orders else orders.filter { it.status == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des commandes", fontWeight = FontWeight.Bold) },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusFilters) { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = status },
                        label = { Text(status, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (displayedOrders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "Aucune commande",
                    subtitle = if (selectedFilter == "Toutes") {
                        "Vos futures commandes apparaîtront ici avec leur suivi temps réel et leur reçu FCFA."
                    } else {
                        "Aucune commande avec le statut « $selectedFilter »."
                    },
                    actionLabel = "Retour à l'accueil",
                    actionTestTag = "orders_empty_back_button",
                    onAction = onBack
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(displayedOrders, key = { it.id }) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Commande #${order.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyBlue)
                                        Text(
                                            text = formatTimestampFr(order.createdAt?.toDate()),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OrderStatusBadge(order.status)
                                }

                                Text(order.itemsSummary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Mode : ${order.paymentMethod}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatFcfa(order.totalAmountXof), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NavyBlue)
                                }

                                LinearProgressIndicator(
                                    progress = { order.trackingProgress.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = if (order.status == OrderStatus.CANCELED || order.status == OrderStatus.PAYMENT_FAILED) AccentRed else Gold,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenConfirmation(order) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Suivi", fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { onOpenReceipt(order) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue, contentColor = Gold),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reçu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (order.paymentMethod == PaymentMethod.FEDAPAY &&
                                    (order.status == OrderStatus.PENDING_PAYMENT || order.status == OrderStatus.PAYMENT_FAILED)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onPayOrder(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Payer (FedaPay)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        TextButton(onClick = { viewModel.cancelOrder(order.id) }) {
                                            Text("Annuler", color = AccentRed)
                                        }
                                    }
                                } else if (order.status == OrderStatus.PENDING) {
                                    TextButton(
                                        onClick = { viewModel.cancelOrder(order.id) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Annuler la commande", color = AccentRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderReceiptScreen(
    viewModel: MarketViewModel,
    initialOrder: FirestoreOrder?,
    onBack: () -> Unit
) {
    val liveOrder by viewModel.activeConfirmationOrder.collectAsStateWithLifecycle()
    val order = if (liveOrder?.id == initialOrder?.id && liveOrder != null) liveOrder else initialOrder

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reçu de commande", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Reçu introuvable.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("order_receipt_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Gold)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NEXORA MARKET", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NavyBlue)
                            Text("Reçu officiel • FCFA (XOF)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OrderStatusBadge(order.status)
                    }

                    HorizontalDivider()

                    ReceiptRow("N° de commande", "#${order.id}")
                    ReceiptRow("Date", formatTimestampFr(order.createdAt?.toDate()))
                    ReceiptRow("Client", order.customerName)
                    ReceiptRow("E-mail", order.customerEmail)
                    ReceiptRow("Livraison", "${order.shippingMethod}\n${order.shippingAddress}")
                    ReceiptRow("Mode de paiement", order.paymentMethod)
                    if (!order.fedapayTransactionId.isNullOrBlank()) {
                        ReceiptRow("Transaction FedaPay", order.fedapayTransactionId)
                    }

                    HorizontalDivider()

                    Text("Détails des articles", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyBlue)
                    if (order.items.isNotEmpty()) {
                        order.items.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${line.quantity}x ${line.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    if (line.variant.isNotBlank()) {
                                        Text(line.variant, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = formatFcfa(line.unitPriceXof * line.quantity),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Text(order.itemsSummary, fontSize = 13.sp)
                    }

                    HorizontalDivider()

                    ReceiptRow("Sous-total", formatFcfa(order.subtotalXof))
                    if (order.discountXof > 0L) {
                        ReceiptRow("Remise", "-${formatFcfa(order.discountXof)}")
                    }
                    ReceiptRow("Frais de livraison", formatFcfa(order.shippingFeeXof))
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL TTC", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = NavyBlue)
                        Text(formatFcfa(order.totalAmountXof), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NavyBlue)
                    }
                }
            }

            CommissionBreakdownCard(
                totalAmountXof = order.totalAmountXof,
                platformCommissionXof = order.platformCommissionXof,
                sellerShareXof = order.sellerShareXof
            )
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
    }
}
