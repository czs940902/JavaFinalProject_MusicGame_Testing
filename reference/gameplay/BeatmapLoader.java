package com.rhythmgame.gameplay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.badlogic.gdx.files.FileHandle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BeatmapLoader {
    public static Beatmap load(FileHandle file) throws Exception {
        try (InputStream is = file.read()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            Beatmap beatmap = new Beatmap();
            beatmap.title = root.path("title").asText("Untitled");
            beatmap.audio = root.path("audio").asText();
            beatmap.notes = new ArrayList<>();
            JsonNode notesNode = root.path("notes");
            if (notesNode.isArray()) {
                for (JsonNode noteNode : notesNode) {
                    String type = noteNode.path("type").asText("tap");
                    int lane = noteNode.path("lane").asInt();
                    if ("hold".equalsIgnoreCase(type)) {
                        HoldNote holdNote = new HoldNote();
                        holdNote.type = "hold";
                        holdNote.lane = lane;
                        holdNote.time = noteNode.has("time") ? noteNode.path("time").asLong() : noteNode.path("start").asLong();
                        holdNote.endTime = noteNode.has("endTime") ? noteNode.path("endTime").asLong() : noteNode.path("end").asLong();
                        beatmap.notes.add(holdNote);
                    } else {
                        Note note = new Note();
                        note.type = "tap";
                        note.lane = lane;
                        note.time = noteNode.path("time").asLong();
                        beatmap.notes.add(note);
                    }
                }
            }
            return beatmap;
        }
    }
}
