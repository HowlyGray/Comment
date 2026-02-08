package com.memoryshare.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.memoryshare.app.ui.theme.*
import com.memoryshare.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkSurfaceElevated,
                            DarkBackground
                        )
                    )
                )
        )

        // Decorative gradient orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .blur(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CoralPrimary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 80.dp, y = 100.dp)
                .blur(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VioletPrimary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Animated logo with gradient
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CoralPrimary, VioletPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App name with gradient text effect
            Text(
                text = "MemoryShare",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSignUp) "Rejoignez l'aventure" else "Content de vous revoir",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Glass card for form
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = DarkSurfaceCard.copy(alpha = 0.6f),
                border = ButtonDefaults.outlinedButtonBorder(enabled = false)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Sign Up fields
                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
                                label = { Text("Nom d'utilisateur") },
                                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null, tint = CoralLight) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = authTextFieldColors(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Nom d'affichage") },
                                leadingIcon = { Icon(Icons.Outlined.Person, null, tint = CoralLight) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = authTextFieldColors(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = CoralLight) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = authTextFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = CoralLight) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondaryDark
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = authTextFieldColors(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                if (!isSignUp) {
                                    authViewModel.signIn(email, password)
                                }
                            }
                        )
                    )

                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmer le mot de passe") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = CoralLight) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = authTextFieldColors(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (password == confirmPassword) {
                                        authViewModel.signUp(email, password, username, displayName)
                                    }
                                }
                            ),
                            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                            supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                { Text("Les mots de passe ne correspondent pas", color = ErrorRose) }
                            } else null
                        )
                    }

                    // Forgot password
                    AnimatedVisibility(visible = !isSignUp) {
                        TextButton(
                            onClick = { showForgotPassword = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                "Mot de passe oublié ?",
                                color = CoralLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gradient submit button
            Button(
                onClick = {
                    if (isSignUp) {
                        if (password == confirmPassword) {
                            authViewModel.signUp(email, password, username, displayName)
                        }
                    } else {
                        authViewModel.signIn(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank() &&
                        (!isSignUp || (username.isNotBlank() && displayName.isNotBlank() && password == confirmPassword)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                                    listOf(CoralPrimary, VioletPrimary)
                                } else {
                                    listOf(
                                        CoralPrimary.copy(alpha = 0.3f),
                                        VioletPrimary.copy(alpha = 0.3f)
                                    )
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Créer mon compte" else "Se connecter",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Toggle sign in / sign up
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUp) "Déjà un compte ?" else "Pas encore de compte ?",
                    color = TextSecondaryDark
                )
                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        password = ""
                        confirmPassword = ""
                    }
                ) {
                    Text(
                        text = if (isSignUp) "Se connecter" else "S'inscrire",
                        color = CoralPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Forgot password dialog
    if (showForgotPassword) {
        var resetEmail by remember { mutableStateOf(email) }

        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = {
                Text(
                    "Réinitialiser le mot de passe",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Entrez votre email pour recevoir un lien de réinitialisation.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.sendPasswordResetEmail(resetEmail)
                        showForgotPassword = false
                    }
                ) {
                    Text("Envoyer", color = CoralPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = GlassBorder,
    focusedBorderColor = CoralPrimary,
    unfocusedLabelColor = TextSecondaryDark,
    focusedLabelColor = CoralLight,
    cursorColor = CoralPrimary,
    unfocusedTextColor = TextPrimaryDark,
    focusedTextColor = TextPrimaryDark,
    unfocusedContainerColor = Color.Transparent,
    focusedContainerColor = Color.Transparent
)
