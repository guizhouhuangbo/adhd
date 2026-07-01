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
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return resultSet.next();
        }
    }
}
