package com.jonesys.vitalsy.model

data class FormularioLogin(
    val email: String = "",
    val password: String = "",
    val error: String? = null,
    val isLogin: Boolean = false,
    val nombre: String? = null,
    val userId: Long = 0
)
