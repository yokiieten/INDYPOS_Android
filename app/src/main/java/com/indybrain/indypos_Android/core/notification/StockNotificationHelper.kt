package com.indybrain.indypos_Android.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.indybrain.indypos_Android.MainActivity
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Keep
@Singleton
class StockNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val productRepository: ProductRepository
) {
    companion object {
        private const val CHANNEL_ID = "stock_notification_channel"
        /** Same as iOS `StockNotificationManager.lowStockThreshold` (alerts when qty is 0…5). */
        private const val LOW_STOCK_THRESHOLD = 5
        private val nextNotificationId = AtomicInteger(1001)
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.stock_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.stock_notification_channel_description)
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * หลังสร้างออเดอร์สำเร็จ (ชำระ **เงินสด** หรือ **โอนเงิน**): โหลดรายการสินค้าจาก API (ยอดหลังตัด)
     * แล้วแจ้ง low stock **ทั้งร้าน** (ส่งผลจาก [checkAndNotifyLowStock]: `orderedItems` ว่าง = ไม่หักซ้ำกับยอด API)
     *
     * ต้องรันใน suspend เดียวกับที่ commit การชำระเงิน เพื่อไม่ให้ ViewModel ถูกปิดก่อนดึง API เสร็จ
     */
    suspend fun checkLowStockAfterOrderUsingFreshCatalog() {
        try {
            productRepository.getProductListFromApi().onSuccess { data ->
                checkAndNotifyLowStock(data.products, emptyMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ตรวจสอบสินค้าใกล้หมด / หมด แล้วแจ้งเตือน (สอดคล้องข้อความกับ iOS)
     *
     * @param orderedItems ถ้าสต็อกใน [products] เป็นค่าหลังตัดจาก API แล้ว — ให้ส่ง emptyMap()
     */
    fun checkAndNotifyLowStock(
        products: List<ProductEntity>,
        orderedItems: Map<String, Int> = emptyMap()
    ) {
        val alertingProducts = products.mapNotNull { product ->
            if (product.isStockEnabled != true || product.stockQuantity == null) {
                return@mapNotNull null
            }
            val orderedQuantity = orderedItems[product.id] ?: 0
            val remainingStock = product.stockQuantity!! - orderedQuantity
            val normalizedStock = remainingStock.coerceAtLeast(0)
            when {
                remainingStock <= 0 ->
                    product.copy(stockQuantity = normalizedStock)

                remainingStock <= LOW_STOCK_THRESHOLD ->
                    product.copy(stockQuantity = remainingStock)

                else -> null
            }
        }

        if (alertingProducts.isNotEmpty()) {
            showLowStockNotification(alertingProducts)
        }
    }

    private fun showLowStockNotification(products: List<ProductEntity>) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val productCount = products.size
        val notificationId = nextNotificationId.getAndIncrement()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "stock_management")
            putExtra("type", "low_stock")
            putExtra("productCount", productCount)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            pendingIntentFlags
        )

        val title = context.getString(R.string.stock_notification_title)
        val fallbackName = context.getString(R.string.stock_notification_fallback_product_name)

        val message = if (products.size == 1) {
            val product = products.first()
            val name = product.name.ifBlank { fallbackName }
            val qty = product.stockQuantity ?: 0
            if (qty == 0) {
                context.getString(R.string.stock_notification_out_of_stock, name)
            } else {
                context.getString(R.string.stock_notification_single_product, name, qty)
            }
        } else {
            val outOfStockCount =
                products.count { (it.stockQuantity ?: 0) == 0 }
            val lowStockCount =
                products.count {
                    val q = it.stockQuantity ?: 0
                    q > 0 && q <= LOW_STOCK_THRESHOLD
                }

            when {
                outOfStockCount > 0 && lowStockCount > 0 ->
                    context.getString(
                        R.string.stock_notification_mixed_products,
                        outOfStockCount,
                        lowStockCount
                    )

                outOfStockCount > 0 ->
                    context.getString(
                        R.string.stock_notification_out_of_stock_multiple,
                        outOfStockCount
                    )

                else ->
                    context.getString(
                        R.string.stock_notification_multiple_products,
                        products.size
                    )
            }
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            // App drawable (not android.R.*) so release + shrinkResources keep a valid icon; some devices drop bad icons silently.
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setNumber(productCount)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
