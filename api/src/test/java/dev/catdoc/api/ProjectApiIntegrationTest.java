package dev.catdoc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

/** Testcontainers の MySQL を相手に、HTTP から取り込み、読み出し、削除まで通しで確かめる。 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ProjectApiIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    private String importDevProject() {
        var file = new MockMultipartFile("file", "dev-project.zip", "application/zip", ExampleArchives.zip("dev-project"));
        var result = mvc.post().uri("/api/projects").multipart().file(file).exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.name").isEqualTo("dev-project");
        var location = result.getResponse().getHeader("Location");
        assertThat(location).matches("/api/projects/[0-9a-f-]{36}");
        return location;
    }

    @Test
    void 何も取り込んでいないとき_一覧は空になる() {
        assertThat(mvc.get().uri("/api/projects"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("[]");
    }

    @Test
    void 取り込んだプロジェクトは一覧と詳細に出る() {
        var location = importDevProject();

        assertThat(mvc.get().uri("/api/projects")).hasStatusOk().bodyJson()
                .extractingPath("$[0].name").isEqualTo("dev-project");
        assertThat(mvc.get().uri(location)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"name": "dev-project",
                 "categories": [
                   {"id": "requirements", "name": "Requirements", "objectCount": 2, "morphismCount": 3},
                   {"id": "specifications"}, {"id": "design"}, {"id": "internal-spec"},
                   {"id": "infrastructure", "objectCount": 3, "morphismCount": 6}],
                 "functors": [
                   {"id": "specify-functor", "sourceCategory": "requirements", "targetCategory": "specifications"},
                   {"id": "design-functor"}, {"id": "implement-functor"}, {"id": "deploy-functor"}],
                 "naturalTransformations": []}
                """);
    }

    @Test
    void カテゴリのグラフと文書の本文を返す() {
        var location = importDevProject();

        assertThat(mvc.get().uri(location + "/categories/requirements/graph")).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {"id": "requirements",
                         "objects": [{"id": "req-user-management", "title": "ユーザー管理要件", "domain": "requirements"},
                                     {"id": "req-auth-system"}],
                         "morphisms": [{"id": "req-auth-depends-user", "source": "req-auth-system",
                                        "target": "req-user-management", "identity": false},
                                       {"id": "id-req-user", "identity": true}, {"id": "id-req-auth"}]}
                        """);

        var object = mvc.get().uri(location + "/objects/req-auth-system").exchange();
        assertThat(object).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"id": "req-auth-system", "categoryId": "requirements",
                 "metadata": {"priority": "high", "stakeholder": "セキュリティチーム"}}
                """);
        assertThat(object).bodyJson().extractingPath("$.content").asString().startsWith("# 認証システム要件");
    }

    @Test
    void 検証と経路と追跡の表を計算する() {
        var location = importDevProject();

        assertThat(mvc.get().uri(location + "/verification")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"valid": true,
                 "categories": [{"id": "requirements", "valid": true, "errors": [], "warnings": []},
                                {}, {}, {}, {}],
                 "functors": [{"id": "specify-functor", "valid": true}, {}, {}, {}],
                 "naturalTransformations": []}
                """);

        assertThat(mvc.get().uri(location + "/paths").param("from", "infra-k8s-auth").param("to", "infra-database"))
                .hasStatusOk().bodyJson().isLenientlyEqualTo("""
                        {"from": "infra-k8s-auth", "to": "infra-database", "categoryId": "infrastructure",
                         "shortest": [{"id": "infra-auth-uses-db"}],
                         "all": [[{"id": "infra-auth-uses-db"}],
                                 [{"id": "infra-auth-uses-user"}, {"id": "infra-user-uses-db"}]]}
                        """);

        assertThat(mvc.get().uri(location + "/traceability").param("functors", "specify-functor,design-functor"))
                .hasStatusOk().bodyJson().isLenientlyEqualTo("""
                        {"functors": [{"id": "specify-functor"}, {"id": "design-functor"}],
                         "rows": [{"objectId": "req-user-management", "title": "ユーザー管理要件",
                                   "mappedObjectIds": ["spec-user-api", "design-user-service"], "complete": true},
                                  {"objectId": "req-auth-system"}]}
                        """);
    }

    @Test
    void 消すと読めなくなる() {
        var location = importDevProject();

        assertThat(mvc.delete().uri(location)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void 壊れたzipは422で誤りを並べる() {
        var yaml = """
                categories:
                  - id: c
                    name: C
                functors:
                  - id: F
                    name: F
                    sourceCategory: c
                    targetCategory: missing
                """;
        var zip = ZipBytes.of("category.yaml", yaml);
        var file = new MockMultipartFile("file", "broken.zip", "application/zip", zip);

        assertThat(mvc.post().uri("/api/projects").multipart().file(file))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        {"status": 422, "errors": ["Functor 'F' refers to unknown target category 'missing'"]}
                        """);
        assertThat(mvc.get().uri("/api/projects")).bodyJson().isLenientlyEqualTo("[]");
    }

    @Test
    void 誤ったパラメータは400_ないものは404() {
        var location = importDevProject();

        assertThat(mvc.get().uri(location + "/paths").param("from", "req-auth-system").param("to", "infra-database"))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail")
                .isEqualTo("'req-auth-system' and 'infra-database' are in different categories");
        assertThat(mvc.get().uri(location + "/paths").param("from", "req-auth-system"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri(location + "/traceability").param("functors", "specify-functor,deploy-functor"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri(location + "/traceability").param("functors", "nope"))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.detail")
                .isEqualTo("Unknown functor 'nope'");
        assertThat(mvc.get().uri(location + "/objects/nope")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(location + "/categories/nope/graph")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/projects/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    /** テストの中で小さな zip を作る。 */
    static final class ZipBytes {

        static byte[] of(String name, String content) {
            var bytes = new java.io.ByteArrayOutputStream();
            try (var out = new java.util.zip.ZipOutputStream(bytes)) {
                out.putNextEntry(new java.util.zip.ZipEntry(name));
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            return bytes.toByteArray();
        }
    }
}
