package dev.catdoc.api.infrastructure.archive;

/** zip を展開するときの上限。展開した量は、宣言された大きさではなく実際に読んだ量で数える。 */
public record ArchiveLimits(long maxTotalBytes, int maxEntries) {

    public static final ArchiveLimits DEFAULT = new ArchiveLimits(50L * 1024 * 1024, 1_000);
}
