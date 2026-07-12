package com.huangbo.adhd.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfig {

    private final DataSource dataSource;

    public DatabaseMigrationConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasColumn(connection, "task", "current_step_index")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "ALTER TABLE task ADD COLUMN current_step_index INT NOT NULL DEFAULT 0 AFTER completed"
                    );
                }
            }
            if (!hasColumn(connection, "check_in", "check_date")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "ALTER TABLE check_in ADD COLUMN check_date DATE NULL AFTER task_id"
                    );
                    statement.executeUpdate("UPDATE check_in SET check_date = DATE(created_at) WHERE check_date IS NULL");
                    statement.executeUpdate("ALTER TABLE check_in MODIFY check_date DATE NOT NULL");
                }
            }
            if (!hasIndex(connection, "check_in", "uk_check_in_user_task_date")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "ALTER TABLE check_in ADD UNIQUE KEY uk_check_in_user_task_date (user_id, task_id, check_date)"
                    );
                }
            }
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
