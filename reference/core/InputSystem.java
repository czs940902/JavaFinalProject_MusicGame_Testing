package com.rhythmgame.core;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

public class InputSystem extends InputAdapter {
    public interface KeyHandler {
        void onKeyDown(int lane);
        void onKeyUp(int lane);
    }

    private KeyHandler handler;

    public void setHandler(KeyHandler h) { handler = h; }

    @Override
    public boolean keyDown(int keycode) {
        int lane = -1;
        if (keycode == Keys.D) lane = 0;
        if (keycode == Keys.F) lane = 1;
        if (keycode == Keys.J) lane = 2;
        if (keycode == Keys.K) lane = 3;
        if (lane != -1 && handler != null) handler.onKeyDown(lane);
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        int lane = -1;
        if (keycode == Keys.D) lane = 0;
        if (keycode == Keys.F) lane = 1;
        if (keycode == Keys.J) lane = 2;
        if (keycode == Keys.K) lane = 3;
        if (lane != -1 && handler != null) handler.onKeyUp(lane);
        return false;
    }
}
