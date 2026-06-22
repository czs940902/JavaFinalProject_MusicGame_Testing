package com.rhythmgame.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class MusicPlayer {
    private static Music music;
    private static long fallbackStart = -1;
    private static boolean playbackCompleted = false;

    public static boolean hasLoadedMusic() {
        return music != null;
    }

    public static void load(String internalPath) {
        if (music != null) stop();
        try {
            music = Gdx.audio.newMusic(Gdx.files.internal(internalPath));
            music.setOnCompletionListener(completed -> playbackCompleted = true);
            playbackCompleted = false;
        } catch (Exception e) {
            Gdx.app.error("MusicPlayer", "Failed to load audio, falling back to timer", e);
            music = null;
            playbackCompleted = false;
        }
    }

    public static void loadAbsolute(String absolutePath) {
        if (music != null) stop();
        try {
            music = Gdx.audio.newMusic(Gdx.files.absolute(absolutePath));
            music.setOnCompletionListener(completed -> playbackCompleted = true);
            playbackCompleted = false;
        } catch (Exception e) {
            Gdx.app.error("MusicPlayer", "Failed to load absolute audio, falling back to timer", e);
            music = null;
            playbackCompleted = false;
        }
    }

    public static void play() {
        if (music != null) {
            playbackCompleted = false;
            music.play();
            fallbackStart = -1;
        } else {
            // fallback to system timer so gameplay can run without an actual music file
            fallbackStart = System.currentTimeMillis();
            playbackCompleted = false;
        }
    }

    public static void stop() {
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
        fallbackStart = -1;
        playbackCompleted = false;
    }

    public static long getPositionMs() {
        if (music != null) return (long) (music.getPosition() * 1000L);
        if (fallbackStart > 0) return System.currentTimeMillis() - fallbackStart;
        return 0L;
    }

    public static boolean isPlaying() { return (music != null && music.isPlaying()) || (fallbackStart > 0); }

    public static boolean isPlaybackCompleted() {
        return music != null && playbackCompleted;
    }
}
