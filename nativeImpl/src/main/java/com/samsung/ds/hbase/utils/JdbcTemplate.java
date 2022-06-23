package com.samsung.ds.hbase.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;

public class JdbcTemplate {

    private HikariDataSource dataSource = null;

    private static JdbcTemplate instance = null;

    private JdbcTemplate() {
        ConfigProperties configProperties = ConfigProperties.getInstance();
        String driverClass = configProperties.getValue("datasource.driver-class-name");
        String jdbcUrl = configProperties.getValue("datasource.jdbc-url");
        String userName = configProperties.getValue("datasource.username");
        String password = configProperties.getValue("datasource.password");

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClass);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(userName);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setAutoCommit(false);

        dataSource = new HikariDataSource(config);
    }

    public synchronized static JdbcTemplate getInstance() {
        if (instance == null) {
            instance = new JdbcTemplate();
        }

        return instance;
    }

    public Connection getConnection() throws Exception {
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (Exception e) {
            System.err.println("Connection Error : " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Connection Error : " + e.getMessage());
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

    public List<Map<String, Object>> queryForList(String sql, LinkedHashMap<String, Object> datas) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);) {

            if (datas != null) {
                datas.forEach((key, value) -> {
                    try {
                        preparedStatement.setObject(Integer.getInteger(key), value);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.isBeforeFirst()) {
                Map<String, Object> data;

                ResultSetMetaData rsmd = resultSet.getMetaData();
                int columnCount = rsmd.getColumnCount();

                while (resultSet.next()) {
                    data = new HashMap<>();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsmd.getColumnName(i);
                        data.put(columnName, resultSet.getObject(columnName));
                    }

                    list.add(data);
                }
            }
        } catch (SQLException e) {
            throw new Exception(e);
        }

        return list;
    }

    public Map<String, Object> queryForObject(String sql, LinkedHashMap<String, Object> datas) throws Exception {
        List<Map<String, Object>> results = queryForList(sql, datas);

        if (results == null || results.size() < 1) {
            return null;
        }

        if (results.size() != 1) {
            throw new Exception("size not one, size : " + results.size());
        }

        return results.get(0);
    }
}
