package com.jonesys.vitalsy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class VitalsyBackendApplication {

	static {
		// CRÍTICO: Fijar la JVM en UTC antes de que Spring inicialice cualquier bean.
		// Garantiza que ZonedDateTime.now() sin zona explícita use UTC,
		// y que Hibernate negocie la sesión JDBC siempre en UTC.
		// La zona del paciente se aplica en la capa de servicio via usuario.getZoneId().
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(VitalsyBackendApplication.class, args);
	}

}
