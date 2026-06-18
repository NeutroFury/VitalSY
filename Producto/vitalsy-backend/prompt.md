Actúa como Arquitecto Backend Senior en Spring Boot 3.2 (Java 21). Hemos tomado una decisión arquitectónica crítica para el proyecto VitalSY: por motivos de seguridad clínica e integridad de datos médicos, descartaremos el uso de IA (OCR probabilístico) para leer las tablas de insulina. 

En su lugar, implementaremos una carga determinista mediante un archivo Excel (.xlsx) estandarizado.

Por favor, diseña e implementa el flujo de subida y procesamiento del Excel aplicando las siguientes directrices de resiliencia:

1. ENDPOINT DE CARGA (Controller)
- Crea el endpoint `POST /api/v1/pautas/upload/{usuarioId}` que acepte un archivo `multipart/form-data`.

2. PARSEO ESTRICTO CON APACHE POI (Servicio)
- El archivo tendrá 4 pestañas exactas: "Desayuno", "Almuerzo", "Once-Cena (Sin Ejercicio)", "Once-Cena (Con Ejercicio)".
- Mapea cada pestaña al Enum o String correspondiente para el campo 'momentoDia' en la base de datos.
- Itera sobre las filas y columnas. Debes usar Expresiones Regulares (Regex) o limpieza de Strings nativa para extraer los datos puros:
  * Cabeceras (Carbohidratos): Transforma textos como "15g" o " 15 g " a un Double estricto (15.0).
  * Primera columna (Glicemia): Divide rangos como "71-80" en glicemia_min=71 y glicemia_max=80. Si detecta "501+" o "501 o más", define min=501 y max=999.

3. VALIDACIÓN "FAIL-FAST" Y TRANSACCIONALIDAD
- Todo el proceso debe estar anotado con `@Transactional`. 
- Si el código detecta una celda vacía, texto ilegible donde va un número, o una pestaña faltante, DEBE abortar inmediatamente el proceso (lanzar una excepción) detallando el error (Ej: "Formato inválido en Pestaña Almuerzo, Fila 4, Columna 2"). No se debe guardar nada a medias.

4. PERSISTENCIA EN SUPABASE
- Si el Excel es 100% válido, el servicio primero hará un DELETE de todos los registros existentes en 'escala_dosis_fija' para ese 'usuario_id' (para evitar duplicados o solapamientos).
- Finalmente, insertará masivamente mediante el repositorio JPA todos los registros extraídos (usuario_id, momento_dia, glicemia_min, glicemia_max, carbohidratos_gr, dosis_insulina).

Entrégame el código del Controlador y del Servicio optimizado, junto con las dependencias de Apache POI necesarias para el `pom.xml` o `build.gradle`.