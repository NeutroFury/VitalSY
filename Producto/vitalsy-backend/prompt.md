Actúa como un Arquitecto de Software Senior especializado en HealthTech. > Estoy desarrollando "VitalSY", una plataforma de monitoreo metabólico de grado empresarial. Mi stack tecnológico es: Backend en Spring Boot (Java), Frontend en Ionic con React, Base de Datos en Supabase (PostgreSQL), e integración con Firebase Cloud Messaging (FCM) para notificaciones.

Tu tarea: Generar el código paso a paso para el nuevo módulo de "Alertas de Glucosa Personalizadas con Notificaciones Push".

Por favor, divide tu respuesta en los siguientes 3 pasos obligatorios, respetando Clean Architecture y el manejo de errores:

Paso 1: Capa de Datos (Supabase)

Redacta el script SQL exacto para agregar 3 nuevas columnas a mi tabla usuarios existente: umbral_hipoglicemia (INT, default 70), umbral_hiperglicemia (INT, default 180) y fcm_token (VARCHAR).

Paso 2: Capa Backend (Spring Boot)

Actualiza la entidad Usuario.java agregando estos tres nuevos campos.

Crea un servicio NotificacionService.java que utilice el SDK de firebase-admin para enviar un mensaje Push al token FCM.

Modifica mi lógica en GlucosaService.java (o redacta un interceptor conceptual) que evalúe si una nueva medición de glucosa ingresada es menor/igual al umbral de hipoglicemia o mayor/igual al de hiperglicemia del usuario. Si es así, debe invocar a NotificacionService enviando una alerta urgente.

Paso 3: Capa Frontend (Ionic + React)

Redacta el código de un componente para la Vista de Perfil donde el paciente pueda modificar visualmente sus umbrales.

Implementa la lógica utilizando el plugin @capacitor/push-notifications para solicitar permisos al usuario de iOS/Android, capturar el fcm_token del dispositivo, y realizar un PUT/PATCH a la API REST de Spring Boot para guardarlo en la base de datos.

Restricciones:

Mantén el código enfocado en la seguridad y la fiabilidad clínica.

Asume que el endpoint REST del backend para actualizar el usuario ya está estructurado, solo enfócate en el payload JSON que enviará el frontend y cómo el backend lo procesa.