# VitalSY 📱💉

**VitalSY** es una plataforma tecnológica integral diseñada para optimizar la gestión de pacientes con **Diabetes Tipo 1**. Este ecosistema busca superar la fragmentación de las herramientas actuales mediante una arquitectura proactiva que centraliza el monitoreo, la nutrición y el análisis predictivo basado en Inteligencia Artificial.

## 🚀 Propósito del Proyecto
La solución busca ofrecer un soporte robusto tanto para el paciente como para el profesional clínico. Mientras la **aplicación móvil** se enfoca en la captura rápida de datos y el acompañamiento diario, el **backend orquestador** asegura la persistencia de datos históricos y procesa alertas inteligentes mediante un motor de IA local, permitiendo una toma de decisiones informada y segura.

## 🏗️ Estructura del Ecosistema (Monorepo)
El proyecto se organiza bajo una estructura de monorepo para garantizar la consistencia entre el procesado de datos y la interfaz de usuario:

* **`vitalsy-backend`**: Orquestador central desarrollado en **Spring Boot 4.0.5**. Gestiona la lógica de negocio, la persistencia en **PostgreSQL** y la comunicación con el motor de IA.
* **`vitalSy-movil`**: Cliente nativo en **Kotlin** para Android. Proporciona una UX enfocada al paciente para el registro y visualización de niveles glucémicos.

## 🧠 El Diferenciador: Engine de IA Predictiva (Gemma 4)
A diferencia de otras soluciones, VitalSY integra el modelo de lenguaje **Gemma 4 E2B (Mixture of Experts)** procesado de forma **100% local**:

* **Privacidad por Diseño:** Mediante **LM Studio** y **Spring AI**, los datos sensibles se analizan localmente, cumpliendo con estándares de seguridad clínica.
* **Análisis Metabólico:** La IA interpreta tendencias y genera recomendaciones personalizadas para anticipar riesgos de hipo o hiperglicemia.
* **Eficiencia MoE:** Arquitectura diseñada para obtener razonamientos complejos con baja latencia, asegurando el atributo de **Oportunidad**.

## ✨ Características Principales
* **Monitoreo Centralizado:** Visualización de niveles de glucosa en tiempo real (Sincronización con sensores).
* **Gestión Nutricional:** Registro avanzado de alimentación para el conteo de carbohidratos.
* **Alertas e Intervenciones:** Notificaciones proactivas basadas en el análisis predictivo del motor de IA.
* **Seguridad Robusta:** Cifrado de datos con **BCrypt** y arquitectura de seguridad **Stateless**.

## 🛠️ Stack Tecnológico Unificado

### Backend e IA
* **Lenguaje:** Java 21
* **Framework:** Spring Boot 4.0.5 + Spring AI
* **IA:** Gemma 4 E2B (Local via LM Studio)
* **Base de Datos:** PostgreSQL 16

### Mobile (Android)
* **Lenguaje:** Kotlin
* **Interfaz:** Jetpack Compose con Material3
* **Arquitectura:** MVVM (Model-View-ViewModel)
* **Conectividad:** Retrofit + Coroutines para comunicación con API REST.

## 📋 Metodología y Desarrollo
El proyecto se desarrolla bajo la metodología **Scrum**, estructurándose en fases de:
1. **Diseño:** Levantamiento de requerimientos y definición de backlog técnico.
2. **Desarrollo Iterativo:** Sprints enfocados en construir la base del backend, el cliente móvil y el motor de IA.
3. **Validación:** Pruebas funcionales e integración completa para garantizar consistencia y seguridad.

---
*Este proyecto representa el esfuerzo por integrar arquitecturas modernas y modelos de lenguaje de última generación para mejorar la calidad de vida de los pacientes. Desarrollado por Joaquín Andrés Santana Castillo - Gabriel Hernández - Gabriel Nercelles - 2026.*
