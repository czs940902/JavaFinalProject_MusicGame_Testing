package com.rhythmgame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rhythmgame.RhythmGame;
import com.rhythmgame.gameplay.Beatmap;
import com.rhythmgame.gameplay.BeatmapIO;
import com.rhythmgame.gameplay.HoldNote;
import com.rhythmgame.gameplay.Note;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EditorScreen implements Screen {
    private final RhythmGame game;
    private final Path beatmapDirectory;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font;
    private FreeTypeFontGenerator fontGenerator;
    private final GlyphLayout layout = new GlyphLayout();
    private final List<Note> notes = new ArrayList<>();

    private Beatmap beatmap;
    private long cursorTimeMs = 0L;
    private long snapMs = 50L;
    private long holdLengthMs = 1000L;
    private boolean holdMode = false;
    private boolean dirty = false;
    private String statusMessage = "";

    private final float judgeLineY = 180f;
    private final float pixelsPerMs = 0.2f;

    public EditorScreen(RhythmGame game, Path beatmapDirectory) {
        this.game = game;
        this.beatmapDirectory = beatmapDirectory;
        this.font = loadFont();
        loadBeatmap();
        Gdx.input.setInputProcessor(new EditorInput());
    }

    private BitmapFont loadFont() {
        try {
            FileHandle fontFile = Gdx.files.internal("fonts/NotoSansHK-VF.ttf");
            if (fontFile.exists()) {
                fontGenerator = new FreeTypeFontGenerator(fontFile);
                FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
                params.size = 24;
                params.minFilter = Texture.TextureFilter.Linear;
                params.magFilter = Texture.TextureFilter.Linear;
                params.incremental = true;
                return fontGenerator.generateFont(params);
            }
        } catch (Exception e) {
            Gdx.app.error("EditorScreen", "loadFont", e);
        }
        return new BitmapFont();
    }

    private void loadBeatmap() {
        try {
            beatmap = BeatmapIO.loadFromDirectory(beatmapDirectory);
            notes.clear();
            if (beatmap.notes != null) {
                for (Note note : beatmap.notes) {
                    if (note instanceof HoldNote hold) {
                        notes.add(new HoldNote(hold.lane, hold.time, hold.endTime));
                    } else {
                        notes.add(new Note(note.lane, note.time));
                    }
                }
            }
            notes.sort(Comparator.comparingLong(n -> n.time));
            if (notes.isEmpty()) {
                cursorTimeMs = 0L;
                statusMessage = "Loaded beatmap. No notes yet.";
            } else {
                long firstNote = notes.get(0).time;
                cursorTimeMs = Math.max(0L, firstNote - 2000L);
                statusMessage = "Loaded beatmap. Notes: " + notes.size() + ", cursor -> " + cursorTimeMs + "ms";
            }
            dirty = false;
        } catch (Exception e) {
            statusMessage = "Failed to load beatmap.";
            Gdx.app.error("EditorScreen", "loadBeatmap", e);
            beatmap = new Beatmap();
            beatmap.title = "Untitled";
            beatmap.artist = "Unknown";
            beatmap.author = "Unknown";
            beatmap.bpm = 120.0;
            beatmap.offset = 0.0;
            cursorTimeMs = 0L;
        }
    }

    private void addTapNote(int lane, long timeMs) {
        long snapped = snapTime(timeMs);
        Note note = holdMode ? new HoldNote(lane, Math.max(0L, snapped), Math.max(0L, snapped + holdLengthMs))
                : new Note(lane, Math.max(0L, snapped));
        notes.add(note);
        notes.sort(Comparator.comparingLong(n -> n.time));
        dirty = true;
        if (note instanceof HoldNote hold) {
            statusMessage = "Added hold lane " + (lane + 1) + " @ " + hold.time + "ms -> " + hold.endTime + "ms";
        } else {
            statusMessage = "Added note lane " + (lane + 1) + " @ " + note.time + "ms";
        }
    }

    private void deleteNearestNoteInLane(int lane, long aroundTimeMs) {
        Note nearest = null;
        long best = Long.MAX_VALUE;
        for (Note note : notes) {
            if (note.lane != lane) {
                continue;
            }
            long delta = Math.abs(note.time - aroundTimeMs);
            if (delta < best) {
                best = delta;
                nearest = note;
            }
        }
        if (nearest != null && best <= 200L) {
            notes.remove(nearest);
            dirty = true;
            statusMessage = "Deleted note lane " + (lane + 1) + " @ " + nearest.time + "ms";
        }
    }

    private long snapTime(long timeMs) {
        if (snapMs <= 1) {
            return Math.max(0L, timeMs);
        }
        long rounded = Math.round(timeMs / (double) snapMs) * snapMs;
        return Math.max(0L, rounded);
    }

    private long timeFromScreenY(int y) {
        float deltaPixels = y - judgeLineY;
        long deltaMs = Math.round(deltaPixels / pixelsPerMs);
        return Math.max(0L, snapTime(cursorTimeMs + deltaMs));
    }

    private int laneFromScreenX(int x) {
        float laneW = Gdx.graphics.getWidth() / 4f;
        int lane = (int) (x / laneW);
        if (lane < 0 || lane > 3) {
            return -1;
        }
        return lane;
    }

    private void moveCursor(long deltaMs) {
        cursorTimeMs = Math.max(0L, cursorTimeMs + deltaMs);
    }

    private void saveBeatmap() {
        try {
            Beatmap output = new Beatmap();
            output.title = beatmap.title;
            output.artist = beatmap.artist;
            output.author = beatmap.author;
            output.bpm = beatmap.bpm;
            output.offset = beatmap.offset;
            output.notes = new ArrayList<>();
            for (Note note : notes) {
                if (note instanceof HoldNote hold) {
                    output.notes.add(new HoldNote(hold.lane, hold.time, hold.endTime));
                } else {
                    output.notes.add(new Note(note.lane, note.time));
                }
            }

            Path audioSource = resolveAssetPath(beatmap.audio);
            Path coverSource = resolveAssetPath(beatmap.cover);
            BeatmapIO.saveToDirectory(output, beatmapDirectory, audioSource, coverSource);
            beatmap = BeatmapIO.loadFromDirectory(beatmapDirectory);
            dirty = false;
            statusMessage = "Saved " + output.notes.size() + " notes.";
        } catch (IOException e) {
            statusMessage = "Save failed.";
            Gdx.app.error("EditorScreen", "saveBeatmap", e);
        }
    }

    private Path resolveAssetPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path path = Paths.get(value);
            if (!path.isAbsolute()) {
                Path local = beatmapDirectory.resolve(value);
                if (local.toFile().exists()) {
                    return local;
                }
                return path;
            }
            return path;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void backToLibrary() {
        if (dirty) {
            saveBeatmap();
        }
        game.setScreen(new LibraryScreen(game));
        dispose();
    }

    private void openGameTest() {
        if (dirty) {
            saveBeatmap();
        }
        game.setScreen(new GameScreen(game, beatmapDirectory));
        dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new EditorInput());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.07f, 0.07f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderLanes();
        renderNotes();
        renderHud();
    }

    private void renderLanes() {
        float laneW = Gdx.graphics.getWidth() / 4f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int lane = 0; lane < 4; lane++) {
            float x = lane * laneW;
            shapes.setColor(0.13f, 0.13f, 0.17f, 1f);
            shapes.rect(x, 0, laneW - 2f, Gdx.graphics.getHeight());
        }

        shapes.setColor(0.95f, 0.85f, 0.2f, 1f);
        shapes.rect(0, judgeLineY - 2f, Gdx.graphics.getWidth(), 4f);
        shapes.end();
    }

    private void renderNotes() {
        float laneW = Gdx.graphics.getWidth() / 4f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Note note : notes) {
            float y = judgeLineY + (note.time - cursorTimeMs) * pixelsPerMs;
            if (y < -30 || y > Gdx.graphics.getHeight() + 30) {
                continue;
            }
            float x = note.lane * laneW;
            if (note instanceof HoldNote hold) {
                float endY = judgeLineY + (hold.endTime - cursorTimeMs) * pixelsPerMs;
                float top = Math.min(y, endY);
                float h = Math.max(8f, Math.abs(endY - y));
                shapes.setColor(0.75f, 0.45f, 0.20f, 1f);
                shapes.rect(x + 22f, top, laneW - 44f, h);
                shapes.setColor(0.95f, 0.62f, 0.25f, 1f);
                shapes.rect(x + 10f, y - 8f, laneW - 20f, 16f);
            } else {
                shapes.setColor(0.18f, 0.72f, 0.72f, 1f);
                shapes.rect(x + 10f, y - 8f, laneW - 20f, 16f);
            }
        }
        shapes.end();
    }

    private void renderHud() {
        String title = beatmap != null && beatmap.title != null ? beatmap.title : "Untitled";
        String artist = beatmap != null && beatmap.artist != null ? beatmap.artist : "Unknown";

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Editor: " + title + " - " + artist, 12, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Time: " + cursorTimeMs + "ms   Snap: " + snapMs + "ms   Notes: " + notes.size() + (dirty ? "  (unsaved)" : ""),
                12, Gdx.graphics.getHeight() - 34);
        font.draw(batch, "Mode: " + (holdMode ? "HOLD" : "TAP") + "   Hold length: " + holdLengthMs + "ms", 12, Gdx.graphics.getHeight() - 58);
        font.draw(batch, "D/F/J/K add note. Shift + D/F/J/K delete nearest note in lane.", 12, Gdx.graphics.getHeight() - 82);
        font.draw(batch, "H toggles hold mode, [ / ] change hold length.", 12, Gdx.graphics.getHeight() - 106);
        font.draw(batch, "Left/Right +/-snap, Up/Down +/-1000ms, Mouse wheel scroll timeline.", 12, Gdx.graphics.getHeight() - 130);
        font.draw(batch, "Left click lane to place note at cursor line. Right click deletes nearest in lane.", 12, Gdx.graphics.getHeight() - 154);
        font.draw(batch, "S save, Enter test in Game, ESC save and back to Library.", 12, Gdx.graphics.getHeight() - 178);
        if (!statusMessage.isBlank()) {
            layout.setText(font, statusMessage);
            font.draw(batch, statusMessage, 12, 26);
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
    }

    private class EditorInput extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                backToLibrary();
                return true;
            }
            if (keycode == Input.Keys.S) {
                saveBeatmap();
                return true;
            }
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                openGameTest();
                return true;
            }
            if (keycode == Input.Keys.LEFT) {
                moveCursor(-snapMs);
                return true;
            }
            if (keycode == Input.Keys.RIGHT) {
                moveCursor(snapMs);
                return true;
            }
            if (keycode == Input.Keys.UP) {
                moveCursor(1000L);
                return true;
            }
            if (keycode == Input.Keys.DOWN) {
                moveCursor(-1000L);
                return true;
            }
            if (keycode == Input.Keys.PAGE_UP) {
                moveCursor(10000L);
                return true;
            }
            if (keycode == Input.Keys.PAGE_DOWN) {
                moveCursor(-10000L);
                return true;
            }
            if (keycode == Input.Keys.MINUS) {
                snapMs = Math.max(10L, snapMs - 10L);
                return true;
            }
            if (keycode == Input.Keys.EQUALS) {
                snapMs = Math.min(500L, snapMs + 10L);
                return true;
            }
            if (keycode == Input.Keys.H) {
                holdMode = !holdMode;
                statusMessage = holdMode ? "Hold mode on." : "Tap mode on.";
                return true;
            }
            if (keycode == Input.Keys.LEFT_BRACKET) {
                holdLengthMs = Math.max(100L, holdLengthMs - 100L);
                statusMessage = "Hold length: " + holdLengthMs + "ms";
                return true;
            }
            if (keycode == Input.Keys.RIGHT_BRACKET) {
                holdLengthMs = Math.min(10000L, holdLengthMs + 100L);
                statusMessage = "Hold length: " + holdLengthMs + "ms";
                return true;
            }

            int lane = laneForKey(keycode);
            if (lane != -1) {
                boolean deleting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                        || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                if (deleting) {
                    deleteNearestNoteInLane(lane, cursorTimeMs);
                } else {
                    addTapNote(lane, cursorTimeMs);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            moveCursor((long) (amountY * snapMs * 2L));
            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            int y = Gdx.graphics.getHeight() - screenY;
            int lane = laneFromScreenX(screenX);
            if (lane == -1) {
                return false;
            }
            long noteTime = timeFromScreenY(y);
            if (button == Input.Buttons.LEFT) {
                addTapNote(lane, noteTime);
                return true;
            }
            if (button == Input.Buttons.RIGHT) {
                deleteNearestNoteInLane(lane, noteTime);
                return true;
            }
            return false;
        }

        private int laneForKey(int keycode) {
            if (keycode == Input.Keys.D) return 0;
            if (keycode == Input.Keys.F) return 1;
            if (keycode == Input.Keys.J) return 2;
            if (keycode == Input.Keys.K) return 3;
            return -1;
        }
    }
}
