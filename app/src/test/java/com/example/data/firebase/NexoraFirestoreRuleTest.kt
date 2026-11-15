package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class NexoraFirestoreRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleItems() = listOf(
        OrderLineItem(
            productId = "p1",
            name = "Phantom X3 Pro 5G",
            quantity = 1,
            unitPriceXof = 180000L,
            variant = "Bleu Nuit",
            sellerName = "Nexora Officiel"
        )
    )

    @Test
    fun createUserProfile_clientOrVendeur_succeeds_butAdminSelfAssignFails() = runBlocking {
        val uid = signInTestUser("client_${UUID.randomUUID().toString().take(6)}@nexora.test")
        val repo = FirestoreUserRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertUserProfile("Aïcha Koné", "aicha@nexora.test", UserRole.CLIENT)
        }
        assertEquals(uid, profile.userId)
        assertEquals("client", profile.role)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.upsertUserProfile("Aïcha Koné", "aicha@nexora.test", UserRole.ADMIN)
            }
            fail("Self-assigning admin role should fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
        }
    }

    @Test
    fun createOrder_fedaPay_createsWithPendingPaymentAndCommissionSplit() = runBlocking {
        val uid = signInTestUser("buyer_${UUID.randomUUID().toString().take(6)}@nexora.test")
        val repo = FirestoreOrderRepository(firestore, auth)
        val orderId = "NEX-${UUID.randomUUID().toString().take(8)}"

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                customerName = "Koffi Mensah",
                customerEmail = "koffi@nexora.test",
                sellerName = "Nexora Officiel",
                items = sampleItems(),
                subtotalXof = 180000L,
                discountXof = 0L,
                shippingFeeXof = 3000L,
                totalAmountXof = 183000L,
                paymentMethod = PaymentMethod.FEDAPAY,
                shippingAddress = "Plateau, Abidjan",
                shippingMethod = "Livraison Express à Domicile",
                orderId = orderId
            )
        }

        assertEquals(OrderStatus.PENDING_PAYMENT, created.status)
        assertEquals(18300L, created.platformCommissionXof)
        assertEquals(164700L, created.sellerShareXof)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserOrders(uid).first { list -> list.any { it.id == orderId } }
        }
        assertTrue(observed.any { it.id == orderId && it.status == OrderStatus.PENDING_PAYMENT })
    }

    @Test
    fun clientCannotMarkOrderAsPaidFromApp() = runBlocking {
        signInTestUser("spoof_${UUID.randomUUID().toString().take(6)}@nexora.test")
        val repo = FirestoreOrderRepository(firestore, auth)
        val orderId = "NEX-${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                customerName = "Test Spoof",
                customerEmail = "spoof@nexora.test",
                sellerName = "Nexora Officiel",
                items = sampleItems(),
                subtotalXof = 50000L,
                discountXof = 0L,
                shippingFeeXof = 3000L,
                totalAmountXof = 53000L,
                paymentMethod = PaymentMethod.FEDAPAY,
                shippingAddress = "Dakar",
                shippingMethod = "Point Relais",
                orderId = orderId
            )
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("orders").document(orderId).update(
                    mapOf(
                        "status" to OrderStatus.PAID,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            fail("Client must never be able to mark order as Payée")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
        }
    }

    @Test
    fun crossUserOrderAccess_andUnauthenticatedQuery_failWithPermissionDenied() = runBlocking {
        signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@nexora.test")
        val aliceRepo = FirestoreOrderRepository(firestore, auth)
        val orderId = "NEX-${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                customerName = "Alice",
                customerEmail = "alice@nexora.test",
                sellerName = "Nexora Officiel",
                items = sampleItems(),
                subtotalXof = 25000L,
                discountXof = 0L,
                shippingFeeXof = 3000L,
                totalAmountXof = 28000L,
                paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
                shippingAddress = "Cotonou",
                shippingMethod = "Livraison Express",
                orderId = orderId
            )
        }

        // Bob ne peut pas lire la commande d'Alice
        signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@nexora.test")
        val bobRepo = FirestoreOrderRepository(firestore, auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getOrderById(orderId)
            }
            fail("Bob should not be able to read Alice's order")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
        }

        // Utilisateur déconnecté ne peut pas observer les commandes
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserOrders("any_uid").first()
            }
            fail("Unauthenticated listener should fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
        }
    }

    @Test
    fun sellerCreation_andSelfApprovalBlocked() = runBlocking {
        val uid = signInTestUser("seller_${UUID.randomUUID().toString().take(6)}@nexora.test")
        val sellerRepo = FirestoreSellerRepository(firestore, auth)
        val sellerId = "SEL-${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.createSellerStore(
                businessName = "Boutique Étoile",
                sellerName = "Moussa Diallo",
                category = "Mode",
                email = "moussa@etoile.sn",
                sellerId = sellerId
            )
        }

        val mySellers = withTimeout(FLOW_TIMEOUT_MS) {
            sellerRepo.observeMySellers(uid).first { list -> list.any { it.id == sellerId } }
        }
        assertEquals("En attente", mySellers.first { it.id == sellerId }.status)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                sellerRepo.approveSeller(sellerId)
            }
            fail("Non-admin seller should not be able to self-approve")
        } catch (e: Exception) {
            val fsErr = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsErr)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
