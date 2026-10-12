package bw.co.gimble.cove.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate generates CHECK constraints listing every enum value when it first
 * creates a table, but ddl-auto=update never refreshes them. Adding an enum
 * value (a new listing status, notification type...) then fails on insert.
 * Enum columns are validated by the application, so drop those stale checks.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SchemaMaintenance implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    private static final List<String[]> ENUM_CHECKS = List.of(
            new String[]{"listings", "listings_status_check"},
            new String[]{"notifications", "notifications_type_check"},
            new String[]{"user_roles", "user_roles_role_check"},
            new String[]{"audit_log_entries", "audit_log_entries_action_check"}
    );

    @Override
    public void run(ApplicationArguments args) {
        for (String[] check : ENUM_CHECKS) {
            try {
                jdbc.execute("ALTER TABLE IF EXISTS " + check[0] + " DROP CONSTRAINT IF EXISTS " + check[1]);
            } catch (Exception e) {
                log.warn("Could not drop constraint {} on {}: {}", check[1], check[0], e.getMessage());
            }
        }
    }
}
