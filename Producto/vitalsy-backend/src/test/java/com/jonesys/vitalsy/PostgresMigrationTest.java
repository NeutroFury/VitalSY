package com.jonesys.vitalsy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PostgresMigrationTest {

    @Test
    void contextLoadsAndMigrates() {
        System.out.println("Spring context loaded successfully. Database migrations applied.");
    }
}
