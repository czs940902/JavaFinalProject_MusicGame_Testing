package com.rhythmgame.core;

import com.rhythmgame.audio.MusicPlayer;

public class TimingEngine {
    public static long getTimeMs() {
        return MusicPlayer.getPositionMs();
    }
}
