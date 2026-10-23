package dev.catdoc.api.infrastructure.archive;

import dev.catdoc.api.application.ArchiveTooLargeException;
import dev.catdoc.api.application.ImportRejectedException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * zip から category.yaml と Markdown を取り出す。ディスクには書き出さない。
 *
 * <p>category.yaml がちょうど1つあり、その場所を根とする。フォルダごと圧縮した zip も受け付けるため。
 */
public class ZipArchiveReader {

    private static final String CATEGORY_YAML = "category.yaml";

    private final ArchiveLimits limits;

    public ZipArchiveReader(ArchiveLimits limits) {
        this.limits = limits;
    }

    public ArchiveContents read(InputStream archive) {
        var yamlFiles = new LinkedHashMap<String, String>();
        var markdownFiles = new LinkedHashMap<String, String>();
        try (var zip = new ZipInputStream(archive)) {
            int entries = 0;
            long total = 0;
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entries > limits.maxEntries()) {
                    throw new ArchiveTooLargeException("Archive has more than %d entries".formatted(limits.maxEntries()));
                }
                var name = entry.getName();
                requireSafe(name);
                if (entry.isDirectory() || ignored(name)) {
                    continue;
                }
                boolean yaml = basename(name).equals(CATEGORY_YAML);
                boolean markdown = name.endsWith(".md") || name.endsWith(".markdown");
                if (!yaml && !markdown) {
                    continue;
                }
                var bytes = readLimited(zip, limits.maxTotalBytes() - total);
                total += bytes.length;
                (yaml ? yamlFiles : markdownFiles).put(name, decode(name, bytes));
            }
        } catch (ZipException e) {
            throw new ImportRejectedException("Archive is not a valid zip file");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (yamlFiles.size() != 1) {
            throw new ImportRejectedException(
                    "Archive must contain exactly one %s, found %d".formatted(CATEGORY_YAML, yamlFiles.size()));
        }
        var yamlPath = yamlFiles.keySet().iterator().next();
        var root = yamlPath.substring(0, yamlPath.length() - CATEGORY_YAML.length());
        var underRoot = new LinkedHashMap<String, String>();
        markdownFiles.forEach((path, text) -> {
            if (path.startsWith(root)) {
                underRoot.put(path.substring(root.length()), text);
            }
        });
        return new ArchiveContents(yamlFiles.get(yamlPath), underRoot);
    }

    /** 絶対パス、親への移動、バックスラッシュを含む名前を弾く。Zip Slip への備え。 */
    private static void requireSafe(String name) {
        List<String> segments = List.of(name.split("/", -1));
        if (name.startsWith("/") || name.contains("\\") || name.contains(":") || segments.contains("..")) {
            throw new ImportRejectedException("Unsafe path in archive: '%s'".formatted(name));
        }
    }

    /** macOS の付随ファイルと、隠しファイルや隠しフォルダの中身。 */
    private static boolean ignored(String name) {
        for (var segment : name.split("/")) {
            if (segment.startsWith(".") || segment.equals("__MACOSX")) {
                return true;
            }
        }
        return false;
    }

    private static String basename(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    private byte[] readLimited(InputStream in, long remaining) throws IOException {
        var out = new ByteArrayOutputStream();
        var buffer = new byte[8192];
        long read = 0;
        for (int n = in.read(buffer); n != -1; n = in.read(buffer)) {
            read += n;
            if (read > remaining) {
                throw new ArchiveTooLargeException(
                        "Archive expands to more than %d bytes".formatted(limits.maxTotalBytes()));
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static String decode(String name, byte[] bytes) {
        try {
            var text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw new ImportRejectedException("'%s' is not valid UTF-8".formatted(name));
        }
    }
}
