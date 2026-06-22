package com.rhythmgame.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class SFXPlayer {
    private static Sound hitSound;

    public static void loadHitSound(String internalPath) {
        if (hitSound != null) {
            hitSound.dispose();
            hitSound = null;
        }
        try {
            hitSound = Gdx.audio.newSound(Gdx.files.internal(internalPath));
        } catch (Exception e) {
            Gdx.app.error("SFXPlayer", "Failed to load hit sound: " + internalPath, e);
            hitSound = null;
        }
    }

    public static void playHitSound(float volume) {
        if (hitSound != null) {
            hitSound.play(volume);
        }
    }

    public static void dispose() {
        if (hitSound != null) {
            hitSound.dispose();
            hitSound = null;
        }
    }
}
