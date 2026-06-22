package com.rhythmgame;

import com.badlogic.gdx.Game;
import com.rhythmgame.render.LibraryScreen;

public class RhythmGame extends Game {
    @Override
    public void create() {
        setScreen(new LibraryScreen(this));
    }
}
