package com.indybrain.indypos_Android.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.indybrain.indypos_Android.MainActivity
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "stock_notification_channel"
        private const val CHANNEL_NAME = "แจ้งเตือนสินค้าใกล้หมด"
        private const val NOTIFICATION_ID = 1001
        private const val LOW_STOCK_THRESHOLD = 10 // จำนวนสินค้าที่ถือว่าใกล้หมด (สามารถปรับได้)
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * สร้าง notification channel สำหรับ Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "แจ้งเตือนเมื่อสินค้าใกล้หมด"
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * ตรวจสอบสินค้าใกล้หมดและแสดง notification
     * @param products รายการสินค้า (หรือย่อยเฉพาะที่เกี่ยวข้อง)
     * @param orderedItems Map ของ productId -> จำนวนที่เพิ่งสั่ง — ใช้หักจาก [ProductEntity.stockQuantity] เมื่อสต็อกในรายการยังเป็นค่าก่อนสั่ง (เช่น จาก Room)
     *  ถ้า [products] มาจาก API หลังสั่งซื้อแล้วและสต็อกเป็นค่าหลังตัดบนเซิร์ฟเวอร์แล้ว ให้ส่ง `emptyMap()` เพื่อไม่หักซ้ำ
     */
    fun checkAndNotifyLowStock(
        products: List<ProductEntity>,
        orderedItems: Map<String, Int> = emptyMap()
    ) {
        val lowStockProducts = products.mapNotNull { product ->
            // ตรวจสอบเฉพาะสินค้าที่เปิดใช้งาน stock tracking
            if (product.isStockEnabled != true || product.stockQuantity == null) {
                return@mapNotNull null
            }
            
            // คำนวณ stock หลังจากหักจำนวนที่สั่งไปแล้ว
            val orderedQuantity = orderedItems[product.id] ?: 0
            val remainingStock = product.stockQuantity!! - orderedQuantity
            
            // ตรวจสอบว่าใกล้หมดหรือไม่ (หลังจากหักจำนวนที่สั่งไปแล้ว)
            if (remainingStock <= LOW_STOCK_THRESHOLD && remainingStock > 0) {
                // สร้าง ProductEntity ใหม่ที่มี stock ที่อัพเดทแล้ว
                product.copy(stockQuantity = remainingStock)
            } else {
                null
            }
        }
        
        if (lowStockProducts.isNotEmpty()) {
            showLowStockNotification(lowStockProducts)
        }
    }
    
    /**
     * แสดง notification สำหรับสินค้าใกล้หมด
     */
    private fun showLowStockNotification(lowStockProducts: List<ProductEntity>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // สร้าง intent เพื่อเปิด MainActivity และ navigate ไปที่หน้า Stock Management
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // เพิ่ม extra เพื่อบอก MainActivity ว่าให้ navigate ไปที่หน้า Stock Management
            putExtra("navigate_to", "stock_management")
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) requires FLAG_IMMUTABLE
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            // Android 11 and below
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )
        
        // สร้างข้อความ notification
        val title = if (lowStockProducts.size == 1) {
            "สินค้าใกล้หมด: ${lowStockProducts.first().name}"
        } else {
            "มีสินค้า ${lowStockProducts.size} รายการใกล้หมด"
        }
        
        val message = if (lowStockProducts.size == 1) {
            "เหลือเพียง ${lowStockProducts.first().stockQuantity} ชิ้น"
        } else {
            val productNames = lowStockProducts.take(3).joinToString(", ") { 
                "${it.name} (${it.stockQuantity} ชิ้น)"
            }
            val moreText = if (lowStockProducts.size > 3) {
                " และอีก ${lowStockProducts.size - 3} รายการ"
            } else {
                ""
            }
            "$productNames$moreText"
        }
        
        // NotificationCompat.Builder จะจัดการ channel ID อัตโนมัติสำหรับ Android ต่ำกว่า 8.0
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        // สำหรับ Android ต่ำกว่า 8.0 ต้องใช้ setPriority แทน channel importance
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }
        
        val notification = notificationBuilder.build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
