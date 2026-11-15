package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.data.firebase.UserRole
import com.example.ui.theme.AccentRed
import com.example.ui.theme.Gold
import com.example.ui.theme.NavyBlue
import com.google.firebase.auth.FirebaseUser

@Composable
fun AuthGateScreen(
    firebaseUser: FirebaseUser?,
    initialName: String,
    initialEmail: String,
    initialRole: UserRole,
    isVerifiedAdmin: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onSaveProfile: (fullName: String, email: String, role: UserRole) -> Unit,
    onSignOut: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedRole by remember(initialRole) { mutableStateOf(initialRole) }
    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    val displayError = localError ?: errorMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyBlue, Color(0xFF071526))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(2.dp, Gold, RoundedCornerShape(28.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nexora_logo_1783164098966),
                    contentDescription = "Logo Nexora Market",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nexora Market",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Votre Marché Sans Limites • FCFA (XOF)",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111E2E).copy(alpha = 0.94f)),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (firebaseUser == null) "Inscription & Connexion" else "Finaliser votre rôle",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (firebaseUser == null) {
                            "Choisissez votre rôle puis connectez-vous avec votre compte Google."
                        } else {
                            "Vérifiez vos coordonnées (utilisées pour FedaPay) et votre rôle."
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Choisir un rôle",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    UserRole.entries.forEach { role ->
                        val isSelected = selectedRole == role
                        val icon = when (role) {
                            UserRole.CLIENT -> Icons.Default.ShoppingBag
                            UserRole.VENDEUR -> Icons.Default.Storefront
                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedRole = role
                                    localError = null
                                    onClearError()
                                }
                                .testTag("role_option_${role.key}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Gold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Gold else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = role.labelFr, tint = if (isSelected) Gold else Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = role.labelFr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = role.descriptionFr,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                        lineHeight = 16.sp
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedRole = role },
                                    colors = RadioButtonDefaults.colors(selectedColor = Gold, unselectedColor = Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }

                    if (selectedRole == UserRole.ADMIN && firebaseUser != null && !isVerifiedAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Note : Le Tableau de Bord Admin est réservé aux comptes administrateurs autorisés.",
                            color = Gold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (firebaseUser != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nom complet") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Gold) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                focusedLabelColor = Gold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Adresse e-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Gold) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                focusedLabelColor = Gold,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                        )
                    }

                    if (!displayError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = displayError,
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (firebaseUser == null) {
                        Button(
                            onClick = {
                                isSigningIn = true
                                localError = null
                                onClearError()
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                    },
                                    onAuthError = { err ->
                                        isSigningIn = false
                                        localError = err
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = NavyBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Continuer avec Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (fullName.isBlank() || email.isBlank() || !email.contains("@")) {
                                    localError = "Veuillez entrer un nom complet et une adresse e-mail valide."
                                } else {
                                    localError = null
                                    onSaveProfile(fullName.trim(), email.trim(), selectedRole)
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NavyBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_role_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyBlue, strokeWidth = 2.dp)
                            } else {
                                Text("Accéder à Nexora Market", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = onSignOut) {
                            Text("Changer de compte Google", color = Gold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    isEN: Boolean = false,
    onGetStarted: () -> Unit
) {
    AuthGateScreen(
        firebaseUser = null,
        initialName = "",
        initialEmail = "",
        initialRole = UserRole.CLIENT,
        isVerifiedAdmin = false,
        isLoading = false,
        errorMessage = null,
        onSaveProfile = { _, _, _ -> onGetStarted() },
        onSignOut = {},
        onClearError = {}
    )
}
