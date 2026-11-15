package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.analytics.AnalyticsScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.firebase.FirestoreOrder
import com.example.data.firebase.UserRole
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun NexoraAppContent(viewModel: MarketViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val firebaseUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isVerifiedAdmin by viewModel.isVerifiedAdmin.collectAsStateWithLifecycle()
    val profileLoading by viewModel.profileLoading.collectAsStateWithLifecycle()
    val authMessage by viewModel.authMessage.collectAsStateWithLifecycle()

    var isEditingRole by remember { mutableStateOf(false) }

    LaunchedEffect(firebaseUser?.uid) {
        val currentUser = firebaseUser
        if (currentUser != null) {
            viewModel.attachAuthenticatedUser(
                uid = currentUser.uid,
                displayName = currentUser.displayName,
                email = currentUser.email
            )
        } else {
            viewModel.clearAuthenticatedUser()
            isEditingRole = false
        }
    }

    val activeUser = firebaseUser
    if (activeUser == null || userProfile == null || isEditingRole) {
        LaunchedEffect(Unit) {
            viewModel.trackScreenView(AnalyticsScreen.WELCOME)
        }
        AuthGateScreen(
            firebaseUser = activeUser,
            initialName = userProfile?.fullName ?: activeUser?.displayName ?: "",
            initialEmail = userProfile?.email ?: activeUser?.email ?: "",
            initialRole = UserRole.fromKey(userProfile?.role),
            isVerifiedAdmin = isVerifiedAdmin,
            isLoading = profileLoading,
            errorMessage = authMessage,
            onSaveProfile = { name, email, role ->
                viewModel.saveUserProfile(name, email, role) {
                    isEditingRole = false
                }
            },
            onSignOut = {
                signOutUser(credentialManager, scope) {
                    viewModel.clearAuthenticatedUser()
                    isEditingRole = false
                }
            },
            onClearError = { viewModel.clearAuthMessage() }
        )
    } else {
        key(activeUser.uid) {
            NexoraMainRouter(
                viewModel = viewModel,
                onEditRole = { isEditingRole = true },
                onSignOut = {
                    signOutUser(credentialManager, scope) {
                        viewModel.clearAuthenticatedUser()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexoraMainRouter(
    viewModel: MarketViewModel,
    onEditRole: () -> Unit,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("main") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val pendingFedaPayOrder by viewModel.pendingFedaPayOrder.collectAsStateWithLifecycle()
    val activeConfirmationOrderId by viewModel.activeConfirmationOrderId.collectAsStateWithLifecycle()
    val selectedReceiptOrder by viewModel.selectedReceiptOrder.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isVerifiedAdmin by viewModel.isVerifiedAdmin.collectAsStateWithLifecycle()

    val canAccessAdmin = UserRole.fromKey(userProfile?.role) == UserRole.ADMIN && isVerifiedAdmin

    LaunchedEffect(canAccessAdmin, currentScreen) {
        if (currentScreen == "admin" && !canAccessAdmin) {
            currentScreen = "main"
        }
    }

    LaunchedEffect(currentScreen, selectedTab) {
        val screenAnalytics = when (currentScreen) {
            "main" -> when (selectedTab) {
                0 -> AnalyticsScreen.HOME
                1 -> AnalyticsScreen.SHOP
                2 -> AnalyticsScreen.WISHLIST
                3 -> AnalyticsScreen.CART
                else -> AnalyticsScreen.ACCOUNT
            }
            "detail" -> AnalyticsScreen.PRODUCT_DETAIL
            "checkout", "fedapay_checkout" -> AnalyticsScreen.CHECKOUT
            "order_confirmation", "orders", "receipt" -> AnalyticsScreen.ORDER_TRACKING
            "chat" -> AnalyticsScreen.LIVE_CHAT
            "seller_dash" -> AnalyticsScreen.SELLER_DASHBOARD
            "admin" -> AnalyticsScreen.ADMIN_DASHBOARD
            else -> AnalyticsScreen.HOME
        }
        viewModel.trackScreenView(screenAnalytics)
    }

    when (currentScreen) {
        "detail" -> {
            BackHandler { currentScreen = "main" }
            ProductDetailScreen(
                viewModel = viewModel,
                product = selectedProduct,
                onBack = { currentScreen = "main" },
                onBuyNow = {
                    selectedTab = 3
                    currentScreen = "main"
                }
            )
        }

        "checkout" -> {
            BackHandler {
                selectedTab = 3
                currentScreen = "main"
            }
            CheckoutScreen(
                viewModel = viewModel,
                onBack = {
                    selectedTab = 3
                    currentScreen = "main"
                },
                onOpenFedaPayCheckout = { order ->
                    currentScreen = "fedapay_checkout"
                },
                onCashOrderCreated = { order ->
                    currentScreen = "order_confirmation"
                }
            )
        }

        "fedapay_checkout" -> {
            val order: FirestoreOrder? = pendingFedaPayOrder
            BackHandler {
                if (order != null) {
                    viewModel.onFedaPayCheckoutFinished(order.id)
                    currentScreen = "order_confirmation"
                } else {
                    selectedTab = 3
                    currentScreen = "main"
                }
            }
            if (order != null) {
                FedaPayCheckoutScreen(
                    order = order,
                    onBack = {
                        viewModel.onFedaPayCheckoutFinished(order.id)
                        currentScreen = "order_confirmation"
                    },
                    onFinished = { completedOrderId ->
                        viewModel.onFedaPayCheckoutFinished(completedOrderId)
                        currentScreen = "order_confirmation"
                    }
                )
            } else {
                currentScreen = "main"
            }
        }

        "order_confirmation" -> {
            val orderId = activeConfirmationOrderId
            BackHandler {
                selectedTab = 0
                currentScreen = "main"
            }
            if (orderId != null) {
                OrderConfirmationScreen(
                    viewModel = viewModel,
                    orderId = orderId,
                    onBackHome = {
                        selectedTab = 0
                        currentScreen = "main"
                    },
                    onOpenOrders = {
                        currentScreen = "orders"
                    },
                    onOpenReceipt = { order ->
                        viewModel.openReceipt(order)
                        currentScreen = "receipt"
                    },
                    onRetryFedaPay = { order ->
                        viewModel.openFedaPayForExistingOrder(order)
                        currentScreen = "fedapay_checkout"
                    }
                )
            } else {
                currentScreen = "orders"
            }
        }

        "orders" -> {
            BackHandler {
                selectedTab = 4
                currentScreen = "main"
            }
            OrderHistoryScreen(
                viewModel = viewModel,
                onBack = {
                    selectedTab = 4
                    currentScreen = "main"
                },
                onOpenConfirmation = { order ->
                    viewModel.observeConfirmationOrder(order.id)
                    currentScreen = "order_confirmation"
                },
                onOpenReceipt = { order ->
                    viewModel.openReceipt(order)
                    currentScreen = "receipt"
                },
                onPayOrder = { order ->
                    viewModel.openFedaPayForExistingOrder(order)
                    currentScreen = "fedapay_checkout"
                }
            )
        }

        "receipt" -> {
            BackHandler { currentScreen = "orders" }
            OrderReceiptScreen(
                viewModel = viewModel,
                initialOrder = selectedReceiptOrder,
                onBack = { currentScreen = "orders" }
            )
        }

        "chat" -> {
            BackHandler { currentScreen = "main" }
            LiveChatScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "main" }
            )
        }

        "seller_dash" -> {
            BackHandler { currentScreen = "main" }
            SellerDashboardScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "main" }
            )
        }

        "admin" -> {
            BackHandler { currentScreen = "main" }
            AdminDashboardScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "main" }
            )
        }

        else -> {
            val totalCartItems = cartItems.sumOf { it.cartItem.quantity }
            val currentRole = UserRole.fromKey(userProfile?.role)

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nexora Market",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Gold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Gold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "FCFA",
                                        color = Gold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            if (canAccessAdmin) {
                                IconButton(
                                    onClick = { currentScreen = "admin" },
                                    modifier = Modifier.testTag("topbar_admin_button")
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin", tint = Gold)
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = currentRole.labelFr,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(onClick = { currentScreen = "chat" }) {
                                Icon(Icons.Default.SupportAgent, contentDescription = "Support", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = NavyBlue,
                            titleContentColor = Gold
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
                            label = { Text("Accueil", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = "Boutique") },
                            label = { Text("Boutique", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoris") },
                            label = { Text("Favoris", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (totalCartItems > 0) {
                                            Badge(containerColor = Gold, contentColor = NavyBlue) {
                                                Text(totalCartItems.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Panier")
                                }
                            },
                            label = { Text("Panier", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Compte") },
                            label = { Text("Compte", fontSize = 11.sp) }
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
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onProductClick = { prod ->
                                viewModel.trackProductClick(prod, "accueil_vedettes")
                                viewModel.selectProduct(prod)
                                currentScreen = "detail"
                            },
                            onBrowseMore = { selectedTab = 1 }
                        )
                        1 -> ShopScreen(
                            viewModel = viewModel,
                            onProductClick = { prod ->
                                viewModel.trackProductClick(prod, "catalogue_boutique")
                                viewModel.selectProduct(prod)
                                currentScreen = "detail"
                            }
                        )
                        2 -> WishlistScreen(
                            viewModel = viewModel,
                            onProductClick = { prod ->
                                viewModel.trackProductClick(prod, "favoris")
                                viewModel.selectProduct(prod)
                                currentScreen = "detail"
                            },
                            onExploreShop = { selectedTab = 1 }
                        )
                        3 -> CartScreen(
                            viewModel = viewModel,
                            onExploreShop = { selectedTab = 1 },
                            onNavigateToCheckout = { currentScreen = "checkout" },
                            onOpenFedaPayCheckout = { currentScreen = "fedapay_checkout" },
                            onCashOrderCreated = { currentScreen = "order_confirmation" }
                        )
                        4 -> AccountScreen(
                            viewModel = viewModel,
                            onNavigateToOrders = { currentScreen = "orders" },
                            onNavigateToSeller = { currentScreen = "seller_dash" },
                            onNavigateToAdmin = {
                                if (canAccessAdmin) currentScreen = "admin"
                            },
                            onNavigateToChat = { currentScreen = "chat" },
                            onChangeRole = onEditRole,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
