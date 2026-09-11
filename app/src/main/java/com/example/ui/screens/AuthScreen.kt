package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.FirestoreSchema
import com.example.model.UserRole
import com.example.repository.AuthRepository
import com.example.repository.AuthState
import com.example.repository.RegistrationInput
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val authState by authRepository.authState.collectAsState()

    var mobileNumber by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var upiInput by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf(FirestoreSchema.ALLOWED_STATES.first()) }
    var isAge18Plus by remember { mutableStateOf(true) }
    var stateDropdownExpanded by remember { mutableStateOf(false) }

    // Tab state: 0 = Login, 1 = Sign Up
    var authTab by remember { mutableIntStateOf(0) }

    // Validation computations for details
    val isMobileValid = mobileNumber.length == 10
    val isFullNameValid = fullName.trim().length >= 2
    val isEmailValid = emailInput.trim().contains("@") && emailInput.trim().contains(".") && emailInput.trim().length >= 5
    val isUpiValid = upiInput.trim().contains("@") && upiInput.trim().length >= 4 && !upiInput.trim().startsWith("@") && !upiInput.trim().endsWith("@")
    val isStateValid = selectedState.isNotBlank() && !FirestoreSchema.isStateRestricted(selectedState)
    val isAgeValid = isAge18Plus

    // Sign Up form completion check
    val isSignupDetailsComplete = isMobileValid && isFullNameValid && isEmailValid && isUpiValid && isStateValid && isAgeValid
    val signupCompletedCount = listOf(isMobileValid, isFullNameValid, isEmailValid, isUpiValid, isStateValid, isAgeValid).count { it }

    // Registration Required profile completion check
    val isRegDetailsComplete = isFullNameValid && isEmailValid && isUpiValid && isStateValid && isAgeValid
    val regCompletedCount = listOf(isFullNameValid, isEmailValid, isUpiValid, isStateValid, isAgeValid).count { it }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Admin Dialog State
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminMasterKeyInput by remember { mutableStateOf("") }
    var adminError by remember { mutableStateOf<String?>(null) }

    // Resend OTP Countdown Timer (60 seconds)
    var countdownSeconds by remember { mutableIntStateOf(60) }

    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
        if (authState is AuthState.OtpSent) {
            countdownSeconds = 60
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EgleNavyDark, Color(0xFF091222), EgleNavyDark)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(EgleGoldLight, EgleGoldDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsCricket,
                    contentDescription = "Egle11 Logo",
                    tint = EgleNavyDark,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "EGLE11",
                color = EgleGoldPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = "Official Cricket Fantasy Platform",
                color = EgleTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Authentication Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(EgleGoldPrimary.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    // Card Title & Subtitle based on state
                    when (val state = authState) {
                        is AuthState.LoggedOut -> {
                            // Seamless Tab Toggle: Login vs Sign Up
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EgleNavyCard)
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    color = if (authTab == 0) EgleGoldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            authTab = 0
                                            errorMessage = null
                                            infoMessage = null
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneIphone,
                                            contentDescription = null,
                                            tint = if (authTab == 0) EgleNavyDark else EgleTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Login",
                                            color = if (authTab == 0) EgleNavyDark else EgleTextSecondary,
                                            fontWeight = if (authTab == 0) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Surface(
                                    color = if (authTab == 1) EgleGoldPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            authTab = 1
                                            errorMessage = null
                                            infoMessage = null
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = if (authTab == 1) EgleNavyDark else EgleTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sign Up",
                                            color = if (authTab == 1) EgleNavyDark else EgleTextSecondary,
                                            fontWeight = if (authTab == 1) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        if (isSignupDetailsComplete) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (authTab == 1) EgleNavyDark else EgleGreenSuccess,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (authTab == 0) {
                                Text(
                                    text = "Mobile Number Login",
                                    color = EgleTextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter your 10-digit Indian mobile number to log in via SMS OTP verification.",
                                    color = EgleTextSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "Sign Up with Mobile Number",
                                    color = EgleTextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter your 10-digit Indian mobile number & registration details to create your account.",
                                    color = EgleTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        is AuthState.OtpSent -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = EgleGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verify OTP Code",
                                    color = EgleTextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A 6-digit OTP code has been sent via SMS to ${state.mobileNumber}",
                                color = EgleTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        is AuthState.RegistrationRequired -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = EgleCyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Your Profile",
                                    color = EgleTextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Setup your profile details. Winnings will be disbursed directly to your registered UPI ID.",
                                color = EgleTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        is AuthState.Authenticated -> {
                            Text(
                                text = "Welcome to Egle11, ${state.user.name}",
                                color = EgleGreenSuccess,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error Banner
                    if (errorMessage != null) {
                        Surface(
                            color = EgleRedAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(EgleRedAlert, Color.Transparent))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = EgleRedAlert,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = EgleRedAlert,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Success Banner
                    if (infoMessage != null) {
                        Surface(
                            color = EgleGreenSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(EgleGreenSuccess, Color.Transparent))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EgleGreenSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = infoMessage ?: "",
                                    color = EgleGreenSuccess,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // STATE 1: Enter Indian Mobile Number
                    // -----------------------------------------------------------------
                    when (val state = authState) {
                        is AuthState.LoggedOut -> {
                            if (authTab == 0) {
                                // ---------------------------------------------------------
                                // LOGIN TAB: Mobile Number + Send OTP
                                // ---------------------------------------------------------
                                OutlinedTextField(
                                    value = mobileNumber,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        if (digitsOnly.length <= 10) {
                                            mobileNumber = digitsOnly
                                        }
                                    },
                                    label = { Text("Indian Mobile Number", color = EgleTextSecondary) },
                                    placeholder = { Text("9876543210", color = EgleTextMuted) },
                                    leadingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                        ) {
                                            Text(
                                                text = "🇮🇳 +91",
                                                color = EgleGoldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .height(20.dp)
                                                    .width(1.dp)
                                                    .background(EgleNavyBorder)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (mobileNumber.length == 10) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid Number",
                                                tint = EgleGreenSuccess
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("mobile_number_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EgleGoldPrimary,
                                        unfocusedBorderColor = EgleNavyBorder,
                                        focusedTextColor = EgleTextPrimary,
                                        unfocusedTextColor = EgleTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        if (mobileNumber.length != 10) {
                                            errorMessage = "Please enter a valid 10-digit mobile number"
                                            return@Button
                                        }
                                        isLoading = true
                                        authRepository.sendPhoneOtp(
                                            activity = activity,
                                            mobileNumber = mobileNumber,
                                            onSuccess = { msg ->
                                                isLoading = false
                                                infoMessage = msg
                                            },
                                            onError = { err ->
                                                isLoading = false
                                                errorMessage = err
                                            }
                                        )
                                    },
                                    enabled = mobileNumber.length == 10 && !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("send_otp_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EgleGoldPrimary,
                                        disabledContainerColor = EgleNavyCard,
                                        disabledContentColor = EgleTextMuted
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = EgleNavyDark,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            tint = if (mobileNumber.length == 10) EgleNavyDark else EgleTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (mobileNumber.length == 10) "Send OTP & Continue" else "Enter Mobile Number",
                                            color = if (mobileNumber.length == 10) EgleNavyDark else EgleTextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            } else {
                                // ---------------------------------------------------------
                                // SIGN UP TAB: Required Details Form
                                // Continue/Confirm is enabled ONLY after all details are filled
                                // ---------------------------------------------------------

                                // 1. Mobile Number
                                OutlinedTextField(
                                    value = mobileNumber,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        if (digitsOnly.length <= 10) {
                                            mobileNumber = digitsOnly
                                        }
                                    },
                                    label = { Text("Mobile Number (10 Digits)", color = EgleTextSecondary) },
                                    placeholder = { Text("9876543210", color = EgleTextMuted) },
                                    leadingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                        ) {
                                            Text(
                                                text = "🇮🇳 +91",
                                                color = EgleGoldPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .height(20.dp)
                                                    .width(1.dp)
                                                    .background(EgleNavyBorder)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (isMobileValid) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid Mobile",
                                                tint = EgleGreenSuccess
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_mobile_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EgleGoldPrimary,
                                        unfocusedBorderColor = EgleNavyBorder,
                                        focusedTextColor = EgleTextPrimary,
                                        unfocusedTextColor = EgleTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2. Full Legal Name
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Legal Name", color = EgleTextSecondary) },
                                    placeholder = { Text("e.g. Kiran Sharma", color = EgleTextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = EgleGoldPrimary)
                                    },
                                    trailingIcon = {
                                        if (isFullNameValid) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid Name",
                                                tint = EgleGreenSuccess
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_name_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EgleGoldPrimary,
                                        unfocusedBorderColor = EgleNavyBorder,
                                        focusedTextColor = EgleTextPrimary,
                                        unfocusedTextColor = EgleTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3. Email Address
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address", color = EgleTextSecondary) },
                                    placeholder = { Text("kiran@example.com", color = EgleTextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = EgleGoldPrimary)
                                    },
                                    trailingIcon = {
                                        if (isEmailValid) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid Email",
                                                tint = EgleGreenSuccess
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_email_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EgleGoldPrimary,
                                        unfocusedBorderColor = EgleNavyBorder,
                                        focusedTextColor = EgleTextPrimary,
                                        unfocusedTextColor = EgleTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 4. Registered UPI ID
                                OutlinedTextField(
                                    value = upiInput,
                                    onValueChange = { upiInput = it },
                                    label = { Text("Registered UPI ID for Payouts", color = EgleTextSecondary) },
                                    placeholder = { Text("yourname@upi / yourname@okaxis", color = EgleTextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EgleCyanAccent)
                                    },
                                    trailingIcon = {
                                        if (isUpiValid) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Valid UPI",
                                                tint = EgleGreenSuccess
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_upi_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EgleGoldPrimary,
                                        unfocusedBorderColor = EgleNavyBorder,
                                        focusedTextColor = EgleTextPrimary,
                                        unfocusedTextColor = EgleTextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🔒 No Aadhaar/PAN needed. Winnings will be disbursed directly to this UPI ID.",
                                    color = EgleTextMuted,
                                    fontSize = 10.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // 5. State Selector
                                ExposedDropdownMenuBox(
                                    expanded = stateDropdownExpanded,
                                    onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedState,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("State of Residence", color = EgleTextSecondary) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                                        leadingIcon = {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = EgleGoldPrimary)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EgleGoldPrimary,
                                            unfocusedBorderColor = EgleNavyBorder,
                                            focusedTextColor = EgleTextPrimary,
                                            unfocusedTextColor = EgleTextPrimary
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = stateDropdownExpanded,
                                        onDismissRequest = { stateDropdownExpanded = false }
                                    ) {
                                        FirestoreSchema.ALLOWED_STATES.forEach { stateName ->
                                            DropdownMenuItem(
                                                text = { Text(stateName, color = Color.White) },
                                                onClick = {
                                                    selectedState = stateName
                                                    stateDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // State Law Notice
                                Surface(
                                    color = EgleNavyCard,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = EgleGoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Per State Laws: Assam, Telangana, Andhra Pradesh, Nagaland, Odisha, Sikkim & Tamil Nadu residents cannot participate in paid contests.",
                                            color = EgleTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 6. Age 18+ Checkbox
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isAge18Plus = !isAge18Plus },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isAge18Plus,
                                        onCheckedChange = { isAge18Plus = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = EgleGoldPrimary,
                                            checkmarkColor = EgleNavyDark
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "I confirm that I am 18 years of age or older and reside in an Indian state where real-money fantasy sports are legally permitted.",
                                        color = EgleTextPrimary,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dynamic Details Validation Status Card
                                Surface(
                                    color = if (isSignupDetailsComplete) EgleGreenSuccess.copy(alpha = 0.15f) else EgleNavyCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(
                                            if (isSignupDetailsComplete) listOf(EgleGreenSuccess, EgleGreenSuccess.copy(alpha = 0.3f))
                                            else listOf(EgleNavyBorder, Color.Transparent)
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSignupDetailsComplete) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isSignupDetailsComplete) EgleGreenSuccess else EgleGoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isSignupDetailsComplete)
                                                "All details completed! Continue option is now enabled."
                                            else
                                                "Details entered: $signupCompletedCount/6. Fill all details to enable Continue/Confirm.",
                                            color = if (isSignupDetailsComplete) EgleGreenSuccess else EgleTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Continue/Confirm Button - STRICTLY ENABLED ONLY AFTER DETAILS ARE FILLED
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        if (!isSignupDetailsComplete) {
                                            errorMessage = "Please fill in all details above to continue"
                                            return@Button
                                        }
                                        val input = RegistrationInput(
                                            name = fullName.trim(),
                                            email = emailInput.trim(),
                                            upiId = upiInput.trim(),
                                            state = selectedState,
                                            isAge18Plus = true,
                                            role = UserRole.USER
                                        )
                                        authRepository.pendingRegistrationInput = input
                                        isLoading = true
                                        authRepository.sendPhoneOtp(
                                            activity = activity,
                                            mobileNumber = mobileNumber,
                                            onSuccess = { msg ->
                                                isLoading = false
                                                infoMessage = msg
                                            },
                                            onError = { err ->
                                                isLoading = false
                                                errorMessage = err
                                            }
                                        )
                                    },
                                    enabled = isSignupDetailsComplete && !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("signup_confirm_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EgleGoldPrimary,
                                        disabledContainerColor = EgleNavyCard,
                                        disabledContentColor = EgleTextMuted
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = EgleNavyDark,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isSignupDetailsComplete) Icons.Default.CheckCircle else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = if (isSignupDetailsComplete) EgleNavyDark else EgleTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isSignupDetailsComplete) "Confirm & Continue (Send OTP)" else "Fill Details to Enable Continue",
                                                color = if (isSignupDetailsComplete) EgleNavyDark else EgleTextMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Test Credentials helper banner for reviewers
                            Surface(
                                color = EgleNavyCard,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = EgleCyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Test Number: 9876500001 (or any 10-digit number). Test OTP is 123456.",
                                        color = EgleTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // -----------------------------------------------------------------
                        // STATE 2: Enter 6-digit OTP
                        // -----------------------------------------------------------------
                        is AuthState.OtpSent -> {
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    if (digits.length <= 6) {
                                        otpInput = digits
                                    }
                                },
                                label = { Text("Enter 6-Digit OTP", color = EgleTextSecondary) },
                                placeholder = { Text("••••••", color = EgleTextMuted) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = EgleGoldPrimary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (otpInput.length == 6) {
                                            isLoading = true
                                            authRepository.verifyOtpAndSignIn(
                                                otpCode = otpInput,
                                                onSuccess = {
                                                    isLoading = false
                                                    onAuthSuccess()
                                                },
                                                onError = { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EgleGoldPrimary,
                                    unfocusedBorderColor = EgleNavyBorder,
                                    focusedTextColor = EgleTextPrimary,
                                    unfocusedTextColor = EgleTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Resend OTP Section with Countdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (countdownSeconds > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = EgleTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Resend OTP in ${countdownSeconds}s",
                                            color = EgleTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            errorMessage = null
                                            infoMessage = null
                                            authRepository.resendPhoneOtp(
                                                activity = activity,
                                                mobileNumber = state.mobileNumber,
                                                onSuccess = { msg ->
                                                    infoMessage = msg
                                                    countdownSeconds = 60
                                                },
                                                onError = { err -> errorMessage = err }
                                            )
                                        },
                                        modifier = Modifier.testTag("resend_otp_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = EgleCyanAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Resend OTP",
                                            color = EgleCyanAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        otpInput = ""
                                        authRepository.resetToLoggedOut()
                                    }
                                ) {
                                    Text(
                                        text = "Change Number",
                                        color = EgleTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    if (otpInput.length != 6) {
                                        errorMessage = "Please enter the full 6-digit OTP"
                                        return@Button
                                    }
                                    isLoading = true
                                    authRepository.verifyOtpAndSignIn(
                                        otpCode = otpInput,
                                        onSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                },
                                enabled = otpInput.length == 6 && !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("verify_otp_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EgleGoldPrimary,
                                    disabledContainerColor = EgleGoldDark.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = EgleNavyDark,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = EgleNavyDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Verify OTP & Continue",
                                        color = EgleNavyDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        // -----------------------------------------------------------------
                        // STATE 3: Complete User Profile (Name, Email, UPI ID, State, 18+)
                        // -----------------------------------------------------------------
                        is AuthState.RegistrationRequired -> {
                            // Verified Mobile Number Banner
                            Surface(
                                color = EgleNavyCard,
                                shape = RoundedCornerShape(10.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(EgleGoldPrimary, EgleNavyBorder))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneIphone,
                                        contentDescription = null,
                                        tint = EgleGoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Registered Mobile Number",
                                            color = EgleTextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = state.mobileNumber.ifEmpty { "+91 $mobileNumber" },
                                            color = EgleTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = EgleGreenSuccess.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EgleGreenSuccess,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "VERIFIED",
                                                color = EgleGreenSuccess,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Full Name
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Legal Name", color = EgleTextSecondary) },
                                placeholder = { Text("e.g. Kiran Adhav", color = EgleTextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = EgleGoldPrimary)
                                },
                                trailingIcon = {
                                    if (isFullNameValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid Name",
                                            tint = EgleGreenSuccess
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EgleGoldPrimary,
                                    unfocusedBorderColor = EgleNavyBorder,
                                    focusedTextColor = EgleTextPrimary,
                                    unfocusedTextColor = EgleTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Email Address
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Email Address", color = EgleTextSecondary) },
                                placeholder = { Text("kiran@example.com", color = EgleTextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = EgleGoldPrimary)
                                },
                                trailingIcon = {
                                    if (isEmailValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid Email",
                                            tint = EgleGreenSuccess
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EgleGoldPrimary,
                                    unfocusedBorderColor = EgleNavyBorder,
                                    focusedTextColor = EgleTextPrimary,
                                    unfocusedTextColor = EgleTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Registered UPI ID (Mandated for Payouts, No Aadhaar/PAN)
                            OutlinedTextField(
                                value = upiInput,
                                onValueChange = { upiInput = it },
                                label = { Text("Registered UPI ID for Payouts", color = EgleTextSecondary) },
                                placeholder = { Text("yourname@okaxis / yourname@upi", color = EgleTextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EgleCyanAccent)
                                },
                                trailingIcon = {
                                    if (isUpiValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid UPI",
                                            tint = EgleGreenSuccess
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_upi_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EgleGoldPrimary,
                                    unfocusedBorderColor = EgleNavyBorder,
                                    focusedTextColor = EgleTextPrimary,
                                    unfocusedTextColor = EgleTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔒 Aadhaar & PAN verification omitted per specification. All winnings will disburse to this UPI ID.",
                                color = EgleTextMuted,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // State Selector (Assam, Telangana, Andhra Pradesh, Nagaland, Odisha, Sikkim, Tamil Nadu Restricted)
                            ExposedDropdownMenuBox(
                                expanded = stateDropdownExpanded,
                                onExpandedChange = { stateDropdownExpanded = !stateDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedState,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("State of Residence", color = EgleTextSecondary) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = EgleGoldPrimary)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EgleGoldPrimary,
                                        unfocusedBorderColor = EgleNavyBorder,
                                        focusedTextColor = EgleTextPrimary,
                                        unfocusedTextColor = EgleTextPrimary
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = stateDropdownExpanded,
                                    onDismissRequest = { stateDropdownExpanded = false }
                                ) {
                                    FirestoreSchema.ALLOWED_STATES.forEach { stateName ->
                                        DropdownMenuItem(
                                            text = { Text(stateName, color = Color.White) },
                                            onClick = {
                                                selectedState = stateName
                                                stateDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Restricted States Compliance Warning
                            Surface(
                                color = EgleNavyCard,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = EgleGoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Per State Laws: Assam, Telangana, Andhra Pradesh, Nagaland, Odisha, Sikkim & Tamil Nadu residents cannot participate in paid contests.",
                                        color = EgleTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Requirement 20: Mandatory Age 18+ Checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAge18Plus = !isAge18Plus },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAge18Plus,
                                    onCheckedChange = { isAge18Plus = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EgleGoldPrimary,
                                        checkmarkColor = EgleNavyDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "I confirm that I am 18 years of age or older and reside in an Indian state where real-money fantasy sports are legally permitted.",
                                    color = EgleTextPrimary,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dynamic Details Validation Status Card for Profile Registration
                            Surface(
                                color = if (isRegDetailsComplete) EgleGreenSuccess.copy(alpha = 0.15f) else EgleNavyCard,
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(
                                        if (isRegDetailsComplete) listOf(EgleGreenSuccess, EgleGreenSuccess.copy(alpha = 0.3f))
                                        else listOf(EgleNavyBorder, Color.Transparent)
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isRegDetailsComplete) Icons.Default.CheckCircle else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isRegDetailsComplete) EgleGreenSuccess else EgleGoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRegDetailsComplete)
                                            "All details completed! Confirm option is now enabled."
                                        else
                                            "Details entered: $regCompletedCount/5. Fill all details to enable Confirm.",
                                        color = if (isRegDetailsComplete) EgleGreenSuccess else EgleTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    if (!isRegDetailsComplete) {
                                        errorMessage = "Please enter all required details"
                                        return@Button
                                    }

                                    isLoading = true
                                    coroutineScope.launch {
                                        val input = RegistrationInput(
                                            name = fullName.trim(),
                                            email = emailInput.trim(),
                                            upiId = upiInput.trim(),
                                            state = selectedState,
                                            isAge18Plus = true,
                                            role = UserRole.USER
                                        )
                                        val res = authRepository.completeUserProfile(
                                            firebaseUid = state.firebaseUid,
                                            mobileNumber = state.mobileNumber,
                                            input = input
                                        )
                                        isLoading = false
                                        res.onSuccess {
                                            onAuthSuccess()
                                        }.onFailure {
                                            errorMessage = it.message
                                        }
                                    }
                                },
                                enabled = isRegDetailsComplete && !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("complete_registration_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EgleGoldPrimary,
                                    disabledContainerColor = EgleNavyCard,
                                    disabledContentColor = EgleTextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = EgleNavyDark,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isRegDetailsComplete) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isRegDetailsComplete) EgleNavyDark else EgleTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isRegDetailsComplete) "Confirm & Complete Profile" else "Enter Details to Enable Confirm",
                                            color = if (isRegDetailsComplete) EgleNavyDark else EgleTextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        is AuthState.Authenticated -> {
                            Text(
                                text = "Redirecting to Home...",
                                color = EgleGoldPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Requirement 14: Separate Admin Access Portal
            // Kept strictly separate from the user authentication experience
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .clickable { showAdminLoginDialog = true }
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Portal",
                        tint = EgleTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Admin Access Portal (Staff Only)",
                        color = EgleTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // -----------------------------------------------------------------
    // Separate Admin Access Dialog
    // -----------------------------------------------------------------
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminLoginDialog = false
                adminError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = EgleGoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Access Verification", color = EgleTextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Admin privileges are segregated from standard user authentication. Enter the Admin Master Key to access the administration dashboard:",
                        color = EgleTextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = adminMasterKeyInput,
                        onValueChange = { adminMasterKeyInput = it },
                        label = { Text("Admin Master Key", color = EgleTextSecondary) },
                        placeholder = { Text("EGLE11_ADMIN_SECURE", color = EgleTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EgleGoldPrimary,
                            unfocusedBorderColor = EgleNavyBorder,
                            focusedTextColor = EgleTextPrimary,
                            unfocusedTextColor = EgleTextPrimary
                        )
                    )

                    if (adminError != null) {
                        Text(
                            text = adminError ?: "",
                            color = EgleRedAlert,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "Default master key for testing: EGLE11_ADMIN_SECURE",
                        color = EgleTextMuted,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = authRepository.loginAsAdmin(adminMasterKeyInput.trim())
                        res.onSuccess {
                            showAdminLoginDialog = false
                            adminError = null
                            onAuthSuccess()
                        }.onFailure {
                            adminError = it.message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldDark)
                ) {
                    Text("Verify & Access Admin", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdminLoginDialog = false
                    adminError = null
                }) {
                    Text("Cancel", color = EgleTextSecondary)
                }
            }
        )
    }
}
