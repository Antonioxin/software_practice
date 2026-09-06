package wemove;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import javax.sql.DataSource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import wemove.catalog.domain.ProductEntity;
import wemove.catalog.platform.CatalogPort;
import wemove.catalog.platform.InventoryPort;
import wemove.config.WemoveProperties;
import wemove.identity.domain.UserEntity;
import wemove.platform.IdentityPort;
import wemove.platform.UnitOfWork;
import wemove.platform.idempotency.IdempotencyRecordEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:package-scan;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "wemove.bootstrap.admin-email=scan-admin@example.test",
    "wemove.bootstrap.admin-password=ScanTestPassword123!"
})
@AutoConfigureMockMvc
class ApplicationIntegrationTest {
    @Autowired ApplicationContext context;
    @Autowired EntityManager entities;
    @Autowired MockMvc mvc;
    @Autowired DataSource dataSource;

    // H2 verifies application wiring and HTTP security; MySQL migration checks are separate.
    @Test
    @Sql("classpath:org/springframework/session/jdbc/schema-h2.sql")
    void rootScanIncludesBothModulesAndSharedInfrastructure() throws Exception {
        assertThat(context.getBean(IdentityPort.class)).isNotNull();
        assertThat(context.getBean(CatalogPort.class)).isNotNull();
        assertThat(context.getBean(InventoryPort.class)).isNotNull();
        assertThat(context.getBean(UnitOfWork.class)).isNotNull();
        assertThat(context.getBean(WemoveProperties.class).bootstrap().adminEmail())
            .isEqualTo("scan-admin@example.test");
        assertThat(entities.getMetamodel().entity(UserEntity.class)).isNotNull();
        assertThat(entities.getMetamodel().entity(ProductEntity.class)).isNotNull();
        assertThat(entities.getMetamodel().entity(IdempotencyRecordEntity.class)).isNotNull();

        mvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").exists());
        mvc.perform(get("/api/v1/admin/products")).andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .header("Origin", "http://localhost:5173")
                .contentType("application/json")
                .content("{\"email\":\"scan-admin@example.test\",\"password\":\"ScanTestPassword123!\"}"))
            .andExpect(status().isOk()).andReturn();
        mvc.perform(get("/api/v1/auth/me").cookie(login.getResponse().getCookies()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.baseRole").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/products").cookie(login.getResponse().getCookies()))
            .andExpect(status().isOk());

        var anonymous = mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk()).andReturn();
        var jdbc = new JdbcTemplate(dataSource);
        Integer sessionsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES "
            + "WHERE ATTRIBUTE_NAME = 'SPRING_SECURITY_CONTEXT'", Integer.class)).isEqualTo(1);
        new ResourceDatabasePopulator(new ClassPathResource(
            "db/migration/V3__invalidate_legacy_principal_sessions.sql")).execute(dataSource);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class))
            .isEqualTo(sessionsBefore - 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/v1/auth/me").cookie(login.getResponse().getCookies()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/csrf").cookie(anonymous.getResponse().getCookies()))
            .andExpect(status().isOk());
    }
}
