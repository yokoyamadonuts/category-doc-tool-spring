package dev.catdoc.api.infrastructure.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.catdoc.api.application.ImportRejectedException;
import org.junit.jupiter.api.Test;

class FrontMatterTest {

    @Test
    void 先頭のYAMLと本文に分ける() {
        var parsed = FrontMatter.parse("docs/a.md", """
                ---
                id: req-auth-system
                title: 認証システム要件
                ---

                # 認証システム要件
                本文
                """);

        assertThat(parsed.metadata()).containsEntry("id", "req-auth-system");
        assertThat(parsed.body()).isEqualTo("# 認証システム要件\n本文\n");
    }

    @Test
    void CRLFの改行でも分けられる() {
        var parsed = FrontMatter.parse("a.md", "---\r\nid: a\r\n---\r\n# A\r\n");

        assertThat(parsed.metadata()).containsEntry("id", "a");
        assertThat(parsed.body()).isEqualTo("# A\r\n");
    }

    @Test
    void front_matterがなければ全体を本文にする() {
        var parsed = FrontMatter.parse("README.md", "# README\n---\nid: not-front-matter\n");

        assertThat(parsed.metadata()).isEmpty();
        assertThat(parsed.body()).startsWith("# README");
    }

    @Test
    void 閉じていないfront_matterは拒む() {
        assertThatThrownBy(() -> FrontMatter.parse("a.md", "---\nid: a\n# A\n"))
                .isInstanceOfSatisfying(ImportRejectedException.class, e -> assertThat(e.errors())
                        .containsExactly("'a.md': front matter is not closed with '---'"));
    }

    @Test
    void YAMLとして読めないfront_matterは拒む() {
        assertThatThrownBy(() -> FrontMatter.parse("a.md", "---\nid: [a\n---\n"))
                .isInstanceOfSatisfying(ImportRejectedException.class,
                        e -> assertThat(e.errors()).singleElement().asString().startsWith("'a.md': invalid front matter"));
    }
}
