import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;

public class Db {
    private HikariDataSource dataSource = null;
    private static Db instance = null;
    private Db() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/test");
        config.setUsername("kangside21");
        config.setPassword("kangside21");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setAutoCommit(false);
        dataSource = new HikariDataSource(config);
    }

    public synchronized static Db getInstance() {
        if(instance == null) {
            instance = new Db();
        }

        return instance;
    }

    public Connection getConnection() throws Exception {
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (Exception e) {
            System.err.println("연결오류 : " + e.getMessage());
            e.printStackTrace();
            throw new Exception("연결오류 : " + e.getMessage());
        }

        return connection;
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            connection = null;
        }
    }

    public void destroyDatabasePool() {
        try {
            dataSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
