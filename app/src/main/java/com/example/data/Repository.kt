package com.example.data

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// --- Nexora Repository ---

class NexoraRepository(private val db: AppDatabase) {

    val allProducts: Flow<List<Product>> = db.productDao().getAllProductsFlow()
    val allCartItems: Flow<List<CartItem>> = db.cartDao().getCartItemsFlow()
    val allWishlistItems: Flow<List<WishlistItem>> = db.wishlistDao().getWishlistItemsFlow()
    val allOrders: Flow<List<OrderItem>> = db.orderDao().getAllOrdersFlow()
    val chatMessages: Flow<List<SupportMessage>> = db.chatDao().getAllMessagesFlow()
    val coupons: Flow<List<Coupon>> = db.couponDao().getAllCouponsFlow()
    val sellers: Flow<List<Seller>> = db.sellerDao().getAllSellersFlow()

    // --- Product Accessors ---
    
    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (db.productDao().getProductCount() == 0) {
            val defaultProducts = listOf(
                Product(
                    id = "p1",
                    name = "Phantom X3 Pro 5G (Dual SIM, 256GB)",
                    category = "Smartphones",
                    price = 299.99,
                    oldPrice = 349.99,
                    rating = 4.8f,
                    reviewCount = 124,
                    description = "Experience premium smartphone performance with 5G connectivity, 256GB storage, outstanding camera capabilities, and a vibrant AMOLED display. Perfect for browsing, gaming, and productivity in Africa and beyond.",
                    isFeatured = true,
                    isBestSeller = true,
                    imageResName = "img_phone"
                ),
                Product(
                    id = "p2",
                    name = "Multi-Function Yam Pounding Machine",
                    category = "Home & Kitchen",
                    price = 89.99,
                    oldPrice = 110.00,
                    rating = 4.9f,
                    reviewCount = 86,
                    description = "Pound your yam to perfection in less than 5 minutes! This heavy-duty, multi-function machine makes traditional yam pounding effortless. Features robust stainless steel blades and multiple speed modes.",
                    isFeatured = true,
                    isBestSeller = true,
                    imageResName = "img_yam_pounder"
                ),
                Product(
                    id = "p3",
                    name = "Premium Kente Print Modern Blazer",
                    category = "Fashion",
                    price = 45.00,
                    oldPrice = 60.00,
                    rating = 4.7f,
                    reviewCount = 42,
                    description = "Make a bold statement with our authentic hand-woven pattern Kente printed modern blazer. Expertly tailored, lightweight, and perfect for ceremonial occasions, professional meetups, or upscale social nights.",
                    isFeatured = false,
                    isNewArrival = true,
                    imageResName = "img_fashion"
                ),
                Product(
                    id = "p4",
                    name = "Pure Organic Shea Butter Body Cream (500g)",
                    category = "Beauty",
                    price = 12.50,
                    oldPrice = 18.00,
                    rating = 4.9f,
                    reviewCount = 215,
                    description = "100% pure, raw, and organic Shea Butter harvested directly in West Africa. Intensely moisturizes dry skin, prevents stretch marks, and heals blemishes. Completely natural and chemical-free.",
                    isFeatured = true,
                    isBestSeller = true,
                    imageResName = "img_shea_butter"
                ),
                Product(
                    id = "p5",
                    name = "Solar-Powered Rechargeable LED Floodlight (100W)",
                    category = "Imported Products from China",
                    price = 19.99,
                    oldPrice = 28.00,
                    rating = 4.6f,
                    reviewCount = 310,
                    description = "Beat electricity blackouts! This high-power, weather-resistant 100W solar floodlight charges during the day and provides brilliant lighting all night. Comes with remote control and automatic dawn/dusk sensors.",
                    isChinaImport = true,
                    isFeatured = true,
                    imageResName = "img_solar_light"
                ),
                Product(
                    id = "p6",
                    name = "Premium Jasmine Rice (5kg Bag)",
                    category = "Groceries",
                    price = 18.00,
                    oldPrice = 22.00,
                    rating = 4.5f,
                    reviewCount = 98,
                    description = "Fragrant, long-grain Premium Jasmine Rice. Carefully processed and packed to guarantee peak freshness and wonderful flavor. Ideal for classic Jollof rice, fried rice, or steamed dishes.",
                    imageResName = "img_rice"
                ),
                Product(
                    id = "p7",
                    name = "Moringa Powder Organic Superfood (250g)",
                    category = "Health & Wellness",
                    price = 14.99,
                    rating = 4.8f,
                    reviewCount = 55,
                    description = "Boost your immunity! 100% natural, premium Moringa leaf powder. Rich in vitamins, minerals, and antioxidants. Blend into your daily smoothies, teas, or meals for active cellular health.",
                    imageResName = "img_moringa"
                ),
                Product(
                    id = "p8",
                    name = "Universal Heavy-Duty Car Floor Mats Set",
                    category = "Automotive",
                    price = 22.00,
                    oldPrice = 27.50,
                    rating = 4.3f,
                    reviewCount = 28,
                    description = "Set of 4 heavy-duty, all-weather rubber floor mats for cars and SUVs. Engineered to trap mud, water, and debris, preserving your vehicle's interior. Easy to wash and trim to fit.",
                    imageResName = "img_mats"
                ),
                Product(
                    id = "p9",
                    name = "Comfort Swaddle Organic Cotton Baby Blanket",
                    category = "Baby Products",
                    price = 15.99,
                    rating = 4.7f,
                    reviewCount = 36,
                    description = "Super-soft, highly breathable organic cotton swaddling blanket for newborn babies. Gentle on sensitive baby skin, regulating temperatures naturally for peaceful, comfortable sleep.",
                    imageResName = "img_swaddle"
                ),
                Product(
                    id = "p10",
                    name = "Nexora CinemaMax 55\" QLED 4K Smart TV",
                    category = "Electronics",
                    price = 349.99,
                    oldPrice = 419.99,
                    rating = 4.9f,
                    reviewCount = 180,
                    description = "Bring the cinematic theater home! Brilliant 55-inch QLED 4K screen with immersive HDR10+, integrated Dolby Digital Audio, and pre-installed smart streaming apps. Supports high-speed Wi-Fi.",
                    isFeatured = true,
                    isBestSeller = true,
                    imageResName = "img_tv"
                )
            )
            db.productDao().insertProducts(defaultProducts)
            
            // Populate coupons
            val defaultCoupons = listOf(
                Coupon("NEXORA10", 10, "Get 10% off your entire purchase, no minimum spend!", 0.0),
                Coupon("AFRICA20", 20, "Enjoy 20% off on orders above $100!", 100.0),
                Coupon("FREESHIP", 100, "100% shipping fee waiver on your first order!", 25.0)
            )
            db.couponDao().insertCoupons(defaultCoupons)

            // Populate some initial reviews
            db.reviewDao().insertReview(Review(productId = "p1", userName = "Kofi Mensah (Ghana)", rating = 5, comment = "Best smartphone I've ever owned! Network reception is strong, screen is incredibly bright, and battery easily lasts 2 days of heavy use."))
            db.reviewDao().insertReview(Review(productId = "p2", userName = "Fatoumata Diallo (Sénégal)", rating = 5, comment = "This machine is a absolute life-saver! Traditional pounding was so tiring, but now I get smooth, fluffly pounded yam in minutes. Truly worth every single dollar."))
            db.reviewDao().insertReview(Review(productId = "p4", userName = "Amina Bello (Nigeria)", rating = 5, comment = "Authentic raw shea butter. Extremely rich and smooth. It did wonders for my dry skin and my baby's diaper rashes. Smells completely natural!"))
            db.reviewDao().insertReview(Review(productId = "p5", userName = "Chinedu Okafor (Nigeria)", rating = 4, comment = "Very useful during current grid blackouts. Installs easily outside, the solar panels charge perfectly. Very strong light!"))
            
            // Populate default chatbot welcome message
            db.chatDao().insertMessage(SupportMessage(
                sender = "ai",
                text = "Welcome to Nexora Market support! 🌟 I am Nexora, your AI virtual shopping assistant. How can I assist you today? You can ask me to:\n- Recommend top African products\n- Help you track your orders\n- Explain payments like MTN MoMo, Orange Money, Moov, Visa, Stripe\n- Answer questions about starting your merchant store on Nexora Seller Hub!"
            ))

            // Populate some default sellers
            db.sellerDao().insertSeller(Seller(id = "s1", businessName = "AfriCraft Co.", sellerName = "Amadou Diop", category = "Fashion & Handcrafts", email = "amadou@africraft.com", isVerified = true, balance = 450.0))
            db.sellerDao().insertSeller(Seller(id = "s2", businessName = "SheaGold Organics", sellerName = "Yasmine Traore", category = "Beauty & Cosmetics", email = "yasmine@sheagold.com", isVerified = true, balance = 1205.0))
        }
    }

    suspend fun addProduct(product: Product) {
        db.productDao().insertProduct(product)
    }

    suspend fun removeProduct(productId: String) {
        db.productDao().deleteProduct(productId)
    }

    suspend fun getProductById(productId: String): Product? {
        return db.productDao().getProductById(productId)
    }

    // --- Cart Actions ---

    suspend fun getCartItemsWithProducts(): List<CartItemWithProduct> = withContext(Dispatchers.IO) {
        val cartItems = db.cartDao().getCartItems()
        cartItems.mapNotNull { item ->
            val product = db.productDao().getProductById(item.productId)
            product?.let { CartItemWithProduct(cartItem = item, product = it) }
        }
    }

    suspend fun addToCart(productId: String, quantity: Int, color: String? = null, size: String? = null) {
        val existing = db.cartDao().getCartItems().find { it.productId == productId && it.selectedColor == color && it.selectedSize == size }
        if (existing != null) {
            db.cartDao().updateCartItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            db.cartDao().insertCartItem(CartItem(productId = productId, quantity = quantity, selectedColor = color, selectedSize = size))
        }
    }

    suspend fun updateCartQuantity(cartItemId: Int, quantity: Int) {
        val items = db.cartDao().getCartItems()
        val existing = items.find { it.id == cartItemId }
        if (existing != null) {
            if (quantity <= 0) {
                db.cartDao().deleteCartItem(cartItemId)
            } else {
                db.cartDao().updateCartItem(existing.copy(quantity = quantity))
            }
        }
    }

    suspend fun removeFromCart(cartItemId: Int) {
        db.cartDao().deleteCartItem(cartItemId)
    }

    suspend fun clearCart() {
        db.cartDao().clearCart()
    }

    // --- Wishlist Actions ---

    suspend fun toggleWishlist(productId: String) {
        val exists = db.wishlistDao().isInWishlist(productId)
        if (exists) {
            db.wishlistDao().deleteWishlistItem(productId)
        } else {
            db.wishlistDao().insertWishlistItem(WishlistItem(productId))
        }
    }

    suspend fun isInWishlist(productId: String): Boolean {
        return db.wishlistDao().isInWishlist(productId)
    }

    // --- Orders Actions ---

    suspend fun placeOrder(totalAmount: Double, paymentMethod: String, address: String, items: List<CartItemWithProduct>) {
        val itemsSummary = items.map { "${it.product.name} (x${it.cartItem.quantity})" }.joinToString(", ")
        val order = OrderItem(
            id = "NEX-${(100000..999999).random()}",
            totalAmount = totalAmount,
            status = "Received",
            itemsJson = itemsSummary,
            paymentMethod = paymentMethod,
            shippingAddress = address,
            trackingProgress = 0.25f
        )
        db.orderDao().insertOrder(order)
        db.cartDao().clearCart()
    }

    suspend fun updateOrderStatus(orderId: String, status: String, progress: Float) {
        val order = db.orderDao().getOrderById(orderId)
        if (order != null) {
            db.orderDao().insertOrder(order.copy(status = status, trackingProgress = progress))
        }
    }

    // --- Chat Actions ---

    suspend fun addChatMessage(sender: String, text: String) {
        db.chatDao().insertMessage(SupportMessage(sender = sender, text = text))
    }

    suspend fun clearChat() {
        db.chatDao().clearChat()
    }

    // --- Reviews Actions ---

    suspend fun addReview(productId: String, userName: String, rating: Int, comment: String) {
        val review = Review(productId = productId, userName = userName, rating = rating, comment = comment)
        db.reviewDao().insertReview(review)
        
        // Optionally update product average rating
        val product = db.productDao().getProductById(productId)
        if (product != null) {
            val currentReviewsCount = product.reviewCount
            val currentRating = product.rating
            val newCount = currentReviewsCount + 1
            val newRating = ((currentRating * currentReviewsCount) + rating) / newCount
            db.productDao().insertProduct(product.copy(rating = newRating, reviewCount = newCount))
        }
    }

    fun getReviewsForProduct(productId: String): Flow<List<Review>> {
        return db.reviewDao().getReviewsForProductFlow(productId)
    }

    // --- Seller Hub actions ---

    suspend fun registerSeller(businessName: String, name: String, category: String, email: String) {
        val seller = Seller(
            id = "SEL-${(1000..9999).random()}",
            businessName = businessName,
            sellerName = name,
            category = category,
            email = email,
            isVerified = true,
            joinDate = System.currentTimeMillis(),
            balance = 0.0
        )
        db.sellerDao().insertSeller(seller)
    }

    suspend fun updateSellerBalance(sellerId: String, amount: Double) {
        val seller = db.sellerDao().getSellerById(sellerId)
        if (seller != null) {
            db.sellerDao().insertSeller(seller.copy(balance = seller.balance + amount))
        }
    }

    // --- Coupon Validation ---

    suspend fun getCoupon(code: String): Coupon? {
        return db.couponDao().getCouponByCode(code.uppercase())
    }

    // --- Gemini Chat API Call ---

    suspend fun sendChatToAI(userPrompt: String, history: List<SupportMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getRuleBasedChatResponse(userPrompt)
        }

        val systemInstructionText = """
            You are Nexora, the friendly, modern AI Virtual Shopping Assistant for Nexora Market, a premier e-commerce marketplace in Africa.
            You help customers find products (phones, fashion blazers, groceries, shea butter, Chinese imports), track orders, choose local/global payment methods, and answer seller store questions.
            Keep your responses very warm, professional, clean, concise, and structured with bullet points where appropriate.
            Reference African mobile money networks like MTN MoMo, Moov Money, Orange Money, Flutterwave, and Paystack when explaining payments.
        """.trimIndent()

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()

            // Add recent history messages
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val text = "${if (msg.sender == "user") "Customer" else "Nexora Assistant"}: ${msg.text}"
                val partObj = JSONObject().put("text", text)
                val contentObj = JSONObject().put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }

            // Add the current prompt
            val currentPartObj = JSONObject().put("text", "Customer: $userPrompt")
            val currentContentObj = JSONObject().put("parts", JSONArray().put(currentPartObj))
            contentsArray.put(currentContentObj)

            rootJson.put("contents", contentsArray)

            // Add generationConfig
            val configObj = JSONObject().put("temperature", 0.7)
            rootJson.put("generationConfig", configObj)

            // Add systemInstruction
            val systemPartObj = JSONObject().put("text", systemInstructionText)
            val systemContentObj = JSONObject().put("parts", JSONArray().put(systemPartObj))
            rootJson.put("systemInstruction", systemContentObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)
            
            val request = okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Unexpected response code: ${response.code}")
                }
                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Empty response text")
                        }
                    }
                }
                "I apologize, but I received an empty response. How can I assist you with Nexora Market?"
            }
        } catch (e: Exception) {
            // Fallback gracefully to offline rule-based responder
            getRuleBasedChatResponse(userPrompt)
        }
    }

    private fun getRuleBasedChatResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                "Hello there! 🌟 I'm Nexora, your virtual AI assistant. How can I help you explore Nexora Market today? Ask me about products, order tracking, local payment options, or setting up a Seller Account."
            }
            lower.contains("track") || lower.contains("order") || lower.contains("delivery") -> {
                "To track your order on Nexora Market:\n1. Head to your **Account Tab**.\n2. Tap on **Track My Orders**.\n3. Enter your Order ID (e.g., `NEX-583274`) to see a live visual progress tracker! Delivery options include Express home delivery, or picking up from local Secure Pickup points."
            }
            lower.contains("payment") || lower.contains("pay") || lower.contains("momo") || lower.contains("money") || lower.contains("card") -> {
                "Nexora Market supports highly secure and flexible payment methods:\n- **Mobile Money (MoMo)**: MTN Mobile Money, Moov Money, Orange Money.\n- **Cards**: Visa, Mastercard via **Flutterwave** and **Paystack** gateways.\n- **Global Gateways**: PayPal, Stripe.\n- **QR Payments**: Instant scan on delivery or pickup."
            }
            lower.contains("seller") || lower.contains("merchant") || lower.contains("store") || lower.contains("sell") -> {
                "Glad you asked! Starting your own store on Nexora Seller Hub is extremely simple:\n1. Go to your **Account Tab**.\n2. Click on **Seller Portal**.\n3. Register your business name, email, and choose a category.\n4. Once verified instantly, you will access your Seller Dashboard to add products, check live sales metrics, manage stock, and receive secure payouts!"
            }
            lower.contains("china") || lower.contains("import") -> {
                "Yes, we support direct China imports! 🚢 View our specialized **Imported from China** category on the Shop screen to buy affordable electronics, solar devices, and accessories with consolidated low-cost shipping directly to your local pickup points in Africa."
            }
            lower.contains("recom") || lower.contains("suggest") || lower.contains("buy") || lower.contains("featured") -> {
                "I highly recommend checking out:\n- **Phantom X3 Pro 5G**: A fast, premium dual SIM phone\n- **Multi-Function Yam Pounding Machine**: Effortless 5-minute yam preparation\n- **Pure West-African Shea Butter**: Excellent natural organic body moisturizing\n- **Solar-Powered 100W LED Floodlight**: Stay powered and illuminated during blackouts!"
            }
            else -> {
                "I hear you! As your Nexora AI Companion, I can tell you that Nexora Market is fully operational. We support:\n- MTN, Moov, Orange Mobile Money\n- Multi-currency (USD, Franc CFA - XOF)\n- Bilingual English & French interface\n- Instant AI chat support.\n\nCould you please specify how I can help you with this? (e.g., recommend products, track orders, explain Momo payments, or register a seller account)?"
            }
        }
    }
}
