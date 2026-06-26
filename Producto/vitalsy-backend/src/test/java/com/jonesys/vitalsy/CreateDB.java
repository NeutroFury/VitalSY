package com.jonesys.vitalsy;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CreateDB {

    @Test
    public void createDatabaseIfNotExist() {
        String url = "jdbc:postgresql://localhost:5432/postgres";
        String user = "postgres";
        String password = "Perroloco12345#";
        
        try {
            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            
            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("Connected to PostgreSQL system database.");
                
                // Check if vitalsy_db database exists
                String checkSql = "SELECT 1 FROM pg_database WHERE datname = 'vitalsy_db'";
                try (ResultSet rs = stmt.executeQuery(checkSql)) {
                    if (rs.next()) {
                        System.out.println("Database 'vitalsy_db' already exists.");
                    } else {
                        System.out.println("Database 'vitalsy_db' does not exist. Creating it...");
                        stmt.executeUpdate("CREATE DATABASE vitalsy_db");
                        System.out.println("Database 'vitalsy_db' created successfully.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating database: " + e.getMessage());
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail(e);
        }
    }
}
