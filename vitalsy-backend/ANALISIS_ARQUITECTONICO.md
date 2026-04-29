# 🏗️ ANÁLISIS ARQUITECTÓNICO - VITALSY BACKEND

**Fecha:** 29 de abril de 2026  
**Rol:** Arquitecto de Software Senior  
**Proyecto:** VitalSY Backend - Spring Boot 4.0.5 + Java 21

---

## 📊 RESUMEN EJECUTIVO

**Estado General:** ⚠️ **INCOMPLETO - Requiere Reestructuración**

Tu base técnica es **sólida a nivel de configuración inicial**, pero **carece de la completitud** necesaria para producción. Hay **gaps críticos** en autenticación, estructura de capas y mapeo de entidades. **Se recomienda limpiar y reestructurar antes de conectar el frontend**.

---

## 1️⃣ ESTRUCTURA DE CARPETAS

### ✅ POSITIVO

- Sigue el patrón MVC/DDD correcto
- Separación clara: `controller` → `service` → `repository`
- Uso de `@Component`, `@Service`, `@Repository` apropiado

### ❌ CRÍTICO - FALTA COMPLETITUD

```
Estructura ACTUAL              Estructura RECOMENDADA
├── config/ ✓                 ├── config/
├── controller/ ⚠️            ├── controller/
├── model/ ❌                 ├── dto/ ❌ FALTA
├── repository/ ✓             ├── exception/ ❌ FALTA
├── service/ ⚠️               ├── security/ ❌ FALTA (JWT filters)
├── util/ ✓                   ├── model/
└── VitalsyBackendApplication ├── repository/
                              ├── service/
                              └── util/
```

### Problemas Identificados

- ❌ **No hay DTOs** - Expones directamente las entidades JPA (vulnerabilidad de seguridad)
- ❌ **No hay manejo de excepciones** (`GlobalExceptionHandler`)
- ❌ **No hay capa de autenticación** (JWT, Login, Register)
- ⚠️ **Solo 1 entidad** (GlucoseReading) pero mencionas usuarios, nutrición, glucemia
- ❌ **Un único controlador** (IaController) - Falta AuthController, GlucosaController

---

## 2️⃣ GESTIÓN DE DEPENDENCIAS (build.gradle)

### ✅ POSITIVO

- **Java 21** (LTS, excelente para 2026)
- **Spring Boot 4.0.5** (última versión estable)
- **PostgreSQL driver** incluido
- **Lombok** para reducir boilerplate
- **Spring Data JPA** para persistencia
- **Spring Security** base incluida

### ❌ DEPENDENCIAS CRÍTICAS FALTANTES

```gradle
// ❌ FALTA: JWT para el C.U. 02 (Autenticación)
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'

// ❌ FALTA: Validación de datos
implementation 'org.springframework.boot:spring-boot-starter-validation'

// ❌ FALTA: API Documentation
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2'

// ❌ FALTA: Testing robustos
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.testcontainers:testcontainers:1.19.3'

// ❌ FALTA: Manejo de migraciones BD
implementation 'org.flywaydb:flyway-core'
```

### ⚠️ ADVERTENCIA

- **Spring AI versión MILESTONE** (v2.0.0-M4) - Para producción, espera versión estable
- Está correctamente configurado pero monitorear release oficial

### Veredicto

**⚠️ CRÍTICO** - Falta JWT → Imposible implementar C.U. 02 (Autenticación) sin esto

---

## 3️⃣ CONSISTENCIA DE ENTIDADES vs. SCRIPT SQL

### Mapeo Actual

```java
@Entity
@Table(name = "lecturas_glucosa")
public class GlucoseReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "valor_mgdl")
    private Double valorMgdl;

    private String tendencia;

    @Column(name = "dispositivo_id")
    private String dispositivoId;

    @Column(name = "analisis_ia", columnDefinition = "TEXT")
    private String analisisIa;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
```

### ❌ PROBLEMAS IDENTIFICADOS

| Falta de Entidades | Campo Faltante en GlucoseReading | Impacto |
|---|---|---|
| 🔴 **Usuario** | ❌ `usuario_id` (FK) | No hay auditoría de lecturas |
| 🔴 **Rol** | - | Falta para autenticación |
| 🔴 **Nutrición** | - | No hay tabla para C.U. nutrición |
| 🔴 **Dispositivo** | - | `dispositivoId` debería ser FK, no String |
| 🟡 `@ManyToOne` | FK a Usuario | Las lecturas flotando sin propietario |

### Mapeo Recomendado

```java
@Entity
@Table(name = "lecturas_glucosa")
public class GlucoseReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;  // ← CRÍTICO

    @Column(nullable = false)
    private Double valorMgdl;

    @Column(nullable = false, length = 50)
    private String tendencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispositivo_id")
    private Dispositivo dispositivo;  // ← MEJOR QUE STRING

    @Column(columnDefinition = "TEXT")
    private String analisisIa;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
```

### Entidades Faltantes por Implementar

#### 1. Usuario (Crítica)
```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // Hasheada con BCrypt

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime ultimoAcceso;
}
```

#### 2. Rol (Crítica)
```java
@Entity
@Table(name = "roles")
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre; // ROLE_USER, ROLE_ADMIN

    @Column(length = 255)
    private String descripcion;
}
```

#### 3. Nutrición (Necesaria)
```java
@Entity
@Table(name = "registros_nutricion")
public class RegistroNutricion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Integer calorias;

    @Column(nullable = false)
    private Double proteinas;

    @Column(nullable = false)
    private Double carbohidratos;

    @Column(nullable = false)
    private Double grasas;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;
}
```

---

## 4️⃣ SEGURIDAD Y JWT (C.U. 02 - AUTENTICACIÓN)

### Estado Actual

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll() // ⚠️ No existe
                .requestMatchers("/api/v1/glucosa/**").permitAll() // ⚠️ No existe
                .requestMatchers("/api/v1/ia/**").permitAll()
                .anyRequest().authenticated()); // ⚠️ Nunca se llega aquí
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### ❌ PROBLEMAS CRÍTICOS

1. **✋ SIN JWT FILTER** - `permitAll()` en `/api/v1/auth/**` pero **no hay validación en otros endpoints**
2. **✋ SIN VALIDACIÓN DE TOKEN** - Stateless sin filter = vulnerabilidad
3. **✋ ENDPOINTS SIN PROTECCIÓN** - Todo está permitido sin autenticación real
4. **✋ SIN CONTROLADOR DE AUTH** - No existe `AuthController` con `/login` y `/register`
5. **✋ SIN FLUJO JWT** - No hay generación, validación ni extracción de tokens

### Componentes Faltantes para C.U. 02

#### 1. JWT Provider (Falta)
```java
@Component
public class JwtProvider {
    
    @Value("${jwt.secret:my-secret-key-please-change-in-production}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:3600000}") // 1 hora en ms
    private int jwtExpiration;
    
    public String generateToken(String email, String userId) {
        return Jwts.builder()
            .setSubject(email)
            .claim("userId", userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public String getEmailFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

#### 2. JWT Authentication Filter (Falta)
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String token = getJwtFromRequest(request);
            
            if (token != null && jwtProvider.validateToken(token)) {
                String email = jwtProvider.getEmailFromToken(token);
                
                UserDetails userDetails = new org.springframework.security
                    .core.userdetails.User(email, "", new ArrayList<>());
                
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

#### 3. Auth Service (Falta)
```java
@Service
public class AuthService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtProvider jwtProvider;
    
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        
        String token = jwtProvider.generateToken(usuario.getEmail(), usuario.getId().toString());
        
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        
        return new LoginResponse(token, usuario.getId(), usuario.getEmail(), 3600);
    }
    
    public RegisterResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(request.getNombre());
        usuario.setFechaRegistro(LocalDateTime.now());
        
        Rol rolUser = rolRepository.findByNombre("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("Rol por defecto no encontrado"));
        usuario.setRoles(Set.of(rolUser));
        
        usuarioRepository.save(usuario);
        
        return new RegisterResponse(usuario.getId(), usuario.getEmail());
    }
}
```

#### 4. Auth Controller (Falta)
```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### 5. DTOs para Autenticación (Falta)
```java
public class LoginRequest {
    @NotBlank(message = "Email requerido")
    @Email(message = "Email inválido")
    private String email;
    
    @NotBlank(message = "Contraseña requerida")
    @Size(min = 6, message = "Mínimo 6 caracteres")
    private String password;
}

public class LoginResponse {
    private String token;
    private Long userId;
    private String email;
    private Integer expiresIn;
}

public class RegisterRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 6)
    private String password;
    
    @NotBlank
    @Size(min = 2, max = 100)
    private String nombre;
}

public class RegisterResponse {
    private Long id;
    private String email;
}
```

### Para Ionic App - Retorno Esperado

```json
{
  "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwidXNlcklkIjoiMSIsImlhdCI6MTcxNDQwMDAwMCwiZXhwIjoxNzE0NDAzNjAwfQ...",
  "userId": 1,
  "email": "user@example.com",
  "expiresIn": 3600
}
```

---

## 5️⃣ CONFIGURACIÓN DE BASE DE DATOS

### application.properties - Estado Actual

```properties
spring.application.name=vitalsy-backend

spring.datasource.url=jdbc:postgresql://localhost:5432/vitalsy_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.ai.openai.base-url=http://localhost:1234
spring.ai.openai.api-key=vitalsy-local
spring.ai.openai.chat.options.model=google/gemma-4-e2b:2

spring.jpa.open-in-view=false
```

### ❌ PROBLEMAS CRÍTICOS

| Problema | Riesgo | Solución |
|---|---|---|
| 🔴 `ddl-auto=update` | Migración automática rompe en producción | Usar `validate` + Flyway/Liquibase |
| 🔴 `show-sql=true` | Exposición de queries en logs | Usar perfiles dev/prod |
| 🔴 Sin perfiles | Sin diferencia dev vs prod | Crear `application-dev.properties`, `application-prod.properties` |
| 🔴 Contraseña hardcoded | Seguridad crítica | Usar variables de entorno o .env |
| 🔴 Sin connection pooling | Performance bajo | Configurar HikariCP |
| 🔴 Sin validación | Migración falla silenciosamente | Usar Flyway desde el inicio |

### Configuración Recomendada

#### application.properties (Base)
```properties
spring.application.name=vitalsy-backend
spring.profiles.active=dev

# JWT
jwt.secret=${JWT_SECRET:change-me-in-production}
jwt.expiration=3600000

# Datasource
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

#### application-dev.properties
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/vitalsy_db
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

logging.level.root=INFO
logging.level.com.jonesys.vitalsy=DEBUG

spring.ai.openai.base-url=http://localhost:1234
spring.ai.openai.api-key=vitalsy-local
```

#### application-prod.properties
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

logging.level.root=WARN
logging.level.com.jonesys.vitalsy=INFO

spring.ai.openai.base-url=https://api.openai.com/v1
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

### Estructura de Migraciones con Flyway

```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_usuarios.sql
├── V3__add_roles.sql
├── V4__add_nutricion.sql
└── V5__add_audit_columns.sql
```

#### Ejemplo: V1__initial_schema.sql
```sql
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso TIMESTAMP
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (rol_id) REFERENCES roles(id)
);

CREATE TABLE dispositivos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE lecturas_glucosa (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    valor_mgdl DECIMAL(6,2) NOT NULL,
    tendencia VARCHAR(50),
    dispositivo_id BIGINT,
    analisis_ia TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id)
);

CREATE TABLE registros_nutricion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    calorias INTEGER NOT NULL,
    proteinas DECIMAL(5,2) NOT NULL,
    carbohidratos DECIMAL(5,2) NOT NULL,
    grasas DECIMAL(5,2) NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_lecturas_usuario ON lecturas_glucosa(usuario_id);
CREATE INDEX idx_nutricion_usuario ON registros_nutricion(usuario_id);
CREATE INDEX idx_usuarios_email ON usuarios(email);
```

---

## 6️⃣ OBSERVACIONES ADICIONALES

### DataSeeder
✅ **Bien implementado** para desarrollo
- Verifica duplicados correctamente
- Output clara

⚠️ **Mejoras**:
```java
// Debería seed de Usuario, Rol también para testing completo
if (rolRepository.count() == 0) {
    Rol rolUser = new Rol("ROLE_USER", "Usuario estándar");
    rolRepository.save(rolUser);
}

if (usuarioRepository.count() == 0) {
    Usuario usuario = new Usuario();
    usuario.setEmail("demo@vitalsy.com");
    usuario.setPassword(passwordEncoder.encode("demo123"));
    usuario.setNombre("Demo User");
    usuario.setRoles(Set.of(rolRepository.findByNombre("ROLE_USER").orElseThrow()));
    usuarioRepository.save(usuario);
}
```

### IaService/IaController
✅ **Integración con Gemma correcta**

⚠️ **Problemas**:
1. Sin autenticación (requiere usuario logueado)
2. `@PathVariable Long id` vs Entity `Integer id` (inconsistencia)
3. Sin validación de entrada
4. Sin manejo de errores

**Recomendación:**
```java
@PostMapping("/analizar/{id}")
public ResponseEntity<GlucoseReadingDTO> procesarLectura(
    @PathVariable Long id,
    @AuthenticationPrincipal UserDetails userDetails) {
    
    GlucoseReading lectura = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Lectura no encontrada"));
    
    // Verificar que el usuario sea propietario
    if (!lectura.getUsuario().getEmail().equals(userDetails.getUsername())) {
        throw new AccessDeniedException("No tienes acceso a esta lectura");
    }
    
    String analisis = iaService.analizarGlucosa(lectura.getValorMgdl(), lectura.getTendencia());
    lectura.setAnalisisIa(analisis);
    
    return ResponseEntity.ok(toDTO(repository.save(lectura)));
}
```

### Tests
❌ **Vacíos** (`VitalsyBackendApplicationTests.java`)

**Necesario:**
```
tests/
├── service/
│   ├── AuthServiceTest
│   └── IaServiceTest
├── controller/
│   ├── AuthControllerTest
│   └── IaControllerTest
└── repository/
    └── GlucoseReadingRepositoryTest
```

---

## 7️⃣ RECOMENDACIONES DE INFRAESTRUCTURA

### Estructura DTOs por Carpeta

```
com/jonesys/vitalsy/dto/
├── request/
│   ├── LoginRequest
│   ├── RegisterRequest
│   └── GlucoseReadingRequest
├── response/
│   ├── LoginResponse
│   ├── UsuarioDTO
│   └── GlucoseReadingDTO
└── mapper/
    ├── UsuarioDTOMapper
    └── GlucoseReadingDTOMapper
```

### Exception Handler Global

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(403, ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, message));
    }
}
```

---

## 🎯 PLAN DE ACCIÓN RECOMENDADO (Prioridad)

### **FASE 1: ESTRUCTURA Y DEPENDENCIAS (1 día)**

```
[ ] 1. Actualizar build.gradle con JWT, Validation, Flyway
[ ] 2. Crear carpetas: /dto, /exception, /security
[ ] 3. Agregar GlobalExceptionHandler
[ ] 4. Crear DTOs: LoginRequest, LoginResponse, RegisterRequest
[ ] 5. Crear mappers en /dto/mapper
```

**Tiempo:** ~4 horas

### **FASE 2: ENTIDADES Y BASE DE DATOS (1.5 días)**

```
[ ] 1. Crear entidades: Usuario, Rol, Dispositivo, RegistroNutricion
[ ] 2. Mapear GlucoseReading con @ManyToOne a Usuario
[ ] 3. Crear repositories: UsuarioRepository, RolRepository
[ ] 4. Crear migraciones Flyway (V1__initial_schema.sql)
[ ] 5. Validar esquema en PostgreSQL
[ ] 6. Actualizar DataSeeder con Usuario y Rol
```

**Tiempo:** ~6 horas

### **FASE 3: AUTENTICACIÓN JWT (1.5 días)**

```
[ ] 1. Crear JwtProvider con sign/verify
[ ] 2. Crear JwtAuthenticationFilter
[ ] 3. Crear AuthService (login, register)
[ ] 4. Crear AuthController (/login, /register)
[ ] 5. Configurar SecurityConfig con JWT filter
[ ] 6. Agregar propiedades JWT a application-dev.properties
```

**Tiempo:** ~6 horas

### **FASE 4: REFACTOR Y SEGURIDAD (1 día)**

```
[ ] 1. Refactor IaController para requerir autenticación
[ ] 2. Agregar validación con @Valid a todos los DTO
[ ] 3. Proteger endpoints según roles
[ ] 4. Crear application-prod.properties
[ ] 5. Agregar logging seguro
```

**Tiempo:** ~4 horas

### **FASE 5: TESTING (1 día)**

```
[ ] 1. Tests unitarios para AuthService
[ ] 2. Tests de integración para AuthController
[ ] 3. Tests para IaService
[ ] 4. Postman/Insomnia: flujos login → acceso protegido
[ ] 5. Validar retorno JWT para Ionic
```

**Tiempo:** ~4 horas

---

## 📋 VEREDICTO FINAL

| Aspecto | Calificación | Estado |
|---|---|---|
| **Capas MVC** | 6/10 | Incompleto, estructura correcta |
| **Dependencias** | 4/10 | Falta JWT crítico |
| **Entidades** | 3/10 | Solo 1, falta usuario y relaciones |
| **Seguridad** | 2/10 | ❌ Sin autenticación real |
| **Base de Datos** | 5/10 | Funcionando pero no prod-ready |
| **Documentación** | 5/10 | Code comments son buenos |
| **Tests** | 1/10 | Vacío |
| **Perfiles (dev/prod)** | 1/10 | No configurado |
| **Migraciones BD** | 0/10 | Sin Flyway |
| **API Documentation** | 1/10 | Sin Swagger/OpenAPI |

### **SCORE TOTAL: 4.2/10**

### 🚨 RECOMENDACIÓN PRINCIPAL

**❌ NO CONECTAR FRONTEND ANGULAR/IONIC HASTA:**

1. ✅ Implementar JWT y autenticación (C.U. 02)
2. ✅ Crear DTOs para desacoplar controladores de entidades
3. ✅ Mapear todas las entidades con relaciones `@ManyToOne`
4. ✅ Configurar perfiles dev/prod con Flyway
5. ✅ Agregar manejo de excepciones global
6. ✅ Tests básicos pasando

**Con estas correcciones, pasarías de 4.2/10 a 8.5/10 ✅**

---

## 📞 PRÓXIMOS PASOS

Se recomienda implementar las **5 fases** en orden secuencial. Tiempo total estimado: **5-6 días de desarrollo**.

**Prioridad:** FASE 3 (Autenticación JWT) es crítica para el frontend Ionic/Angular.

---

*Análisis realizado: 29 de abril de 2026*  
*Arquitecto: Senior Software Architect*  
*Proyecto: VitalSY Backend v0.0.1-SNAPSHOT*
