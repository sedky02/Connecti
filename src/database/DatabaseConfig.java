package database;

public final class DatabaseConfig {
    public static final String URL = requireEnv("DB_URL");
    public static final String USER = requireEnv("DB_USER");
    public static final String PASSWORD = requireEnv("DB_PASSWORD");
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private DatabaseConfig() {
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }
}