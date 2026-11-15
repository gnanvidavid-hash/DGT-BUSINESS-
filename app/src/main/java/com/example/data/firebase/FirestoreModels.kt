package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val key: String, val labelFr: String, val descriptionFr: String) {
    CLIENT("client", "Client", "Acheter des produits, suivre vos commandes et payer en FCFA."),
    VENDEUR("vendeur", "Vendeur", "Gérer votre boutique, vos stocks et suivre vos revenus."),
    ADMIN("admin", "Admin", "Superviser les vendeurs, commandes et opérations de la plateforme.");

    companion object {
        fun fromKey(key: String?): UserRole {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: CLIENT
        }
    }
}

object OrderStatus {
    const val PENDING_PAYMENT = "En attente de paiement"
    const val PENDING = "En attente"
    const val PAID = "Payée"
    const val PAYMENT_FAILED = "Paiement échoué"
    const val SHIPPED = "Expédiée"
    const val DELIVERED = "Livrée"
    const val CANCELED = "Annulée"
}

object PaymentMethod {
    const val FEDAPAY = "FedaPay"
    const val CASH_ON_DELIVERY = "Paiement à la livraison"
}

object CommissionCalculator {
    const val PLATFORM_RATE = 0.10

    data class Split(
        val platformCommissionXof: Long,
        val sellerShareXof: Long
    )

    fun calculate(totalAmountXof: Long): Split {
        val safeTotal = totalAmountXof.coerceAtLeast(0L)
        val platformShare = (safeTotal * 10L) / 100L
        val sellerShare = safeTotal - platformShare
        return Split(
            platformCommissionXof = platformShare,
            sellerShareXof = sellerShare
        )
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = UserRole.CLIENT.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSeller(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val businessName: String = "",
    val sellerName: String = "",
    val category: String = "",
    val email: String = "",
    val status: String = "En attente", // "En attente", "Approuvé", "Suspendu"
    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,
    val balanceXof: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val unitPriceXof: Long = 0L,
    val variant: String = "",
    val sellerName: String = ""
)

@IgnoreExtraProperties
data class FirestoreOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val sellerName: String = "",
    val itemsSummary: String = "",
    val items: List<OrderLineItem> = emptyList(),
    val subtotalXof: Long = 0L,
    val discountXof: Long = 0L,
    val shippingFeeXof: Long = 0L,
    val totalAmountXof: Long = 0L,
    val platformCommissionXof: Long = 0L,
    val sellerShareXof: Long = 0L,
    val commissionRate: Double = CommissionCalculator.PLATFORM_RATE,
    val paymentMethod: String = PaymentMethod.FEDAPAY,
    val status: String = OrderStatus.PENDING_PAYMENT,
    val shippingAddress: String = "",
    val shippingMethod: String = "",
    val trackingProgress: Double = 0.1,
    val fedapayTransactionId: String? = null,
    val lastWebhookEventId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val paidAt: Timestamp? = null
)

fun formatFcfa(amountXof: Long): String {
    val digits = kotlin.math.abs(amountXof).toString()
    val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
    val prefix = if (amountXof < 0) "-" else ""
    return "$prefix$grouped FCFA"
}
