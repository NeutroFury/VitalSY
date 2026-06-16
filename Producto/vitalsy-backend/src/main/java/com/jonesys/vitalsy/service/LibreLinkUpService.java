package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.LibreLinkUpConfig;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.LibreLinkUpConfigRepository;
import com.jonesys.vitalsy.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LibreLinkUpService {

    private static final Logger log = LoggerFactory.getLogger(LibreLinkUpService.class);
    private static final String DEFAULT_LOGIN_URL = "https://api-us.libreview.io";
    private static final String VERSION_HEADER = "4.16.0";
    private static final String PRODUCT_HEADER = "llu.android";

    private final LibreLinkUpConfigRepository configRepository;
    private final GlucoseReadingRepository glucoseRepository;
    private final EncryptionUtil encryptionUtil;
    private final RestClient baseRestClient;

    // Caché en memoria para las sesiones de Abbott (evita logins excesivos)
    private final Map<Integer, AbbottSession> sessionCache = new ConcurrentHashMap<>();

    public LibreLinkUpService(LibreLinkUpConfigRepository configRepository,
                              GlucoseReadingRepository glucoseRepository,
                              EncryptionUtil encryptionUtil) {
        this.configRepository = configRepository;
        this.glucoseRepository = glucoseRepository;
        this.encryptionUtil = encryptionUtil;
        this.baseRestClient = RestClient.builder()
                .defaultHeader("accept-encoding", "gzip")
                .defaultHeader("cache-control", "no-cache")
                .defaultHeader("connection", "Keep-Alive")
                .defaultHeader("content-type", "application/json")
                .defaultHeader("product", PRODUCT_HEADER)
                .defaultHeader("version", VERSION_HEADER)
                .build();
    }

    /**
     * Tarea programada: Sincroniza todas las cuentas activas cada 5 minutos.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutos en ms
    @Transactional
    public void scheduleSync() {
        log.info("⏰ Iniciando tarea programada de sincronización de glucosa (LibreLinkUp)...");
        List<LibreLinkUpConfig> configs = configRepository.findByActivoTrue();
        for (LibreLinkUpConfig config : configs) {
            try {
                syncUserReadings(config);
            } catch (Exception e) {
                log.error("❌ Error sincronizando lecturas para usuario {}: {}", config.getUsuario().getEmail(), e.getMessage());
            }
        }
    }

    /**
     * Sincroniza lecturas para un usuario específico y las guarda en BD sin duplicados.
     */
    @Transactional
    public int syncUserReadings(LibreLinkUpConfig config) {
        Usuario usuario = config.getUsuario();
        String password = encryptionUtil.decrypt(config.getLibrePassword());
        
        log.info("🔄 Sincronizando glucosa para usuario: {}", usuario.getEmail());

        // Obtener sesión desde la caché o realizar login si no existe
        AbbottSession session = sessionCache.computeIfAbsent(usuario.getId(), 
                k -> login(config.getLibreEmail(), password));
        
        String patientId = config.getLibrePatientId();
        
        try {
            if (patientId == null || patientId.isEmpty()) {
                patientId = fetchPatientIdAndSave(config, session);
            }
            Map<String, Object> graphResponse = getGraphData(session, patientId);
            return finalizeSync(usuario, config, graphResponse);
        } catch (Exception e) {
            log.warn("⚠️ Llamada a la API de Abbott falló. Posible expiración de token. Reintentando login: {}", e.getMessage());
            
            // Invalidar caché y forzar nuevo login
            sessionCache.remove(usuario.getId());
            session = login(config.getLibreEmail(), password);
            sessionCache.put(usuario.getId(), session);
            
            // Reintentar flujo
            if (patientId == null || patientId.isEmpty()) {
                patientId = fetchPatientIdAndSave(config, session);
            }
            Map<String, Object> graphResponse = getGraphData(session, patientId);
            return finalizeSync(usuario, config, graphResponse);
        }
    }

    private String fetchPatientIdAndSave(LibreLinkUpConfig config, AbbottSession session) {
        List<Map<String, Object>> connections = getConnections(session);
        if (connections.isEmpty()) {
            throw new RuntimeException("La cuenta de LibreLinkUp no sigue a ningún paciente.");
        }
        String patientId = (String) connections.get(0).get("patientId");
        config.setLibrePatientId(patientId);
        configRepository.save(config);
        log.info("📌 Conexión detectada y guardada. Patient ID: {}", patientId);
        return patientId;
    }

    private int finalizeSync(Usuario usuario, LibreLinkUpConfig config, Map<String, Object> graphResponse) {
        int guardados = processAndSaveGraphData(usuario, graphResponse);
        config.setUltimoSync(ZonedDateTime.now());
        configRepository.save(config);
        log.info("✅ Sincronización exitosa. Registros nuevos guardados: {}", guardados);
        return guardados;
    }

    // --- MÉTODOS DE COMUNICACIÓN CON LA API DE ABBOTT ---

    private AbbottSession login(String email, String password) {
        String url = DEFAULT_LOGIN_URL + "/llu/auth/login";
        Map<String, String> body = Map.of("email", email, "password", password);

        Map<String, Object> response = baseRestClient.post()
                .uri(url)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("status")) {
            throw new RuntimeException("Respuesta inválida al autenticar con Abbott");
        }

        Integer status = (Integer) response.get("status");
        if (status == 2) {
            throw new RuntimeException("Credenciales de LibreLinkUp incorrectas.");
        } else if (status == 4) {
            throw new RuntimeException("Se requiere acción adicional en la cuenta (MFA). Por favor ingresa a la App oficial.");
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null && Boolean.TRUE.equals(data.get("redirect"))) {
            // Manejar redirección regional
            String region = (String) data.get("region");
            log.info("🌍 Redirección regional detectada hacia: {}", region);
            String regionalBaseUrl = getRegionalUrl(region);
            
            // Reintento en el servidor regional
            return loginRegional(regionalBaseUrl, email, password);
        }

        return extractSession(response, DEFAULT_LOGIN_URL);
    }

    private AbbottSession loginRegional(String regionalBaseUrl, String email, String password) {
        String url = regionalBaseUrl + "/llu/auth/login";
        Map<String, String> body = Map.of("email", email, "password", password);

        Map<String, Object> response = baseRestClient.post()
                .uri(url)
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractSession(response, regionalBaseUrl);
    }

    private AbbottSession extractSession(Map<String, Object> response, String baseUrl) {
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> authTicket = (Map<String, Object>) data.get("authTicket");
        Map<String, Object> user = (Map<String, Object>) data.get("user");

        String token = (String) authTicket.get("token");
        String accountId = (String) user.get("id");

        return new AbbottSession(token, accountId, baseUrl);
    }

    private String getRegionalUrl(String region) {
        // Hacemos un llamado a configurar países para buscar la url de la región
        String url = DEFAULT_LOGIN_URL + "/llu/config/country?country=DE";
        Map<String, Object> response = baseRestClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> regionalMap = (Map<String, Object>) data.get("regionalMap");
        Map<String, Object> regionDef = (Map<String, Object>) regionalMap.get(region);
        
        if (regionDef == null) {
            throw new RuntimeException("Región de LibreLinkUp no soportada: " + region);
        }

        return (String) regionDef.get("lslApi");
    }

    private List<Map<String, Object>> getConnections(AbbottSession session) {
        String url = session.baseUrl() + "/llu/connections";
        Map<String, Object> response = baseRestClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + session.token())
                .header("account-id", sha256(session.accountId()))
                .retrieve()
                .body(Map.class);

        return (List<Map<String, Object>>) response.get("data");
    }

    private Map<String, Object> getGraphData(AbbottSession session, String patientId) {
        String url = session.baseUrl() + "/llu/connections/" + patientId + "/graph";
        Map<String, Object> response = baseRestClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + session.token())
                .header("account-id", sha256(session.accountId()))
                .retrieve()
                .body(Map.class);

        return (Map<String, Object>) response.get("data");
    }

    // --- PROCESAMIENTO Y FILTRADO DE DATOS ---

    private int processAndSaveGraphData(Usuario usuario, Map<String, Object> graphResponse) {
        List<Map<String, Object>> graphData = (List<Map<String, Object>>) graphResponse.get("graphData");
        Map<String, Object> connection = (Map<String, Object>) graphResponse.get("connection");
        
        List<Map<String, Object>> rawReadings = new ArrayList<>();
        
        // Agregar lectura de tiempo real actual
        if (connection != null && connection.get("glucoseMeasurement") != null) {
            rawReadings.add((Map<String, Object>) connection.get("glucoseMeasurement"));
        }
        // Agregar lecturas del gráfico histórico (últimas 24 horas)
        if (graphData != null) {
            rawReadings.addAll(graphData);
        }

        // Obtener números de serie activos si los hay para setear dispositivoId
        Map<String, Object> activeSensor = (connection != null && connection.get("sensor") != null) 
                ? (Map<String, Object>) connection.get("sensor") : null;
        String sn = (activeSensor != null) ? (String) activeSensor.get("sn") : "FreeStyle Libre Cloud";

        int savedCount = 0;
        
        // Formateador típico para el timestamp de Abbott (ej: "10/24/2021 10:14:00 AM")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:m:s a", Locale.ENGLISH);

        for (Map<String, Object> rawItem : rawReadings) {
            try {
                String factoryTimestamp = (String) rawItem.get("FactoryTimestamp");
                Integer valor = (Integer) rawItem.get("Value");
                Integer trendArrow = (Integer) rawItem.get("TrendArrow");
                
                if (factoryTimestamp == null || valor == null) continue;

                // Parsea el timestamp (Abbott envía timestamps en UTC locales al dispositivo)
                LocalDateTime localDateTime = LocalDateTime.parse(factoryTimestamp, formatter);
                ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));

                // Verificar duplicados para evitar re-escribir la base de datos
                List<GlucoseReading> existente = glucoseRepository.findByUsuarioAndFechaHoraBetween(
                        usuario, 
                        zonedDateTime.minusSeconds(10), 
                        zonedDateTime.plusSeconds(10)
                );

                if (existente.isEmpty()) {
                    GlucoseReading reading = new GlucoseReading();
                    reading.setUsuario(usuario);
                    reading.setValorMgdl(valor);
                    reading.setTipoRegistro("SENSOR_CLOUD");
                    reading.setDispositivoId(sn);
                    reading.setTendencia(mapTrendArrow(trendArrow));
                    reading.setFechaHora(zonedDateTime);
                    reading.setCreadoEn(ZonedDateTime.now());
                    
                    // Si el valor es de hipo/hiper severa, podemos agregar un comentario
                    if (valor < 70) {
                        reading.setComentarios("Alerta automática: Nivel bajo de azúcar en sangre.");
                    } else if (valor > 180) {
                        reading.setComentarios("Alerta automática: Nivel alto de azúcar en sangre.");
                    }

                    glucoseRepository.save(reading);
                    savedCount++;
                }
            } catch (Exception e) {
                // Si falla un parseo, continuamos con los demás elementos
                log.warn("Fallo al procesar item de glucemia individual: {}", e.getMessage());
            }
        }
        
        return savedCount;
    }

    private String mapTrendArrow(Integer arrow) {
        if (arrow == null) return "Stable";
        return switch (arrow) {
            case 1 -> "FallingFast"; // Flecha recta abajo
            case 2 -> "Falling";     // Flecha diagonal abajo
            case 3 -> "Stable";      // Flecha recta derecha
            case 4 -> "Rising";      // Flecha diagonal arriba
            case 5 -> "RisingFast";  // Flecha recta arriba
            default -> "Stable";
        };
    }

    // --- UTILITARIOS CRIPTOGRÁFICOS Y MODELOS ---

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record AbbottSession(String token, String accountId, String baseUrl) {}
}
