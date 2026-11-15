package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google avant d'accéder à Firestore.")
    }

    suspend fun isAdminAccount(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.email.equals("gnanvidavid@gmail.com", ignoreCase = true) && user.isEmailVerified) {
            return true
        }
        val adminDocPath = "admins/${user.uid}"
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java) }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, "users/$userId", auth)
                }
                throw error
            }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            db.collection("users").document(userId).get().await().toObject(UserProfile::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path, auth)
            throw e
        }
    }

    suspend fun upsertUserProfile(fullName: String, email: String, role: UserRole): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val safeName = fullName.trim().ifEmpty { "Utilisateur Nexora" }.take(100)
        val safeEmail = email.trim().ifEmpty { auth.currentUser?.email ?: "client@nexora.africa" }.take(150)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "userId" to uid,
                    "fullName" to safeName,
                    "email" to safeEmail,
                    "role" to role.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to safeName,
                    "email" to safeEmail,
                    "role" to role.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            docRef.get().await().toObject(UserProfile::class.java)
                ?: UserProfile(id = uid, userId = uid, fullName = safeName, email = safeEmail, role = role.key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }
}

class FirestoreSellerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Connexion requise pour gérer la boutique vendeur.")
    }

    fun observeMySellers(ownerId: String): Flow<List<FirestoreSeller>> =
        db.collection("sellers")
            .whereEqualTo("ownerId", ownerId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreSeller::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == ownerId) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "sellers", auth)
                }
                throw error
            }

    fun observeAllSellersForAdmin(): Flow<List<FirestoreSeller>> =
        db.collection("sellers")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreSeller::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "sellers", auth)
                }
                throw error
            }

    suspend fun createSellerStore(
        businessName: String,
        sellerName: String,
        category: String,
        email: String,
        sellerId: String = "SEL-${UUID.randomUUID().toString().take(8).uppercase()}"
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("sellers").document(sellerId)
        val payload = mapOf(
            "ownerId" to uid,
            "businessName" to businessName.trim().take(120),
            "sellerName" to sellerName.trim().take(100),
            "category" to category.trim().take(80),
            "email" to email.trim().take(150),
            "status" to "En attente",
            "isVerified" to false,
            "balanceXof" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            sellerId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun seedDemoSellersIfAdmin() {
        requireUserId()
        val demoSellers = listOf(
            mapOf(
                "ownerId" to requireUserId(),
                "businessName" to "AfriCraft Sénégal",
                "sellerName" to "Amadou Diop",
                "category" to "Mode & Artisanat",
                "email" to "amadou@africraft.sn",
                "status" to "Approuvé",
                "isVerified" to true,
                "balanceXof" to 270000L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "ownerId" to requireUserId(),
                "businessName" to "Karité d'Or Bio",
                "sellerName" to "Yasmine Traoré",
                "category" to "Beauté & Cosmétiques",
                "email" to "yasmine@karitedor.ci",
                "status" to "En attente",
                "isVerified" to false,
                "balanceXof" to 723000L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "ownerId" to requireUserId(),
                "businessName" to "Solaire Express Bénin",
                "sellerName" to "Kossi Agbo",
                "category" to "Importations de Chine",
                "email" to "kossi@solaireexpress.bj",
                "status" to "En attente",
                "isVerified" to false,
                "balanceXof" to 145000L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            val existing = db.collection("sellers").limit(1).get().await()
            if (existing.isEmpty) {
                demoSellers.forEachIndexed { idx, payload ->
                    db.collection("sellers").document("demo-seller-${idx + 1}").set(payload).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "sellers", auth)
        }
    }

    suspend fun approveSeller(sellerId: String) {
        requireUserId()
        val docRef = db.collection("sellers").document(sellerId)
        try {
            docRef.update(
                mapOf(
                    "status" to "Approuvé",
                    "isVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun suspendSeller(sellerId: String) {
        requireUserId()
        val docRef = db.collection("sellers").document(sellerId)
        try {
            docRef.update(
                mapOf(
                    "status" to "Suspendu",
                    "isVerified" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }
}

class FirestoreOrderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avant de passer commande.")
    }

    fun observeUserOrders(userId: String): Flow<List<FirestoreOrder>> = flow {
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreOrder::class.java)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val fsErr = error.unwrapFirestoreException()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeAllOrdersForAdmin(): Flow<List<FirestoreOrder>> =
        db.collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreOrder::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "orders", auth)
                }
                throw error
            }

    /**
     * Observation en temps réel d'une commande précise (utilisée après retour de Feda Checkout.js
     * pour détecter la mise à jour du webhook FedaPay vers "Payée" ou "Paiement échoué").
     */
    fun observeOrder(orderId: String): Flow<FirestoreOrder?> =
        db.collection("orders").document(orderId)
            .snapshots()
            .map { snap -> snap.toObject(FirestoreOrder::class.java) }
            .catch { error ->
                val fsErr = error.unwrapFirestoreException()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, "orders/$orderId", auth)
                }
                throw error
            }

    /**
     * Crée la commande dans Firestore AVANT d'ouvrir Feda Checkout.js ou pour paiement à la livraison.
     * Statut initial imposé :
     * - FedaPay -> "En attente de paiement"
     * - Paiement à la livraison -> "En attente"
     * Ne marque JAMAIS la commande comme "Payée" depuis l'application Android.
     */
    suspend fun createOrder(
        customerName: String,
        customerEmail: String,
        sellerName: String,
        items: List<OrderLineItem>,
        subtotalXof: Long,
        discountXof: Long,
        shippingFeeXof: Long,
        totalAmountXof: Long,
        paymentMethod: String,
        shippingAddress: String,
        shippingMethod: String,
        orderId: String = "NEX-${UUID.randomUUID().toString().take(8).uppercase()}"
    ): FirestoreOrder {
        val uid = requireUserId()
        val initialStatus = if (paymentMethod == PaymentMethod.FEDAPAY) {
            OrderStatus.PENDING_PAYMENT
        } else {
            OrderStatus.PENDING
        }
        val initialProgress = if (paymentMethod == PaymentMethod.FEDAPAY) 0.1 else 0.25
        val split = CommissionCalculator.calculate(totalAmountXof)
        val summary = items.joinToString(", ") { "${it.name} (x${it.quantity})" }.take(2000)

        val itemMaps = items.map { item ->
            mapOf(
                "productId" to item.productId,
                "name" to item.name,
                "quantity" to item.quantity,
                "unitPriceXof" to item.unitPriceXof,
                "variant" to item.variant,
                "sellerName" to item.sellerName
            )
        }

        val docRef = db.collection("orders").document(orderId)
        val payload = mapOf(
            "userId" to uid,
            "customerName" to customerName.trim().ifEmpty { "Client Nexora" }.take(120),
            "customerEmail" to customerEmail.trim().ifEmpty { auth.currentUser?.email ?: "client@nexora.africa" }.take(150),
            "sellerName" to sellerName.trim().ifEmpty { "Nexora Officiel" }.take(120),
            "itemsSummary" to summary.ifEmpty { "Commande Nexora Market" },
            "items" to itemMaps,
            "subtotalXof" to subtotalXof,
            "discountXof" to discountXof,
            "shippingFeeXof" to shippingFeeXof,
            "totalAmountXof" to totalAmountXof,
            "platformCommissionXof" to split.platformCommissionXof,
            "sellerShareXof" to split.sellerShareXof,
            "commissionRate" to CommissionCalculator.PLATFORM_RATE,
            "paymentMethod" to paymentMethod,
            "status" to initialStatus,
            "shippingAddress" to shippingAddress.trim().ifEmpty { "Abidjan, Côte d'Ivoire" }.take(300),
            "shippingMethod" to shippingMethod.trim().ifEmpty { "Livraison Express à Domicile" }.take(100),
            "trackingProgress" to initialProgress,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            docRef.get().await().toObject(FirestoreOrder::class.java)
                ?: FirestoreOrder(
                    id = orderId,
                    userId = uid,
                    customerName = customerName,
                    customerEmail = customerEmail,
                    sellerName = sellerName,
                    itemsSummary = summary,
                    items = items,
                    subtotalXof = subtotalXof,
                    discountXof = discountXof,
                    shippingFeeXof = shippingFeeXof,
                    totalAmountXof = totalAmountXof,
                    platformCommissionXof = split.platformCommissionXof,
                    sellerShareXof = split.sellerShareXof,
                    commissionRate = CommissionCalculator.PLATFORM_RATE,
                    paymentMethod = paymentMethod,
                    status = initialStatus,
                    shippingAddress = shippingAddress,
                    shippingMethod = shippingMethod,
                    trackingProgress = initialProgress
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun getOrderById(orderId: String): FirestoreOrder? {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.get().await().toObject(FirestoreOrder::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
    }

    suspend fun cancelPendingOrder(orderId: String) {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "status" to OrderStatus.CANCELED,
                    "trackingProgress" to 0.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateFulfillmentStatusByAdmin(orderId: String, newStatus: String, progress: Double) {
        requireUserId()
        require(
            newStatus in listOf(
                OrderStatus.PENDING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELED
            )
        ) { "Statut de traitement invalide: $newStatus" }
        val docRef = db.collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "trackingProgress" to progress,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }
}
