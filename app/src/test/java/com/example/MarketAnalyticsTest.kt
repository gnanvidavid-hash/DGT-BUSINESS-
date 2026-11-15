package com.example

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.analytics.AnalyticsScreen
import com.example.analytics.AnalyticsService
import com.example.analytics.FirebaseAnalyticsService
import com.example.data.AppDatabase
import com.example.data.CartItemWithProduct
import com.example.data.NexoraRepository
import com.example.data.Product
import com.example.viewmodel.MarketViewModel
import com.example.viewmodel.MarketViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarketAnalyticsTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: NexoraRepository
    private lateinit var fakeAnalytics: RecordingAnalyticsService
    private lateinit var viewModelStore: ViewModelStore
    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleProduct = Product(
        id = "p-test-1",
        name = "Phantom X3 Pro 5G",
        category = "Smartphones",
        price = 450.0,
        rating = 4.8f,
        reviewCount = 42,
        description = "Dual SIM 5G smartphone",
        sellerName = "Nexora Official",
        imageResName = "img_phone"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NexoraRepository(database)
        fakeAnalytics = RecordingAnalyticsService()
        viewModelStore = ViewModelStore()
    }

    @After
    fun teardown() {
        viewModelStore.clear()
        database.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MarketViewModel {
        val factory = MarketViewModelFactory(repository, fakeAnalytics)
        return ViewModelProvider(viewModelStore, factory)[MarketViewModel::class.java]
    }

    @Test
    fun `firebase analytics service handles calls safely without crashing`() {
        val service = FirebaseAnalyticsService(context)
        service.logScreenView(AnalyticsScreen.HOME)
        service.logProductClick(sampleProduct, "home_featured", "XOF")
        service.logProductView(sampleProduct, "XOF")
        service.logAddToCart(sampleProduct, 1, "XOF", "Bleu Nuit")
        service.logSearch("Phantom")
        service.setUserPreferences("FR", "XOF", false)
    }

    @Test
    fun `viewmodel logs screen views and product click and detail view`() = runBlocking {
        val viewModel = createViewModel()

        viewModel.trackScreenView(AnalyticsScreen.WELCOME)
        viewModel.trackScreenView(AnalyticsScreen.HOME, "MainMarketplaceScreen")
        viewModel.trackProductClick(sampleProduct, "home_featured")
        viewModel.selectProduct(sampleProduct)

        assertEquals(
            listOf(AnalyticsScreen.WELCOME to "MainActivity", AnalyticsScreen.HOME to "MainMarketplaceScreen"),
            fakeAnalytics.screenViews
        )
        assertEquals(1, fakeAnalytics.productClicks.size)
        assertEquals("p-test-1", fakeAnalytics.productClicks.first().first.id)
        assertEquals("home_featured", fakeAnalytics.productClicks.first().second)

        assertEquals(1, fakeAnalytics.productViews.size)
        assertEquals("p-test-1", fakeAnalytics.productViews.first().id)
    }

    @Test
    fun `viewmodel logs add to cart begin checkout and checkout completion`() = runBlocking {
        database.productDao().insertProduct(sampleProduct)
        val viewModel = createViewModel()

        viewModel.addToCart(sampleProduct, quantity = 2, color = "Bleu Nuit", size = "Standard")
        withTimeout(5_000) {
            viewModel.cartItems.first { it.isNotEmpty() }
        }
        viewModel.beginCheckout()
        viewModel.placeOrder()
        val pendingOrder = withTimeout(5_000) {
            viewModel.pendingFedaPayOrder.first { it != null }
        }

        assertEquals(1, fakeAnalytics.addToCartEvents.size)
        assertEquals(2, fakeAnalytics.addToCartEvents.first().second)

        assertEquals(1, fakeAnalytics.beginCheckoutEvents.size)
        assertEquals(1, fakeAnalytics.checkoutCompletedEvents.size)

        val completed = fakeAnalytics.checkoutCompletedEvents.first()
        assertTrue(completed.transactionId.startsWith("NEX-"))
        assertEquals("XOF", completed.currency)
        assertEquals("FedaPay", completed.paymentMethod)
        assertEquals("Livraison Express à Domicile", completed.shippingMethod)
        assertEquals(1, completed.items.size)
        assertEquals(pendingOrder?.id, completed.transactionId)
    }

    private data class CompletedOrderRecord(
        val transactionId: String,
        val items: List<CartItemWithProduct>,
        val totalValueUsd: Double,
        val currency: String,
        val paymentMethod: String,
        val shippingMethod: String
    )

    private class RecordingAnalyticsService : AnalyticsService {
        val screenViews = mutableListOf<Pair<String, String>>()
        val productClicks = mutableListOf<Pair<Product, String>>()
        val productViews = mutableListOf<Product>()
        val addToCartEvents = mutableListOf<Pair<Product, Int>>()
        val beginCheckoutEvents = mutableListOf<List<CartItemWithProduct>>()
        val checkoutCompletedEvents = mutableListOf<CompletedOrderRecord>()

        override fun logScreenView(screenName: String, screenClass: String) {
            screenViews.add(screenName to screenClass)
        }

        override fun logProductClick(product: Product, listName: String, currency: String) {
            productClicks.add(product to listName)
        }

        override fun logProductView(product: Product, currency: String) {
            productViews.add(product)
        }

        override fun logAddToCart(product: Product, quantity: Int, currency: String, variant: String?) {
            addToCartEvents.add(product to quantity)
        }

        override fun logRemoveFromCart(product: Product, quantity: Int, currency: String) = Unit
        override fun logAddToWishlist(product: Product, currency: String) = Unit
        override fun logSearch(query: String) = Unit
        override fun logCategorySelected(category: String) = Unit

        override fun logBeginCheckout(
            items: List<CartItemWithProduct>,
            totalValueUsd: Double,
            currency: String,
            couponCode: String?
        ) {
            beginCheckoutEvents.add(items)
        }

        override fun logCheckoutCompleted(
            transactionId: String,
            items: List<CartItemWithProduct>,
            totalValueUsd: Double,
            shippingFeeUsd: Double,
            currency: String,
            paymentMethod: String,
            shippingMethod: String,
            couponCode: String?
        ) {
            checkoutCompletedEvents.add(
                CompletedOrderRecord(
                    transactionId = transactionId,
                    items = items,
                    totalValueUsd = totalValueUsd,
                    currency = currency,
                    paymentMethod = paymentMethod,
                    shippingMethod = shippingMethod
                )
            )
        }

        override fun logApplyCoupon(couponCode: String, success: Boolean) = Unit
        override fun logOrderTracked(orderId: String, status: String) = Unit
        override fun logSupportChatMessageSent() = Unit
        override fun setUserPreferences(language: String, currency: String, isDarkTheme: Boolean) = Unit
    }
}
