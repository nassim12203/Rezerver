package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.security.MessageDigest

// --- Password Utility ---
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// --- Entities ---

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val accountType: String, // "USER" (مستخدم عادي) or "PROVIDER" (صاحب خدمة)
    val state: String,
    val profileImage: String? = null, // Nullable URI string or base64 or drawable resource name
    val isVerified: Boolean = false // Simulates verification code state
)

@Entity(tableName = "services")
data class ServiceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int,
    val ownerName: String,
    val title: String,
    val description: String,
    val type: String, // "HOTEL" (فندق), "CLINIC" (عيادة), "APARTMENT" (شقة)
    val state: String,
    val price: Double,
    val imageResName: String, // name of fallback drawable or custom Uri
    val phone: String
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceId: Int,
    val userId: Int,
    val userName: String,
    val userPhone: String,
    val ownerId: Int,
    val serviceTitle: String,
    val serviceType: String,
    val servicePrice: Double,
    val bookingDate: String,
    val status: String // "PENDING" (قيد الانتظار), "APPROVED" (مقبول), "REJECTED" (مرفوض)
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "serviceId"])
data class Favorite(
    val userId: Int,
    val serviceId: Int
)

// --- DAOs ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSync(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Query("UPDATE users SET passwordHash = :newHash WHERE id = :userId")
    suspend fun updatePassword(userId: Int, newHash: String)

    @Query("UPDATE users SET passwordHash = :newHash WHERE email = :email")
    suspend fun updatePasswordByEmail(email: String, newHash: String)

    @Query("UPDATE users SET passwordHash = :newHash WHERE phone = :phone")
    suspend fun updatePasswordByPhone(phone: String, newHash: String)

    @Query("UPDATE users SET isVerified = 1 WHERE id = :userId")
    suspend fun verifyUser(userId: Int)

    @Query("UPDATE users SET fullName = :name, phone = :phone, email = :email, state = :state, profileImage = :image WHERE id = :userId")
    suspend fun updateUserProfile(userId: Int, name: String, phone: String, email: String, state: String, image: String?)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY id DESC")
    fun getAllServicesFlow(): Flow<List<ServiceItem>>

    @Query("SELECT * FROM services WHERE type = :type ORDER BY id DESC")
    fun getServicesByType(type: String): Flow<List<ServiceItem>>

    @Query("SELECT * FROM services WHERE ownerId = :ownerId ORDER BY id DESC")
    fun getServicesByOwner(ownerId: Int): Flow<List<ServiceItem>>

    @Query("SELECT * FROM services WHERE id = :id LIMIT 1")
    suspend fun getServiceById(id: Int): ServiceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceItem): Long

    @Query("UPDATE services SET title = :title, description = :description, price = :price, state = :state WHERE id = :id")
    suspend fun updateServiceInfo(id: Int, title: String, description: String, price: Double, state: String)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteService(id: Int)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY id DESC")
    fun getBookingsByUser(userId: Int): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE ownerId = :ownerId ORDER BY id DESC")
    fun getBookingsByOwner(ownerId: Int): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: Int, status: String)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBooking(id: Int)
}

@Dao
interface FavoriteDao {
    @Query("SELECT s.* FROM services s INNER JOIN favorites f ON s.id = f.serviceId WHERE f.userId = :userId")
    fun getFavoriteServices(userId: Int): Flow<List<ServiceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE userId = :userId AND serviceId = :serviceId")
    suspend fun removeFavorite(userId: Int, serviceId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND serviceId = :serviceId)")
    fun isFavorite(userId: Int, serviceId: Int): Flow<Boolean>
}

// --- Room Database ---

@Database(entities = [User::class, ServiceItem::class, Booking::class, Favorite::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun serviceDao(): ServiceDao
    abstract fun bookingDao(): BookingDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hajez_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            val userDao = db.userDao()
            val serviceDao = db.serviceDao()

            // 1. Pre-create an Owner user
            val ownerId = userDao.insertUser(
                User(
                    fullName = "سمير الطيب",
                    phone = "0555667788",
                    email = "samir@service.com",
                    passwordHash = hashPassword("owner123"),
                    accountType = "PROVIDER",
                    state = "الجزائر",
                    profileImage = null,
                    isVerified = true
                )
            ).toInt()

            // Pre-create standard user for test
            userDao.insertUser(
                User(
                    fullName = "أمين بوحفص",
                    phone = "0666778899",
                    email = "amin@user.com",
                    passwordHash = hashPassword("user123"),
                    accountType = "USER",
                    state = "وهران",
                    profileImage = null,
                    isVerified = true
                )
            )

            // 2. Add some sample services
            val services = listOf(
                ServiceItem(
                    ownerId = ownerId,
                    ownerName = "سمير الطيب",
                    title = "فندق الهضاب الكبير",
                    description = "فندق فاخر خمس نجوم يقع في وسط مدينة سطيف، يحتوي على خدمات متكاملة، مسبح دافئ وسونا ومطعم يقدم أشهى الأطباق الوطنية والعالمية.",
                    type = "HOTEL",
                    state = "بجاية",
                    price = 12000.0,
                    imageResName = "hotel_grand",
                    phone = "0555667788"
                ),
                ServiceItem(
                    ownerId = ownerId,
                    ownerName = "سمير الطيب",
                    title = "شقة ديلوكس مطلة على البحر",
                    description = "شقة عصرية مفروشة بالكامل تطل مباشرة على الكورنيش والمنظر البحري في وهران الباهية. مجهزة بأحدث المكيفات وشاشات العرض والإنترنت عالي السرعة.",
                    type = "APARTMENT",
                    state = "وهران",
                    price = 8500.0,
                    imageResName = "apt_sea",
                    phone = "0555667788"
                ),
                ServiceItem(
                    ownerId = ownerId,
                    ownerName = "سمير الطيب",
                    title = "شقة هادئة في حي راقٍ",
                    description = "شقة مريحة للإيجار اليومي في قلب العاصمة الجزائر ببلدية دالي براهيم. قريبة من جميع المواصلات ومراكز التسوق الكبرى.",
                    type = "APARTMENT",
                    state = "الجزائر",
                    price = 7000.0,
                    imageResName = "apt_quiet",
                    phone = "0555667788"
                ),
                ServiceItem(
                    ownerId = ownerId,
                    ownerName = "سمير الطيب",
                    title = "عيادة الأمل لجراحة العظام",
                    description = "عيادة مخصصة للكشف والمتابعة في تخصص جراحة العظام والمفاصل والطب الرياضي بإشراف نخبة من الأطباء والمتخصصين.",
                    type = "CLINIC",
                    state = "قسنطينة",
                    price = 3500.0,
                    imageResName = "clinic_ortho",
                    phone = "0555667788"
                ),
                ServiceItem(
                    ownerId = ownerId,
                    ownerName = "سمير الطيب",
                    title = "عيادة الدكتور سليم لطب الأسنان",
                    description = "احصل على ابتسامة أحلامك. خدمات تنظيف، تبييض وتقويم الأسنان بأحدث التقنيات الطبية المعاصرة وبأفضل الأسعار.",
                    type = "CLINIC",
                    state = "الجزائر",
                    price = 2500.0,
                    imageResName = "clinic_dental",
                    phone = "0555667788"
                )
            )

            for (service in services) {
                serviceDao.insertService(service)
            }
        }
    }
}
