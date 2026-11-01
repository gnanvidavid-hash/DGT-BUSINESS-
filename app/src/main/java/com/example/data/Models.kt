package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Double, // in USD
    val oldPrice: Double? = null,
    val rating: Float,
    val reviewCount: Int,
    val description: String,
    val isFeatured: Boolean = false,
    val isBestSeller: Boolean = false,
    val isNewArrival: Boolean = false,
    val isChinaImport: Boolean = false,
    val stock: Int = 10,
    val sellerName: String = "Nexora Official",
    val imageResName: String // name of drawable or simulated
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val quantity: Int,
    val selectedColor: String? = null,
    val selectedSize: String? = null
)

data class CartItemWithProduct(
    val cartItem: CartItem,
    val product: Product
)

@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey val productId: String
)

@Entity(tableName = "orders")
data class OrderItem(
    @PrimaryKey val id: String,
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val status: String, // "Received", "Processing", "Shipped", "Delivered"
    val itemsJson: String, // List of ordered items (productId, quantity, etc.)
    val paymentMethod: String,
    val shippingAddress: String,
    val trackingProgress: Float = 0.25f // 0.25, 0.5, 0.75, 1.0
)

@Entity(tableName = "chat_messages")
data class SupportMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "coupons")
data class Coupon(
    @PrimaryKey val code: String,
    val discountPercent: Int,
    val description: String,
    val minSpend: Double = 0.0
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "sellers")
data class Seller(
    @PrimaryKey val id: String,
    val businessName: String,
    val sellerName: String,
    val category: String,
    val email: String,
    val isVerified: Boolean = false,
    val logoUrl: String? = null,
    val joinDate: Long = System.currentTimeMillis(),
    val balance: Double = 0.0
)
