package com.rhythmgame.gameplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class BeatmapRepository {
    private final Path rootDirectory;

    public BeatmapRepository(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public List<BeatmapEntry> loadEntries() throws IOException {
        if (!Files.exists(rootDirectory)) {
            Files.createDirectories(rootDirectory);
            return Collections.emptyList();
        }

        List<BeatmapEntry> entries = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(rootDirectory)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                Path json = dir.resolve("beatmap.json");
                if (Files.exists(json)) {
                    try {
                        Beatmap beatmap = BeatmapIO.loadFromDirectory(dir);
                        long modified = Files.getLastModifiedTime(dir).toMillis();
                        entries.add(new BeatmapEntry(dir, beatmap, modified));
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        entries.sort(Comparator.comparingLong(BeatmapEntry::modifiedTime).reversed());
        return entries;
    }

    public record BeatmapEntry(Path directory, Beatmap beatmap, long modifiedTime) {
    }
}
