# 🏥 Guía de Poblamiento de Datos (Data Seeding) - VitalSY

Este documento contiene las instrucciones técnicas detalladas para inicializar la base de datos de **VitalSY** con información clínica de prueba. Este proceso (conocido como *Data Seeding*) está diseñado exclusivamente para entornos de desarrollo local o despliegues aislados en contenedores (Docker), utilizando nuestro stack de **Spring Boot (Java 21)** y **PostgreSQL 16**.

> [!WARNING]
> **ADVERTENCIA DE PRODUCCIÓN:** Bajo ninguna circunstancia se debe ejecutar este comando en los entornos de Staging o Producción (ej. AWS, Render, Heroku). La base de datos productiva alojada en **Supabase** ya cuenta con información real y sensible de los usuarios.

---

## 🌐 Contexto del Entorno Cloud vs Local

El ecosistema de VitalSY es complejo y depende de múltiples integraciones de terceros. Si tu objetivo principal es realizar pruebas de flujo completo (End-to-End) o revisiones de UX/UI, te recomendamos **utilizar la versión web y móvil ya desplegada**. 

Nuestro entorno productivo ya se encuentra:
1. **Sincronizado con Supabase:** Gestión de base de datos relacional y servicios de autenticación reales.
2. **Integrado con Gemini IA:** Los algoritmos de análisis y predicción de tendencias glucémicas (riesgo de hipo/hiperglucemia) procesan datos reales de manera asíncrona.
3. **Conectado a APIs Médicas:** Sincronización en tiempo real con *FreeStyle LibreLinkUp* de Abbott para la lectura continua de glucosa (CGM).

**Por lo tanto, el uso de este script local está orientado estrictamente a:**
- Desarrolladores backend que necesiten probar nuevos endpoints en `localhost:8080`.
- Ejecución de pruebas unitarias o de integración automáticas en pipelines de CI/CD.
- Simulación de perfiles clínicos muy específicos para calibrar nuevos algoritmos de IA en un entorno controlado.

---

## 📋 Requisitos Previos

Antes de intentar ejecutar el poblamiento de datos, asegúrate de cumplir con lo siguiente en tu estación de trabajo:
- **Java 21** instalado y configurado correctamente en tus variables de entorno (`JAVA_HOME`).
- **PostgreSQL 16** ejecutándose localmente (o un contenedor de Docker activo con el motor de base de datos).
- La base de datos llamada `vitalsy_db` debe estar creada (las tablas y el esquema relacional se generarán automáticamente vía Hibernate o Flyway).
- El archivo `application-dev.properties` o tu archivo `.env` debe contar con las credenciales locales correctas para conectarse a Postgres.

---

## ⚙️ ¿Qué datos genera el `DataInitializer`?

El componente de inicialización no solo inserta registros al azar, sino que está diseñado para crear un entorno clínico de pruebas robusto, coherente y realista. Su ejecución desencadena las siguientes fases:

### 1. Limpieza de Entorno (Wipe de Datos)
Elimina de forma segura cualquier registro residual de pruebas anteriores. Para evitar errores de integridad referencial (Foreign Keys), la eliminación se realiza mediante un borrado en cascada inverso:
`Alertas Predictivas ➔ Registros de Insulina ➔ Bitácoras de Nutrición ➔ Registros de Glucemia ➔ Parámetros Clínicos ➔ Usuarios`

### 2. Generación de Perfiles de Usuario
- **3 Cuentas de Administrador:** Con permisos totales sobre la plataforma y módulos de analítica.
- **5 Cuentas de Pacientes Clínicos:** Incluyendo perfiles específicos asignados a los miembros del equipo de desarrollo.

### 3. Simulación de Historial Clínico Coherente
Para poner a prueba las gráficas del frontend y el motor de IA, se requiere un volumen significativo y variado de datos:
- **Para el paciente principal (Joaquín Santana):**
  - Se inyectan más de **30 registros de glucemia** históricos con distintos orígenes de captura (`MANUAL`, `SENSOR_NFC`, `SENSOR_BLE`) y métricas de tendencia de flecha (*Stable, Rising, Falling*).
  - Múltiples **registros de insulina** (Rápida y Basal) asociados directamente a ingestas de comidas o correcciones.
  - **Bitácoras nutricionales** detalladas (clasificadas en desayuno, almuerzo, etc.) con el cálculo en gramos de macronutrientes: carbohidratos, proteínas y grasas.
  - **Alertas del sistema predictivo** (`PREDICCION_HIPO`, `PREDICCION_HIPER`) para poder probar el renderizado de notificaciones push o alertas visuales.
  - **Parámetros Clínicos Base:** Ratio de Carbohidratos (IC), Factor de Sensibilidad (IS) y objetivos glucémicos personalizados.
- **Para los demás pacientes de prueba:** Se generan entre 10 y 20 registros mixtos de glucemia y alimentación, cantidad suficiente para testear reportes generales y listas de pacientes.

---

## 👥 Credenciales de Acceso para Pruebas

Una vez que el script finalice, podrás iniciar sesión en el cliente (Frontend, Postman o Swagger) utilizando las siguientes credenciales estandarizadas:

### 🛡️ Perfiles Administrativos (`Rol: ADMIN`)
| Usuario / Integrante | Correo Electrónico | Contraseña |
| :--- | :--- | :--- |
| **Admin General** | `admin@vitalsy.cl` | `vitalsy123` |
| **Gabriel Hernández** | `ghernandez@vitalsy.cl` | `vitalsy123` |
| **Gabriel Nercelles** | `gnercelles@vitalsy.cl` | `vitalsy123` |

### 🩺 Perfiles de Paciente (`Rol: PACIENTE`)
| Usuario / Integrante | Correo Electrónico | Contraseña | Detalle del Contexto |
| :--- | :--- | :--- | :--- |
| **Joaquín Santana** | `jsantana@vitalsy.cl` | `vitalsy123` | *Perfil Completo (+30 registros, parámetros y alertas activas)* |
| **Paciente Prueba 1** | `paciente1@vitalsy.cl` | `vitalsy123` | *Perfil Básico (10-20 registros para gráficos simples)* |
| **Paciente Prueba 2** | `paciente2@vitalsy.cl` | `vitalsy123` | *Perfil Básico (10-20 registros)* |
| **Paciente Prueba 3** | `paciente3@vitalsy.cl` | `vitalsy123` | *Perfil Básico (10-20 registros)* |

---

## 🚀 Instrucciones de Ejecución Paso a Paso

El poblamiento ha sido integrado de forma nativa en el ciclo de vida de la aplicación Spring Boot a través de un `CommandLineRunner` que responde a un argumento específico en el arranque.

### Opción A: Ejecución mediante Terminal (Maven / Gradle)
1. Abre tu terminal y posiciónate en el directorio raíz del backend (donde está ubicado el archivo `pom.xml` o `build.gradle`).
2. Levanta la aplicación pasando el argumento `--populate`:
   ```bash
   # Para proyectos que utilizan Maven:
   mvn spring-boot:run -Dspring-boot.run.arguments=--populate
   
   # Para proyectos que utilizan Gradle:
   ./gradlew bootRun --args="--populate"
   ```

### Opción B: Ejecución mediante un IDE (IntelliJ IDEA, Eclipse, VS Code)
1. Dirígete a la ventana de configuración de ejecución de tu proyecto (*Run/Debug Configurations*).
2. Busca el campo **Program arguments** (Argumentos del programa).
3. Escribe literalmente: `--populate`.
4. Guarda los cambios y ejecuta/debuguea la clase principal `VitalsyApplication.java`.

### Opción C: Ejecución dentro de Docker Compose
Si manejas todo el entorno local con Docker, puedes inyectar los datos ejecutando el comando directamente dentro del contenedor del backend ya en ejecución:
```bash
docker-compose exec backend java -jar app.jar --populate
```

---

## ✅ Verificación y Troubleshooting

**¿Cómo sé que el proceso funcionó correctamente?**
Revisa la consola de logs de Spring Boot. Deberías visualizar una secuencia similar a la siguiente:
```text
[INFO] c.d.v.config.DataInitializer : Iniciando limpieza de tablas por integridad referencial...
[INFO] c.d.v.config.DataInitializer : Creando perfiles de usuarios y administradores...
[INFO] c.d.v.config.DataInitializer : Generando historial clínico complejo para Joaquín Santana...
[INFO] c.d.v.config.DataInitializer : ¡Poblamiento de datos clínicos finalizado exitosamente!
```

**Solución de Problemas Comunes:**
- 🔴 **Error de Conexión (Connection Refused):** Asegúrate de que el servicio de PostgreSQL esté efectivamente corriendo en el puerto `5432` y que las credenciales (`spring.datasource.username` y `password`) en tu archivo `.properties` o `.env` sean correctas.
- 🔴 **Violación de Llave Foránea (Constraint Violation) durante la limpieza:** Si llegase a ocurrir un error al limpiar datos, verifica que la clase `DataInitializer` o su método `run()` esté debidamente anotado con `@Transactional`, asegurando que la limpieza y posterior creación ocurran dentro de la misma transacción de base de datos.

---

> 💡 *Nota de Desarrollo:* Si requieres modificar las constantes generadas (por ejemplo, alterar los ratios de insulina, probar fechas específicas o agregar nuevos alimentos al registro de nutrición), puedes editar la lógica directamente en el código fuente ubicado en: 
> `📁 src/main/java/cl/duocuc/vitalsy/config/DataInitializer.java`
