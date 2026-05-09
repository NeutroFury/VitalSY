package com.jonesys.vitalsy.util;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final GlucoseReadingRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(GlucoseReadingRepository repository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Asegurar usuario semilla
        Usuario seedUser = usuarioRepository.findByEmail("seed@example.com").orElseGet(() -> {
            Usuario u = new Usuario();
            u.setEmail("seed@example.com");
            u.setNombre("Seed User");
            u.setPasswordHash(passwordEncoder.encode("SeedPass123!"));
            u.setRol("PACIENTE");
            u.setActivo(true);
            u.setCreadoEn(ZonedDateTime.now());
            return usuarioRepository.save(u);
        });

        // 2. Insertar lecturas si la tabla está vacía
        if (repository.count() == 0) {
            GlucoseReading lectura1 = new GlucoseReading();
            lectura1.setValorMgdl(110);
            lectura1.setTendencia("Stable");
            lectura1.setAnalisisIa("Lectura inicial normal.");
            lectura1.setTipoRegistro("SEED");
            lectura1.setUsuario(seedUser);
            lectura1.setFechaHora(ZonedDateTime.now().minusHours(2));

            GlucoseReading lectura2 = new GlucoseReading();
            lectura2.setValorMgdl(145);
            lectura2.setTendencia("Rising");
            lectura2.setAnalisisIa("Lectura post-comida simulada.");
            lectura2.setTipoRegistro("SEED");
            lectura2.setUsuario(seedUser);
            lectura2.setFechaHora(ZonedDateTime.now());

            repository.saveAll(List.of(lectura1, lectura2));
            System.out.println("✅ DataSeeder: Datos de prueba insertados.");
        }

        // 3. REPARACIÓN: Asignar rol PACIENTE a cualquier usuario que no lo tenga
        usuarioRepository.findAll().forEach(u -> {
            if (u.getRol() == null || u.getRol().isEmpty()) {
                u.setRol("PACIENTE");
                u.setActivo(true);
                usuarioRepository.save(u);
                System.out.println("🛠️ SEEDER: Reparado rol de usuario: " + u.getEmail());
            }
        });
    }
}
