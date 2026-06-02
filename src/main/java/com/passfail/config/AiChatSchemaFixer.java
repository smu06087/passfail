package com.passfail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiChatSchemaFixer implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!isMySql()) {
            return;
        }

        try {
            List<String> dataTypes = jdbcTemplate.queryForList(
                """
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'ai_chat_message'
                      AND COLUMN_NAME = 'content'
                    """,
                String.class
            );

            if (dataTypes.isEmpty()) {
                log.warn("ai_chat_message.content column was not found. Skip AI chat schema fix.");
                return;
            }

            String dataType = dataTypes.get(0).toLowerCase(Locale.ROOT);
            if ("longtext".equals(dataType)) {
                return;
            }

            jdbcTemplate.execute("ALTER TABLE ai_chat_message MODIFY content LONGTEXT NOT NULL");
            log.info("Changed ai_chat_message.content from {} to LONGTEXT.", dataType);
        } catch (Exception ex) {
            log.warn("Failed to fix ai_chat_message.content column type: {}", ex.getMessage());
        }
    }

    private boolean isMySql() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase(Locale.ROOT).contains("mysql");
        } catch (Exception ex) {
            log.warn("Failed to detect database type for AI chat schema fix: {}", ex.getMessage());
            return false;
        }
    }
}
