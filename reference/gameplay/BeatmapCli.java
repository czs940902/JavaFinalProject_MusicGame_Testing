package com.rhythmgame.gameplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BeatmapCli {
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String command = args[0];
        try {
            switch (command) {
                case "save" -> saveBeatmap(args);
                case "load" -> loadBeatmap(args);
                case "export" -> exportBeatmap(args);
                case "import" -> importBeatmap(args);
                default -> printUsage();
            }
        } catch (IOException ex) {
            System.err.println("I/O error: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    private static void saveBeatmap(String[] args) throws IOException {
        if (args.length != 5) {
            System.err.println("Usage: save <dest-dir> <audio-file> <cover-file> <title>");
            return;
        }
        Path destDir = Paths.get(args[1]);
        Path audio = Paths.get(args[2]);
        Path cover = Paths.get(args[3]);
        String title = args[4];
        Beatmap beatmap = new Beatmap();
        beatmap.title = title;
        beatmap.artist = "CLI Artist";
        beatmap.author = "CLI Author";
        beatmap.bpm = 120.0;
        beatmap.offset = 0.0;
        beatmap.notes = List.of(new Note(0, 500), new Note(1, 1000));

        BeatmapIO.saveToDirectory(beatmap, destDir, audio, cover);
        System.out.println("Saved beatmap to " + destDir);
    }

    private static void loadBeatmap(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: load <beatmap-dir>");
            return;
        }
        Beatmap beatmap = BeatmapIO.loadFromDirectory(Paths.get(args[1]));
        System.out.println("Title: " + beatmap.title);
        System.out.println("Artist: " + beatmap.artist);
        System.out.println("Author: " + beatmap.author);
        System.out.println("Audio path: " + beatmap.audio);
        System.out.println("Cover path: " + beatmap.cover);
        System.out.println("BPM: " + beatmap.bpm);
        System.out.println("Offset: " + beatmap.offset);
        System.out.println("Notes: " + beatmap.notes.size());
    }

    private static void exportBeatmap(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: export <beatmap-dir> <target-zip>");
            return;
        }
        BeatmapIO.exportDirectoryToZip(Paths.get(args[1]), Paths.get(args[2]));
        System.out.println("Exported zip to " + args[2]);
    }

    private static void importBeatmap(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: import <zip-file> <dest-dir>");
            return;
        }
        BeatmapIO.importZipToDirectory(Paths.get(args[1]), Paths.get(args[2]));
        System.out.println("Imported beatmap to " + args[2]);
    }

    private static void printUsage() {
        System.out.println("Beatmap CLI commands:");
        System.out.println("  save <dest-dir> <audio-file> <cover-file> <title>");
        System.out.println("  load <beatmap-dir>");
        System.out.println("  export <beatmap-dir> <target-zip>");
        System.out.println("  import <zip-file> <dest-dir>");
    }
}
