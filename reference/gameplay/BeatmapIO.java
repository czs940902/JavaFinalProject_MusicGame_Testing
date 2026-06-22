package com.rhythmgame.gameplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BeatmapIO {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Save beatmap JSON and copy audio/cover into target directory.
     * The beatmap.audio and beatmap.cover fields will be written as filenames (not absolute paths).
     */
    public static void saveToDirectory(Beatmap beatmap, Path dir, Path audioSource, Path coverSource) throws IOException {
        Files.createDirectories(dir);

        if (audioSource != null) {
            Path audioDest = dir.resolve(audioSource.getFileName().toString());
            if (!audioSource.toAbsolutePath().normalize().equals(audioDest.toAbsolutePath().normalize())) {
                Files.copy(audioSource, audioDest, StandardCopyOption.REPLACE_EXISTING);
            }
            beatmap.audio = audioDest.getFileName().toString();
        }

        if (coverSource != null) {
            Path coverDest = dir.resolve(coverSource.getFileName().toString());
            if (!coverSource.toAbsolutePath().normalize().equals(coverDest.toAbsolutePath().normalize())) {
                Files.copy(coverSource, coverDest, StandardCopyOption.REPLACE_EXISTING);
            }
            beatmap.cover = coverDest.getFileName().toString();
        }

        Path json = dir.resolve("beatmap.json");
        mapper.writeValue(json.toFile(), beatmap);
    }

    /**
     * Load beatmap from directory. The returned Beatmap will have audio and cover fields set to absolute paths.
     */
    public static Beatmap loadFromDirectory(Path dir) throws IOException {
        Path json = dir.resolve("beatmap.json");
        Beatmap b = mapper.readValue(json.toFile(), Beatmap.class);
        if (b.audio != null) b.audio = dir.resolve(b.audio).toString();
        if (b.cover != null) b.cover = dir.resolve(b.cover).toString();
        return b;
    }

    /**
     * Export a beatmap package zip containing only beatmap.json and the referenced audio/cover files.
     */
    public static void exportBeatmapToZip(Path dir, Path zipFile) throws IOException {
        Path json = dir.resolve("beatmap.json");
        if (!Files.exists(json)) {
            throw new IOException("Missing beatmap.json in " + dir);
        }

        Beatmap beatmap = mapper.readValue(json.toFile(), Beatmap.class);
        Set<String> fileNames = new LinkedHashSet<>();
        fileNames.add("beatmap.json");
        addReferencedFile(fileNames, beatmap.audio);
        addReferencedFile(fileNames, beatmap.cover);

        Path parent = zipFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream fos = Files.newOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String fileName : fileNames) {
                Path source = dir.resolve(fileName).normalize();
                if (!source.startsWith(dir.normalize())) {
                    throw new IOException("Invalid beatmap asset path: " + fileName);
                }
                if (!Files.exists(source) || !Files.isRegularFile(source)) {
                    throw new IOException("Missing beatmap asset: " + fileName);
                }
                writeZipEntry(zos, source, fileName);
            }
        }
    }

    /**
     * Export a beatmap folder (all files under it) into a zip file.
     */
    public static void exportDirectoryToZip(Path dir, Path zipFile) throws IOException {
        Files.createDirectories(zipFile.getParent());
        try (OutputStream fos = Files.newOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String rel = dir.relativize(path).toString().replace("\\", "/");
                            ZipEntry entry = new ZipEntry(rel);
                            zos.putNextEntry(entry);
                            try (InputStream is = Files.newInputStream(path)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = is.read(buf)) > 0) zos.write(buf, 0, len);
                            }
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    /**
     * Import a zip file into the specified destination directory (creates files with same structure).
     */
    public static void importZipToDirectory(Path zipFile, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        Path normalizedDest = destDir.toAbsolutePath().normalize();
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = normalizedDest.resolve(entry.getName()).normalize();
                if (!out.startsWith(normalizedDest)) {
                    throw new IOException("Invalid zip entry path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) > 0) os.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void addReferencedFile(Set<String> fileNames, String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Path path = Path.of(fileName);
        if (path.isAbsolute() || path.getNameCount() != 1) {
            throw new IOException("Beatmap asset must be stored as a filename: " + fileName);
        }
        fileNames.add(path.getFileName().toString());
    }

    private static void writeZipEntry(ZipOutputStream zos, Path source, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName.replace("\\", "/"));
        zos.putNextEntry(entry);
        try (InputStream is = Files.newInputStream(source)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
        }
        zos.closeEntry();
    }
}
