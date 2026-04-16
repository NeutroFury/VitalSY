package com.jonesys.vitalsy.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.jonesys.vitalsy.ui.theme.ColorBotonRosa
import com.jonesys.vitalsy.ui.theme.ColorMainBeige
import com.jonesys.vitalsy.ui.theme.ColorMainBlanco
import com.jonesys.vitalsy.ui.theme.ColorTitulos
import com.jonesys.vitalsy.viewmodel.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel, navController: NavController? = null) {
    val uiState by viewModel.formData.collectAsState()
    val contexto = LocalContext.current

    LaunchedEffect(uiState.isLogin) {
        if (uiState.isLogin) {
            Toast.makeText(contexto, "Inicio de sesión exitoso", Toast.LENGTH_LONG).show()
            // Si en el futuro hay navegación, usar: navController?.navigate("home")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorMainBeige),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Iniciar Sesión",
                    color = ColorTitulos,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.actualizarEmail(it) },
                    label = { Text(text = "Correo electrónico") },
                    placeholder = { Text(text = "email@ejemplo.com") },
                    isError = uiState.error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.actualizarPassword(it) },
                    label = { Text(text = "Contraseña") },
                    placeholder = { Text(text = "********") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = uiState.error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.height(12.dp))

                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { viewModel.Login() },
                        modifier = Modifier
                            .width(180.dp)
                            .height(48.dp)
                            .testTag("BTN_LOGIN"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorBotonRosa,
                            contentColor = ColorMainBlanco
                        )
                    ) {
                        Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                val annotatedText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append("¿No tienes cuenta? ")
                    }
                    pushStringAnnotation(tag = "REGISTRO", annotation = "registro")
                    withStyle(style = SpanStyle(color = ColorTitulos, fontWeight = FontWeight.Bold)) {
                        append("Regístrate aquí")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    onClick = { /* manejar navegación si se desea */ }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "o",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Continuar como invitado",
                    color = ColorTitulos,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* acción invitado */ },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
