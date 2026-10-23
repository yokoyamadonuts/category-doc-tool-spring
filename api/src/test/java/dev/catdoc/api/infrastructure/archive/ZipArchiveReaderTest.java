package dev.catdoc.api.infrastructure.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.catdoc.api.application.ArchiveTooLargeException;
import dev.catdoc.api.application.ImportRejectedException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZipArchiveReaderTest {

    private final ZipArchiveReader reader = new ZipArchiveReader(ArchiveLimits.DEFAULT);

    static InputStream zip(Map<String, byte[]> entries) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ZipOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    static InputStream zipOf(String... namesAndContents) throws IOException {
        var entries = new LinkedHashMap<String, byte[]>();
        for (int i = 0; i < namesAndContents.length; i += 2) {
            entries.put(namesAndContents[i], namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return zip(entries);
    }

    @Test
    void category_yamlのある場所を根にしてMarkdownを集める() throws IOException {
        var contents = reader.read(zipOf(
                "dev-project/category.yaml", "categories: []",
                "dev-project/README.md", "# readme",
                "dev-project/docs/a.md", "# a",
                "dev-project/docs/image.png", "png",
                "other/outside.md", "# outside"));

        assertThat(contents.categoryYaml()).isEqualTo("categories: []");
        assertThat(contents.markdownFiles()).containsOnlyKeys("README.md", "docs/a.md");
    }

    @Test
    void macOSの付随ファイルと隠しファイルは読み飛ばす() throws IOException {
        var contents = reader.read(zipOf(
                "category.yaml", "categories: []",
                "__MACOSX/._category.yaml", "junk",
                "docs/.hidden.md", "# hidden"));

        assertThat(contents.markdownFiles()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void category_yamlがちょうど1つでなければ拒む(int count) throws IOException {
        var names = new String[count * 2 + 2];
        for (int i = 0; i < count; i++) {
            names[i * 2] = "p" + i + "/category.yaml";
            names[i * 2 + 1] = "categories: []";
        }
        names[count * 2] = "README.md";
        names[count * 2 + 1] = "#";

        assertThatThrownBy(() -> reader.read(zipOf(names)))
                .isInstanceOfSatisfying(ImportRejectedException.class, e -> assertThat(e.errors())
                        .containsExactly("Archive must contain exactly one category.yaml, found " + count));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../evil.md", "docs/../../evil.md", "/etc/evil.md", "docs\\evil.md"})
    void 危険なパスを含むzipは拒む(String name) throws IOException {
        assertThatThrownBy(() -> reader.read(zipOf("category.yaml", "categories: []", name, "#")))
                .isInstanceOfSatisfying(ImportRejectedException.class, e -> assertThat(e.errors())
                        .containsExactly("Unsafe path in archive: '" + name + "'"));
    }

    @Test
    void 展開した量が上限を超えたら止める() throws IOException {
        var small = new ZipArchiveReader(new ArchiveLimits(100, 10));
        // 圧縮すると小さくなる中身。宣言された大きさではなく、実際に読んだ量で数える
        var big = "a".repeat(101);

        assertThatThrownBy(() -> small.read(zipOf("category.yaml", big)))
                .isInstanceOf(ArchiveTooLargeException.class)
                .hasMessage("Archive expands to more than 100 bytes");
    }

    @Test
    void ファイルの数が上限を超えたら止める() throws IOException {
        var small = new ZipArchiveReader(new ArchiveLimits(1_000, 2));

        assertThatThrownBy(() -> small.read(zipOf("category.yaml", "", "a.md", "", "b.md", "")))
                .isInstanceOf(ArchiveTooLargeException.class)
                .hasMessage("Archive has more than 2 entries");
    }

    @Test
    void UTF_8でないファイルは拒む() throws IOException {
        var entries = new LinkedHashMap<String, byte[]>();
        entries.put("category.yaml", "categories: []".getBytes(StandardCharsets.UTF_8));
        entries.put("a.md", new byte[] {(byte) 0x82, (byte) 0xa0});

        assertThatThrownBy(() -> reader.read(zip(entries)))
                .isInstanceOfSatisfying(ImportRejectedException.class, e -> assertThat(e.errors())
                        .containsExactly("'a.md' is not valid UTF-8"));
    }

    @Test
    void zipでなければ拒む() {
        var notZip = new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> reader.read(notZip))
                .isInstanceOfSatisfying(ImportRejectedException.class, e -> assertThat(e.errors())
                        .containsExactly("Archive must contain exactly one category.yaml, found 0"));
    }
}
