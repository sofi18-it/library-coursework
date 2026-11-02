package ua.edu.nupp.library.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() { }

    public static Connection open() throws SQLException {
        try {
            // Tomcat may load JDBC providers from the web application's classloader after DriverManager starts.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J is missing from the server application.", e);
        }
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/library_coursework?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Kyiv");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
