package com.rhythmgame.gameplay;

import java.util.ArrayList;
import java.util.List;

public class Lane {
    public final int index;
    public final List<Note> active = new ArrayList<>();

    public Lane(int index) { this.index = index; }

    public void add(Note n) { active.add(n); }
}
