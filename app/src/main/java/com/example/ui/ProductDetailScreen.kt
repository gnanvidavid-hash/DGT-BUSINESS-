package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Product
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: MarketViewModel,
    product: Product?,
    onBack: () -> Unit,
    onBuyNow: () -> Unit
) {
    if (product == null) return

    val reviews by viewModel.productReviews.collectAsStateWithLifecycle()
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val isWishlisted = wishlistItems.any { it.productId == product.id }

    val colorOptions = listOf("Bleu Nuit", "Or Impérial", "Noir Mat")
    val sizeOptions = when (product.category) {
        "Smartphones" -> listOf("128 Go", "256 Go", "512 Go")
        "Mode" -> listOf("M", "L", "XL")
        "Beauté", "Santé & Bien-être", "Épicerie" -> listOf("Format Standard", "Format Familial")
        else -> listOf("Standard", "Pack Pro")
    }

    var selectedColor by remember(product.id) { mutableStateOf(colorOptions.first()) }
    var selectedSize by remember(product.id) { mutableStateOf(sizeOptions.first()) }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var reviewInput by remember { mutableStateOf("") }
    var reviewRating by remember { mutableIntStateOf(5) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 3 })

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWishlist(product) }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isWishlisted) AccentRed else Color.Gray
                        )
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Carrousel de photos (HorizontalPager)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(NavyBlue.copy(alpha = 0.14f), Gold.copy(alpha = 0.14f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = categoryIconFor(product.category),
                                            contentDescription = "${product.name} - vue principale",
                                            modifier = Modifier.size(92.dp),
                                            tint = NavyBlue
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = Gold,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (product.isChinaImport) "IMPORTATION CHINE VÉRIFIÉE" else "VUE PRINCIPALE",
                                                color = NavyBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                1 -> Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_hero_banner_1783164108915),
                                        contentDescription = "${product.name} - mise en situation",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(NavyBlue.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Text(
                                            text = "Qualité Nexora • ${product.category}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                else -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(NavyBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Gold, modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Garantie & Paiement Protégé FedaPay",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Expédié par ${product.sellerName}",
                                            color = Gold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Indicateur compteur
                        Surface(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / 3",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Points d'indication
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(3) { idx ->
                                val active = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .size(if (active) 10.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(if (active) Gold else Color.White.copy(alpha = 0.55f))
                                )
                            }
                        }
                    }
                }

                // 2. Titre, prix en FCFA, note & badge stock
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = product.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.formatMoney(product.price),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (product.oldPrice != null) {
                                Text(
                                    text = viewModel.formatMoney(product.oldPrice),
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }

                        // Badge de stock
                        val (stockColor, stockLabel) = when {
                            product.stock <= 0 -> AccentRed to "Rupture de stock"
                            product.stock <= 5 -> Color(0xFFD97706) to "Stock limité (${product.stock} restants)"
                            else -> AccentGreen to "En stock (${product.stock} disponibles)"
                        }
                        Surface(
                            color = stockColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, stockColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = stockLabel,
                                color = stockColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Note", tint = Gold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", product.rating)} (${product.reviewCount} avis)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(product.sellerName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. Description
                item {
                    Text("Description du produit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 4. Choix des variantes & Quantité
                item {
                    Text("Couleur / Finition : $selectedColor", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { col ->
                            FilterChip(
                                selected = selectedColor == col,
                                onClick = { selectedColor = col },
                                label = { Text(col) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Variante / Format : $selectedSize", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sizeOptions.forEach { sz ->
                            FilterChip(
                                selected = selectedSize == sz,
                                onClick = { selectedSize = sz },
                                label = { Text(sz) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Gold,
                                    selectedLabelColor = NavyBlue
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quantité", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuer")
                            }
                            Text(quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(
                                onClick = { if (quantity < product.stock.coerceAtLeast(1)) quantity++ },
                                modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Augmenter")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 5. Avis clients & ajout d'avis
                item {
                    Text("Avis clients vérifiés (${reviews.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (reviews.isEmpty()) {
                        Text("Aucun avis pour le moment. Soyez le premier à donner votre avis !", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    } else {
                        reviews.forEach { rev ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rev.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row {
                                            repeat(rev.rating) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.comment, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Laisser un avis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { r ->
                                    IconButton(onClick = { reviewRating = r }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "$r étoiles",
                                            tint = if (r <= reviewRating) Gold else Color.LightGray
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = reviewInput,
                                onValueChange = { reviewInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Partagez votre expérience avec cet article...") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (reviewInput.trim().isNotEmpty()) {
                                        viewModel.addReview(product.id, reviewRating, reviewInput.trim())
                                        reviewInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Publier l'avis", color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Barre d'action inférieure
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.addToCart(product, quantity, selectedColor, selectedSize)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Article ajouté au panier !")
                            }
                        },
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, NavyBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyBlue)
                    ) {
                        Text("Ajouter au panier", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.addToCart(product, quantity, selectedColor, selectedSize)
                            onBuyNow()
                        },
                        enabled = product.stock > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("buy_now_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue)
                    ) {
                        Text("Acheter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
