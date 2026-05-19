package com.passfail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConstraintFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("🔍 [DB 제약조건 점검] 테이블의 제약조건을 점검합니다.");
        fixConstraints("PAYMENT_HISTORY", "C");
        fixConstraints("POINT_TRANSACTION", "C");
        fixConstraints("SUBMISSION", "U");
        log.info("🚀 [DB 제약조건 점검 완료]");
    }

    private void fixConstraints(String tableName, String constraintType) {
        try {
            String findConstraintSql = 
                "SELECT constraint_name FROM user_constraints " +
                "WHERE table_name = '" + tableName.toUpperCase() + "' " +
                "AND constraint_type = '" + constraintType + "'";
            
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(findConstraintSql);
            
            for (Map<String, Object> row : constraints) {
                String constraintName = (String) row.get("CONSTRAINT_NAME");
                try {
                    log.info("🗑️ {} 테이블 제약조건({}) 삭제 시도: {}", tableName, constraintType, constraintName);
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName);
                    log.info("✅ 삭제 성공: {}", constraintName);
                } catch (Exception e) {
                    log.debug("ℹ️ 제약조건 삭제 건너뜀: {}", constraintName);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ {} 테이블 점검 중 오류 (무시 가능): {}", tableName, e.getMessage());
        }
    }
}
