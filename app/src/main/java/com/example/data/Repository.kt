package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BookingRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val serviceDao = db.serviceDao()
    private val bookingDao = db.bookingDao()
    private val favoriteDao = db.favoriteDao()

    // --- Authentication & Users ---

    suspend fun registerUser(
        fullName: String,
        phone: String,
        email: String,
        passwordPlain: String,
        accountType: String,
        state: String,
        profileImage: String? = null
    ): RegisterResult {
        // Validate duplicates
        val existingEmail = userDao.getUserByEmail(email)
        if (existingEmail != null) {
            return RegisterResult.ErrorEmailExists
        }

        val existingPhone = userDao.getUserByPhone(phone)
        if (existingPhone != null) {
            return RegisterResult.ErrorPhoneExists
        }

        // Encryption of password (MANDATORY: تشفير كلمات المرور)
        val hashed = hashPassword(passwordPlain)
        val user = User(
            fullName = fullName,
            phone = phone,
            email = email,
            passwordHash = hashed,
            accountType = accountType,
            state = state,
            profileImage = profileImage,
            isVerified = false // Needs verification (MANDATORY: تأكيد البريد أو الهاتف)
        )

        val id = userDao.insertUser(user)
        return RegisterResult.Success(id.toInt())
    }

    suspend fun loginUser(emailOrPhone: String, passwordPlain: String): LoginResult {
        val user = if (emailOrPhone.contains("@")) {
            userDao.getUserByEmail(emailOrPhone)
        } else {
            userDao.getUserByPhone(emailOrPhone)
        }

        if (user == null) {
            return LoginResult.ErrorNotFound
        }

        val hashed = hashPassword(passwordPlain)
        if (user.passwordHash != hashed) {
            return LoginResult.ErrorIncorrectPassword
        }

        return LoginResult.Success(user)
    }

    fun getUserById(userId: Int): Flow<User?> = userDao.getUserById(userId)

    suspend fun getUserProfileSync(userId: Int): User? = userDao.getUserByIdSync(userId)

    suspend fun verifyUserProfile(userId: Int) {
        userDao.verifyUser(userId)
    }

    suspend fun updateUserProfile(
        userId: Int,
        name: String,
        phone: String,
        email: String,
        state: String,
        image: String?
    ) {
        userDao.updateUserProfile(userId, name, phone, email, state, image)
    }

    // --- Reset/Forgot Password (MANDATORY: استرجاع كلمة المرور) ---

    suspend fun getUserByEmailOrPhone(input: String): User? {
        return if (input.contains("@")) {
            userDao.getUserByEmail(input)
        } else {
            userDao.getUserByPhone(input)
        }
    }

    suspend fun updatePasswordByContact(contact: String, newPasswordPlain: String): Boolean {
        val hashed = hashPassword(newPasswordPlain)
        return if (contact.contains("@")) {
            val u = userDao.getUserByEmail(contact)
            if (u != null) {
                userDao.updatePasswordByEmail(contact, hashed)
                true
            } else false
        } else {
            val u = userDao.getUserByPhone(contact)
            if (u != null) {
                userDao.updatePasswordByPhone(contact, hashed)
                true
            } else false
        }
    }

    // --- Services (MANDATORY: إضافة وتعديل وعرض الخدمات) ---

    fun getAllServices(): Flow<List<ServiceItem>> = serviceDao.getAllServicesFlow()

    fun getServicesByType(type: String): Flow<List<ServiceItem>> = serviceDao.getServicesByType(type)

    fun getServicesByOwner(ownerId: Int): Flow<List<ServiceItem>> = serviceDao.getServicesByOwner(ownerId)

    suspend fun getServiceById(id: Int): ServiceItem? = serviceDao.getServiceById(id)

    suspend fun addService(
        ownerId: Int,
        ownerName: String,
        title: String,
        description: String,
        type: String,
        state: String,
        price: Double,
        imageResName: String,
        phone: String
    ): Long {
        val service = ServiceItem(
            ownerId = ownerId,
            ownerName = ownerName,
            title = title,
            description = description,
            type = type,
            state = state,
            price = price,
            imageResName = imageResName,
            phone = phone
        )
        return serviceDao.insertService(service)
    }

    suspend fun updateService(
        serviceId: Int,
        title: String,
        description: String,
        price: Double,
        state: String
    ) {
        serviceDao.updateServiceInfo(serviceId, title, description, price, state)
    }

    suspend fun deleteService(serviceId: Int) {
        serviceDao.deleteService(serviceId)
    }

    // --- Bookings (MANDATORY: لحجز الخدمات وإدارتها) ---

    fun getBookingsForUser(userId: Int): Flow<List<Booking>> = bookingDao.getBookingsByUser(userId)

    fun getBookingsForOwner(ownerId: Int): Flow<List<Booking>> = bookingDao.getBookingsByOwner(ownerId)

    suspend fun bookExistingService(
        serviceId: Int,
        userId: Int,
        userName: String,
        userPhone: String,
        ownerId: Int,
        serviceTitle: String,
        serviceType: String,
        servicePrice: Double,
        bookingDate: String
    ): Long {
        val newBooking = Booking(
            serviceId = serviceId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            ownerId = ownerId,
            serviceTitle = serviceTitle,
            serviceType = serviceType,
            servicePrice = servicePrice,
            bookingDate = bookingDate,
            status = "PENDING"
        )
        return bookingDao.insertBooking(newBooking)
    }

    suspend fun updateBookingStatus(bookingId: Int, status: String) {
        bookingDao.updateBookingStatus(bookingId, status)
    }

    suspend fun cancelBooking(bookingId: Int) {
        bookingDao.deleteBooking(bookingId)
    }

    // --- Favorites (MANDATORY: الأماكن المفضلة) ---

    fun getFavoritesForUser(userId: Int): Flow<List<ServiceItem>> = favoriteDao.getFavoriteServices(userId)

    suspend fun toggleFavorite(userId: Int, serviceId: Int, shouldBeFav: Boolean) {
        if (shouldBeFav) {
            favoriteDao.addFavorite(Favorite(userId, serviceId))
        } else {
            favoriteDao.removeFavorite(userId, serviceId)
        }
    }

    fun isFavorite(userId: Int, serviceId: Int): Flow<Boolean> = favoriteDao.isFavorite(userId, serviceId)
}

// Result Wrappers
sealed interface RegisterResult {
    data class Success(val userId: Int) : RegisterResult
    object ErrorEmailExists : RegisterResult
    object ErrorPhoneExists : RegisterResult
}

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    object ErrorNotFound : LoginResult
    object ErrorIncorrectPassword : LoginResult
}
