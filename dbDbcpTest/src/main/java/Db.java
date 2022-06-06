import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Db {
    private BasicDataSource dataSource = null;
    private static Db instance = null;
    private Db() {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUsername("kangside21");
        dataSource.setPassword("kangside21");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/test");
        dataSource.setInitialSize(20);
        dataSource.setMaxTotal(100);
        dataSource.setMaxIdle(100);
        dataSource.setMinIdle(20);
        dataSource.setMaxWaitMillis(30000);

        // 커넥션이 유효한지 검사하기 위한 쿼리
        dataSource.setValidationQuery("select 1");
        // 커넥션이 유효한지 검사하는 쿼리가 실행되고 응답을 기다리는 시간
        dataSource.setValidationQueryTimeout(-1);
        // 커넥션마다 PreparedStatement 풀링 여부
        dataSource.setPoolPreparedStatements(false);
        // 커넥션마다 최대로 풀링할 PreparedStatement의 개수
        dataSource.setMaxOpenPreparedStatements(-1);
        // 커넥션의 생성후 최대로 이용 가능한 시간
        dataSource.setMaxConnLifetimeMillis(-1);
        // 버려진 커넥션에 대한 로그 저장 여부
        dataSource.setLogAbandoned(false);

        try {
            dataSource.start();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
