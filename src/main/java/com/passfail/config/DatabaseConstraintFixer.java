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
        
        log.info("🛠️ [DB 스키마 확장] 컬럼 타입을 LONGTEXT로 확장을 시도합니다.");
        expandColumnType("submission", "code");
        expandColumnType("problem", "description");
        expandColumnType("test_case", "input_data");
        expandColumnType("test_case", "expected_output");
        
        log.info("🚀 [DB 제약조건 점검 완료]");
    }

    private void expandColumnType(String tableName, String columnName) {
        try {
            log.info("⚙️ {} 테이블의 {} 컬럼을 LONGTEXT로 변경 중...", tableName, columnName);
            jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " LONGTEXT");
            log.info("✅ {} 테이블의 {} 컬럼 변경 완료", tableName, columnName);
        } catch (Exception e) {
            log.debug("ℹ️ {} 컬럼 변경 건너뜀 (이미 변경되었거나 권한 부족): {}", columnName, e.getMessage());
        }
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
