package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Product
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.delay

private val NEXORA_CATEGORIES = listOf(
    "Électronique",
    "Smartphones",
    "Mode",
    "Beauté",
    "Santé & Bien-être",
    "Maison & Cuisine",
    "Produits Bébé",
    "Épicerie",
    "Automobile",
    "Importations de Chine"
)

@Composable
fun HomeScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit,
    onBrowseMore: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()

    var timerText by remember { mutableStateOf("04h : 18m : 51s") }
    LaunchedEffect(Unit) {
        var hours = 4
        var minutes = 18
        var seconds = 51
        while (true) {
            delay(1000)
            seconds--
            if (seconds < 0) {
                seconds = 59
                minutes--
                if (minutes < 0) {
                    minutes = 59
                    hours--
                    if (hours < 0) hours = 12
                }
            }
            timerText = String.format("%02dh : %02dm : %02ds", hours, minutes, seconds)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Bannière Promo
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1783164108915),
                        contentDescription = "Bannière Promotionnelle Nexora",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        NavyBlue.copy(alpha = 0.9f),
                                        NavyBlue.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 250.dp)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "SÉLECTION NEXORA",
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jusqu'à -50% en FCFA sur les meilleures marques !",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 23.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onBrowseMore,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Text("Découvrir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Barre de recherche (texte et placeholder entièrement visibles)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("home_search_input"),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.logSearch(query)
                            onBrowseMore()
                        }
                    ),
                    placeholder = {
                        Text(
                            text = "Rechercher un produit, une marque...",
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = NavyBlue
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer la recherche")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.logSearch(query)
                                        onBrowseMore()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Lancer la recherche", tint = NavyBlue)
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                if (suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                    ) {
                        Column {
                            suggestions.forEach { sugg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSearchQuery(sugg)
                                            viewModel.logSearch(sugg)
                                            onBrowseMore()
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = Gold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(sugg, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Catégories
        item {
            Column(modifier = Modifier.padding(top = 22.dp)) {
                Text(
                    text = "Catégories populaires",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(NEXORA_CATEGORIES) { cat ->
                        Card(
                            modifier = Modifier.clickable {
                                viewModel.selectCategory(cat)
                                onBrowseMore()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(NavyBlue.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIconFor(cat),
                                        contentDescription = null,
                                        tint = NavyBlue,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = cat, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Ventes Flash
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = "Ventes flash", tint = AccentRed, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("VENTES FLASH", fontWeight = FontWeight.Bold, color = AccentRed, fontSize = 16.sp)
                            Text(timerText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = onBrowseMore,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Voir tout", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Produits Vedettes
        item {
            Text(
                text = "Produits vedettes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            val featured = products.filter { it.isFeatured }
            if (featured.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chargement des produits...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(featured) { prod ->
                        ProductGridItem(
                            product = prod,
                            viewModel = viewModel,
                            onProductClick = onProductClick
                        )
                    }
                }
            }
        }

        // 6. Témoignages Marchands
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyBlue.copy(alpha = 0.05f))
                    .padding(24.dp)
            ) {
                Text("Témoignages de nos marchands", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                val testimonials = listOf(
                    "« Le Portail Vendeur Nexora a transformé mon activité de karité bio à Abidjan. Les paiements FedaPay sont fiables et rapides ! » — Yasmine Traoré",
                    "« Grâce aux importations de Chine consolidées, je propose des projecteurs solaires 100W à prix abordable en FCFA. » — Koffi Mensah"
                )
                testimonials.forEach { quote ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = quote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // 7. Newsletter & Pied de page
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Abonnez-vous aux offres Nexora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recevez chaque semaine nos codes promo et ventes flash en FCFA.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                var emailSub by remember { mutableStateOf("") }
                var subscribed by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emailSub,
                        onValueChange = { emailSub = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("exemple@email.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold),
                        shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                    )
                    Button(
                        onClick = {
                            if (emailSub.contains("@")) {
                                subscribed = true
                                emailSub = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                        shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "S'abonner", tint = Gold)
                    }
                }
                if (subscribed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Merci ! Vous êtes inscrit à la newsletter Nexora.", color = Color(0xFF10B981), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "© 2026 Nexora Market\nAbidjan | Dakar | Cotonou | Lomé | Accra\nVotre Marché Sans Limites • Paiements sécurisés FedaPay",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ShopScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit
) {
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories = listOf("Tout") + NEXORA_CATEGORIES
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .heightIn(min = 54.dp),
            singleLine = true,
            placeholder = { Text("Filtrer par nom ou description...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavyBlue) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (cat == "Tout" && selectedCategory == null) || (selectedCategory == cat)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.selectCategory(if (cat == "Tout") null else cat)
                    },
                    label = { Text(cat, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredProducts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.SearchOff,
                title = "Aucun produit trouvé",
                subtitle = "Essayez un autre mot-clé ou réinitialisez les filtres de catégorie.",
                actionLabel = "Voir tous les produits",
                actionTestTag = "reset_filters_button",
                onAction = {
                    viewModel.selectCategory(null)
                    viewModel.setSearchQuery("")
                }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { prod ->
                    ProductGridItem(
                        product = prod,
                        viewModel = viewModel,
                        onProductClick = onProductClick
                    )
                }
            }
        }
    }
}

@Composable
fun WishlistScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit,
    onExploreShop: () -> Unit = {}
) {
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()

    val wishlistProducts = allProducts.filter { prod ->
        wishlistItems.any { it.productId == prod.id }
    }

    if (wishlistProducts.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.FavoriteBorder,
            title = "Votre liste de favoris est vide",
            subtitle = "Appuyez sur le cœur d'un article qui vous plaît pour le retrouver ici à tout moment.",
            actionLabel = "Découvrir la boutique",
            actionTestTag = "wishlist_empty_explore_button",
            onAction = onExploreShop
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(wishlistProducts) { prod ->
                ProductGridItem(
                    product = prod,
                    viewModel = viewModel,
                    onProductClick = onProductClick
                )
            }
        }
    }
}
