import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/vitalsy_db", "postgres", "postgres")) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, "usuarios", null);
            System.out.println("Columns in 'usuarios' table:");
            while (rs.next()) {
                System.out.println("- " + rs.getString("COLUMN_NAME"));
            }
            System.out.println("Check complete.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
