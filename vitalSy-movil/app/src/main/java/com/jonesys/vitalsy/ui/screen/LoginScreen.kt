package com.jonesys.vitalsy.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jonesys.vitalsy.ui.components.VitalSYBrandHeader
import com.jonesys.vitalsy.ui.components.VitalSYScreen
import com.jonesys.vitalsy.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel, navController: NavController? = null) {
    val uiState by viewModel.formData.collectAsState()
    val contexto = LocalContext.current
    val navRef = navController

    LaunchedEffect(uiState.isLogin) {
        if (uiState.isLogin) {
            Toast.makeText(contexto, "Inicio de sesión exitoso", Toast.LENGTH_LONG).show()
            navRef?.currentDestination
        }
    }

    VitalSYScreen {
        VitalSYBrandHeader(subtitle = "Manejo inteligente de la diabetes")

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.actualizarEmail(it) },
                label = { Text("Email") },
                placeholder = {
                    Text(
                        "ejemplo@vitalsy.com",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors,
                textStyle = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.actualizarPassword(it) },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors,
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = { viewModel.Login() },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("BTN_LOGIN"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                "INICIAR SESION",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))) {
                append("Eres nuevo? ")
            }
            pushStringAnnotation(tag = "REGISTRO", annotation = "registro")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("Crea una cuenta")
            }
            pop()
        }

        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            onClick = { /* Navegar al Registro */ }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Continuar como invitado",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable { /* Invitado */ }
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}