package com.rhythmgame.gameplay;

import java.util.List;

public class Beatmap {
    public String title;
    public String artist;
    public String author;
    /** filename stored relative to beatmap folder (e.g., audio.mp3) */
    public String audio;
    /** filename stored relative to beatmap folder (e.g., cover.png) */
    public String cover;
    /** beats per minute */
    public double bpm = 120.0;
    /** offset in milliseconds */
    public double offset = 0.0;
    public List<Note> notes;

    public Beatmap() {}
}
