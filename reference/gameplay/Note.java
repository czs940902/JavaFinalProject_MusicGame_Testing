package com.rhythmgame.gameplay;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true, defaultImpl = Note.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Note.class, name = "tap"),
    @JsonSubTypes.Type(value = HoldNote.class, name = "hold")
})
public class Note {
    public String type = "tap";
    public int lane;
    public long time; // ms
    public boolean hit = false;
    public boolean judged = false;
    public boolean completed = false;

    public Note() {}

    public Note(int lane, long time) {
        this.type = "tap";
        this.lane = lane;
        this.time = time;
    }
}
