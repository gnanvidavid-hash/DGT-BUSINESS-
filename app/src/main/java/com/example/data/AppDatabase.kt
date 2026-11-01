package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    @Query("SELECT * FROM products WHERE category = :category")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getCartItemsFlow(): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items")
    suspend fun getCartItems(): List<CartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItem)

    @Update
    suspend fun updateCartItem(cartItem: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items")
    fun getWishlistItemsFlow(): Flow<List<WishlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItem)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun deleteWishlistItem(productId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    suspend fun isInWishlist(productId: String): Boolean
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY date DESC")
    fun getAllOrdersFlow(): Flow<List<OrderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderItem)

    @Update
    suspend fun updateOrder(order: OrderItem)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderItem?
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<SupportMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SupportMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons")
    fun getAllCouponsFlow(): Flow<List<Coupon>>

    @Query("SELECT * FROM coupons WHERE code = :code")
    suspend fun getCouponByCode(code: String): Coupon?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupons(coupons: List<Coupon>)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY date DESC")
    fun getReviewsForProductFlow(productId: String): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)
}

@Dao
interface SellerDao {
    @Query("SELECT * FROM sellers")
    fun getAllSellersFlow(): Flow<List<Seller>>

    @Query("SELECT * FROM sellers WHERE id = :sellerId")
    suspend fun getSellerById(sellerId: String): Seller?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeller(seller: Seller)

    @Update
    suspend fun updateSeller(seller: Seller)
}

@Database(
    entities = [
        Product::class,
        CartItem::class,
        WishlistItem::class,
        OrderItem::class,
        SupportMessage::class,
        Coupon::class,
        Review::class,
        Seller::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao
    abstract fun chatDao(): ChatDao
    abstract fun couponDao(): CouponDao
    abstract fun reviewDao(): ReviewDao
    abstract fun sellerDao(): SellerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexora_market_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
