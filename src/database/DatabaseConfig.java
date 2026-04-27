package database;

public final class DatabaseConfig {
    public static final String URL = "jdbc:mysql://localhost:3306/messaging_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final String USER = "root";
    public static final String PASSWORD = "sedkisedki";
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private DatabaseConfig() {
    }
}