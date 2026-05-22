package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Booking
import com.example.data.BookingRepository
import com.example.data.LoginResult
import com.example.data.RegisterResult
import com.example.data.ServiceItem
import com.example.data.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Screen identifiers
sealed interface Screen {
    object Login : Screen
    object SignUp : Screen
    object ForgotPassword : Screen
    data class Verification(val userId: Int, val contact: String) : Screen
    object ClientDashboard : Screen
    object OwnerDashboard : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val bookingRepository = BookingRepository(db)

    // Current navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Current logged-in user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // UI Toast Messages / Events
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // --- Search & Filters for Standard User ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "HOTEL", "CLINIC", "APARTMENT"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedStateFilter = MutableStateFlow("كل الولايات")
    val selectedStateFilter: StateFlow<String> = _selectedStateFilter.asStateFlow()

    // Realtime Services list based on filters
    private val _allServices = bookingRepository.getAllServices()
    val filteredServices = MutableStateFlow<List<ServiceItem>>(emptyList())

    // Bookings and Favorites for current user
    val currentUserBookings: StateFlow<List<Booking>> = _currentUser.flatMapLatest { user ->
        user?.let { bookingRepository.getBookingsForUser(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentOwnerBookings: StateFlow<List<Booking>> = _currentUser.flatMapLatest { user ->
        user?.let { bookingRepository.getBookingsForOwner(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserFavorites: StateFlow<List<ServiceItem>> = _currentUser.flatMapLatest { user ->
        user?.let { bookingRepository.getFavoritesForUser(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Owner specific services
    val currentOwnerServices: StateFlow<List<ServiceItem>> = _currentUser.flatMapLatest { user ->
        user?.let { bookingRepository.getServicesByOwner(it.id) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe and update filtered services
        viewModelScope.launch {
            _allServices.collect { services ->
                updateFilteredList(services)
            }
        }
        viewModelScope.launch {
            _searchQuery.collect { _ -> updateFilteredAndCurrentList() }
        }
        viewModelScope.launch {
            _selectedTypeFilter.collect { _ -> updateFilteredAndCurrentList() }
        }
        viewModelScope.launch {
            _selectedStateFilter.collect { _ -> updateFilteredAndCurrentList() }
        }
    }

    private fun updateFilteredList(services: List<ServiceItem>) {
        val query = _searchQuery.value.trim().lowercase()
        val type = _selectedTypeFilter.value
        val state = _selectedStateFilter.value

        filteredServices.value = services.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.title.lowercase().contains(query) ||
                    item.description.lowercase().contains(query)
            val matchesType = type == "ALL" || item.type == type
            val matchesState = state == "كل الولايات" || item.state == state

            matchesQuery && matchesType && matchesState
        }
    }

    private fun updateFilteredAndCurrentList() {
        viewModelScope.launch {
            _allServices.collect { services ->
                updateFilteredList(services)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }

    // --- Actions ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setStateFilter(state: String) {
        _selectedStateFilter.value = state
    }

    // Sign Up action
    fun signUp(
        fullName: String,
        phone: String,
        email: String,
        passwordPlain: String,
        accountType: String,
        state: String,
        profileImage: String?
    ) {
        viewModelScope.launch {
            val result = bookingRepository.registerUser(
                fullName = fullName,
                phone = phone,
                email = email,
                passwordPlain = passwordPlain,
                accountType = accountType,
                state = state,
                profileImage = profileImage
            )
            when (result) {
                is RegisterResult.Success -> {
                    showToast("تم إنشاء الحساب بنجاح! الرجاء تأكيد رقم الهاتف أو البريد.")
                    // Go to verification screen
                    navigateTo(Screen.Verification(result.userId, email))
                }
                is RegisterResult.ErrorEmailExists -> {
                    showToast("خطأ: البريد الإلكتروني مسجل مسبقًا.")
                }
                is RegisterResult.ErrorPhoneExists -> {
                    showToast("خطأ: رقم الهاتف مسجل مسبقًا.")
                }
            }
        }
    }

    // Complete verification simulation
    fun verifyUserCode(userId: Int, code: String) {
        viewModelScope.launch {
            if (code == "1234" || code == "9999" || code == "4321") {
                bookingRepository.verifyUserProfile(userId)
                showToast("تم تفعيل حسابك بنجاح! يمكنك الآن تسجيل الدخول.")
                navigateTo(Screen.Login)
            } else {
                showToast("الكود المدخل غير صحيح. جرب 1234!")
            }
        }
    }

    // Login action
    fun login(emailOrPhone: String, passwordPlain: String) {
        viewModelScope.launch {
            val result = bookingRepository.loginUser(emailOrPhone, passwordPlain)
            when (result) {
                is LoginResult.Success -> {
                    val user = result.user
                    if (!user.isVerified) {
                        showToast("الرجاء التحقق من الحساب أولًا.")
                        navigateTo(Screen.Verification(user.id, user.email))
                    } else {
                        _currentUser.value = user
                        showToast("مرحبًا بك، ${user.fullName}!")
                        if (user.accountType == "PROVIDER") {
                            navigateTo(Screen.OwnerDashboard)
                        } else {
                            navigateTo(Screen.ClientDashboard)
                        }
                    }
                }
                is LoginResult.ErrorNotFound -> {
                    showToast("خطأ: الحساب غير موجود في قاعدة البيانات.")
                }
                is LoginResult.ErrorIncorrectPassword -> {
                    showToast("خطأ: كلمة المرور المدخلة غير صحيحة.")
                }
            }
        }
    }

    // Forgot / Recover Password action
    fun recoverPassword(contact: String, newPasswordPlain: String) {
        viewModelScope.launch {
            val user = bookingRepository.getUserByEmailOrPhone(contact)
            if (user == null) {
                showToast("خطأ: لم يتم العثور على أي حساب مسجل بهذا الاتصال.")
            } else {
                val success = bookingRepository.updatePasswordByContact(contact, newPasswordPlain)
                if (success) {
                    showToast("تم تحديث كلمة المرور بنجاح! قم بتسجيل الدخول الآن.")
                    navigateTo(Screen.Login)
                } else {
                    showToast("فشل تحديث كلمة المرور.")
                }
            }
        }
    }

    // Toggle Favorites
    fun toggleFavorite(serviceId: Int, isFav: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            bookingRepository.toggleFavorite(user.id, serviceId, isFav)
        }
    }

    // Book Service
    fun createBooking(service: ServiceItem, date: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (date.isEmpty()) {
                showToast("يرجى اختيار تاريخ الحجز.")
                return@launch
            }
            bookingRepository.bookExistingService(
                serviceId = service.id,
                userId = user.id,
                userName = user.fullName,
                userPhone = user.phone,
                ownerId = service.ownerId,
                serviceTitle = service.title,
                serviceType = service.type,
                servicePrice = service.price,
                bookingDate = date
            )
            showToast("تم إرسال طلب الحجز بنجاح! في انتظار موافقة صاحب الخدمة.")
        }
    }

    // Cancel Booking
    fun cancelBooking(bookingId: Int) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId)
            showToast("تم إلغاء الحجز بنجاح.")
        }
    }

    // Provider adds service
    fun addNewService(
        title: String,
        description: String,
        type: String,
        state: String,
        priceStr: String,
        phone: String,
        imageName: String
    ) {
        val user = _currentUser.value ?: return
        val price = priceStr.toDoubleOrNull() ?: 0.0
        if (title.isEmpty() || description.isEmpty() || price <= 0.0) {
            showToast("يرجى إكمال جميع الحقول بنجاح وبسعر صحيح.")
            return
        }
        viewModelScope.launch {
            bookingRepository.addService(
                ownerId = user.id,
                ownerName = user.fullName,
                title = title,
                description = description,
                type = type,
                state = state,
                price = price,
                imageResName = imageName,
                phone = if (phone.isEmpty()) user.phone else phone
            )
            showToast("تمت إضافة الخدمة ($title) بنجاح!")
        }
    }

    // Provider updates service details
    fun updateServiceDetails(serviceId: Int, title: String, description: String, priceStr: String, state: String) {
        val price = priceStr.toDoubleOrNull() ?: 0.0
        if (title.isEmpty() || description.isEmpty() || price <= 0.0) {
            showToast("يرجى إدخال قيم صالحة.")
            return
        }
        viewModelScope.launch {
            bookingRepository.updateService(serviceId, title, description, price, state)
            showToast("تم تعديل معلومات الخدمة بنجاح!")
        }
    }

    // Provider deletes service
    fun deleteService(serviceId: Int) {
        viewModelScope.launch {
            bookingRepository.deleteService(serviceId)
            showToast("تم حذف الخدمة بنجاح.")
        }
    }

    // Approve/Reject Booking requests
    fun updateBookingStatus(bookingId: Int, accept: Boolean) {
        viewModelScope.launch {
            val status = if (accept) "APPROVED" else "REJECTED"
            bookingRepository.updateBookingStatus(bookingId, status)
            showToast(if (accept) "تم قبول الحجز بنجاح!" else "تم رفض الحجز.")
        }
    }

    // Update profile detail
    fun updateProfile(name: String, phone: String, email: String, state: String, image: String?) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            bookingRepository.updateUserProfile(user.id, name, phone, email, state, image)
            // Reload user
            val updatedUser = bookingRepository.getUserProfileSync(user.id)
            _currentUser.value = updatedUser
            showToast("تم تحديث الملف الشخصي بنجاح.")
        }
    }

    // Logout Action
    fun logout() {
        _currentUser.value = null
        navigateTo(Screen.Login)
        showToast("تم تسجيل الخروج.")
    }
}
