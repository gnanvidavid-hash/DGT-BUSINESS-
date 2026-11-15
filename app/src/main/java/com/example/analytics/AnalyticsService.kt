package com.example.analytics

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import com.example.data.CartItemWithProduct
import com.example.data.Product
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsScreen {
    const val WELCOME = "welcome_screen"
    const val HOME = "home_tab"
    const val SHOP = "shop_tab"
    const val WISHLIST = "wishlist_tab"
    const val CART = "cart_tab"
    const val ACCOUNT = "account_tab"
    const val PRODUCT_DETAIL = "product_detail_screen"
    const val CHECKOUT = "checkout_screen"
    const val ORDER_TRACKING = "order_tracking_screen"
    const val LIVE_CHAT = "live_support_chat_screen"
    const val SELLER_DASHBOARD = "seller_dashboard_screen"
    const val ADMIN_DASHBOARD = "admin_dashboard_screen"
}

interface AnalyticsService {
    fun logScreenView(screenName: String, screenClass: String = "MainActivity")
    fun logProductClick(product: Product, listName: String, currency: String = "USD")
    fun logProductView(product: Product, currency: String)
    fun logAddToCart(product: Product, quantity: Int, currency: String, variant: String? = null)
    fun logRemoveFromCart(product: Product, quantity: Int, currency: String)
    fun logAddToWishlist(product: Product, currency: String)
    fun logSearch(query: String)
    fun logCategorySelected(category: String)
    fun logBeginCheckout(
        items: List<CartItemWithProduct>,
        totalValueUsd: Double,
        currency: String,
        couponCode: String? = null
    )
    fun logCheckoutCompleted(
        transactionId: String,
        items: List<CartItemWithProduct>,
        totalValueUsd: Double,
        shippingFeeUsd: Double,
        currency: String,
        paymentMethod: String,
        shippingMethod: String,
        couponCode: String? = null
    )
    fun logApplyCoupon(couponCode: String, success: Boolean)
    fun logOrderTracked(orderId: String, status: String)
    fun logSupportChatMessageSent()
    fun setUserPreferences(language: String, currency: String, isDarkTheme: Boolean)
}

class FirebaseAnalyticsService(context: Context) : AnalyticsService {

    private val firebaseAnalytics: FirebaseAnalytics? = try {
        FirebaseAnalytics.getInstance(context.applicationContext)
    } catch (e: Exception) {
        Log.w(TAG, "FirebaseAnalytics initialization unavailable: ${e.message}")
        null
    }

    override fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        log(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun logProductClick(product: Product, listName: String, currency: String) {
        val itemBundle = buildItemBundle(product = product, currency = currency)
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_LIST_ID, listName)
            putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, listName)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf<Parcelable>(itemBundle))
        }
        log(FirebaseAnalytics.Event.SELECT_ITEM, bundle)
    }

    override fun logProductView(product: Product, currency: String) {
        val itemBundle = buildItemBundle(product = product, currency = currency)
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, convertPrice(product.price, currency))
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf<Parcelable>(itemBundle))
        }
        log(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
    }

    override fun logAddToCart(
        product: Product,
        quantity: Int,
        currency: String,
        variant: String?
    ) {
        val itemBundle = buildItemBundle(
            product = product,
            currency = currency,
            quantity = quantity,
            variant = variant
        )
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, convertPrice(product.price * quantity, currency))
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf<Parcelable>(itemBundle))
        }
        log(FirebaseAnalytics.Event.ADD_TO_CART, bundle)
    }

    override fun logRemoveFromCart(product: Product, quantity: Int, currency: String) {
        val itemBundle = buildItemBundle(
            product = product,
            currency = currency,
            quantity = quantity
        )
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, convertPrice(product.price * quantity, currency))
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf<Parcelable>(itemBundle))
        }
        log(FirebaseAnalytics.Event.REMOVE_FROM_CART, bundle)
    }

    override fun logAddToWishlist(product: Product, currency: String) {
        val itemBundle = buildItemBundle(product = product, currency = currency, quantity = 1)
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, convertPrice(product.price, currency))
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf<Parcelable>(itemBundle))
        }
        log(FirebaseAnalytics.Event.ADD_TO_WISHLIST, bundle)
    }

    override fun logSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, trimmed)
        }
        log(FirebaseAnalytics.Event.SEARCH, bundle)
    }

    override fun logCategorySelected(category: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "category")
            putString(FirebaseAnalytics.Param.ITEM_ID, category)
        }
        log(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    override fun logBeginCheckout(
        items: List<CartItemWithProduct>,
        totalValueUsd: Double,
        currency: String,
        couponCode: String?
    ) {
        val itemBundles = items.map { entry ->
            val variant = listOfNotNull(entry.cartItem.selectedColor, entry.cartItem.selectedSize)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" / ")
            buildItemBundle(
                product = entry.product,
                currency = currency,
                quantity = entry.cartItem.quantity,
                variant = variant
            )
        }.toTypedArray<Parcelable>()

        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, convertPrice(totalValueUsd, currency))
            if (!couponCode.isNullOrBlank()) {
                putString(FirebaseAnalytics.Param.COUPON, couponCode)
            }
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, itemBundles)
        }
        log(FirebaseAnalytics.Event.BEGIN_CHECKOUT, bundle)
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
        val itemBundles = items.map { entry ->
            val variant = listOfNotNull(entry.cartItem.selectedColor, entry.cartItem.selectedSize)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" / ")
            buildItemBundle(
                product = entry.product,
                currency = currency,
                quantity = entry.cartItem.quantity,
                variant = variant
            )
        }.toTypedArray<Parcelable>()

        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.TRANSACTION_ID, transactionId)
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, convertPrice(totalValueUsd, currency))
            putDouble(FirebaseAnalytics.Param.SHIPPING, convertPrice(shippingFeeUsd, currency))
            putString(FirebaseAnalytics.Param.PAYMENT_TYPE, paymentMethod)
            putString(FirebaseAnalytics.Param.SHIPPING_TIER, shippingMethod)
            if (!couponCode.isNullOrBlank()) {
                putString(FirebaseAnalytics.Param.COUPON, couponCode)
            }
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, itemBundles)
        }
        log(FirebaseAnalytics.Event.PURCHASE, bundle)
    }

    override fun logApplyCoupon(couponCode: String, success: Boolean) {
        val bundle = Bundle().apply {
            putString("coupon_code", couponCode.uppercase())
            putBoolean("success", success)
        }
        log("apply_coupon", bundle)
    }

    override fun logOrderTracked(orderId: String, status: String) {
        val bundle = Bundle().apply {
            putString("order_id", orderId)
            putString("order_status", status)
        }
        log("track_order", bundle)
    }

    override fun logSupportChatMessageSent() {
        log("support_chat_message", Bundle())
    }

    override fun setUserPreferences(language: String, currency: String, isDarkTheme: Boolean) {
        try {
            firebaseAnalytics?.setUserProperty("preferred_language", language)
            firebaseAnalytics?.setUserProperty("preferred_currency", currency)
            firebaseAnalytics?.setUserProperty("theme_mode", if (isDarkTheme) "dark" else "light")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set user properties: ${e.message}")
        }
    }

    private fun buildItemBundle(
        product: Product,
        currency: String,
        quantity: Int? = null,
        variant: String? = null
    ): Bundle {
        return Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, product.id)
            putString(FirebaseAnalytics.Param.ITEM_NAME, product.name)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, product.category)
            putString(FirebaseAnalytics.Param.ITEM_BRAND, product.sellerName)
            putDouble(FirebaseAnalytics.Param.PRICE, convertPrice(product.price, currency))
            if (quantity != null) {
                putLong(FirebaseAnalytics.Param.QUANTITY, quantity.toLong())
            }
            if (!variant.isNullOrBlank()) {
                putString(FirebaseAnalytics.Param.ITEM_VARIANT, variant)
            }
        }
    }

    private fun convertPrice(amountXof: Double, currency: String): Double {
        return amountXof
    }

    private fun log(eventName: String, params: Bundle) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
            Log.d(TAG, "Logged event '$eventName': $params")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log event '$eventName': ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NexoraAnalytics"
    }
}

object NoOpAnalyticsService : AnalyticsService {
    override fun logScreenView(screenName: String, screenClass: String) = Unit
    override fun logProductClick(product: Product, listName: String, currency: String) = Unit
    override fun logProductView(product: Product, currency: String) = Unit
    override fun logAddToCart(product: Product, quantity: Int, currency: String, variant: String?) = Unit
    override fun logRemoveFromCart(product: Product, quantity: Int, currency: String) = Unit
    override fun logAddToWishlist(product: Product, currency: String) = Unit
    override fun logSearch(query: String) = Unit
    override fun logCategorySelected(category: String) = Unit
    override fun logBeginCheckout(
        items: List<CartItemWithProduct>,
        totalValueUsd: Double,
        currency: String,
        couponCode: String?
    ) = Unit
    override fun logCheckoutCompleted(
        transactionId: String,
        items: List<CartItemWithProduct>,
        totalValueUsd: Double,
        shippingFeeUsd: Double,
        currency: String,
        paymentMethod: String,
        shippingMethod: String,
        couponCode: String?
    ) = Unit
    override fun logApplyCoupon(couponCode: String, success: Boolean) = Unit
    override fun logOrderTracked(orderId: String, status: String) = Unit
    override fun logSupportChatMessageSent() = Unit
    override fun setUserPreferences(language: String, currency: String, isDarkTheme: Boolean) = Unit
}
