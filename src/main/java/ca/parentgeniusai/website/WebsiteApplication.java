package ca.parentgeniusai.website;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebsiteApplication {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("Connection successful!");
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(WebsiteApplication.class, args);
        System.out.println("SpringApplication.run(...) finished.\n");
    }
}