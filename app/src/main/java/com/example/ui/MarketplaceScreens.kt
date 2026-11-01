package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CartItemWithProduct
import com.example.data.OrderItem
import com.example.data.Product
import com.example.data.SupportMessage
import com.example.viewmodel.MarketViewModel
import kotlinx.coroutines.delay

// --- App Screens Router ---

@Composable
fun NexoraAppContent(viewModel: MarketViewModel) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val isLanguageEN by viewModel.language.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("welcome") } // welcome, main, detail, checkout, order_tracking, chat, seller_reg, seller_dash, admin
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val orderPlacedSuccess by viewModel.orderPlacedSuccess.collectAsStateWithLifecycle()

    LaunchedEffect(orderPlacedSuccess) {
        if (orderPlacedSuccess) {
            currentScreen = "order_tracking"
            viewModel.resetOrderSuccess()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "welcome" -> WelcomeScreen(
                        isEN = isLanguageEN == "EN",
                        onGetStarted = { currentScreen = "main" }
                    )
                    "main" -> MainMarketplaceScreen(
                        viewModel = viewModel,
                        onProductClick = { product ->
                            viewModel.selectProduct(product)
                            currentScreen = "detail"
                        },
                        onNavigate = { dest ->
                            currentScreen = dest
                        }
                    )
                    "detail" -> ProductDetailScreen(
                        viewModel = viewModel,
                        product = selectedProduct,
                        onBack = {
                            viewModel.selectProduct(null)
                            currentScreen = "main"
                        },
                        onBuyNow = {
                            currentScreen = "checkout"
                        }
                    )
                    "checkout" -> CheckoutScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" },
                        onPlaceOrder = {
                            viewModel.placeOrder()
                        }
                    )
                    "order_tracking" -> OrderTrackingScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
                    "chat" -> LiveChatScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
                    "seller_dash" -> SellerDashboardScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
                    "admin" -> AdminDashboardScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }
}

// --- Welcome / Onboarding Screen ---

@Composable
fun WelcomeScreen(
    isEN: Boolean,
    onGetStarted: () -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullname by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2D52),
                        Color(0xFF071526)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(2.dp, Color(0xFFD4AF37), RoundedCornerShape(32.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nexora_logo_1783164098966),
                    contentDescription = "Nexora Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nexora Market",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4AF37),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isEN) "Your Marketplace Without Limits" else "Votre Marché Sans Limites",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111E2E).copy(alpha = 0.9f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUpMode) {
                            if (isEN) "Create Account" else "Créer un Compte"
                        } else {
                            if (isEN) "Welcome Back" else "Bon Retour"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = fullname,
                            onValueChange = { fullname = it },
                            label = { Text(if (isEN) "Full Name" else "Nom Complet") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFD4AF37)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color(0xFFD4AF37),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFD4AF37)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFFD4AF37),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isEN) "Password" else "Mot de passe") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD4AF37)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFFD4AF37),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onGetStarted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD4AF37),
                            contentColor = Color(0xFF0F2D52)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        Text(
                            text = if (isSignUpMode) {
                                if (isEN) "Sign Up" else "S'inscrire"
                            } else {
                                if (isEN) "Sign In" else "Se Connecter"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { isSignUpMode = !isSignUpMode }
                    ) {
                        Text(
                            text = if (isSignUpMode) {
                                if (isEN) "Already have an account? Sign In" else "Déjà un compte ? Se connecter"
                            } else {
                                if (isEN) "New to Nexora? Create Account" else "Nouveau sur Nexora ? S'inscrire"
                            },
                            color = Color(0xFFD4AF37)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Login Row
            Text(
                text = if (isEN) "Or connect with" else "Ou se connecter avec",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SocialIcon(icon = Icons.Default.Public, description = "Google", onClick = onGetStarted)
                SocialIcon(icon = Icons.Default.Facebook, description = "Facebook", onClick = onGetStarted)
                SocialIcon(icon = Icons.Default.PhoneIphone, description = "Apple", onClick = onGetStarted)
            }
        }
    }
}

@Composable
fun SocialIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

// --- Main Marketplace Dashboard (Navigation Host) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMarketplaceScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit,
    onNavigate: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Shop, 2: Wishlist, 3: Cart, 4: Account
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()

    val titleText = when (selectedTab) {
        0 -> "Nexora Market"
        1 -> if (isEN == "EN") "Shop Directory" else "Répertoire du Magasin"
        2 -> if (isEN == "EN") "My Wishlist" else "Ma Liste"
        3 -> if (isEN == "EN") "Shopping Cart" else "Panier"
        else -> if (isEN == "EN") "Customer Profile" else "Mon Profil"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigate("chat") }) {
                        BadgedBox(
                            badge = { Badge { Text("AI") } }
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "AI Assistant")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Default.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text(if (isEN == "EN") "Home" else "Accueil") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Default.Search else Icons.Outlined.Search, contentDescription = "Shop") },
                    label = { Text(if (isEN == "EN") "Shop" else "Boutique") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist") },
                    label = { Text(if (isEN == "EN") "Wishlist" else "Favoris") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartItems.isNotEmpty()) {
                                    Badge { Text(cartItems.sumOf { it.cartItem.quantity }.toString()) }
                                }
                            }
                        ) {
                            Icon(if (selectedTab == 3) Icons.Default.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    label = { Text(if (isEN == "EN") "Cart" else "Panier") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(if (selectedTab == 4) Icons.Default.Person else Icons.Outlined.Person, contentDescription = "Account") },
                    label = { Text(if (isEN == "EN") "Account" else "Profil") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel, onProductClick, onBrowseMore = { selectedTab = 1 })
                1 -> ShopScreen(viewModel, onProductClick)
                2 -> WishlistScreen(viewModel, onProductClick)
                3 -> CartScreen(viewModel, onCheckout = { onNavigate("checkout") }, onShopNow = { selectedTab = 1 })
                4 -> AccountScreen(viewModel, onNavigate)
            }
        }
    }
}

// --- Tab 0: Home Screen ---

@Composable
fun HomeScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit,
    onBrowseMore: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    // Flash sale ticking countdown simulation
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
                    if (hours < 0) {
                        hours = 12 // reset
                    }
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
        // 1. Hero Promo Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1783164108915),
                        contentDescription = "Promo Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0F2D52).copy(alpha = 0.85f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(220.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isEnglish) "NEXORA SELECTION" else "SÉLECTION NEXORA",
                            color = Color(0xFFD4AF37),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Up to 50% Off Top Brands!" else "Jusqu'à -50% sur le Top Brand !",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBrowseMore,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (isEnglish) "Shop Now" else "Découvrir", fontSize = 11.sp, color = Color(0xFF0F2D52))
                        }
                    }
                }
            }
        }

        // 2. Search Autocomplete Bar
        item {
            val query by viewModel.searchQuery.collectAsStateWithLifecycle()
            val suggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        viewModel.setSearchQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (isEnglish) "Search products, brands, categories..." else "Rechercher des produits...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD4AF37)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Suggestions box
                if (suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column {
                            suggestions.forEach { sugg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSearchQuery(sugg)
                                            onBrowseMore()
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(sugg, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Categories horizontal scroll
        item {
            val categories = listOf(
                "Electronics", "Smartphones", "Fashion", "Beauty", "Health & Wellness",
                "Home & Kitchen", "Baby Products", "Groceries", "Automotive", "Imported Products from China"
            )

            val catIcons = mapOf(
                "Electronics" to Icons.Default.Tv,
                "Smartphones" to Icons.Default.Smartphone,
                "Fashion" to Icons.Default.Checkroom,
                "Beauty" to Icons.Default.Spa,
                "Health & Wellness" to Icons.Default.VolunteerActivism,
                "Home & Kitchen" to Icons.Default.Kitchen,
                "Baby Products" to Icons.Default.ChildCare,
                "Groceries" to Icons.Default.LocalGroceryStore,
                "Automotive" to Icons.Default.DirectionsCar,
                "Imported Products from China" to Icons.Default.Public
            )

            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    text = if (isEnglish) "Shop Categories" else "Catégories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { cat ->
                        Card(
                            modifier = Modifier
                                .clickable {
                                    viewModel.selectCategory(cat)
                                    onBrowseMore()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF0F2D52).copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcons[cat] ?: Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF0F2D52),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Flash Deals Countdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = "Flash deals", tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "FLASH DEALS" else "VENTES FLASH",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                fontSize = 16.sp
                            )
                            Text(
                                text = timerText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = onBrowseMore,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isEnglish) "View All" else "Voir Tout", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // 5. Featured / Best Seller Products Grid Header
        item {
            Text(
                text = if (isEnglish) "Featured Products" else "Produits Vedettes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Horizontal listing of featured products
        item {
            val featured = products.filter { it.isFeatured }
            if (featured.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp), contentAlignment = Alignment.Center
                ) {
                    Text("No featured products found.", color = Color.Gray)
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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

        // 6. Testimonial section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F2D52).copy(alpha = 0.05f))
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isEnglish) "What Our Merchants Say" else "Témoignages Marchands",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val testimonials = listOf(
                    "\"Nexora Seller Hub completely transformed my shea cosmetics business in Senegal. Payouts via Orange Money are instant!\" - Yasmine Traoré",
                    "\"Direct China imports changed everything. I stock 100W solar lights reliably with affordable consolidated low-cost shipping.\" - Kofi Mensah"
                )

                testimonials.forEach { test ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.FormatQuote, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = test, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 7. Newsletter Subscription & Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEnglish) "Subscribe to our Newsletter" else "S'abonner aux Offres",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnglish) "Get weekly coupons, flash deals, and merchant updates." else "Recevez des coupons de réduction chaque semaine.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                var emailSub by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = emailSub,
                        onValueChange = { emailSub = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("email@example.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
                    Button(
                        onClick = { emailSub = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2D52)),
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Subscribe")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "© 2026 Nexora Market Inc.\nLagos | Accra | Abidjan | Dakar | Guangzhou\nYour Marketplace Without Limits",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
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
    val context = LocalContext.current
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val isWishlisted = wishlistItems.any { it.productId == product.id }

    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onProductClick(product) }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                // Product Image placeholder with category icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F2D52).copy(alpha = 0.05f), Color(0xFFD4AF37).copy(alpha = 0.03f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (product.category) {
                        "Smartphones" -> Icons.Default.Smartphone
                        "Fashion" -> Icons.Default.Checkroom
                        "Beauty" -> Icons.Default.Spa
                        "Groceries" -> Icons.Default.LocalGroceryStore
                        "Imported Products from China" -> Icons.Default.Public
                        else -> Icons.Default.Category
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFF0F2D52).copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(4.dp))
                        if (product.isChinaImport) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CHINA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2D52))
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = viewModel.formatMoney(product.price),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F2D52)
                        )
                        if (product.oldPrice != null) {
                            Text(
                                text = viewModel.formatMoney(product.oldPrice),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }

            // Wishlist Toggle
            IconButton(
                onClick = { viewModel.toggleWishlist(product) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle wishlist",
                    tint = if (isWishlisted) Color.Red else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// --- Tab 1: Shop Directory (Browse & Filter) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit
) {
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories = listOf(
        "All", "Electronics", "Smartphones", "Fashion", "Beauty", "Health & Wellness",
        "Home & Kitchen", "Baby Products", "Groceries", "Automotive", "Imported Products from China"
    )
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Horizontal categories scroller filter
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (cat == "All" && selectedCategory == null) || (selectedCategory == cat)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (cat == "All") {
                            viewModel.selectCategory(null)
                        } else {
                            viewModel.selectCategory(cat)
                        }
                    },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F2D52),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isEnglish) "No products match this filter." else "Aucun produit ne correspond.",
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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

private fun Modifier.size(dp: androidx.compose.ui.unit.Dp): Modifier {
    return this.width(dp).height(dp)
}

// --- Tab 2: Wishlist Screen ---

@Composable
fun WishlistScreen(
    viewModel: MarketViewModel,
    onProductClick: (Product) -> Unit
) {
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    val wishlistProducts = allProducts.filter { prod ->
        wishlistItems.any { it.productId == prod.id }
    }

    if (wishlistProducts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isEnglish) "Your Wishlist is Empty" else "Votre Liste est Vide",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEnglish) "Tap the heart on any product to save it here." else "Cliquez sur le coeur pour sauvegarder ici.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

// --- Tab 3: Shopping Cart Screen ---

@Composable
fun CartScreen(
    viewModel: MarketViewModel,
    onCheckout: () -> Unit,
    onShopNow: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val couponApplied by viewModel.couponApplied.collectAsStateWithLifecycle()
    val couponError by viewModel.couponError.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    var couponCodeInput by remember { mutableStateOf("") }

    if (cartItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isEnglish) "Your Cart is Empty" else "Votre Panier est Vide",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onShopNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2D52))
                ) {
                    Text(if (isEnglish) "Shop Our Directory" else "Explorer la Boutique", color = Color.White)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(cartItems) { item ->
                    CartListItem(item = item, viewModel = viewModel)
                }

                // Coupon Input section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isEnglish) "Promo Coupon Code" else "Code Promo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = couponCodeInput,
                                    onValueChange = { couponCodeInput = it },
                                    placeholder = { Text("e.g. NEXORA10") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.applyCoupon(couponCodeInput)
                                        couponCodeInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2D52))
                                ) {
                                    Text(if (isEnglish) "Apply" else "Valider")
                                }
                            }

                            if (couponApplied != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Coupon: ${couponApplied!!.code} Applied! (${couponApplied!!.discountPercent}% off)",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeCoupon() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove coupon", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (couponError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = couponError!!,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Summary Bottom Sheet Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Subtotal" else "Sous-total", color = Color.Gray)
                        Text(viewModel.formatMoney(subtotal), fontWeight = FontWeight.Bold)
                    }
                    if (couponApplied != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Coupon Discount (-${couponApplied!!.discountPercent}%)", color = Color(0xFF10B981))
                            val discountAmt = (subtotal * couponApplied!!.discountPercent) / 100.0
                            Text("-${viewModel.formatMoney(discountAmt)}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Shipping Fee" else "Frais de Livraison", color = Color.Gray)
                        Text(viewModel.formatMoney(5.0), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Total" else "Total à Payer", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F2D52))
                        Text(viewModel.formatMoney(total), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F2D52))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCheckout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F2D52)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button")
                    ) {
                        Text(if (isEnglish) "Proceed to Checkout" else "Passer à la Caisse", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CartListItem(item: CartItemWithProduct, viewModel: MarketViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F2D52).copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (item.product.category) {
                    "Smartphones" -> Icons.Default.Smartphone
                    "Fashion" -> Icons.Default.Checkroom
                    "Beauty" -> Icons.Default.Spa
                    else -> Icons.Default.Category
                }
                Icon(icon, contentDescription = null, tint = Color(0xFF0F2D52), modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.product.category, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(viewModel.formatMoney(item.product.price), fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F2D52))
            }

            // Quantity adjusters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.updateCartQuantity(item.cartItem.id, item.cartItem.quantity - 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                Text(item.cartItem.quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)

                IconButton(
                    onClick = { viewModel.updateCartQuantity(item.cartItem.id, item.cartItem.quantity + 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// --- Tab 4: Customer Account & Hub Settings ---

@Composable
fun AccountScreen(
    viewModel: MarketViewModel,
    onNavigate: (String) -> Unit
) {
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // User Info Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2D52)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFFD4AF37), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JD", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF0F2D52))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Gnanvi David", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("gnanvidavid@gmail.com", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        // Loyalty points
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1,250 Nexora Points", color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Referral Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isEnglish) "Nexora Referral Program" else "Parrainage Nexora", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (isEnglish) "Invite friends to get $10/6,000 FCFA coupon codes! Code: NEXORAMEMBER" else "Partagez votre code et gagnez des réductions !",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Share, contentDescription = "Share code", tint = Color(0xFF0F2D52))
                }
            }
        }

        // Settings / Hub Links Header
        item {
            Text(
                text = if (isEnglish) "Services & Portals" else "Services et Portails",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column {
                    AccountLinkItem(
                        icon = Icons.Default.LocalShipping,
                        title = if (isEnglish) "Track My Orders" else "Suivi de Commande",
                        subtitle = if (isEnglish) "Check shipping stages and delivery details" else "Vérifier l'état de votre livraison",
                        onClick = { onNavigate("order_tracking") }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    AccountLinkItem(
                        icon = Icons.Default.Storefront,
                        title = if (isEnglish) "Seller Marketplace Hub" else "Portail Vendeur",
                        subtitle = if (isEnglish) "Register as merchant, manage stock and analytics" else "Gérer vos produits et vos rapports de ventes",
                        onClick = { onNavigate("seller_dash") }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    AccountLinkItem(
                        icon = Icons.Default.AdminPanelSettings,
                        title = if (isEnglish) "Admin Dashboard" else "Tableau de Bord Admin",
                        subtitle = if (isEnglish) "Manage orders, sellers, users and global platform" else "Administration de la plateforme Nexora",
                        onClick = { onNavigate("admin") }
                    )
                }
            }
        }

        // Language, Theme, Currency settings
        item {
            Text(
                text = if (isEnglish) "App Configuration" else "Configuration de l'App",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Dark Theme Switch
                    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF0F2D52))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isEnglish) "Dark Theme Mode" else "Mode Sombre", fontWeight = FontWeight.SemiBold)
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Language toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF0F2D52))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isEnglish) "App Language" else "Langue de l'App", fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isEnglish,
                                onClick = { viewModel.setLanguage("EN") },
                                label = { Text("English") }
                            )
                            FilterChip(
                                selected = !isEnglish,
                                onClick = { viewModel.setLanguage("FR") },
                                label = { Text("Français") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Currency toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF0F2D52))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isEnglish) "Preferred Currency" else "Devise Préférée", fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = currency == "USD",
                                onClick = { viewModel.setCurrency("USD") },
                                label = { Text("USD ($)") }
                            )
                            FilterChip(
                                selected = currency == "XOF",
                                onClick = { viewModel.setCurrency("XOF") },
                                label = { Text("FCFA (XOF)") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountLinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF0F2D52).copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0F2D52))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

// --- Checkout Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit,
    onPlaceOrder: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()
    val total by viewModel.cartTotal.collectAsStateWithLifecycle()
    val couponApplied by viewModel.couponApplied.collectAsStateWithLifecycle()

    val address by viewModel.checkoutAddress.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.checkoutPaymentMethod.collectAsStateWithLifecycle()
    val shippingMethod by viewModel.checkoutShippingMethod.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    // Simulating mobile money inputs
    var momoNumber by remember { mutableStateOf("") }
    var momoCarrier by remember { mutableStateOf("MTN Mobile Money") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Checkout Form" else "Validation Commande", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Address Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (isEnglish) "Delivery Address" else "Adresse de Livraison", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = address,
                            onValueChange = { viewModel.checkoutAddress.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (isEnglish) "Pickup Points options are also available in store." else "Des points relais relais sont disponibles.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            // Shipping selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (isEnglish) "Delivery Method" else "Option de Livraison", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = shippingMethod == "Express Home Delivery",
                                onClick = { viewModel.checkoutShippingMethod.value = "Express Home Delivery" },
                                label = { Text(if (isEnglish) "Express Home" else "Express Domicile") }
                            )
                            FilterChip(
                                  selected = shippingMethod == "Pickup Point",
                                  onClick = { viewModel.checkoutShippingMethod.value = "Pickup Point" },
                                  label = { Text(if (isEnglish) "Secure Pickup Point" else "Point Relais") }
                            )
                        }
                    }
                }
            }

            // Payments Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (isEnglish) "Payment Option" else "Mode de Paiement", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        val methods = listOf("MTN Mobile Money", "Orange Money", "Moov Money", "Visa/Mastercard", "Stripe", "Stripe_PayPal")
                        methods.forEach { m ->
                            val cleanLabel = if (m == "Stripe_PayPal") "PayPal" else m
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.checkoutPaymentMethod.value = m
                                        momoCarrier = m
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = paymentMethod == m,
                                    onClick = {
                                        viewModel.checkoutPaymentMethod.value = m
                                        momoCarrier = m
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD4AF37))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (m.contains("Money") || m.contains("MTN")) Icons.Default.PhoneAndroid else Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFF0F2D52)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cleanLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Momo fields
                        if (paymentMethod.contains("Money") || paymentMethod.contains("MTN")) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Confirm Mobile Wallet ($momoCarrier)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F2D52))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = momoNumber,
                                onValueChange = { momoNumber = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("+229 97 00 00 00 / +233 24...") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37))
                            )
                        }
                    }
                }
            }

            // Summary Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (isEnglish) "Order Summary" else "Détail de la Facture", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Subtotal" else "Sous-total")
                            Text(viewModel.formatMoney(subtotal))
                        }
                        if (couponApplied != null) {
                            val discountAmt = (subtotal * couponApplied!!.discountPercent) / 100.0
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount (-${couponApplied!!.discountPercent}%)", color = Color(0xFF10B981))
                                Text("-${viewModel.formatMoney(discountAmt)}", color = Color(0xFF10B981))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Delivery Fee" else "Frais d'expédition")
                            Text(viewModel.formatMoney(5.0))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isEnglish) "Total Due" else "Total à Payer", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F2D52))
                            Text(viewModel.formatMoney(total), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F2D52))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onPlaceOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F2D52)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("place_order_button")
                        ) {
                            Text(if (isEnglish) "Confirm Secure Payment" else "Confirmer le Paiement Sécurisé", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Order Tracking Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    var selectedOrder by remember { mutableStateOf<OrderItem?>(null) }

    LaunchedEffect(orders) {
        if (orders.isNotEmpty() && selectedOrder == null) {
            selectedOrder = orders.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Order Delivery Tracking" else "Suivi de Livraison", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp), contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(if (isEnglish) "No active orders found to track." else "Aucune commande en cours.")
                    }
                }
            } else {
                // Dropdown or selector if multiple orders
                if (orders.size > 1) {
                    Text(
                        text = if (isEnglish) "Select Order" else "Sélectionner Commande",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orders) { o ->
                            val isSelected = selectedOrder?.id == o.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedOrder = o },
                                label = { Text(o.id) }
                            )
                        }
                    }
                }

                selectedOrder?.let { order ->
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        // Order details summary
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Order ID: ${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = order.status,
                                            color = Color(0xFF0F2D52),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Items: ${order.itemsJson}", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Delivery Address: ${order.shippingAddress}", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Total Paid: ${viewModel.formatMoney(order.totalAmount)} (${order.paymentMethod})", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Progress line tracker
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(if (isEnglish) "Real-Time Courier Stages" else "Étapes d'expédition", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            val stages = listOf(
                                Triple("Order Received", "We have received your payment wallet instructions.", 0.25f),
                                Triple("Processing & Packaged", "Merchant is packing item securely.", 0.50f),
                                Triple("Out For Delivery", "Nexora Express rider has loaded the package.", 0.75f),
                                Triple("Delivered", "Handed over safely or collected from pickup point.", 1.0f)
                            )

                            stages.forEach { stage ->
                                val isActive = order.trackingProgress >= stage.third
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    if (isActive) Color(0xFFD4AF37) else Color.LightGray.copy(alpha = 0.5f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isActive) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F2D52), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        // Visual line down to next node
                                        if (stage.first != "Delivered") {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(40.dp)
                                                    .background(if (order.trackingProgress > stage.third) Color(0xFFD4AF37) else Color.LightGray.copy(alpha = 0.5f))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = stage.first,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
                                            color = if (isActive) MaterialTheme.colorScheme.onBackground else Color.Gray
                                        )
                                        Text(stage.second, fontSize = 11.sp, color = Color.Gray)
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

// --- Live Support Chatbot Screen (Conversational Assistant) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color(0xFFD4AF37))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Nexora AI Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(if (isEnglish) "Online assistant support" else "Assistant Virtuel", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear chat", tint = Color.Red)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg = msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(if (isEnglish) "Nexora is writing..." else "Nexora est en train d'écrire...", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Input Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { viewModel.setChatInput(it) },
                        placeholder = { Text(if (isEnglish) "Ask me anything about Nexora..." else "Posez-moi une question...") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.sendChatMessage() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF0F2D52), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: SupportMessage) {
    val isAI = msg.sender == "ai"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isAI) 4.dp else 16.dp,
                        bottomEnd = if (isAI) 16.dp else 4.dp
                    )
                )
                .background(
                    if (isAI) Color(0xFF0F2D52).copy(alpha = 0.08f) else Color(0xFF0F2D52)
                )
                .border(
                    1.dp,
                    if (isAI) Color(0xFF0F2D52).copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isAI) MaterialTheme.colorScheme.onSurface else Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// --- Seller Dashboard Screen (Merchant Hub) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val currentSeller by viewModel.currentSeller.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    // Form inputs for registration
    var bizName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Fashion") }
    var bizEmail by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEnglish) "Seller Marketplace Hub" else "Espace Vendeurs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentSeller != null) {
                        IconButton(onClick = { viewModel.logoutSeller() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Log out", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentSeller == null) {
                // Register Seller Form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isEnglish) "Open Your Seller Store" else "Ouvrez Votre Boutique",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnglish) "List items, track merchant sales, withdraw CFA/USD instantly." else "Gérez vos ventes et recevez vos fonds par Momo.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = bizName,
                        onValueChange = { bizName = it },
                        label = { Text(if (isEnglish) "Business Brand Name" else "Nom de l'Entreprise") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(if (isEnglish) "Owner Full Name" else "Nom de Gérant") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bizEmail,
                        onValueChange = { bizEmail = it },
                        label = { Text("Contact Business Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (bizName.isNotEmpty() && ownerName.isNotEmpty()) {
                                viewModel.registerAsSeller(bizName, ownerName, categorySelection, bizEmail)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2D52)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(if (isEnglish) "Register Seller Store" else "Créer la Boutique", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Active Seller Dashboard
                val seller = currentSeller!!
                val sellerProductsList = products.filter { it.sellerName == seller.businessName }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Merchant Metrics Row
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2D52))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Seller: ${seller.businessName}", fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37), fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Contact: ${seller.email} | Owner: ${seller.sellerName}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Balance & Earnings", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        Text(viewModel.formatMoney(seller.balance), fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFD4AF37).copy(alpha = 0.2f))
                                            .padding(8.dp)
                                    ) {
                                        Text("VERIFIED VENDOR", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Product upload panel
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Supply & List New Product", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                val name by viewModel.sellerNewName.collectAsStateWithLifecycle()
                                val price by viewModel.sellerNewPrice.collectAsStateWithLifecycle()
                                val category by viewModel.sellerNewCategory.collectAsStateWithLifecycle()
                                val desc by viewModel.sellerNewDesc.collectAsStateWithLifecycle()

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { viewModel.sellerNewName.value = it },
                                    label = { Text("Product Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = price,
                                    onValueChange = { viewModel.sellerNewPrice.value = it },
                                    label = { Text("Price in USD ($)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = desc,
                                    onValueChange = { viewModel.sellerNewDesc.value = it },
                                    label = { Text("Description & Specifications") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { viewModel.addNewProductFromSeller() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F2D52)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add Product to Directory", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Listed Items headers
                    item {
                        Text(
                            text = "My Active Catalog Listings (${sellerProductsList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (sellerProductsList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp), contentAlignment = Alignment.Center
                            ) {
                                Text("No listings active in catalog.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(sellerProductsList) { p ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Category: ${p.category} | Price: ${viewModel.formatMoney(p.price)}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Admin Dashboard Panel ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val sellers by viewModel.registeredSellers.collectAsStateWithLifecycle()
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    var adminTab by remember { mutableStateOf("Sellers") } // Sellers, Products, Orders

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nexora Admin Terminal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Horizontal Admin navigation tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sellers", "Products", "Orders").forEach { tab ->
                    FilterChip(
                        selected = adminTab == tab,
                        onClick = { adminTab = tab },
                        label = { Text(tab) }
                    )
                }
            }

            // Tab Content
            when (adminTab) {
                "Sellers" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (sellers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp), contentAlignment = Alignment.Center
                                ) {
                                    Text("No registered sellers active.")
                                }
                            }
                        } else {
                            items(sellers) { s ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(s.businessName, fontWeight = FontWeight.Bold)
                                            Text("Owner: ${s.sellerName} | Email: ${s.email}", fontSize = 11.sp, color = Color.Gray)
                                            Text("Category: ${s.category}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF0F2D52))
                                    }
                                }
                            }
                        }
                    }
                }
                "Products" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(products) { p ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Seller: ${p.sellerName} | Category: ${p.category} | Price: $${p.price}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteProductByAdmin(p.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete product", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
                "Orders" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        if (orders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp), contentAlignment = Alignment.Center
                                ) {
                                    Text("No orders placed yet.")
                                }
                            }
                        } else {
                            items(orders) { o ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ID: ${o.id}", fontWeight = FontWeight.Bold)
                                            Text(o.status, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F2D52))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Delivery To: ${o.shippingAddress}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Total Amount: ${viewModel.formatMoney(o.totalAmount)}", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Status controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.updateOrderStatusByAdmin(o.id, "Processing", 0.50f) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.5f), contentColor = Color.Black),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp)
                                            ) {
                                                Text("Process", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.updateOrderStatusByAdmin(o.id, "Shipped", 0.75f) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F2D52)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp)
                                            ) {
                                                Text("Ship", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.updateOrderStatusByAdmin(o.id, "Delivered", 1.0f) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp)
                                            ) {
                                                Text("Deliver", fontSize = 10.sp)
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
    }
}

// --- Product Detail Screen ---

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
    val isEN by viewModel.language.collectAsStateWithLifecycle()
    val isEnglish = isEN == "EN"

    var selectedColor by remember { mutableStateOf("Classic Navy") }
    var selectedSize by remember { mutableStateOf("Standard") }
    var reviewInput by remember { mutableStateOf("") }
    var reviewRating by remember { mutableStateOf(5) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWishlist(product) }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle wishlist",
                            tint = if (isWishlisted) Color.Red else Color.Gray
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
                    .padding(16.dp)
            ) {
                // Large product visual placeholder with gradient background
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF0F2D52).copy(alpha = 0.08f), Color(0xFFD4AF37).copy(alpha = 0.04f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (product.category) {
                            "Smartphones" -> Icons.Default.Smartphone
                            "Fashion" -> Icons.Default.Checkroom
                            "Beauty" -> Icons.Default.Spa
                            "Groceries" -> Icons.Default.LocalGroceryStore
                            "Imported Products from China" -> Icons.Default.Public
                            else -> Icons.Default.Category
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF0F2D52).copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            if (product.isChinaImport) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFD4AF37), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("CONSOLIDATED CHINA IMPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F2D52))
                                }
                            }
                        }
                    }
                }

                // Core details: Title, prices, rating
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(product.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F2D52))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = viewModel.formatMoney(product.price),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F2D52)
                            )
                            if (product.oldPrice != null) {
                                Text(
                                    text = viewModel.formatMoney(product.oldPrice),
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }

                        // Rating badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${product.rating} (${product.reviewCount} reviews)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Seller info & description
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Merchant: ${product.sellerName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F2D52)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Custom options selection (Color / Size)
                item {
                    Text("Product Style", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedColor == "Classic Navy",
                            onClick = { selectedColor = "Classic Navy" },
                            label = { Text("Classic Navy") }
                        )
                        FilterChip(
                            selected = selectedColor == "Imperial Gold",
                            onClick = { selectedColor = "Imperial Gold" },
                            label = { Text("Imperial Gold") }
                        )
                        FilterChip(
                            selected = selectedColor == "Slate Gray",
                            onClick = { selectedColor = "Slate Gray" },
                            label = { Text("Slate Gray") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // User reviews panel & entry
                item {
                    Text(if (isEnglish) "Verified Customer Reviews" else "Avis Clients Vérifiés", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (reviews.isEmpty()) {
                        Text(if (isEnglish) "No reviews yet. Be the first to leave one!" else "Aucun avis pour l'instant.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        reviews.forEach { rev ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rev.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Row {
                                            repeat(rev.rating) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.comment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    // Add Review Input
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (isEnglish) "Add Your Review" else "Laisser un Avis", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Star selection
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { r ->
                                    IconButton(
                                        onClick = { reviewRating = r },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (r <= reviewRating) Color(0xFFD4AF37) else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = reviewInput,
                                onValueChange = { reviewInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(if (isEnglish) "Share your experience with this product..." else "Votre commentaire...") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37)),
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (reviewInput.trim().isNotEmpty()) {
                                        viewModel.addReview(product.id, reviewRating, reviewInput.trim())
                                        reviewInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2D52)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (isEnglish) "Submit Review" else "Envoyer")
                            }
                        }
                    }
                }
            }

            // Bottom Buttons Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.addToCart(product, 1, selectedColor, selectedSize)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF0F2D52)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F2D52))
                    ) {
                        Text(if (isEnglish) "Add to Cart" else "Ajouter au Panier", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.addToCart(product, 1, selectedColor, selectedSize)
                            onBuyNow()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("buy_now_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color(0xFF0F2D52))
                    ) {
                        Text(if (isEnglish) "Buy Now" else "Acheter", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
