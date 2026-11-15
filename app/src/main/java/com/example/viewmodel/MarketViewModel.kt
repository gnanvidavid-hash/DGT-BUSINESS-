package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analytics.AnalyticsService
import com.example.analytics.NoOpAnalyticsService
import com.example.data.CartItem
import com.example.data.CartItemWithProduct
import com.example.data.Coupon
import com.example.data.NexoraRepository
import com.example.data.Product
import com.example.data.Review
import com.example.data.SupportMessage
import com.example.data.WishlistItem
import com.example.data.firebase.CommissionCalculator
import com.example.data.firebase.FirestoreOrder
import com.example.data.firebase.FirestoreOrderRepository
import com.example.data.firebase.FirestoreSeller
import com.example.data.firebase.FirestoreSellerRepository
import com.example.data.firebase.FirestoreUserRepository
import com.example.data.firebase.OrderLineItem
import com.example.data.firebase.PaymentMethod
import com.example.data.firebase.UserProfile
import com.example.data.firebase.UserRole
import com.example.data.firebase.formatFcfa
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class MarketViewModel(
    private val repository: NexoraRepository,
    private val analyticsService: AnalyticsService = NoOpAnalyticsService,
    private val userRepository: FirestoreUserRepository? = null,
    private val sellerRepository: FirestoreSellerRepository? = null,
    private val orderRepository: FirestoreOrderRepository? = null
) : ViewModel() {

    companion object {
        private const val TAG = "MarketViewModel"
        const val DEFAULT_SHIPPING_FEE_XOF = 3000.0
    }

    // --- Analytics Helpers ---
    fun trackScreenView(screenName: String, screenClass: String = "MainActivity") {
        analyticsService.logScreenView(screenName, screenClass)
    }

    fun trackProductClick(product: Product, sourceList: String) {
        analyticsService.logProductClick(product, sourceList, _currency.value)
    }

    fun logSearch(query: String) {
        analyticsService.logSearch(query)
    }

    // --- App Preferences (Français uniquement & FCFA XOF) ---
    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _language = MutableStateFlow("FR")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _currency = MutableStateFlow("XOF")
    val currency: StateFlow<String> = _currency.asStateFlow()

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
        analyticsService.setUserPreferences(_language.value, _currency.value, _darkTheme.value)
    }

    fun formatMoney(amountXof: Double): String {
        return formatFcfa(amountXof.roundToLong())
    }

    // --- Authenticated User & Role State ---
    private val _authenticatedUid = MutableStateFlow<String?>(null)
    val authenticatedUid: StateFlow<String?> = _authenticatedUid.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isVerifiedAdmin = MutableStateFlow(false)
    val isVerifiedAdmin: StateFlow<Boolean> = _isVerifiedAdmin.asStateFlow()

    private val _profileLoading = MutableStateFlow(false)
    val profileLoading: StateFlow<Boolean> = _profileLoading.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    // --- Marketplace Products & Category State ---
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        products, _selectedCategory, _searchQuery
    ) { allProds, cat, query ->
        allProds.filter { prod ->
            val matchesCategory = cat == null || prod.category == cat
            val matchesSearch = query.isEmpty() ||
                prod.name.lowercase().contains(query.lowercase()) ||
                prod.category.lowercase().contains(query.lowercase()) ||
                prod.description.lowercase().contains(query.lowercase())
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val autocompleteSuggestions: StateFlow<List<String>> = _searchQuery
        .combine(products) { query, allProds ->
            if (query.isBlank()) {
                emptyList()
            } else {
                allProds.map { it.name }
                    .filter { it.lowercase().contains(query.lowercase()) }
                    .take(5)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        analyticsService.logCategorySelected(category ?: "Tout")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Product Details ---
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _productReviews = MutableStateFlow<List<Review>>(emptyList())
    val productReviews: StateFlow<List<Review>> = _productReviews.asStateFlow()

    private var reviewsJob: Job? = null

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
        reviewsJob?.cancel()
        if (product != null) {
            analyticsService.logProductView(product, _currency.value)
            reviewsJob = viewModelScope.launch {
                repository.getReviewsForProduct(product.id).collect { reviews ->
                    _productReviews.value = reviews
                }
            }
        }
    }

    fun addReview(productId: String, rating: Int, comment: String) {
        val reviewerName = _userProfile.value?.fullName?.ifBlank { null }
            ?: checkoutCustomerName.value.ifBlank { "Client Nexora" }
        viewModelScope.launch {
            repository.addReview(productId, reviewerName, rating, comment)
            repository.getReviewsForProduct(productId).collect { reviews ->
                _productReviews.value = reviews
            }
        }
    }

    // --- Shopping Cart & Checkout ---
    private val _cartItems = MutableStateFlow<List<CartItemWithProduct>>(emptyList())
    val cartItems: StateFlow<List<CartItemWithProduct>> = _cartItems.asStateFlow()

    val cartSubtotal: StateFlow<Double> = _cartItems
        .combine(_cartItems) { items, _ ->
            items.sumOf { it.product.price * it.cartItem.quantity }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _couponApplied = MutableStateFlow<Coupon?>(null)
    val couponApplied: StateFlow<Coupon?> = _couponApplied.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    val shippingFee = MutableStateFlow(DEFAULT_SHIPPING_FEE_XOF)

    val cartDiscountXof: StateFlow<Long> = combine(cartSubtotal, _couponApplied) { subtotal, coupon ->
        if (coupon != null) {
            ((subtotal * coupon.discountPercent) / 100.0).roundToLong()
        } else {
            0L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val cartTotal: StateFlow<Double> = combine(
        cartSubtotal, _couponApplied, shippingFee
    ) { subtotal, coupon, ship ->
        var finalTotal = subtotal
        if (coupon != null) {
            val discount = (subtotal * coupon.discountPercent) / 100.0
            finalTotal -= discount
        }
        if (finalTotal > 0) {
            finalTotal += ship
        }
        if (finalTotal < 0) 0.0 else finalTotal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Checkout Customer & Delivery Info ---
    val checkoutCustomerName = MutableStateFlow("Gnanvi David")
    val checkoutCustomerEmail = MutableStateFlow("gnanvidavid@gmail.com")
    val checkoutAddress = MutableStateFlow("Cocody Riviera, Abidjan, Côte d'Ivoire")
    val checkoutPaymentMethod = MutableStateFlow(PaymentMethod.FEDAPAY)
    val checkoutShippingMethod = MutableStateFlow("Livraison Express à Domicile")

    private val _orderPlacedSuccess = MutableStateFlow(false)
    val orderPlacedSuccess: StateFlow<Boolean> = _orderPlacedSuccess.asStateFlow()

    private val _isCreatingOrder = MutableStateFlow(false)
    val isCreatingOrder: StateFlow<Boolean> = _isCreatingOrder.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    private val _pendingFedaPayOrder = MutableStateFlow<FirestoreOrder?>(null)
    val pendingFedaPayOrder: StateFlow<FirestoreOrder?> = _pendingFedaPayOrder.asStateFlow()

    private val _activeConfirmationOrderId = MutableStateFlow<String?>(null)
    val activeConfirmationOrderId: StateFlow<String?> = _activeConfirmationOrderId.asStateFlow()

    private val _activeConfirmationOrder = MutableStateFlow<FirestoreOrder?>(null)
    val activeConfirmationOrder: StateFlow<FirestoreOrder?> = _activeConfirmationOrder.asStateFlow()

    private val _selectedReceiptOrder = MutableStateFlow<FirestoreOrder?>(null)
    val selectedReceiptOrder: StateFlow<FirestoreOrder?> = _selectedReceiptOrder.asStateFlow()

    // --- Firestore Real-time Orders & Sellers (Auth-gated) ---
    private val _userOrders = MutableStateFlow<List<FirestoreOrder>>(emptyList())
    val userOrders: StateFlow<List<FirestoreOrder>> = _userOrders.asStateFlow()

    private val _adminOrders = MutableStateFlow<List<FirestoreOrder>>(emptyList())
    val adminOrders: StateFlow<List<FirestoreOrder>> = _adminOrders.asStateFlow()

    private val _mySellers = MutableStateFlow<List<FirestoreSeller>>(emptyList())
    val mySellers: StateFlow<List<FirestoreSeller>> = _mySellers.asStateFlow()

    private val _adminSellers = MutableStateFlow<List<FirestoreSeller>>(emptyList())
    val adminSellers: StateFlow<List<FirestoreSeller>> = _adminSellers.asStateFlow()

    private var userProfileJob: Job? = null
    private var userOrdersJob: Job? = null
    private var mySellersJob: Job? = null
    private var adminDataJob: Job? = null
    private var confirmationOrderJob: Job? = null

    init {
        analyticsService.setUserPreferences(_language.value, _currency.value, _darkTheme.value)
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
        viewModelScope.launch {
            repository.allCartItems.collect { _ ->
                _cartItems.value = repository.getCartItemsWithProducts()
            }
        }
    }

    fun attachAuthenticatedUser(uid: String, displayName: String?, email: String?) {
        if (_authenticatedUid.value == uid) return
        _authenticatedUid.value = uid
        _authMessage.value = null
        if (!displayName.isNullOrBlank()) {
            checkoutCustomerName.value = displayName
        }
        if (!email.isNullOrBlank()) {
            checkoutCustomerEmail.value = email
        }

        userProfileJob?.cancel()
        userOrdersJob?.cancel()
        mySellersJob?.cancel()
        adminDataJob?.cancel()

        val uRepo = userRepository ?: return
        _profileLoading.value = true

        viewModelScope.launch {
            val isAdmin = uRepo.isAdminAccount()
            _isVerifiedAdmin.value = isAdmin
            if (isAdmin) {
                sellerRepository?.seedDemoSellersIfAdmin()
            }
        }

        userProfileJob = viewModelScope.launch {
            uRepo.observeUserProfile(uid)
                .catch { error ->
                    val fsErr = unwrapFirestore(error)
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Erreur profil utilisateur", fsErr ?: error)
                    _profileLoading.value = false
                }
                .collect { profile ->
                    _userProfile.value = profile
                    _profileLoading.value = false
                    if (profile != null) {
                        checkoutCustomerName.value = profile.fullName.ifBlank { checkoutCustomerName.value }
                        checkoutCustomerEmail.value = profile.email.ifBlank { checkoutCustomerEmail.value }
                        if (profile.role == UserRole.ADMIN.key) {
                            startAdminListeners()
                        } else {
                            adminDataJob?.cancel()
                            _adminSellers.value = emptyList()
                            _adminOrders.value = emptyList()
                        }
                    }
                }
        }

        orderRepository?.let { oRepo ->
            userOrdersJob = viewModelScope.launch {
                oRepo.observeUserOrders(uid)
                    .catch { error ->
                        val fsErr = unwrapFirestore(error)
                        if (fsErr == null && error is CancellationException) throw error
                        Log.w(TAG, "Erreur écoute commandes utilisateur", fsErr ?: error)
                    }
                    .collect { ordersList ->
                        _userOrders.value = ordersList
                    }
            }
        }

        sellerRepository?.let { sRepo ->
            mySellersJob = viewModelScope.launch {
                sRepo.observeMySellers(uid)
                    .catch { error ->
                        val fsErr = unwrapFirestore(error)
                        if (fsErr == null && error is CancellationException) throw error
                        Log.w(TAG, "Erreur écoute boutiques vendeur", fsErr ?: error)
                    }
                    .collect { sellersList ->
                        _mySellers.value = sellersList
                    }
            }
        }
    }

    private fun startAdminListeners() {
        adminDataJob?.cancel()
        adminDataJob = viewModelScope.launch {
            val isAdmin = userRepository?.isAdminAccount() == true
            _isVerifiedAdmin.value = isAdmin
            if (!isAdmin) return@launch

            launch {
                sellerRepository?.observeAllSellersForAdmin()
                    ?.catch { error ->
                        val fsErr = unwrapFirestore(error)
                        if (fsErr == null && error is CancellationException) throw error
                        Log.w(TAG, "Erreur écoute vendeurs admin", fsErr ?: error)
                    }
                    ?.collect { list -> _adminSellers.value = list }
            }
            launch {
                orderRepository?.observeAllOrdersForAdmin()
                    ?.catch { error ->
                        val fsErr = unwrapFirestore(error)
                        if (fsErr == null && error is CancellationException) throw error
                        Log.w(TAG, "Erreur écoute commandes admin", fsErr ?: error)
                    }
                    ?.collect { list -> _adminOrders.value = list }
            }
        }
    }

    fun clearAuthenticatedUser() {
        _authenticatedUid.value = null
        _userProfile.value = null
        _isVerifiedAdmin.value = false
        _userOrders.value = emptyList()
        _mySellers.value = emptyList()
        _adminSellers.value = emptyList()
        _adminOrders.value = emptyList()
        userProfileJob?.cancel()
        userOrdersJob?.cancel()
        mySellersJob?.cancel()
        adminDataJob?.cancel()
        confirmationOrderJob?.cancel()
    }

    fun saveUserProfile(fullName: String, email: String, role: UserRole, onSuccess: () -> Unit = {}) {
        val uRepo = userRepository ?: return
        viewModelScope.launch {
            _profileLoading.value = true
            _authMessage.value = null
            try {
                if (role == UserRole.ADMIN && !uRepo.isAdminAccount()) {
                    _authMessage.value = "Le rôle Administrateur est réservé au compte autorisé. Choisissez Client ou Vendeur."
                    _profileLoading.value = false
                    return@launch
                }
                val saved = uRepo.upsertUserProfile(fullName, email, role)
                _userProfile.value = saved
                checkoutCustomerName.value = saved.fullName
                checkoutCustomerEmail.value = saved.email
                if (role == UserRole.ADMIN) {
                    startAdminListeners()
                }
                onSuccess()
            } catch (e: Exception) {
                _authMessage.value = "Impossible d'enregistrer le profil : ${e.localizedMessage ?: "erreur Firestore"}"
            } finally {
                _profileLoading.value = false
            }
        }
    }

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            _couponError.value = null
            val coupon = repository.getCoupon(code)
            if (coupon != null) {
                val subtotal = cartSubtotal.value
                if (subtotal >= coupon.minSpend) {
                    _couponApplied.value = coupon
                    analyticsService.logApplyCoupon(code, success = true)
                } else {
                    _couponError.value = "Montant minimum requis : ${formatMoney(coupon.minSpend)}"
                    analyticsService.logApplyCoupon(code, success = false)
                }
            } else {
                _couponError.value = "Code promo invalide"
                analyticsService.logApplyCoupon(code, success = false)
            }
        }
    }

    fun removeCoupon() {
        _couponApplied.value = null
        _couponError.value = null
    }

    fun addToCart(product: Product, quantity: Int, color: String?, size: String?) {
        val variant = listOfNotNull(color, size).takeIf { it.isNotEmpty() }?.joinToString(" / ")
        analyticsService.logAddToCart(product, quantity, _currency.value, variant)
        viewModelScope.launch {
            repository.addToCart(product.id, quantity, color, size)
            _cartItems.value = repository.getCartItemsWithProducts()
        }
    }

    fun updateCartQuantity(cartItemId: Int, quantity: Int) {
        val target = _cartItems.value.find { it.cartItem.id == cartItemId }
        if (target != null && quantity <= 0) {
            analyticsService.logRemoveFromCart(target.product, target.cartItem.quantity, _currency.value)
        }
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, quantity)
            _cartItems.value = repository.getCartItemsWithProducts()
        }
    }

    fun removeFromCart(cartItemId: Int) {
        val target = _cartItems.value.find { it.cartItem.id == cartItemId }
        if (target != null) {
            analyticsService.logRemoveFromCart(target.product, target.cartItem.quantity, _currency.value)
        }
        viewModelScope.launch {
            repository.removeFromCart(cartItemId)
            _cartItems.value = repository.getCartItemsWithProducts()
        }
    }

    fun beginCheckout(
        buyNowProduct: Product? = null,
        quantity: Int = 1,
        color: String? = null,
        size: String? = null
    ) {
        val checkoutItems = if (buyNowProduct != null) {
            val buyNowEntry = CartItemWithProduct(
                cartItem = CartItem(
                    productId = buyNowProduct.id,
                    quantity = quantity,
                    selectedColor = color,
                    selectedSize = size
                ),
                product = buyNowProduct
            )
            _cartItems.value + buyNowEntry
        } else {
            _cartItems.value
        }
        val estimatedTotal = checkoutItems.sumOf { it.product.price * it.cartItem.quantity } +
            if (checkoutItems.isNotEmpty()) shippingFee.value else 0.0
        analyticsService.logBeginCheckout(
            items = checkoutItems,
            totalValueUsd = estimatedTotal,
            currency = _currency.value,
            couponCode = _couponApplied.value?.code
        )
    }

    /**
     * Crée la commande avec le statut "En attente de paiement" AVANT d'ouvrir Feda Checkout.js.
     * Ou crée la commande avec le statut "En attente" pour "Paiement à la livraison".
     */
    fun createOrderForPayment(
        paymentMethod: String,
        onFedaPayReady: (FirestoreOrder) -> Unit = {},
        onCashOrderCreated: (FirestoreOrder) -> Unit = {}
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _checkoutError.value = "Votre panier est vide."
            return
        }
        val customerName = checkoutCustomerName.value.trim()
        val customerEmail = checkoutCustomerEmail.value.trim()
        val address = checkoutAddress.value.trim()

        if (customerName.isEmpty() || customerEmail.isEmpty() || !customerEmail.contains("@")) {
            _checkoutError.value = "Veuillez saisir votre nom complet et une adresse e-mail valide pour FedaPay."
            return
        }
        if (address.isEmpty()) {
            _checkoutError.value = "Veuillez renseigner une adresse de livraison."
            return
        }

        val subtotalXof = cartSubtotal.value.roundToLong()
        val discountXof = cartDiscountXof.value
        val shippingXof = shippingFee.value.roundToLong()
        val totalXof = cartTotal.value.roundToLong().coerceAtLeast(100L)
        val appliedCouponCode = _couponApplied.value?.code
        val primarySellerName = items.firstOrNull()?.product?.sellerName ?: "Nexora Officiel"

        viewModelScope.launch {
            _isCreatingOrder.value = true
            _checkoutError.value = null
            try {
                val orderRepo = orderRepository
                val createdOrder = if (orderRepo != null) {
                    val lineItems = items.map { entry ->
                        val variant = listOfNotNull(entry.cartItem.selectedColor, entry.cartItem.selectedSize)
                            .joinToString(" / ")
                        OrderLineItem(
                            productId = entry.product.id,
                            name = entry.product.name,
                            quantity = entry.cartItem.quantity,
                            unitPriceXof = entry.product.price.roundToLong(),
                            variant = variant,
                            sellerName = entry.product.sellerName
                        )
                    }
                    orderRepo.createOrder(
                        customerName = customerName,
                        customerEmail = customerEmail,
                        sellerName = primarySellerName,
                        items = lineItems,
                        subtotalXof = subtotalXof,
                        discountXof = discountXof,
                        shippingFeeXof = shippingXof,
                        totalAmountXof = totalXof,
                        paymentMethod = paymentMethod,
                        shippingAddress = address,
                        shippingMethod = checkoutShippingMethod.value
                    )
                } else {
                    // Fallback pour tests unitaires sans Firestore injecté
                    val localOrder = repository.placeOrder(
                        totalAmount = totalXof.toDouble(),
                        paymentMethod = paymentMethod,
                        address = address,
                        items = items
                    )
                    val split = CommissionCalculator.calculate(totalXof)
                    FirestoreOrder(
                        id = localOrder.id,
                        customerName = customerName,
                        customerEmail = customerEmail,
                        sellerName = primarySellerName,
                        itemsSummary = localOrder.itemsJson,
                        subtotalXof = subtotalXof,
                        discountXof = discountXof,
                        shippingFeeXof = shippingXof,
                        totalAmountXof = totalXof,
                        platformCommissionXof = split.platformCommissionXof,
                        sellerShareXof = split.sellerShareXof,
                        paymentMethod = paymentMethod,
                        status = localOrder.status,
                        shippingAddress = address,
                        shippingMethod = checkoutShippingMethod.value
                    )
                }

                analyticsService.logCheckoutCompleted(
                    transactionId = createdOrder.id,
                    items = items,
                    totalValueUsd = totalXof.toDouble(),
                    shippingFeeUsd = shippingXof.toDouble(),
                    currency = _currency.value,
                    paymentMethod = paymentMethod,
                    shippingMethod = checkoutShippingMethod.value,
                    couponCode = appliedCouponCode
                )

                repository.clearCart()
                _cartItems.value = emptyList()
                _couponApplied.value = null

                if (paymentMethod == PaymentMethod.FEDAPAY) {
                    _pendingFedaPayOrder.value = createdOrder
                    onFedaPayReady(createdOrder)
                } else {
                    observeConfirmationOrder(createdOrder.id)
                    _orderPlacedSuccess.value = true
                    onCashOrderCreated(createdOrder)
                }
            } catch (e: Exception) {
                _checkoutError.value = "Erreur lors de la création de la commande : ${e.localizedMessage ?: "réessayez"}"
            } finally {
                _isCreatingOrder.value = false
            }
        }
    }

    // Gardé pour compatibilité des tests existants (MarketAnalyticsTest)
    fun placeOrder() {
        createOrderForPayment(paymentMethod = checkoutPaymentMethod.value)
    }

    fun clearCheckoutError() {
        _checkoutError.value = null
    }

    fun onFedaPayCheckoutFinished(orderId: String) {
        _pendingFedaPayOrder.value = null
        observeConfirmationOrder(orderId)
    }

    fun openFedaPayForExistingOrder(order: FirestoreOrder) {
        _pendingFedaPayOrder.value = order
    }

    fun observeConfirmationOrder(orderId: String) {
        _activeConfirmationOrderId.value = orderId
        confirmationOrderJob?.cancel()
        val oRepo = orderRepository ?: return
        confirmationOrderJob = viewModelScope.launch {
            oRepo.observeOrder(orderId)
                .catch { error ->
                    val fsErr = unwrapFirestore(error)
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Erreur observation commande temps réel", fsErr ?: error)
                }
                .collect { order ->
                    if (order != null) {
                        _activeConfirmationOrder.value = order
                    }
                }
        }
    }

    fun openReceipt(order: FirestoreOrder) {
        _selectedReceiptOrder.value = order
        observeConfirmationOrder(order.id)
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository?.cancelPendingOrder(orderId)
            } catch (e: Exception) {
                Log.w(TAG, "Impossible d'annuler la commande $orderId", e)
            }
        }
    }

    fun resetOrderSuccess() {
        _orderPlacedSuccess.value = false
    }

    // --- Wishlist ---
    val wishlistItems: StateFlow<List<WishlistItem>> = repository.allWishlistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            val added = repository.toggleWishlist(product.id)
            if (added) {
                analyticsService.logAddToWishlist(product, _currency.value)
            }
        }
    }

    // --- AI Chatbot Support ---
    val chatMessages: StateFlow<List<SupportMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun setChatInput(text: String) {
        _chatInput.value = text
    }

    fun sendChatMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty()) return
        _chatInput.value = ""
        analyticsService.logSupportChatMessageSent()

        viewModelScope.launch {
            repository.addChatMessage("user", text)
            _isChatLoading.value = true
            val history = chatMessages.value
            val response = repository.sendChatToAI(text, history)
            repository.addChatMessage("ai", response)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
            repository.addChatMessage(
                "ai",
                "Bienvenue sur le support Nexora Market ! 🌟 Comment puis-je vous aider aujourd'hui ?"
            )
        }
    }

    // --- Seller Hub Portal ---
    fun registerAsSeller(businessName: String, ownerName: String, category: String, email: String) {
        viewModelScope.launch {
            sellerRepository?.createSellerStore(
                businessName = businessName,
                sellerName = ownerName,
                category = category,
                email = email
            )
            repository.registerSeller(businessName, ownerName, category, email)
        }
    }

    val sellerNewName = MutableStateFlow("")
    val sellerNewPrice = MutableStateFlow("")
    val sellerNewCategory = MutableStateFlow("Électronique")
    val sellerNewDesc = MutableStateFlow("")

    fun addNewProductFromSeller(seller: FirestoreSeller) {
        val name = sellerNewName.value.trim()
        val priceStr = sellerNewPrice.value.trim()
        val cat = sellerNewCategory.value
        val desc = sellerNewDesc.value.trim()

        if (name.isEmpty() || priceStr.isEmpty()) return
        val price = priceStr.toDoubleOrNull() ?: return

        viewModelScope.launch {
            val newProduct = Product(
                id = "p-sel-${(1000..9999).random()}",
                name = name,
                category = cat,
                price = price,
                rating = 5.0f,
                reviewCount = 0,
                stock = 15,
                description = desc.ifEmpty { "Produit de qualité fourni par ${seller.businessName}." },
                sellerName = seller.businessName,
                imageResName = "img_phone"
            )
            repository.addProduct(newProduct)
            sellerNewName.value = ""
            sellerNewPrice.value = ""
            sellerNewDesc.value = ""
        }
    }

    // --- Admin Actions ---
    fun approveSellerByAdmin(sellerId: String) {
        viewModelScope.launch {
            try {
                sellerRepository?.approveSeller(sellerId)
            } catch (e: Exception) {
                Log.w(TAG, "Erreur approbation vendeur", e)
            }
        }
    }

    fun suspendSellerByAdmin(sellerId: String) {
        viewModelScope.launch {
            try {
                sellerRepository?.suspendSeller(sellerId)
            } catch (e: Exception) {
                Log.w(TAG, "Erreur suspension vendeur", e)
            }
        }
    }

    fun deleteProductByAdmin(productId: String) {
        viewModelScope.launch {
            repository.removeProduct(productId)
        }
    }

    fun updateOrderStatusByAdmin(orderId: String, newStatus: String, progress: Double) {
        viewModelScope.launch {
            try {
                orderRepository?.updateFulfillmentStatusByAdmin(orderId, newStatus, progress)
            } catch (e: Exception) {
                Log.w(TAG, "Erreur mise à jour statut commande par admin", e)
            }
        }
    }

    private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }
}

class MarketViewModelFactory(
    private val repository: NexoraRepository,
    private val analyticsService: AnalyticsService = NoOpAnalyticsService,
    private val userRepository: FirestoreUserRepository? = null,
    private val sellerRepository: FirestoreSellerRepository? = null,
    private val orderRepository: FirestoreOrderRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketViewModel(
                repository = repository,
                analyticsService = analyticsService,
                userRepository = userRepository,
                sellerRepository = sellerRepository,
                orderRepository = orderRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
