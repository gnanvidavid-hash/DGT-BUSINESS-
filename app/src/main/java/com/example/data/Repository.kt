package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NexoraRepository(private val db: AppDatabase) {

    val allProducts: Flow<List<Product>> = db.productDao().getAllProductsFlow()
    val allCartItems: Flow<List<CartItem>> = db.cartDao().getCartItemsFlow()
    val allWishlistItems: Flow<List<WishlistItem>> = db.wishlistDao().getWishlistItemsFlow()
    val allOrders: Flow<List<OrderItem>> = db.orderDao().getAllOrdersFlow()
    val chatMessages: Flow<List<SupportMessage>> = db.chatDao().getAllMessagesFlow()
    val coupons: Flow<List<Coupon>> = db.couponDao().getAllCouponsFlow()
    val sellers: Flow<List<Seller>> = db.sellerDao().getAllSellersFlow()

    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (db.productDao().getProductCount() == 0) {
            val defaultProducts = listOf(
                Product(
                    id = "p1",
                    name = "Phantom X3 Pro 5G (Double SIM, 256 Go)",
                    category = "Smartphones",
                    price = 180000.0,
                    oldPrice = 210000.0,
                    rating = 4.8f,
                    reviewCount = 124,
                    stock = 14,
                    description = "Profitez d'une performance smartphone haut de gamme avec connectivité 5G, 256 Go de stockage, triple appareil photo 108 MP et écran AMOLED 120 Hz. Idéal pour le travail, les réseaux et le divertissement.",
                    isFeatured = true,
                    isBestSeller = true,
                    sellerName = "Nexora Officiel",
                    imageResName = "img_phone"
                ),
                Product(
                    id = "p2",
                    name = "Machine à Piler l'Igname Multifonction",
                    category = "Maison & Cuisine",
                    price = 54000.0,
                    oldPrice = 66000.0,
                    rating = 4.9f,
                    reviewCount = 86,
                    stock = 9,
                    description = "Préparez un foutou d'igname onctueux en moins de 5 minutes ! Moteur puissant, lames en acier inoxydable alimentaire et 3 modes de cuisson/pétrissage pour toute la famille.",
                    isFeatured = true,
                    isBestSeller = true,
                    sellerName = "Nexora Officiel",
                    imageResName = "img_yam_pounder"
                ),
                Product(
                    id = "p3",
                    name = "Blazer Moderne Tissu Kente Premium",
                    category = "Mode",
                    price = 27000.0,
                    oldPrice = 36000.0,
                    rating = 4.7f,
                    reviewCount = 42,
                    stock = 7,
                    description = "Affirmez votre élégance avec notre blazer coupe moderne inspiré du tissage Kente authentique. Tissu respirant, doublure satinée et finitions sur mesure pour vos cérémonies et rendez-vous d'affaires.",
                    isFeatured = false,
                    isNewArrival = true,
                    sellerName = "AfriCraft Sénégal",
                    imageResName = "img_fashion"
                ),
                Product(
                    id = "p4",
                    name = "Beurre de Karité Bio Pur (Pot 500 g)",
                    category = "Beauté",
                    price = 7500.0,
                    oldPrice = 10800.0,
                    rating = 4.9f,
                    reviewCount = 215,
                    stock = 32,
                    description = "100% pur beurre de karité brut et biologique récolté en Afrique de l'Ouest. Hydrate intensément la peau sèche, nourrit les cheveux et apaise naturellement sans aucun additif chimique.",
                    isFeatured = true,
                    isBestSeller = true,
                    sellerName = "Karité d'Or Bio",
                    imageResName = "img_shea_butter"
                ),
                Product(
                    id = "p5",
                    name = "Projecteur LED Solaire Rechargeable 100W",
                    category = "Importations de Chine",
                    price = 12000.0,
                    oldPrice = 16800.0,
                    rating = 4.6f,
                    reviewCount = 310,
                    stock = 22,
                    description = "Oubliez les coupures d'électricité ! Ce projecteur solaire 100W étanche IP67 se recharge le jour et éclaire jusqu'à 14 heures la nuit. Télécommande et détecteur crépusculaire inclus.",
                    isChinaImport = true,
                    isFeatured = true,
                    sellerName = "Solaire Express Bénin",
                    imageResName = "img_solar_light"
                ),
                Product(
                    id = "p6",
                    name = "Riz Parfumé Jasmin Supérieur (Sac 5 kg)",
                    category = "Épicerie",
                    price = 10800.0,
                    oldPrice = 13200.0,
                    rating = 4.5f,
                    reviewCount = 98,
                    stock = 40,
                    description = "Riz long grain parfumé soigneusement sélectionné pour une cuisson parfaite, non collante et savoureuse. Parfait pour le riz gras, le thiéboudienne ou les plats familiaux.",
                    sellerName = "Nexora Officiel",
                    imageResName = "img_rice"
                ),
                Product(
                    id = "p7",
                    name = "Poudre de Moringa Bio Superaliment (250 g)",
                    category = "Santé & Bien-être",
                    price = 9000.0,
                    rating = 4.8f,
                    reviewCount = 55,
                    stock = 18,
                    description = "Renforcez votre vitalité ! Poudre de feuilles de Moringa 100% naturelle riche en vitamines, fer et antioxydants. Idéale dans vos jus, infusions ou repas quotidiens.",
                    sellerName = "Karité d'Or Bio",
                    imageResName = "img_moringa"
                ),
                Product(
                    id = "p8",
                    name = "Jeu de Tapis Auto Caoutchouc Renforcé",
                    category = "Automobile",
                    price = 13200.0,
                    oldPrice = 16500.0,
                    rating = 4.3f,
                    reviewCount = 28,
                    stock = 4,
                    description = "Lot de 4 tapis toutes saisons antidérapants pour berlines et SUV. Protège efficacement l'habitacle contre la boue, le sable et l'humidité. Nettoyage rapide à l'eau.",
                    sellerName = "Nexora Officiel",
                    imageResName = "img_mats"
                ),
                Product(
                    id = "p9",
                    name = "Couverture d'Emmaillotage Coton Bio Bébé",
                    category = "Produits Bébé",
                    price = 9500.0,
                    rating = 4.7f,
                    reviewCount = 36,
                    stock = 12,
                    description = "Couverture ultra-douce et respirante en coton biologique certifié pour nouveau-nés. Respecte la peau sensible de bébé et assure un sommeil paisible.",
                    sellerName = "AfriCraft Sénégal",
                    imageResName = "img_swaddle"
                ),
                Product(
                    id = "p10",
                    name = "Smart TV QLED 4K Nexora CinemaMax 55\"",
                    category = "Électronique",
                    price = 210000.0,
                    oldPrice = 252000.0,
                    rating = 4.9f,
                    reviewCount = 180,
                    stock = 6,
                    description = "Le cinéma à la maison ! Écran QLED 55 pouces 4K HDR10+ avec son Dolby Audio intégré, Wi-Fi rapide et applications de streaming préinstallées.",
                    isFeatured = true,
                    isBestSeller = true,
                    sellerName = "Nexora Officiel",
                    imageResName = "img_tv"
                )
            )
            db.productDao().insertProducts(defaultProducts)

            val defaultCoupons = listOf(
                Coupon("NEXORA10", 10, "10% de réduction sur tout le panier, sans minimum d'achat !", 0.0),
                Coupon("AFRICA20", 20, "20% de réduction dès 60 000 FCFA d'achat !", 60000.0),
                Coupon("PROMO15", 15, "15% de réduction sur votre commande dès 15 000 FCFA !", 15000.0)
            )
            db.couponDao().insertCoupons(defaultCoupons)

            db.reviewDao().insertReview(
                Review(
                    productId = "p1",
                    userName = "Koffi Mensah (Ghana)",
                    rating = 5,
                    comment = "Excellent smartphone ! Réseau 5G très stable, écran lumineux même en plein soleil et batterie qui tient facilement deux jours."
                )
            )
            db.reviewDao().insertReview(
                Review(
                    productId = "p2",
                    userName = "Fatoumata Diallo (Sénégal)",
                    rating = 5,
                    comment = "Un gain de temps incroyable en cuisine ! L'igname est pilée de façon homogène en quelques minutes sans effort."
                )
            )
            db.reviewDao().insertReview(
                Review(
                    productId = "p4",
                    userName = "Amina Bello (Bénin)",
                    rating = 5,
                    comment = "Beurre de karité authentique, très doux et naturel. Parfait pour toute la famille."
                )
            )
            db.reviewDao().insertReview(
                Review(
                    productId = "p5",
                    userName = "Chinedu Okafor (Côte d'Ivoire)",
                    rating = 4,
                    comment = "Très pratique lors des coupures. Le panneau solaire charge vite et l'éclairage est puissant."
                )
            )

            db.chatDao().insertMessage(
                SupportMessage(
                    sender = "ai",
                    text = "Bienvenue sur le support Nexora Market ! 🌟 Je suis Nexora, votre assistant d'achat. Comment puis-je vous aider aujourd'hui ?\n• Recommander des produits en FCFA\n• Suivre votre commande en temps réel\n• Expliquer le paiement FedaPay (Mobile Money & Carte) ou à la livraison\n• Ouvrir votre boutique sur l'Espace Vendeurs"
                )
            )

            db.sellerDao().insertSeller(
                Seller(
                    id = "s1",
                    businessName = "AfriCraft Sénégal",
                    sellerName = "Amadou Diop",
                    category = "Mode & Artisanat",
                    email = "amadou@africraft.sn",
                    isVerified = true,
                    balance = 270000.0
                )
            )
            db.sellerDao().insertSeller(
                Seller(
                    id = "s2",
                    businessName = "Karité d'Or Bio",
                    sellerName = "Yasmine Traoré",
                    category = "Beauté & Cosmétiques",
                    email = "yasmine@karitedor.ci",
                    isVerified = true,
                    balance = 723000.0
                )
            )
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

    suspend fun getCartItemsWithProducts(): List<CartItemWithProduct> = withContext(Dispatchers.IO) {
        val cartItems = db.cartDao().getCartItems()
        cartItems.mapNotNull { item ->
            val product = db.productDao().getProductById(item.productId)
            product?.let { CartItemWithProduct(cartItem = item, product = it) }
        }
    }

    suspend fun addToCart(productId: String, quantity: Int, color: String? = null, size: String? = null) {
        val existing = db.cartDao().getCartItems().find {
            it.productId == productId && it.selectedColor == color && it.selectedSize == size
        }
        if (existing != null) {
            db.cartDao().updateCartItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            db.cartDao().insertCartItem(
                CartItem(
                    productId = productId,
                    quantity = quantity,
                    selectedColor = color,
                    selectedSize = size
                )
            )
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

    suspend fun toggleWishlist(productId: String): Boolean {
        val exists = db.wishlistDao().isInWishlist(productId)
        return if (exists) {
            db.wishlistDao().deleteWishlistItem(productId)
            false
        } else {
            db.wishlistDao().insertWishlistItem(WishlistItem(productId))
            true
        }
    }

    suspend fun isInWishlist(productId: String): Boolean {
        return db.wishlistDao().isInWishlist(productId)
    }

    suspend fun placeOrder(
        totalAmount: Double,
        paymentMethod: String,
        address: String,
        items: List<CartItemWithProduct>
    ): OrderItem {
        val itemsSummary = items.joinToString(", ") { "${it.product.name} (x${it.cartItem.quantity})" }
        val order = OrderItem(
            id = "NEX-${(100000..999999).random()}",
            totalAmount = totalAmount,
            status = if (paymentMethod == "FedaPay") "En attente de paiement" else "En attente",
            itemsJson = itemsSummary,
            paymentMethod = paymentMethod,
            shippingAddress = address,
            trackingProgress = 0.25f
        )
        db.orderDao().insertOrder(order)
        db.cartDao().clearCart()
        return order
    }

    suspend fun updateOrderStatus(orderId: String, status: String, progress: Float) {
        val order = db.orderDao().getOrderById(orderId)
        if (order != null) {
            db.orderDao().insertOrder(order.copy(status = status, trackingProgress = progress))
        }
    }

    suspend fun addChatMessage(sender: String, text: String) {
        db.chatDao().insertMessage(SupportMessage(sender = sender, text = text))
    }

    suspend fun clearChat() {
        db.chatDao().clearChat()
    }

    suspend fun addReview(productId: String, userName: String, rating: Int, comment: String) {
        val review = Review(productId = productId, userName = userName, rating = rating, comment = comment)
        db.reviewDao().insertReview(review)

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

    suspend fun registerSeller(businessName: String, name: String, category: String, email: String) {
        val seller = Seller(
            id = "SEL-${(1000..9999).random()}",
            businessName = businessName,
            sellerName = name,
            category = category,
            email = email,
            isVerified = false,
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

    suspend fun getCoupon(code: String): Coupon? {
        return db.couponDao().getCouponByCode(code.uppercase())
    }

    suspend fun sendChatToAI(userPrompt: String, history: List<SupportMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getRuleBasedChatResponse(userPrompt)
        }

        val systemInstructionText = """
            Tu es Nexora, l'assistant virtuel amical et professionnel de Nexora Market, la marketplace africaine de référence.
            Tu réponds TOUJOURS en français et exprimes tous les montants en FCFA (XOF).
            Tu aides les clients à trouver des produits, suivre leurs commandes, payer via FedaPay (MTN Mobile Money, Moov Money, Orange Money, Visa/Mastercard) ou en Paiement à la livraison, et à gérer leur boutique vendeur.
            Sois clair, chaleureux et concis.
        """.trimIndent()

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val rootJson = JSONObject()
            val contentsArray = JSONArray()

            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                val text = "${if (msg.sender == "user") "Client" else "Assistant Nexora"}: ${msg.text}"
                val partObj = JSONObject().put("text", text)
                val contentObj = JSONObject().put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }

            val currentPartObj = JSONObject().put("text", "Client: $userPrompt")
            val currentContentObj = JSONObject().put("parts", JSONArray().put(currentPartObj))
            contentsArray.put(currentContentObj)

            rootJson.put("contents", contentsArray)
            rootJson.put("generationConfig", JSONObject().put("temperature", 0.7))
            rootJson.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
            )

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
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Réponse vide")
                    }
                }
                "Je suis désolé, je n'ai pas pu obtenir de réponse. Comment puis-je vous aider sur Nexora Market ?"
            }
        } catch (e: Exception) {
            getRuleBasedChatResponse(userPrompt)
        }
    }

    private fun getRuleBasedChatResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("bonjour") || lower.contains("salut") || lower.contains("hello") -> {
                "Bonjour ! 🌟 Je suis Nexora, votre assistant d'achat. Je peux vous aider à trouver un produit en FCFA, suivre une commande ou payer via FedaPay (Mobile Money & Carte)."
            }
            lower.contains("suiv") || lower.contains("command") || lower.contains("livraison") -> {
                "Pour suivre votre commande :\n1. Ouvrez l'onglet **Profil** puis **Historique & Suivi des commandes**.\n2. Consultez le statut en temps réel (*En attente de paiement*, *Payée*, *Expédiée*, *Livrée*).\n3. Téléchargez votre **Reçu officiel** avec détail de commission."
            }
            lower.contains("paiement") || lower.contains("fedapay") || lower.contains("momo") || lower.contains("payer") || lower.contains("carte") -> {
                "Nexora Market accepte :\n• **FedaPay (Sandbox/Test)** : MTN Mobile Money, Moov Money, Orange Money, Visa et Mastercard.\n• **Paiement à la livraison** : réglez en espèces en FCFA à la réception.\nAprès un paiement FedaPay, la vérification s'effectue automatiquement via notre webhook sécurisé."
            }
            lower.contains("vendeur") || lower.contains("boutique") || lower.contains("vendre") -> {
                "Pour vendre sur Nexora Market :\n1. Sélectionnez le rôle **Vendeur** ou ouvrez le **Portail Vendeur** dans votre Profil.\n2. Enregistrez votre boutique et soumettez-la à validation.\n3. Une fois approuvée par l'administrateur, publiez vos produits en FCFA et suivez votre part nette (90% par vente)."
            }
            lower.contains("chine") || lower.contains("import") -> {
                "Découvrez notre rayon **Importations de Chine** dans la Boutique : projecteurs solaires, accessoires électroniques et équipements livrés à tarif optimisé en FCFA !"
            }
            else -> {
                "Nexora Market est 100% disponible en français et en **FCFA (XOF)** !\nPosez-moi une question sur nos produits, le code promo **NEXORA10**, le paiement **FedaPay** ou l'**Espace Vendeurs**."
            }
        }
    }
}
