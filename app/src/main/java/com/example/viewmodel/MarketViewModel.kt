package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CartItemWithProduct
import com.example.data.Coupon
import com.example.data.NexoraRepository
import com.example.data.OrderItem
import com.example.data.Product
import com.example.data.Review
import com.example.data.Seller
import com.example.data.SupportMessage
import com.example.data.WishlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarketViewModel(private val repository: NexoraRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    // --- App Preferences ---
    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _language = MutableStateFlow("EN") // "EN" or "FR"
    val language: StateFlow<String> = _language.asStateFlow()

    private val _currency = MutableStateFlow("USD") // "USD" or "XOF"
    val currency: StateFlow<String> = _currency.asStateFlow()

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun setCurrency(curr: String) {
        _currency.value = curr
    }

    // Helper for multi-currency formatting
    fun formatMoney(usdAmount: Double): String {
        return if (_currency.value == "XOF") {
            val xofAmount = (usdAmount * 600).toInt()
            "$xofAmount FCFA"
        } else {
            String.format("$%.2f", usdAmount)
        }
    }

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
            if (query.isEmpty()) {
                emptyList()
            } else {
                allProds.map { it.name }
                    .filter { it.lowercase().contains(query.lowercase()) }
                    .take(5)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Product Details ---
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _productReviews = MutableStateFlow<List<Review>>(emptyList())
    val productReviews: StateFlow<List<Review>> = _productReviews.asStateFlow()

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
        if (product != null) {
            viewModelScope.launch {
                repository.getReviewsForProduct(product.id).collect { reviews ->
                    _productReviews.value = reviews
                }
            }
        }
    }

    fun addReview(productId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.addReview(productId, "Nexora Shopper", rating, comment)
            // Reload reviews
            repository.getReviewsForProduct(productId).collect { reviews ->
                _productReviews.value = reviews
            }
        }
    }

    // --- Shopping Cart & Checkout ---
    private val _cartItems = MutableStateFlow<List<CartItemWithProduct>>(emptyList())
    val cartItems: StateFlow<List<CartItemWithProduct>> = _cartItems.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allCartItems.collect { _ ->
                _cartItems.value = repository.getCartItemsWithProducts()
            }
        }
    }

    val cartSubtotal: StateFlow<Double> = _cartItems
        .combine(_cartItems) { items, _ ->
            items.sumOf { it.product.price * it.cartItem.quantity }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _couponApplied = MutableStateFlow<Coupon?>(null)
    val couponApplied: StateFlow<Coupon?> = _couponApplied.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            _couponError.value = null
            val coupon = repository.getCoupon(code)
            if (coupon != null) {
                val subtotal = cartSubtotal.value
                if (subtotal >= coupon.minSpend) {
                    _couponApplied.value = coupon
                } else {
                    _couponError.value = "Min spend for this coupon is $${coupon.minSpend}"
                }
            } else {
                _couponError.value = "Invalid coupon code"
            }
        }
    }

    fun removeCoupon() {
        _couponApplied.value = null
        _couponError.value = null
    }

    val shippingFee = MutableStateFlow(5.0) // Flat $5 delivery

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

    fun addToCart(product: Product, quantity: Int, color: String?, size: String?) {
        viewModelScope.launch {
            repository.addToCart(product.id, quantity, color, size)
            _cartItems.value = repository.getCartItemsWithProducts()
        }
    }

    fun updateCartQuantity(cartItemId: Int, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, quantity)
            _cartItems.value = repository.getCartItemsWithProducts()
        }
    }

    fun removeFromCart(cartItemId: Int) {
        viewModelScope.launch {
            repository.removeFromCart(cartItemId)
            _cartItems.value = repository.getCartItemsWithProducts()
        }
    }

    // --- Checkout & Address Preferences ---
    val checkoutAddress = MutableStateFlow("124 Independence Avenue, Accra, Ghana")
    val checkoutPaymentMethod = MutableStateFlow("MTN Mobile Money")
    val checkoutShippingMethod = MutableStateFlow("Express Home Delivery")

    private val _orderPlacedSuccess = MutableStateFlow(false)
    val orderPlacedSuccess: StateFlow<Boolean> = _orderPlacedSuccess.asStateFlow()

    fun placeOrder() {
        viewModelScope.launch {
            val items = _cartItems.value
            val total = cartTotal.value
            if (items.isNotEmpty()) {
                repository.placeOrder(
                    totalAmount = total,
                    paymentMethod = checkoutPaymentMethod.value,
                    address = checkoutAddress.value,
                    items = items
                )
                _couponApplied.value = null
                _orderPlacedSuccess.value = true
                _cartItems.value = emptyList()
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
            repository.toggleWishlist(product.id)
        }
    }

    // --- Order History & Tracking ---
    val orders: StateFlow<List<OrderItem>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _trackedOrder = MutableStateFlow<OrderItem?>(null)
    val trackedOrder: StateFlow<OrderItem?> = _trackedOrder.asStateFlow()

    fun selectTrackedOrder(orderId: String) {
        viewModelScope.launch {
            orders.value.find { it.id == orderId }?.let {
                _trackedOrder.value = it
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

        viewModelScope.launch {
            // 1. Add user message
            repository.addChatMessage("user", text)
            _isChatLoading.value = true

            // 2. Query Gemini
            val history = chatMessages.value
            val response = repository.sendChatToAI(text, history)

            // 3. Add AI response
            repository.addChatMessage("ai", response)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
            // Re-add default greeting
            repository.addChatMessage("ai", "Welcome to Nexora Market support! 🌟 I am Nexora, your AI virtual shopping assistant. How can I assist you today?")
        }
    }

    // --- Seller Hub Portal ---
    val registeredSellers: StateFlow<List<Seller>> = repository.sellers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSeller = MutableStateFlow<Seller?>(null)
    val currentSeller: StateFlow<Seller?> = _currentSeller.asStateFlow()

    fun registerAsSeller(businessName: String, ownerName: String, category: String, email: String) {
        viewModelScope.launch {
            repository.registerSeller(businessName, ownerName, category, email)
            // Retrieve first registered seller to log in
            val currentList = repository.sellers.first()
            if (currentList.isNotEmpty()) {
                _currentSeller.value = currentList.first()
            }
        }
    }

    fun logoutSeller() {
        _currentSeller.value = null
    }

    // Seller Product addition inputs
    val sellerNewName = MutableStateFlow("")
    val sellerNewPrice = MutableStateFlow("")
    val sellerNewCategory = MutableStateFlow("Electronics")
    val sellerNewDesc = MutableStateFlow("")

    fun addNewProductFromSeller() {
        val name = sellerNewName.value.trim()
        val priceStr = sellerNewPrice.value.trim()
        val cat = sellerNewCategory.value
        val desc = sellerNewDesc.value.trim()
        val seller = _currentSeller.value ?: return

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
                description = desc.ifEmpty { "Premium product supplied by ${seller.businessName}." },
                sellerName = seller.businessName,
                imageResName = "img_phone" // default visual placeholder
            )
            repository.addProduct(newProduct)
            
            // Increment seller balance (simulated dynamic stock supply sales credit!)
            repository.updateSellerBalance(seller.id, 50.0) // credit vendor for stocking product
            // Refresh current seller
            val updatedSellers = repository.sellers.first()
            _currentSeller.value = updatedSellers.find { it.id == seller.id }

            // Reset fields
            sellerNewName.value = ""
            sellerNewPrice.value = ""
            sellerNewDesc.value = ""
        }
    }

    // --- Admin Dashboard State ---
    val adminSelectedTab = MutableStateFlow("Sellers")

    fun deleteProductByAdmin(productId: String) {
        viewModelScope.launch {
            repository.removeProduct(productId)
        }
    }

    fun updateOrderStatusByAdmin(orderId: String, newStatus: String, progress: Float) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus, progress)
        }
    }
}

class MarketViewModelFactory(private val repository: NexoraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
