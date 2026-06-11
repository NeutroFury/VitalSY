# 🚀 VitalSY 📱⚕️

![Java 21](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT_Auth-black?style=for-the-badge&logo=JSON%20web%20tokens)
![AI Integration](https://img.shields.io/badge/AI_Cognitive-FF6F00?style=for-the-badge&logo=google-gemini&logoColor=white)

**VitalSY** es una plataforma de monitoreo biométrico y de salud de grado empresarial orientada a revolucionar el manejo metabólico. Este repositorio aloja el **Backend Core** del sistema, construido sobre una arquitectura distribuida que expone una API RESTful de alto rendimiento para integrarse de forma fluida con un frontend desarrollado en React e Ionic.

Más que un simple gestor de registros médicos, VitalSY actúa como un **motor cognitivo** que aprovecha la Inteligencia Artificial y una infraestructura Cloud moderna para proporcionar análisis predictivos, mantener un blindaje de seguridad robusto y garantizar alta disponibilidad. Todo el ecosistema está orquestado ágilmente mediante contenedores Docker.

---

## 📌 Tabla de Contenidos

- [📖 Sobre el Proyecto](#-sobre-el-proyecto)
- [🛠️ Stack Tecnológico](#%EF%B8%8F-stack-tecnológico)
- [☁️ Infraestructura Cloud y Contenedores](#%E2%98%81%EF%B8%8F-infraestructura-cloud-y-contenedores)
- [🤖 Inteligencia Artificial y Cognición](#-inteligencia-artificial-y-cognición)
- [🏗️ Arquitectura Limpia y DevSecOps](#%EF%B8%8F-arquitectura-limpia-y-devsecops)
- [⚙️ Instalación y Despliegue (Docker Compose)](#%E2%9A%99%EF%B8%8F-instalación-y-despliegue-docker-compose)
- [🔑 Variables de Entorno](#1-variables-de-entorno)
- [📂 Estructura de Directorios](#-estructura-de-directorios)
- [👨‍💻 Equipo de Desarrollo](#-equipo-de-desarrollo)

---

## 📖 Sobre el Proyecto

El objetivo principal de VitalSY es proporcionar a los pacientes y profesionales de la salud una herramienta integral y predictiva. La plataforma centraliza las mediciones de glucosa y ofrece un flujo de trabajo continuo que incluye la generación de reportes en formato PDF y el registro detallado del perfil clínico del usuario.

### ✨ Características Principales

- **Gestión Biométrica:** Almacenamiento y procesamiento de lecturas glucémicas y datos antropométricos.
- **Predicción Inteligente:** Alertas preventivas calculadas por modelos de lenguaje (LLMs) sobre tendencias metabólicas.
- **Exportación de Reportes:** Generación nativa de PDFs clínicos detallados listos para presentar a especialistas.
- **Administración Centralizada:** Panel de control para gestionar permisos y accesos de los usuarios.

---

## 🛠️ Stack Tecnológico

La plataforma se apoya en tecnologías modernas, asegurando mantenimiento a largo plazo y gran escalabilidad.

**Core y Lógica de Negocio:**

- Java 21 LTS (Records, Pattern Matching)
- Spring Boot 3.2.x
- Spring Security (Seguridad de Endpoints)
- Spring Data JPA (Capa de persistencia)
- Spring Web (Controladores REST)

**Base de Datos y Migraciones:**

- PostgreSQL 16 (Serie de tiempo y alta concurrencia)
- Supabase (Plataforma Cloud para hosting de Base de Datos)
- Flyway (Versionado de esquemas relacionales)

**DevSecOps y Seguridad:**

- JSON Web Tokens (JWT) para autenticación sin estado
- Bucket4j (Defensa perimetral y Rate Limiting)

**Inteligencia Artificial:**

- Google Gemini API (Análisis y generación de recomendaciones)
- Spring AI (Integración abstracta de modelos locales y remotos)

**Utilidades y Reportes:**

- OpenPDF (Generación de documentos clínicos)
- Lombok (Reducción de código repetitivo)
- Jakarta Validation (Desinfección de entrada de datos)

**Infraestructura y Orquestación:**

- Docker (Aislamiento de procesos)
- Docker Compose (Orquestación del stack completo)
- Nginx (Servidor web para el cliente)

---

## ☁️ Infraestructura Cloud y Contenedores

VitalSY está diseñado para ser agnóstico del proveedor gracias a **Docker Compose**. Sin embargo, la plataforma está optimizada para operar sobre **Supabase** como proveedor de base de datos en la nube.

- **Motor Relacional:** Utilizamos PostgreSQL 16 alojado de forma remota en Supabase (o de forma local para entornos de prueba)
- **Rendimiento:** La conexión se establece a través de un pooler nativo optimizado para transacciones rápidas
- **Orquestación Total:** El frontend (Ionic web), el backend (Spring Boot) y la base de datos se despliegan simultáneamente con un único archivo de configuración

---

## 🤖 Inteligencia Artificial y Cognición

El sistema proporciona inteligencia clínica activa interactuando con modelos de lenguaje como Google Gemini o instancias locales corriendo en Ollama.

- **Análisis de Contexto:** Al guardar una lectura de glucosa, el sistema evalúa el momento del día y las notas agregadas por el paciente para generar recomendaciones y escribirlas en la propiedad `analisisIa`
- **Predictibilidad:** El motor cruza datos históricos buscando advertir preventivamente al usuario frente a riesgos metabólicos
- **Transparencia:** Todo este proceso se maneja asíncronamente y se refleja en los DTOs de respuesta hacia la interfaz del usuario

---

## 🏗️ Arquitectura Limpia y DevSecOps

El código fuente obedece a las prácticas más altas de desarrollo empresarial.

### Clean Architecture

La base de código está dividida estrictamente:

- **Controllers:** Capa exclusiva para enrutamiento HTTP y respuestas estandarizadas
- **Services:** Centralizan validaciones clínicas y la inteligencia del dominio
- **Repositories:** Manejan las transacciones hacia PostgreSQL
- **Mappers y DTOs:** Total aislamiento de las entidades de dominio para no exponer el modelo de datos

### Seguridad Avanzada

- **Autenticación:** Flujos seguros apoyados cien por ciento en JWT
- **Filtro de Rate Limiting:** Usando Bucket4j protegemos los servidores de ataques de fuerza bruta. Limitamos rigurosamente el inicio de sesión a 5 intentos por minuto por cada IP y dejamos una cuota holgada de 100 peticiones para el resto de recursos

### Tolerancia a Fallos y Manejo de Errores

- Hemos consolidado un `GlobalExceptionHandler` (`@RestControllerAdvice`) que atrapa cualquier evento inesperado
- Esta capa de protección asegura que el cliente móvil siempre reciba una respuesta JSON estructurada con un formato predecible llamado `ErrorResponse` garantizando la estabilidad de la aplicación y la ausencia de trazas de pila (stack traces) filtradas a la web
- Registramos todo evento anómalo con anotaciones `@Slf4j`

---

## ⚙️ Instalación y Despliegue (Docker Compose)

El proyecto completo se puede levantar con unos pocos comandos.

### Prerrequisitos

- Docker instalado y ejecutándose en el equipo
- Docker Compose configurado
- Java 21 (solo necesario si deseas compilar manualmente fuera de los contenedores)

### 1. Variables de Entorno

El sistema lee su configuración desde un archivo `.env` ubicado en la raíz del proyecto (junto al `docker-compose.yml`). Se provee el archivo [`.env.example`](./Producto/.env.example) como plantilla de referencia.

```bash
# En Windows (PowerShell)
copy Producto\.env.example Producto\.env

# En macOS / Linux
cp Producto/.env.example Producto/.env
```

> ⚠️ **Nunca subas el archivo `.env` al repositorio.** Ya está protegido en el `.gitignore`.

A continuación se describen todas las variables disponibles, agrupadas por función. Las marcadas como **Sí** en la columna *Requerido* deben definirse obligatoriamente antes de iniciar los contenedores.

---

#### 🗄️ Base de Datos (PostgreSQL local)

| Variable | Valor por defecto | Requerido | Descripción |
|---|---|:---:|---|
| `POSTGRES_DB` | `vitalsy_db` | No | Nombre de la base de datos creada en el contenedor |
| `POSTGRES_USER` | `postgres` | No | Usuario del motor PostgreSQL local |
| `POSTGRES_PASSWORD` | — | **Sí** | Contraseña del usuario de la base de datos |
| `DB_PORT` | `5432` | No | Puerto del host mapeado al contenedor de PostgreSQL |

---

#### ☁️ Supabase / DataSource Cloud (override opcional)

Estas variables **reemplazan** la conexión al contenedor local. Úsalas si deseas apuntar a **Supabase** u otro proveedor PostgreSQL remoto. Si no se definen, el backend se conecta automáticamente al contenedor `vitalsy-db`.

| Variable | Requerido | Descripción |
|---|:---:|---|
| `SPRING_DATASOURCE_URL` | No | URL JDBC completa del servidor remoto (ej. `jdbc:postgresql://<host>:5432/postgres`) |
| `SPRING_DATASOURCE_USERNAME` | No | Usuario del servidor remoto |
| `SPRING_DATASOURCE_PASSWORD` | No | Contraseña del servidor remoto |

---

#### 🔐 Seguridad JWT

| Variable | Valor por defecto | Requerido | Descripción |
|---|---|:---:|---|
| `JWT_SECRET` | — | **Sí** | Clave secreta para firmar tokens JWT. Debe ser una cadena aleatoria larga (mín. 32 caracteres) |
| `JWT_EXPIRATION` | `3600000` | No | Tiempo de vida del token en milisegundos (por defecto: 1 hora) |

---

#### 🤖 Inteligencia Artificial

| Variable | Valor por defecto | Requerido | Descripción |
|---|---|:---:|---|
| `GEMINI_API_KEY` | — | **Sí** | API Key obtenida desde [Google AI Studio](https://aistudio.google.com) para acceder al modelo Gemini |
| `SPRING_AI_OPENAI_BASE_URL` | `http://127.0.0.1:1234` | No | URL de una instancia de IA local compatible con OpenAI (LM Studio / Ollama) |
| `SPRING_AI_OPENAI_API_KEY` | `vitalsy-local` | No | Clave ficticia requerida por el cliente HTTP para la IA local |

---

#### 🌐 Puertos de los Servicios

| Variable | Valor por defecto | Requerido | Descripción |
|---|---|:---:|---|
| `BACKEND_PORT` | `8080` | No | Puerto del host mapeado al backend Spring Boot |
| `FRONTEND_PORT` | `80` | No | Puerto del host mapeado al frontend Ionic/Nginx |

### 2. Levantar el Proyecto

Abre tu terminal en la carpeta raíz y ejecuta el siguiente comando para construir y desplegar todas las capas.

```bash
docker-compose up -d --build
```

Esto procesará tres servicios principales:

- `vitalsy-db`: Tu base de datos relacional
- `vitalsy-backend`: Tu API Spring Boot en el puerto 8080
- `vitalsy-frontend`: Tu interfaz de usuario web en el puerto 80

Si deseas detener la ejecución de forma ordenada ejecuta:

```bash
docker-compose down
```

---

## 📂 Estructura de Directorios

Una vista general simplificada del módulo de código fuente.

```text
vitalsy-backend/src/main/java/com/jonesys/vitalsy/
├── config/         # Configuraciones de seguridad CORS y Swagger
├── controller/     # Enrutamiento de la API (Glucosa Usuarios Admin)
├── dto/            # Objetos de transferencia de datos y Mappers
├── exception/      # Manejador global de excepciones
├── model/          # Entidades persistentes de la base de datos
├── repository/     # Interfaces de Spring Data
├── security/       # Filtros JWT y Rate Limiting (Bucket4j)
└── service/        # Lógica clínica e inteligencia artificial
```

---

## 👨‍💻 Equipo de Desarrollo

El ecosistema **VitalSY** fue ideado, desarrollado y es mantenido de forma profesional por:

- 💻 **Joaquín Santana**
- 💻 **Gabriel Hernández**
- 💻 **Gabriel Nercelles**
