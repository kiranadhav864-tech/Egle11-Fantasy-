package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.repository.AuthRepository
import com.example.repository.AuthState
import com.example.repository.Egle11Repository
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BackendInspectorScreen
import com.example.ui.screens.UserAppScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val repository = Egle11Repository(authRepository)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Egle11RootApp(authRepository = authRepository, repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Egle11RootApp(
    authRepository: AuthRepository,
    repository: Egle11Repository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val authState by authRepository.authState.collectAsState()

    var activeViewMode by remember { mutableStateOf("USER_APP") } // "USER_APP", "ADMIN_PANEL", "BACKEND_INSPECTOR"
    var showRoleSwitchDialog by remember { mutableStateOf(false) }

    if (authState !is AuthState.Authenticated || currentUser == null) {
        AuthScreen(
            authRepository = authRepository,
            onAuthSuccess = {
                activeViewMode = if (authRepository.currentUser.value?.role == UserRole.ADMIN) "ADMIN_PANEL" else "USER_APP"
            }
        )
    } else {
        val user = currentUser!!

        // Requirement 14: Strict separation of Admin vs User UI
        // Regular users must NEVER have access to admin functions or see admin controls.
        if (user.role == UserRole.USER) {
            // Pure User Application Interface - No admin switchers or admin tabs
            UserAppScreen(repository = repository, authRepository = authRepository)
        } else {
            // Dedicated Admin Experience for Admin Role
            Scaffold(
                topBar = {
                    Surface(
                        color = EgleNavySurface,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Admin Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(EgleGoldLight, EgleGoldDark))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = EgleNavyDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = "EGLE11 ADMIN",
                                        color = EgleGoldPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Backend Management Console",
                                        color = EgleTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Logout Admin Button
                                OutlinedButton(
                                    onClick = { authRepository.logout() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EgleRedAlert),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = "Logout Admin", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Logout", fontSize = 11.sp, color = EgleRedAlert)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Admin Tabs (Admin Panel vs Backend Spec)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EgleNavyCard)
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val adminTabs = listOf(
                                    Triple("Admin Panel", "ADMIN_PANEL", Icons.Default.AdminPanelSettings),
                                    Triple("Backend Spec", "BACKEND_INSPECTOR", Icons.Default.Dns)
                                )

                                adminTabs.forEach { (label, key, icon) ->
                                    val isSelected = activeViewMode == key
                                    Surface(
                                        color = if (isSelected) EgleGoldPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { activeViewMode = key }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) EgleNavyDark else EgleTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                color = if (isSelected) EgleNavyDark else EgleTextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = EgleNavyDark
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (activeViewMode) {
                        "ADMIN_PANEL" -> AdminPanelScreen(repository = repository, authRepository = authRepository)
                        "BACKEND_INSPECTOR" -> BackendInspectorScreen(repository = repository)
                        else -> AdminPanelScreen(repository = repository, authRepository = authRepository)
                    }
                }
            }
        }
    }
}
