package dev.catdoc.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProjectApiIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void 何も取り込んでいないとき_一覧は空になる() {
        assertThat(mvc.get().uri("/api/projects"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("[]");
    }
}
