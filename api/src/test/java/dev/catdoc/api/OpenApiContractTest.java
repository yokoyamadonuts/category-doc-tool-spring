package dev.catdoc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * openapi/openapi.yaml を正本とし、springdoc が実装から出力する仕様と比べる。
 * 比べるのは、パスとメソッドの組と、各操作の成功時の状態コード。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    private static final Set<String> METHODS = Set.of("get", "post", "put", "delete", "patch");

    @Autowired
    MockMvcTester mvc;

    @Test
    void 実装の操作と成功時の状態コードが仕様書と一致する() throws Exception {
        JsonNode spec = new YAMLMapper().readTree(Path.of(System.getProperty("openapi.spec")).toFile());
        String generatedJson = mvc.get().uri("/v3/api-docs").exchange().getResponse().getContentAsString();
        JsonNode generated = new ObjectMapper().readTree(generatedJson);

        assertThat(successCodesByOperation(generated)).isEqualTo(successCodesByOperation(spec));
    }

    private static Map<String, Set<String>> successCodesByOperation(JsonNode openApi) {
        Map<String, Set<String>> result = new TreeMap<>();
        for (Map.Entry<String, JsonNode> path : openApi.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (!METHODS.contains(operation.getKey())) continue;
                Set<String> codes = new TreeSet<>();
                for (Map.Entry<String, JsonNode> response : operation.getValue().path("responses").properties()) {
                    if (response.getKey().startsWith("2")) codes.add(response.getKey());
                }
                result.put(operation.getKey().toUpperCase() + " " + path.getKey(), codes);
            }
        }
        return result;
    }
}
