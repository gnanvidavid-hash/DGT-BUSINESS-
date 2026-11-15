package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Product
import com.example.data.firebase.OrderStatus
import com.example.data.firebase.formatFcfa
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun categoryIconFor(category: String): ImageVector {
    return when (category) {
        "Électronique" -> Icons.Default.Tv
        "Smartphones" -> Icons.Default.Smartphone
        "Mode" -> Icons.Default.Checkroom
        "Beauté" -> Icons.Default.Spa
        "Santé & Bien-être" -> Icons.Default.VolunteerActivism
        "Maison & Cuisine" -> Icons.Default.Kitchen
        "Produits Bébé" -> Icons.Default.ChildCare
        "Épicerie" -> Icons.Default.LocalGroceryStore
        "Automobile" -> Icons.Default.DirectionsCar
        "Importations de Chine" -> Icons.Default.Public
        else -> Icons.Default.Category
    }
}

fun formatTimestampFr(date: Date?): String {
    if (date == null) return "À l'instant"
    val sdf = SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.FRANCE)
    return sdf.format(date)
}

@Composable
fun OrderStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        OrderStatus.PAID -> AccentGreen.copy(alpha = 0.16f) to Color(0xFF047857)
        OrderStatus.SHIPPED -> Color(0xFF2563EB).copy(alpha = 0.14f) to Color(0xFF1D4ED8)
        OrderStatus.DELIVERED -> AccentGreen.copy(alpha = 0.22f) to Color(0xFF065F46)
        OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELED -> AccentRed.copy(alpha = 0.15f) to AccentRed
        OrderStatus.PENDING_PAYMENT -> Gold.copy(alpha = 0.24f) to NavyBlue
        else -> NavyBlue.copy(alpha = 0.12f) to NavyBlue
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionTestTag: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(NavyBlue.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(44.dp),
                        tint = NavyBlue
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = NavyBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(actionTestTag)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun CommissionBreakdownCard(
    totalAmountXof: Long,
    platformCommissionXof: Long,
    sellerShareXof: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyBlue.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Répartition de la commission (10% / 90%)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NavyBlue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Part plateforme Nexora (10%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatFcfa(platformCommissionXof), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Part nette vendeur (90%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatFcfa(sellerShareXof), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Montant total commande", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(formatFcfa(totalAmountXof), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = NavyBlue)
            }
        }
    }
}

@Composable
fun ProductGridItem(
    product: Product,
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit
) {
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val isWishlisted = wishlistItems.any { it.productId == product.id }

    Card(
        modifier = Modifier
            .width(172.dp)
            .clickable { onProductClick(product) }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(NavyBlue.copy(alpha = 0.08f), Gold.copy(alpha = 0.06f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = categoryIconFor(product.category),
                            contentDescription = product.name,
                            modifier = Modifier.size(44.dp),
                            tint = NavyBlue.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (product.isChinaImport) {
                            Box(
                                modifier = Modifier
                                    .background(Gold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("IMPORT CHINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.formatMoney(product.price),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.oldPrice != null) {
                        Text(
                            text = viewModel.formatMoney(product.oldPrice),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.toggleWishlist(product) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Ajouter aux favoris",
                    tint = if (isWishlisted) AccentRed else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
