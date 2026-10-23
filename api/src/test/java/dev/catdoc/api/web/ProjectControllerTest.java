package dev.catdoc.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import dev.catdoc.api.application.ProjectCommandService;
import dev.catdoc.api.application.ProjectQueryService;
import dev.catdoc.api.application.ProjectSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    ProjectQueryService projectQueryService;

    @MockitoBean
    ProjectCommandService projectCommandService;

    @Test
    void プロジェクトがないときは空の配列を返す() {
        given(projectQueryService.listProjects()).willReturn(List.of());

        assertThat(mvc.get().uri("/api/projects"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("[]");
    }

    @Test
    void プロジェクトの識別子と名前と取り込んだ日時を返す() {
        var id = UUID.fromString("0f8c3b5e-1d2a-4c7b-9e6f-123456789abc");
        given(projectQueryService.listProjects())
                .willReturn(List.of(new ProjectSummary(id, "math-docs", Instant.parse("2026-10-23T01:02:03Z"))));

        assertThat(mvc.get().uri("/api/projects"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id": "0f8c3b5e-1d2a-4c7b-9e6f-123456789abc",
                          "name": "math-docs",
                          "importedAt": "2026-10-23T01:02:03Z"}]
                        """);
    }
}
