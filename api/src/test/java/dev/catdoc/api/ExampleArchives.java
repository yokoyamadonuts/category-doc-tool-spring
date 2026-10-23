package dev.catdoc.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** リポジトリの examples/ の下のフォルダを、フォルダごと zip にする。 */
public final class ExampleArchives {

    private ExampleArchives() {
    }

    public static byte[] zip(String example) {
        var root = Path.of(System.getProperty("examples.dir")).resolve(example);
        var bytes = new ByteArrayOutputStream();
        try (var out = new ZipOutputStream(bytes); var files = Files.walk(root)) {
            for (var file : files.filter(Files::isRegularFile).sorted().toList()) {
                out.putNextEntry(new ZipEntry(example + "/" + root.relativize(file).toString().replace('\\', '/')));
                out.write(Files.readAllBytes(file));
                out.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }
}
