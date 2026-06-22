package com.rhythmgame.gameplay;

public class HoldNote extends Note {
    public long endTime;
    public boolean holding = false;

    public HoldNote() {
        this.type = "hold";
    }

    public HoldNote(int lane, long startTime, long endTime) {
        super(lane, startTime);
        this.type = "hold";
        this.endTime = endTime;
    }
}
