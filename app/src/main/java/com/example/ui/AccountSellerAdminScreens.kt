package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.FirestoreOrder
import com.example.data.firebase.FirestoreSeller
import com.example.data.firebase.OrderStatus
import com.example.data.firebase.UserRole
import com.example.data.firebase.formatFcfa
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.example.viewmodel.MarketViewModel

@Composable
fun AccountScreen(
    viewModel: MarketViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToSeller: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToChat: () -> Unit,
    onChangeRole: () -> Unit,
    onSignOut: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isVerifiedAdmin by viewModel.isVerifiedAdmin.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()

    val currentRole = UserRole.fromKey(userProfile?.role)
    val canSeeAdmin = currentRole == UserRole.ADMIN && isVerifiedAdmin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Carte Profil
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyBlue)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp).background(Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = (userProfile?.fullName ?: "NM")
                        .split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifBlank { "NM" }
                    Text(initials, color = NavyBlue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userProfile?.fullName?.ifBlank { "Client Nexora" } ?: "Client Nexora",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = userProfile?.email ?: "",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = Gold.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "Rôle : ${currentRole.labelFr}",
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Fidélité
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.14f)),
            border = BorderStroke(1.dp, Gold)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = NavyBlue, modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Programme Fidélité Nexora", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyBlue)
                    Text("2 450 points accumulés • Parrainage : NEXORA-VIP26", fontSize = 12.sp, color = NavyBlue.copy(alpha = 0.8f))
                }
            }
        }

        Text("Services & Espace personnel", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        AccountMenuItem(
            icon = Icons.Default.ReceiptLong,
            title = "Historique & Suivi des commandes",
            subtitle = "Statuts temps réel, paiements FedaPay et reçus FCFA",
            testTag = "orders_menu_button",
            onClick = onNavigateToOrders
        )

        AccountMenuItem(
            icon = Icons.Default.Storefront,
            title = "Portail Vendeur",
            subtitle = "Gérer votre boutique et ajouter des articles en FCFA",
            testTag = "seller_menu_button",
            onClick = onNavigateToSeller
        )

        // Le Tableau de Bord Admin n'apparaît STRICTEMENT que pour un admin vérifié
        if (canSeeAdmin) {
            AccountMenuItem(
                icon = Icons.Default.AdminPanelSettings,
                title = "Tableau de Bord Admin",
                subtitle = "Approuver ou suspendre les vendeurs, gérer les commandes",
                testTag = "admin_menu_button",
                onClick = onNavigateToAdmin
            )
        }

        AccountMenuItem(
            icon = Icons.Default.SupportAgent,
            title = "Assistant Support Nexora",
            subtitle = "Aide en direct 24h/24 sur vos commandes et paiements",
            testTag = "chat_menu_button",
            onClick = onNavigateToChat
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = NavyBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Mode sombre", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Switch(checked = darkTheme, onCheckedChange = { viewModel.toggleTheme() })
            }
        }

        OutlinedButton(
            onClick = onChangeRole,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("change_role_button"),
            border = BorderStroke(1.5.dp, NavyBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = NavyBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Changer de rôle (Client / Vendeur / Admin)", color = NavyBlue, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
            border = BorderStroke(1.5.dp, AccentRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = AccentRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se déconnecter", color = AccentRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(NavyBlue.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = NavyBlue)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NavyBlue)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val input by viewModel.chatInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Client Nexora", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clearChat() }) {
                        Text("Effacer", color = AccentRed)
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
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 290.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) NavyBlue else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Text("L'assistant Nexora rédige sa réponse...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { viewModel.setChatInput(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Posez votre question en français...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold)
                )
                IconButton(
                    onClick = { viewModel.sendChatMessage() },
                    modifier = Modifier.size(48.dp).background(Gold, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = NavyBlue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val mySellers by viewModel.mySellers.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    val newProdName by viewModel.sellerNewName.collectAsStateWithLifecycle()
    val newProdPrice by viewModel.sellerNewPrice.collectAsStateWithLifecycle()
    val newProdCategory by viewModel.sellerNewCategory.collectAsStateWithLifecycle()
    val newProdDesc by viewModel.sellerNewDesc.collectAsStateWithLifecycle()

    var businessName by remember { mutableStateOf("") }
    var ownerName by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "") }
    var category by remember { mutableStateOf("Électronique") }
    var sellerEmail by remember(userProfile) { mutableStateOf(userProfile?.email ?: "") }

    val activeSeller = mySellers.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portail Vendeur Nexora", fontWeight = FontWeight.Bold) },
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
            if (activeSeller == null) {
                Text("Créer votre boutique vendeur", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Ouvrez votre espace marchand sur Nexora Market et vendez vos produits en FCFA partout en Afrique.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Nom de la boutique") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Nom du gérant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie principale") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sellerEmail,
                    onValueChange = { sellerEmail = it },
                    label = { Text("E-mail professionnel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (businessName.isNotBlank() && ownerName.isNotBlank() && sellerEmail.isNotBlank()) {
                            viewModel.registerAsSeller(businessName.trim(), ownerName.trim(), category.trim(), sellerEmail.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue, contentColor = Gold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Enregistrer ma boutique", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyBlue)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(activeSeller.businessName, color = Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Gérant : ${activeSeller.sellerName} • ${activeSeller.category}", color = Color.White, fontSize = 14.sp)
                        Text("Statut : ${activeSeller.status}", color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Solde net (90%) : ${formatFcfa(activeSeller.balanceXof)}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Publier un nouveau produit (FCFA)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        OutlinedTextField(
                            value = newProdName,
                            onValueChange = { viewModel.sellerNewName.value = it },
                            label = { Text("Nom du produit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newProdPrice,
                            onValueChange = { viewModel.sellerNewPrice.value = it },
                            label = { Text("Prix en FCFA (ex: 45000)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newProdCategory,
                            onValueChange = { viewModel.sellerNewCategory.value = it },
                            label = { Text("Catégorie") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newProdDesc,
                            onValueChange = { viewModel.sellerNewDesc.value = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.addNewProductFromSeller(activeSeller) },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Publier l'article", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val myProducts = products.filter { it.sellerName == activeSeller.businessName }
                Text("Mes articles en ligne (${myProducts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                myProducts.forEach { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${prod.category} • Stock : ${prod.stock}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(viewModel.formatMoney(prod.price), fontWeight = FontWeight.ExtraBold, color = NavyBlue)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MarketViewModel,
    onBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isVerifiedAdmin by viewModel.isVerifiedAdmin.collectAsStateWithLifecycle()
    val sellers by viewModel.adminSellers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val orders by viewModel.adminOrders.collectAsStateWithLifecycle()

    val canAccess = UserRole.fromKey(userProfile?.role) == UserRole.ADMIN && isVerifiedAdmin

    var selectedTab by remember { mutableIntStateOf(0) }
    var sellerDetailTarget by remember { mutableStateOf<FirestoreSeller?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableau de Bord Admin", fontWeight = FontWeight.Bold) },
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
        if (!canAccess) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, AccentRed)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Accès réservé à l'Administrateur", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentRed)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = NavyBlue)) {
                            Text("Retour", color = Color.White)
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Vendeurs (${sellers.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Produits (${products.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Commandes (${orders.size})", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                // Gestion Vendeurs avec textes plus grands & boutons Approuver / Suspendre / Voir les détails
                0 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(sellers, key = { it.id }) { seller ->
                        AdminSellerCard(
                            seller = seller,
                            onApprove = { viewModel.approveSellerByAdmin(seller.id) },
                            onSuspend = { viewModel.suspendSellerByAdmin(seller.id) },
                            onViewDetails = { sellerDetailTarget = seller }
                        )
                    }
                }

                // Gestion Produits
                1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { prod ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${prod.sellerName} • ${viewModel.formatMoney(prod.price)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteProductByAdmin(prod.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer le produit", tint = AccentRed)
                                }
                            }
                        }
                    }
                }

                // Gestion Commandes (l'admin gère l'expédition/livraison, jamais la validation manuelle de paiement FedaPay)
                2 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        AdminOrderCard(
                            order = order,
                            onShip = { viewModel.updateOrderStatusByAdmin(order.id, OrderStatus.SHIPPED, 0.70) },
                            onDeliver = { viewModel.updateOrderStatusByAdmin(order.id, OrderStatus.DELIVERED, 1.0) },
                            onCancel = { viewModel.updateOrderStatusByAdmin(order.id, OrderStatus.CANCELED, 0.0) }
                        )
                    }
                }
            }
        }
    }

    // Dialogue Détails Vendeur
    sellerDetailTarget?.let { seller ->
        AlertDialog(
            onDismissRequest = { sellerDetailTarget = null },
            title = {
                Text(
                    text = seller.businessName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Responsable : ${seller.sellerName}", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Catégorie : ${seller.category}", fontSize = 15.sp)
                    Text("E-mail : ${seller.email}", fontSize = 15.sp)
                    Text("Statut actuel : ${seller.status}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Compte vérifié : ${if (seller.isVerified) "Oui" else "Non"}", fontSize = 15.sp)
                    Text("Solde généré : ${formatFcfa(seller.balanceXof)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = NavyBlue)
                }
            },
            confirmButton = {
                Button(
                    onClick = { sellerDetailTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue, contentColor = Gold)
                ) {
                    Text("Fermer", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun AdminSellerCard(
    seller: FirestoreSeller,
    onApprove: () -> Unit,
    onSuspend: () -> Unit,
    onViewDetails: () -> Unit
) {
    val statusColor = when (seller.status) {
        "Approuvé" -> AccentGreen
        "Suspendu" -> AccentRed
        else -> Color(0xFFD97706)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("admin_seller_card_${seller.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = seller.businessName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyBlue,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = seller.status,
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = "Gérant : ${seller.sellerName} • ${seller.category}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "E-mail : ${seller.email}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Solde vendeur : ${formatFcfa(seller.balanceXof)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NavyBlue
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("approve_seller_${seller.id}")
                ) {
                    Text("Approuver", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSuspend,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("suspend_seller_${seller.id}")
                ) {
                    Text("Suspendre", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onViewDetails,
                border = BorderStroke(1.5.dp, NavyBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("details_seller_${seller.id}")
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = NavyBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voir les détails", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyBlue)
            }
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: FirestoreOrder,
    onShip: () -> Unit,
    onDeliver: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("#${order.id} • ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                OrderStatusBadge(order.status)
            }
            Text("Total : ${formatFcfa(order.totalAmountXof)} (${order.paymentMethod})", fontWeight = FontWeight.Bold, color = NavyBlue, fontSize = 14.sp)
            Text(
                text = "Commission Nexora : ${formatFcfa(order.platformCommissionXof)} | Part vendeur : ${formatFcfa(order.sellerShareXof)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShip, modifier = Modifier.weight(1f)) {
                    Text("Expédier", fontSize = 12.sp)
                }
                Button(
                    onClick = onDeliver,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Livrée", fontSize = 12.sp)
                }
                TextButton(onClick = onCancel) {
                    Text("Annuler", color = AccentRed, fontSize = 12.sp)
                }
            }
        }
    }
}
