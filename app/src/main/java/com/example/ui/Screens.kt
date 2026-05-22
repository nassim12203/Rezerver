package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Booking
import com.example.data.ServiceItem
import com.example.data.User

// Preset Avatars for custom selection without real storage errors
val PRESET_AVATARS = listOf(
    Pair("👨‍💼", "أمين (شاب)"),
    Pair("👩‍💼", "فاطمة (فتاة)"),
    Pair("🧑‍⚕️", "طبيب (عيادة)"),
    Pair("🏨", "مدير (فندق)"),
    Pair("🏡", "مالك (شقة)"),
    Pair("👤", "افتراضي")
)

// preset illustrative service images mapping to emojis for gorgeous presentation
val SERVICE_EMOJIS = mapOf(
    "hotel_grand" to "🏨",
    "apt_sea" to "🏖️",
    "apt_quiet" to "🏡",
    "clinic_ortho" to "🩺",
    "clinic_dental" to "🦷",
    "default_hotel" to "🏨",
    "default_clinic" to "🏥",
    "default_apt" to "🏢"
)

// List of Algerian Provinces (الولايات)
val ALGERIAN_STATES = listOf(
    "الجزائر",
    "وهران",
    "قسنطينة",
    "سطيف",
    "تلمسان",
    "بجاية",
    "عنابة",
    "غرداية"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationWrapper(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Force Right-to-Left (RTL) Layout for beautiful Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Login -> LoginScreen(viewModel)
                is Screen.SignUp -> SignUpScreen(viewModel)
                is Screen.ForgotPassword -> ForgotPasswordScreen(viewModel)
                is Screen.Verification -> VerificationScreen(
                    viewModel = viewModel,
                    userId = screen.userId,
                    contact = screen.contact
                )
                is Screen.ClientDashboard -> ClientDashboardScreen(viewModel, currentUser)
                is Screen.OwnerDashboard -> OwnerDashboardScreen(viewModel, currentUser)
            }
        }
    }
}

// ==========================================
// 1. SIGN UP SCREEN (صفحة التسجيل)
// ==========================================
@Composable
fun SignUpScreen(viewModel: MainViewModel) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var chosenState by remember { mutableStateOf(ALGERIAN_STATES.first()) }
    var accountType by remember { mutableStateOf("USER") } // "USER" or "PROVIDER"

    var selectedAvatarIndex by remember { mutableStateOf(0) }
    var showStateDropdown by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validations
    val isEmailValid = email.isEmpty() || email.contains("@") && email.contains(".")
    val isPhoneValid = phone.isEmpty() || (phone.length >= 9 && phone.all { it.isDigit() })
    val isPasswordMatch = confirmPassword.isEmpty() || password == confirmPassword
    val isFormValid = fullName.isNotBlank() &&
            phone.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            password.length >= 6 &&
            password == confirmPassword &&
            isEmailValid &&
            isPhoneValid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 32.dp)
    ) {
        item {
            Text(
                text = "إنشاء حساب جديد",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "سجل معلوماتك لتتمكن من الحجز أو إدارة خدماتك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Account Type Selector
            Text(
                text = "نوع الحساب",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    onClick = { accountType = "USER" },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (accountType == "USER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (accountType == "USER") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🙋‍♂️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مستخدم عادي",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "للبحث والحجز",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Card(
                    onClick = { accountType = "PROVIDER" },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (accountType == "PROVIDER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (accountType == "PROVIDER") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏨", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "صاحب خدمة",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "فندق، عيادة، شقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar Selector (Optional)
            Text(
                text = "اختر الصورة الشخصية (اختياري)",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PRESET_AVATARS.size) { index ->
                    val pair = PRESET_AVATARS[index]
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedAvatarIndex == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedAvatarIndex = index }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = pair.first, fontSize = 28.sp)
                        if (selectedAvatarIndex == index) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Form Fields
        item {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("الاسم الكامل") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_fullname_input"),
                colors = OutlinedTextFieldDefaults.colors()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                placeholder = { Text("مثال: 0555112233") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("signup_phone_input"),
                isError = !isPhoneValid,
                supportingText = {
                    if (!isPhoneValid) {
                        Text("رقم الهاتف يجب أن يحتوي على أرقام فقط (9 خانات كأقل تقدير)", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                placeholder = { Text("example@domain.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                isError = !isEmailValid,
                supportingText = {
                    if (!isEmailValid) {
                        Text("تنسيق البريد الإلكتروني غير صالح", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // State (الولاية) Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = chosenState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("الولاية") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showStateDropdown = !showStateDropdown }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { showStateDropdown = true }
                )
                DropdownMenu(
                    expanded = showStateDropdown,
                    onDismissRequest = { showStateDropdown = false },
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                ) {
                    ALGERIAN_STATES.forEach { stateItem ->
                        DropdownMenuItem(
                            text = { Text(stateItem) },
                            onClick = {
                                chosenState = stateItem
                                showStateDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                supportingText = {
                    if (password.isNotEmpty() && password.length < 6) {
                        Text("يجب أن تكون كلمة المرور 6 أحرف على الأقل", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("تأكيد كلمة المرور") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val icon = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password_input"),
                isError = !isPasswordMatch,
                supportingText = {
                    if (!isPasswordMatch) {
                        Text("كلمات المرور غير متطابقة", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        viewModel.signUp(
                            fullName = fullName,
                            phone = phone,
                            email = email,
                            passwordPlain = password,
                            accountType = accountType,
                            state = chosenState,
                            profileImage = PRESET_AVATARS[selectedAvatarIndex].first
                        )
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("signup_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إنشاء حساب جديد", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("لديك حساب بالفعل؟ ")
                Text(
                    text = "تسجيل الدخول",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.navigateTo(Screen.Login) }
                )
            }
        }
    }
}

// ==========================================
// 2. LOGIN SCREEN (صفحة تسجيل الدخول)
// ==========================================
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isInputValid = emailOrPhone.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo container/branding
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("حاجز", fontSize = 34.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "مرحبًا بك في تطبيق حاجز",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "تسجيل الدخول للحجز الفوري وإدارة الخدمات",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = emailOrPhone,
            onValueChange = { emailOrPhone = it },
            label = { Text("البريد الإلكتروني أو رقم الهاتف") },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_username_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = null)
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("login_password_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot password Link
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "نسيت كلمة المرور؟",
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.ForgotPassword) }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isInputValid) {
                    viewModel.login(emailOrPhone.trim(), password.trim())
                }
            },
            enabled = isInputValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("تسجيل الدخول", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Future Google/Facebook login placeholder
        Text(
            text = "أو تسجيل الدخول السريع مستقبلاً عبر:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            OutlinedButton(
                onClick = { viewModel.showToast("تسجيل الدخول بـ Google سيتوفر في التحديث القادم!") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌐  ")
                    Text("Google")
                }
            }
            OutlinedButton(
                onClick = { viewModel.showToast("تسجيل الدخول بـ Facebook سيتوفر في التحديث القادم!") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔵  ")
                    Text("Facebook")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.clickable { viewModel.navigateTo(Screen.SignUp) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ليس لديك حساب؟ ")
            Text(
                text = "إنشاء حساب جديد",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// 3. SECURE VERIFICATION CODE SCREEN
// ==========================================
@Composable
fun VerificationScreen(viewModel: MainViewModel, userId: Int, contact: String) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📬", fontSize = 70.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "تأكيد بريدك الإلكتروني أو هاتفك",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "لمنع الحسابات الوهمية، تم إرسال كود تحقق افتراضي إلى:\n$contact",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 4) code = it },
            label = { Text("كود التحقق المتكون من 4 أرقام") },
            placeholder = { Text("مثال: 1234") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .width(200.dp)
                .testTag("verification_code_input"),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ملاحظة للتجربة: أدخل الكود الافتراضي '1234' أو '9999'",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (code.length >= 4) {
                    viewModel.verifyUserCode(userId, code)
                }
            },
            enabled = code.length >= 4,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("verify_submit_button")
        ) {
            Text("تأكيد الحساب ومتابعة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.showToast("تمت إعادة إرسال الكود الافتراضي '1234'") }) {
            Text("إعادة إرسال الكود")
        }
    }
}

// ==========================================
// 4. FORGOT PASSWORD SCREEN (استرجاع كلمة المرور)
// ==========================================
@Composable
fun ForgotPasswordScreen(viewModel: MainViewModel) {
    var emailOrPhone by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: contact request, 2: OTP, 3: New Pass
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    var newPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔑", fontSize = 60.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "استرجاع كلمة المرور",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = step == 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "أدخل بريدك الإلكتروني أو رقم هاتفك المسجل وسنرسل لك كود تحقق لتغيير كلمة المرور.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recovery_contact_input")
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (emailOrPhone.isNotBlank()) {
                            // Simulate sending OTP
                            viewModel.showToast("تم إرسال كود تحقق افتراضي إلى $emailOrPhone")
                            step = 2
                        } else {
                            viewModel.showToast("يرجى إدخال البريد الإلكتروني أو الهاتف أولاً.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("إرسال كود التحقق")
                }
            }
        }

        AnimatedVisibility(visible = step == 2) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "يرجى كتابة كود التحقق الذي وصلك بنجاح.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 4) otpCode = it },
                    label = { Text("كود التحقق") },
                    placeholder = { Text("مثال: 1234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(180.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 20.sp, letterSpacing = 6.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("تلميح تجريبي: أدخل أي 4 أرقام", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (otpCode.length == 4) {
                            step = 3
                        } else {
                            viewModel.showToast("الرجاء إدخال 4 أرقام.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("التحقق من الكود")
                }
            }
        }

        AnimatedVisibility(visible = step == 3) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "اكتب كلمة المرور الجديدة وتأكيدها لإتمام الاسترجاع والتشغيل.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val icon = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(icon, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("recovery_new_password_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("تأكيد كلمة المرور الجديدة") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("recovery_confirm_password_input")
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (newPassword == confirmNewPassword && newPassword.length >= 6) {
                            viewModel.recoverPassword(emailOrPhone.trim(), newPassword.trim())
                        } else {
                            viewModel.showToast("الكلمتان غير متطابقتين أو أقل من 6 أحرف.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("recovery_submit_button")
                ) {
                    Text("تغيير كلمة المرور والتشغيل")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { viewModel.navigateTo(Screen.Login) }) {
            Text("العودة لتسجيل الدخول")
        }
    }
}

// ==========================================
// 5. CLIENT DASHBOARD (حساب مستخدم عادي)
// ==========================================
@Composable
fun ClientDashboardScreen(viewModel: MainViewModel, user: User?) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Browse, 1: Bookings, 2: Favorites, 3: Profile

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = { Text("استكشف", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                    label = { Text("حجوزاتي", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
                    label = { Text("المفضلة", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("ملفي", fontSize = 12.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BrowseServicesTab(viewModel, user)
                1 -> ClientBookingsTab(viewModel, user)
                2 -> ClientFavoritesTab(viewModel, user)
                3 -> ClientProfileTab(viewModel, user)
            }
        }
    }
}

// BROWSE SERVICES SUB-TAB
@Composable
fun BrowseServicesTab(viewModel: MainViewModel, user: User?) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val chosenType by viewModel.selectedTypeFilter.collectAsState()
    val chosenStateFilter by viewModel.selectedStateFilter.collectAsState()
    val services by viewModel.filteredServices.collectAsState()

    var showStateFilterDialog by remember { mutableStateOf(false) }
    var selectedServiceForBooking by remember { mutableStateOf<ServiceItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Welcoming & Profile header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "أهلاً بك، ${user?.fullName ?: "ضيفنا"}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ابحث واحجز فندقك، عيادتك، أو شقتك بنقرة زر",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = user?.profileImage ?: "👤", fontSize = 22.sp)
            }
        }

        // Search panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("ابحث عن فندق، عيادة، شقة...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedIconButton(
                onClick = { showStateFilterDialog = true },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "FilterState",
                    tint = if (chosenStateFilter == "كل الولايات") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Active State filter badge
        if (chosenStateFilter != "كل الولايات") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    label = "الولاية: $chosenStateFilter",
                    onDismiss = { viewModel.setStateFilter("كل الولايات") }
                )
            }
        }

        // Category Horizontal Row Selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                Triple("ALL", "الكل", "🌐"),
                Triple("HOTEL", "فنادق", "🏨"),
                Triple("CLINIC", "عيادات", "🏥"),
                Triple("APARTMENT", "شقق سكنية", "🏢")
            )

            items(categories.size) { index ->
                val (type, name, emoji) = categories[index]
                val isSelected = chosenType == type
                Surface(
                    onClick = { viewModel.setTypeFilter(type) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp,
                    modifier = Modifier.clickable { viewModel.setTypeFilter(type) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emoji, modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Services List Render
        if (services.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لا توجد أي خدمات مطابقة لبحثك حاليًا",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(services) { service ->
                    ServiceRowCard(
                        service = service,
                        viewModel = viewModel,
                        onClickBook = { selectedServiceForBooking = service }
                    )
                }
            }
        }
    }

    // STATE FILTER SELECTION DIALOG
    if (showStateFilterDialog) {
        Dialog(onDismissRequest = { showStateFilterDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "اختر تصفية حسب الولاية",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setStateFilter("كل الولايات")
                                        showStateFilterDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text("كل كلي الولايات 🌐", fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider()
                        }
                        items(ALGERIAN_STATES.size) { index ->
                            val state = ALGERIAN_STATES[index]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setStateFilter(state)
                                        showStateFilterDialog = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(state, fontWeight = FontWeight.Medium)
                            }
                            HorizontalDivider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showStateFilterDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }

    // SERVICE BOOKING ACTION DIALOG
    if (selectedServiceForBooking != null) {
        val service = selectedServiceForBooking!!
        var bookingDate by remember { mutableStateOf("2026-05-23") }
        var showDateWheel by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedServiceForBooking = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تأكيد طلب الحجز",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { selectedServiceForBooking = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Brief item card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = SERVICE_EMOJIS[service.imageResName] ?: "🏢",
                                fontSize = 36.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(service.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("${service.state} • ${service.price} دج", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "اختر تاريخ الحجز:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = bookingDate,
                        onValueChange = { bookingDate = it },
                        label = { Text("التاريخ (السنة-الشهر-اليوم)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ملاحظة: تأكيد الحجز يتطلب موافقة صاحب الخدمة مباشرة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.createBooking(service, bookingDate)
                            selectedServiceForBooking = null
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("إرسال طلب الحجز للولاية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(label: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDismiss() }
            )
        }
    }
}

@Composable
fun ServiceRowCard(
    service: ServiceItem,
    viewModel: MainViewModel,
    onClickBook: () -> Unit
) {
    val userFlow = viewModel.currentUser.collectAsState()
    val isFav by viewModel.bookingRepository.isFavorite(userFlow.value?.id ?: 0, service.id)
        .collectAsState(initial = false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickBook() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer).run { CardDefaults.cardElevation(defaultElevation = 2.dp) }
    ) {
        Column {
            Box {
                // Predefined color placeholder instead of missing images
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = SERVICE_EMOJIS[service.imageResName] ?: "🏢",
                        fontSize = 55.sp
                    )
                }

                // Favorite Toggle Badge
                IconButton(
                    onClick = { viewModel.toggleFavorite(service.id, !isFav) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = if (isFav) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Price tag
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "${service.price} دج",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        service.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val textType = when (service.type) {
                            "HOTEL" -> "فندق"
                            "CLINIC" -> "عيادة"
                            else -> "شقة"
                        }
                        Text(
                            text = textType,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Text(
                        text = "ولاية ${service.state}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onClickBook() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("احجز الآن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// BROWSE CLIENT BOOKINGS SUB-TAB
@Composable
fun ClientBookingsTab(viewModel: MainViewModel, user: User?) {
    val bookings by viewModel.currentUserBookings.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "حجوزاتي وإدارتها",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val upcomingBookings = bookings.filter { it.status != "REJECTED" }
        val pastBookings = bookings.filter { it.status == "REJECTED" }

        Text(
            text = "الحجوزات القادمة والنشطة (${upcomingBookings.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (upcomingBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أي حجوزات قادمة.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(upcomingBookings) { booking ->
                    BookingItemCard(booking = booking, viewModel = viewModel, canCancel = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "الحجوزات المرفوضة والسابقة (${pastBookings.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (pastBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("الأرشيف فارغ.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(0.6f)) {
                items(pastBookings) { booking ->
                    BookingItemCard(booking = booking, viewModel = viewModel, canCancel = false)
                }
            }
        }
    }
}

@Composable
fun BookingItemCard(booking: Booking, viewModel: MainViewModel, canCancel: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val em = when (booking.serviceType) {
                        "HOTEL" -> "🏨"
                        "CLINIC" -> "🩺"
                        else -> "🏢"
                    }
                    Text(em, fontSize = 20.sp, modifier = Modifier.padding(end = 4.dp))
                    Text(booking.serviceTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("تاريخ الحجز: ${booking.bookingDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("تكلفة الخدمة: ${booking.servicePrice} دج", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }

            Column(horizontalAlignment = Alignment.End) {
                // Status tag
                val (color, text) = when (booking.status) {
                    "PENDING" -> Pair(MaterialTheme.colorScheme.secondary, "قيد الانتظار")
                    "APPROVED" -> Pair(Color(0xFF2E7D32), "مقبول")
                    else -> Pair(MaterialTheme.colorScheme.error, "مرفوض")
                }
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, color)
                ) {
                    Text(
                        text = text,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (canCancel && booking.status == "PENDING") {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { viewModel.cancelBooking(booking.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("إلغاء الحجز", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// BROWSE CLIENT FAVORITES SUB-TAB
@Composable
fun ClientFavoritesTab(viewModel: MainViewModel, user: User?) {
    val favorites by viewModel.currentUserFavorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "الأماكن المفضلة لك",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❤️", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("قائمة المفضلة فارغة حالياً.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(favorites) { service ->
                    ServiceRowCard(
                        service = service,
                        viewModel = viewModel,
                        onClickBook = {}
                    )
                }
            }
        }
    }
}

// REGISTERED USER EDIT PROFILE SUB-TAB
@Composable
fun ClientProfileTab(viewModel: MainViewModel, user: User?) {
    var name by remember { mutableStateOf(user?.fullName ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var state by remember { mutableStateOf(user?.state ?: "") }

    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper profile design card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = user?.profileImage ?: "👤", fontSize = 44.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = user?.fullName ?: "",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (user?.accountType == "PROVIDER") "صاحب خدمة معتمد" else "حساب مستخدم للحجوزات",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isEditing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileFieldRow(icon = Icons.Default.Person, label = "الاسم الكامل", value = user?.fullName ?: "")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileFieldRow(icon = Icons.Default.Phone, label = "رقم الهاتف", value = user?.phone ?: "")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileFieldRow(icon = Icons.Default.Email, label = "البريد الإلكتروني", value = user?.email ?: "")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    ProfileFieldRow(icon = Icons.Default.Place, label = "الولاية", value = user?.state ?: "")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل البيانات")
                }
            }
        } else {
            // Edit profile form
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم الكامل") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text("الولاية") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.updateProfile(name, phone, email, state, user?.profileImage)
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("حفظ")
                }
                OutlinedButton(
                    onClick = { isEditing = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الخروج", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ProfileFieldRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 13.sp)
        }
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

// ==========================================
// 6. OWNER DASHBOARD (حساب صاحب خدمة)
// ==========================================
@Composable
fun OwnerDashboardScreen(viewModel: MainViewModel, user: User?) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Services, 1: Bookings, 2: Profile
    var showAddServiceDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text("خدماتي الكبرى", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                    label = { Text("طلبات الحجز", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text("شخصي", fontSize = 11.sp) }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddServiceDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> OwnerServicesTab(viewModel, user)
                1 -> OwnerBookingsTab(viewModel, user)
                2 -> ClientProfileTab(viewModel, user) // Reuse profile tab
            }
        }
    }

    // ADD NEW SERVICE DIALOG BY PROVIDER
    if (showAddServiceDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("HOTEL") }
        var state by remember { mutableStateOf(ALGERIAN_STATES.first()) }
        var priceStr by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf(user?.phone ?: "") }

        var showDialogStateDropdown by remember { mutableStateOf(false) }
        var imageResChoice by remember { mutableStateOf("default_hotel") }

        Dialog(onDismissRequest = { showAddServiceDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("إضافة خدمة جديدة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { showAddServiceDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("اسم فندقك، عيادتك، أو شقتك") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("نوع الخدمة", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                FilterChip(
                                    selected = type == "HOTEL",
                                    onClick = {
                                        type = "HOTEL"
                                        imageResChoice = "default_hotel"
                                    },
                                    label = { Text("فندق") }
                                )
                                FilterChip(
                                    selected = type == "CLINIC",
                                    onClick = {
                                        type = "CLINIC"
                                        imageResChoice = "default_clinic"
                                    },
                                    label = { Text("عيادة") }
                                )
                                FilterChip(
                                    selected = type == "APARTMENT",
                                    onClick = {
                                        type = "APARTMENT"
                                        imageResChoice = "default_apt"
                                    },
                                    label = { Text("شقة") }
                                )
                            }
                        }
                    }

                    item {
                        // Dropdown Algerian States
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = state,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("ولاية الخدمة") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { showDialogStateDropdown = !showDialogStateDropdown }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { showDialogStateDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showDialogStateDropdown,
                                onDismissRequest = { showDialogStateDropdown = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ALGERIAN_STATES.forEach { stateItem ->
                                    DropdownMenuItem(
                                        text = { Text(stateItem) },
                                        onClick = {
                                            state = stateItem
                                            showDialogStateDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("السعر في الليلة أو الحجز (بالدينار DZD)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("هاتف التواصل للخدمة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف مفسر تفصيلي للخدمة والمزايا") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.addNewService(title, description, type, state, priceStr, phone, imageResChoice)
                                showAddServiceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("إضافة الخدمة ونشرها")
                        }
                    }
                }
            }
        }
    }
}

// BROWSE OWNER SERVICES TAB
@Composable
fun OwnerServicesTab(viewModel: MainViewModel, user: User?) {
    val myServices by viewModel.currentOwnerServices.collectAsState()
    var selectedServiceForEditing by remember { mutableStateOf<ServiceItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "إدارة الخدمات الخاصة بك",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "انشر خدماتك الفندقية، طب وصحة، وشقق سكنية للمستخدمين المهتمين بالحجز للولاية",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Services Count
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("إجمالي خدماتك النشطة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${myServices.size} خدمات معلنة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("📈", fontSize = 32.sp)
            }
        }

        if (myServices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📌", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ليس لديك خدمات نشطة حالياً. انقر زر الزائد للإعلان عن أول خدمة!", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                items(myServices) { service ->
                    OwnerServiceCard(
                        service = service,
                        onEdit = { selectedServiceForEditing = service },
                        onDelete = { viewModel.deleteService(service.id) }
                    )
                }
            }
        }
    }

    // EDIT SERVICE DETAILS DIALOG
    if (selectedServiceForEditing != null) {
        val editingService = selectedServiceForEditing!!
        var title by remember { mutableStateOf(editingService.title) }
        var description by remember { mutableStateOf(editingService.description) }
        var priceStr by remember { mutableStateOf(editingService.price.toString()) }
        var state by remember { mutableStateOf(editingService.state) }
        var showEditStateDropdown by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedServiceForEditing = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("تعديل معلومات الإعلان", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الولاية") },
                            trailingIcon = {
                                IconButton(onClick = { showEditStateDropdown = !showEditStateDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { showEditStateDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showEditStateDropdown,
                            onDismissRequest = { showEditStateDropdown = false }
                        ) {
                            ALGERIAN_STATES.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        state = s
                                        showEditStateDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("السعر بالـ DZD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("الوصف") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                viewModel.updateServiceDetails(editingService.id, title, description, priceStr, state)
                                selectedServiceForEditing = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ التعديلات")
                        }
                        OutlinedButton(
                            onClick = { selectedServiceForEditing = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerServiceCard(
    service: ServiceItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(SERVICE_EMOJIS[service.imageResName] ?: "🏢", fontSize = 34.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(service.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("أساسي: ${service.price} دج • ${service.state}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(service.description, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// BROWSE RECEIVED BOOKINGS FROM CUSTOMERS TAB (إدارة طلبات الحجز المستلمة)
@Composable
fun OwnerBookingsTab(viewModel: MainViewModel, user: User?) {
    val receivedBookings by viewModel.currentOwnerBookings.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "طلبات الحجز المستلمة",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "راجع طلبات حجز غرف الفندق، المواعيد الطبية، أو إيجار الشقق للتفعيل اليدوي",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val pendingList = receivedBookings.filter { it.status == "PENDING" }
        val processedList = receivedBookings.filter { it.status != "PENDING" }

        Text(
            text = "طلبات في الانتظار (${pendingList.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (pendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد طلبات حجز معلقة حالياً.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingList) { booking ->
                    ReceivedBookingRequestRow(booking = booking, viewModel = viewModel, isPending = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "الطلبات المعالجة والسابقة (${processedList.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (processedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد طلبات معالجة مسبقاً.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(processedList) { booking ->
                    ReceivedBookingRequestRow(booking = booking, viewModel = viewModel, isPending = false)
                }
            }
        }
    }
}

@Composable
fun ReceivedBookingRequestRow(
    booking: Booking,
    viewModel: MainViewModel,
    isPending: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.serviceTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "العميل: ${booking.userName} • ${booking.userPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }

                // Render badge for date
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = booking.bookingDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.updateBookingStatus(booking.id, true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قبول", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.updateBookingStatus(booking.id, false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفض", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحالة المحدثة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )

                    val (color, label) = if (booking.status == "APPROVED") {
                        Pair(Color(0xFF2E7D32), "مقبول ومؤكد")
                    } else {
                        Pair(MaterialTheme.colorScheme.error, "مرفوض مسبقاً")
                    }

                    Text(
                        text = label,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
