package wemove.dealership;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class DealershipOpenApiTest {
    @Test
    void contractIsValidYamlAndRegistersImplementedPaths() throws Exception {
        Path contract = Path.of("..", "..", "contracts", "openapi", "dealership.yaml").normalize();
        Map<?, ?> document = new Yaml().load(Files.readString(contract));
        assertThat(document.get("openapi")).isEqualTo("3.1.0");
        java.util.Set<String> paths = ((Map<?, ?>) document.get("paths")).keySet().stream()
                .map(Object::toString).collect(java.util.stream.Collectors.toSet());
        assertThat(paths)
                .contains("/channels", "/dealer-applications", "/dealer/catalog",
                        "/inquiries", "/admin/dealer-applications/{id}/review",
                        "/admin/companies/{id}/suspend", "/admin/channels/{id}/publish");
    }
}
