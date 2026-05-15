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
        log.info("🔍 [DB 제약조건 점검] 결제 관련 테이블의 제약조건을 점검합니다.");
        fixConstraints("PAYMENT_HISTORY");
        fixConstraints("POINT_TRANSACTION");
        log.info("🚀 [DB 제약조건 점검 완료] 이제 'REFUND' 타입 입력이 가능할 것입니다.");
    }

    private void fixConstraints(String tableName) {
        try {
            String findConstraintSql = 
                "SELECT constraint_name FROM user_constraints " +
                "WHERE table_name = '" + tableName + "' " +
                "AND constraint_type = 'C'";
            
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(findConstraintSql);
            
            for (Map<String, Object> row : constraints) {
                String constraintName = (String) row.get("CONSTRAINT_NAME");
                try {
                    log.info("🗑️ {} 테이블 제약조건 삭제 시도: {}", tableName, constraintName);
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
