package com.jonesys.vitalsy.viewmodel

import com.jonesys.vitalsy.model.FormularioLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel {
    private val formuLogin = MutableStateFlow(FormularioLogin())
    val formData: StateFlow<FormularioLogin> = formuLogin.asStateFlow()

    fun actualizarEmail(email: String) {
        formuLogin.value = formuLogin.value.copy(
            email = email
        )
    }

    val actualizarPassword: (String) -> Unit = { pass ->
        formuLogin.value = formuLogin.value.copy(
            password = pass
        )
    }

    private fun errorNull() {
        formuLogin.value = formuLogin.value.copy(
            error = null,
        )
    }

    private fun validarEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun mensajeError(mensaje: String) {
        formuLogin.value = formuLogin.value.copy(
            error = mensaje,
            isLogin = false
        )
    }

    fun Login() {
        val f = formuLogin.value

        // Validaciones de formato
        if (f.email.length < 6) {
            mensajeError("El email debe ser mayor de 6 caracteres")
            return
        }

        if (!validarEmail(f.email)) {
            mensajeError("No tiene formato de email")
            return
        }
    }
}
