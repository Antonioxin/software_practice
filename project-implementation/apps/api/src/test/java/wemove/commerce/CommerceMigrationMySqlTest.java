package wemove.commerce;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.*;
import java.util.*;

@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = "jdbc:mysql:.*commerce_test.*")
class CommerceMigrationMySqlTest {
    @ParameterizedTest
    @ValueSource(strings = {"2", "3"})
    void upgradeRetainsBusinessIdempotencyAndChecksums(String baseline) throws Exception {
        String source = System.getenv("MYSQL_TEST_URL"),
                password = System.getenv("MYSQL_TEST_PASSWORD");
        String database =
                "commerce_upgrade_"
                        + baseline
                        + "_"
                        + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(source, "root", password);
                var statement = connection.createStatement()) {
            statement.execute(
                    "CREATE DATABASE "
                            + database
                            + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        String url = source.replaceFirst("/commerce_test[^?]*", "/" + database);
        Flyway.configure().dataSource(url, "root", password).target(baseline).load().migrate();
        try (var connection = DriverManager.getConnection(url, "root", password);
                var s = connection.createStatement()) {
            s.execute(
                    "INSERT INTO users"
                        + " VALUES(UUID_TO_BIN('aaaaaaaa-0000-0000-0000-000000000001'),'upgrade@example.test','upgrade@example.test','hash','升级账户',NULL,'USER','ACTIVE',0,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))");
            s.execute(
                    "INSERT INTO idempotency_records"
                        + " VALUES(UUID_TO_BIN('aaaaaaaa-0000-0000-0000-000000000002'),UUID_TO_BIN('aaaaaaaa-0000-0000-0000-000000000001'),'catalog.adjustStock:20000000-0000-0000-0000-000000001001',UUID_TO_BIN('aaaaaaaa-0000-0000-0000-000000000003'),'legacy-hash','{\"preserved\":true}',200,UTC_TIMESTAMP(6))");
            s.execute(
                    "INSERT INTO SPRING_SESSION"
                        + " VALUES('upgrade-session','upgrade-session',0,0,1800,9999999999999,'upgrade@example.test')");
            s.execute(
                    "INSERT INTO SPRING_SESSION_ATTRIBUTES"
                            + " VALUES('upgrade-session','SPRING_SECURITY_CONTEXT',X'0102')");
            List<Integer> checksums = new ArrayList<>();
            try (var rs =
                    s.executeQuery(
                            "SELECT checksum FROM flyway_schema_history WHERE version IN ('1','2')"
                                    + " ORDER BY version")) {
                while (rs.next()) checksums.add(rs.getInt(1));
            }
            Flyway flyway = Flyway.configure().dataSource(url, "root", password).load();
            flyway.migrate();
            flyway.validate();
            try (var rs =
                    s.executeQuery(
                            "SELECT checksum FROM flyway_schema_history WHERE version IN ('1','2')"
                                    + " ORDER BY version")) {
                int i = 0;
                while (rs.next()) assertThat(rs.getInt(1)).isEqualTo(checksums.get(i++));
            }
            try (var rs =
                    s.executeQuery(
                            "SELECT response_json,schema_version,execution_state FROM"
                                    + " idempotency_records")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("{\"preserved\":true}");
                assertThat(rs.getInt(2)).isEqualTo(1);
                assertThat(rs.getString(3)).isEqualTo("COMPLETED");
            }
            try (var rs = s.executeQuery("SELECT count(*) FROM users")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
            try (var rs = s.executeQuery("SELECT count(*) FROM catalog_products")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(18);
            }
            try (var rs = s.executeQuery("SELECT count(*) FROM SPRING_SESSION")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(baseline.equals("2") ? 0 : 1);
            }
            System.out.println("Verified MySQL upgrade V" + baseline + " -> latest: " + database);
        }
        // Retain these isolated databases for inspection; never clean a supplied database.
    }
}
