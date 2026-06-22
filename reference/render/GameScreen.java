package com.rhythmgame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.rhythmgame.RhythmGame;
import com.rhythmgame.audio.SFXPlayer;
import com.rhythmgame.audio.MusicPlayer;
import com.rhythmgame.core.InputSystem;
import com.rhythmgame.core.JudgeSystem;
import com.rhythmgame.core.ScoreSystem;
import com.rhythmgame.core.TimingEngine;
import com.rhythmgame.gameplay.Beatmap;
import com.rhythmgame.gameplay.BeatmapIO;
import com.rhythmgame.gameplay.BeatmapLoader;
import com.rhythmgame.gameplay.HoldNote;
import com.rhythmgame.gameplay.Lane;
import com.rhythmgame.gameplay.Note;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GameScreen implements Screen {
    private final RhythmGame game;
    private final GameRenderer renderer = new GameRenderer();
    private final Path beatmapDirectory;
    private Beatmap beatmap;
    private final Lane[] lanes = new Lane[4];
    private final ScoreSystem scoreSystem = new ScoreSystem();
    private final InputSystem input = new InputSystem();
    private final InputMultiplexer inputMultiplexer = new InputMultiplexer();

    private final float judgeLineY = 120f;
    private final float pixelsPerMs = 0.5f; // movement scale (2x faster)
    private final long hitWindowMs = 90;
    private final float hitSoundVolume = 0.5f;

    private final List<Note> toSpawn = new ArrayList<>();
    private final HoldNote[] activeHold = new HoldNote[4];
    private final boolean[] keyDown = new boolean[4];
    private final List<JudgementMessage> judgementMessages = new ArrayList<>();
    private boolean resultVisible = false;
    private boolean hasChartNotes = false;
    private long chartEndTimeMs = 1;
    private final int resultWidth = 1600;
    private final int resultHeight = 900;

    public GameScreen(RhythmGame g) {
        this(g, null);
    }

    public GameScreen(RhythmGame g, Path beatmapDirectory) {
        this.game = g;
        this.beatmapDirectory = beatmapDirectory;
        for (int i = 0; i < 4; i++) lanes[i] = new Lane(i);
        input.setHandler(new InputSystem.KeyHandler() {
            @Override
            public void onKeyDown(int lane) { handleKeyDown(lane); }
            @Override
            public void onKeyUp(int lane) { handleKeyUp(lane); }
        });

        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    backToLibrary();
                    return true;
                }
                if (resultVisible) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                        restartGame();
                        return true;
                    }
                    return true;
                }
                if (keycode == Input.Keys.R) {
                    finishGame();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (!resultVisible || button != Input.Buttons.LEFT) {
                    return false;
                }
                int x = screenX;
                int y = Gdx.graphics.getHeight() - screenY;
                int px = (Gdx.graphics.getWidth() - resultWidth) / 2;
                int py = (Gdx.graphics.getHeight() - resultHeight) / 2;

                int retryX = px + resultWidth - 560;
                int retryY = py + 130;
                int backX = px + resultWidth - 290;
                int backY = py + 130;
                int btnW = 220;
                int btnH = 80;

                if (isInside(x, y, retryX, retryY + btnH, btnW, btnH)) {
                    restartGame();
                    return true;
                }
                if (isInside(x, y, backX, backY + btnH, btnW, btnH)) {
                    backToLibrary();
                    return true;
                }
                return true;
            }
        });
        inputMultiplexer.addProcessor(input);
        Gdx.input.setInputProcessor(inputMultiplexer);
        loadBeatmap();
    }

    private void loadBeatmap() {
        try {
            if (beatmapDirectory != null) {
                beatmap = BeatmapIO.loadFromDirectory(beatmapDirectory);
            } else {
                beatmap = BeatmapLoader.load(Gdx.files.internal("beatmap.json"));
            }

            if (beatmap.audio != null && !beatmap.audio.isBlank()) {
                Path audioPath = Paths.get(beatmap.audio);
                if (audioPath.isAbsolute() && Files.exists(audioPath)) {
                    MusicPlayer.loadAbsolute(beatmap.audio);
                } else {
                    MusicPlayer.load(beatmap.audio);
                }
            }
            MusicPlayer.play();
            SFXPlayer.loadHitSound("audio/hit.wav");

            if (beatmap.notes != null) {
                hasChartNotes = !beatmap.notes.isEmpty();
                toSpawn.addAll(beatmap.notes);
                toSpawn.sort(Comparator.comparingLong(n -> n.time));
                int holdCount = 0;
                long maxEnd = 0;
                for (Note note : beatmap.notes) {
                    if (note instanceof HoldNote) {
                        holdCount++;
                        maxEnd = Math.max(maxEnd, ((HoldNote) note).endTime);
                    } else {
                        maxEnd = Math.max(maxEnd, note.time);
                    }
                }
                scoreSystem.registerTotalJudgements(beatmap.notes.size() + holdCount);
                chartEndTimeMs = Math.max(1, maxEnd + 1000L);
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Failed to load beatmap or audio", e);
        }
    }

    private void handleKeyDown(int lane) {
        if (resultVisible) {
            return;
        }
        long now = TimingEngine.getTimeMs();
        keyDown[lane] = true;

        if (activeHold[lane] != null && !activeHold[lane].completed) {
            activeHold[lane].holding = true;
            return;
        }

        Note best = null;
        long bestDelta = Long.MAX_VALUE;
        for (Note n : lanes[lane].active) {
            if (n.judged) continue;
            long delta = n.time - now;
            if (Math.abs(delta) < Math.abs(bestDelta)) {
                best = n;
                bestDelta = delta;
            }
        }

        if (best == null || Math.abs(bestDelta) > hitWindowMs) {
            return;
        }

        JudgeSystem.JudgmentResult jr = JudgeSystem.judge(bestDelta);
        if (best instanceof HoldNote holdNote) {
            holdNote.judged = true;
            holdNote.hit = jr != JudgeSystem.JudgmentResult.MISS;
            if (holdNote.hit) {
                holdNote.holding = true;
                activeHold[lane] = holdNote;
                SFXPlayer.playHitSound(hitSoundVolume);
            } else {
                holdNote.completed = true;
            }
        } else {
            best.judged = true;
            best.hit = jr != JudgeSystem.JudgmentResult.MISS;
            best.completed = true;
            if (best.hit) {
                SFXPlayer.playHitSound(hitSoundVolume);
            }
        }
        if (jr == JudgeSystem.JudgmentResult.MISS) {
            scoreSystem.applyJudgement(jr);
            showJudgement(jr.name());
        } else {
            scoreSystem.applyJudgement(jr);
            showJudgement(jr.name());
        }
    }

    private void handleKeyUp(int lane) {
        if (resultVisible) {
            return;
        }
        long now = TimingEngine.getTimeMs();
        keyDown[lane] = false;
        HoldNote holdNote = activeHold[lane];
        if (holdNote == null || holdNote.completed) {
            return;
        }

        if (now >= holdNote.endTime - hitWindowMs) {
            completeHoldSuccess(lane, holdNote);
        } else {
            completeHoldMiss(lane, holdNote);
        }
    }

    private void showJudgement(String text) {
        judgementMessages.clear();
        judgementMessages.add(new JudgementMessage(text, TimingEngine.getTimeMs() + 400));
    }

    @Override
    public void render(float delta) {
        long now = TimingEngine.getTimeMs();

        float travelMs = (Gdx.graphics.getHeight() - judgeLineY) / pixelsPerMs;
        List<Note> spawned = new ArrayList<>();
        for (Note n : toSpawn) {
            if (n.time - now <= travelMs) {
                lanes[n.lane].add(n);
                spawned.add(n);
            }
        }
        toSpawn.removeAll(spawned);

        for (Lane lane : lanes) {
            for (Note n : lane.active) {
                if (n instanceof HoldNote hold) {
                    if (!hold.judged && now - hold.time > hitWindowMs) {
                        hold.judged = true;
                        hold.hit = false;
                        hold.completed = true;
                        scoreSystem.applyJudgement(JudgeSystem.JudgmentResult.MISS);
                        showJudgement("MISS");
                    }
                } else {
                    if (!n.judged && now - n.time > hitWindowMs) {
                        n.judged = true;
                        n.hit = false;
                        n.completed = true;
                        scoreSystem.applyJudgement(JudgeSystem.JudgmentResult.MISS);
                        showJudgement("MISS");
                    }
                }
            }
        }

        judgementMessages.removeIf(msg -> msg.expiryMs < now);

        updateActiveHolds(now);

        if (!resultVisible && hasChartNotes && shouldFinishGame()) {
            finishGame();
        }

        Gdx.gl.glClearColor(0.1f,0.1f,0.1f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.shapes.begin(ShapeRenderer.ShapeType.Filled);
        float laneW = Gdx.graphics.getWidth() / 4f;
        for (int i = 0; i < 4; i++) {
            float x = i * laneW;
            if (keyDown[i]) {
                renderer.shapes.setColor(0.20f, 0.20f, 0.28f, 1);
            } else {
                renderer.shapes.setColor(0.15f,0.15f,0.15f,1);
            }
            renderer.shapes.rect(x, 0, laneW-2, Gdx.graphics.getHeight());
            renderer.shapes.setColor(Color.WHITE);
            renderer.shapes.rect(x, judgeLineY-4, laneW-2, 4);

            for (Note n : lanes[i].active) {
                if (n instanceof HoldNote hold) {
                    if (hold.completed && hold.hit) {
                        continue;
                    }
                    float startY = judgeLineY + (hold.time - now) * pixelsPerMs;
                    float endY = judgeLineY + (hold.endTime - now) * pixelsPerMs;
                    float topY = Math.min(startY, endY);
                    float height = Math.abs(endY - startY);
                    if (topY + height < -50 || topY > Gdx.graphics.getHeight() + 50) {
                        continue;
                    }
                    if (hold.completed && !hold.hit) {
                        renderer.shapes.setColor(Color.RED);
                    } else {
                        renderer.shapes.setColor(0.2f,0.6f,0.2f,1);
                    }
                    renderer.shapes.rect(x + 20, topY, laneW - 40, height);
                    renderer.shapes.rect(x + 10, startY, laneW - 30, 16);
                    renderer.shapes.rect(x + 10, endY, laneW - 30, 16);
                } else {
                    if (n.completed && n.hit) continue;
                    float y = judgeLineY + (n.time - now) * pixelsPerMs;
                    if (y < -50 || y > Gdx.graphics.getHeight()+50) continue;
                    if (n.judged && !n.hit) {
                        renderer.shapes.setColor(Color.RED);
                    } else {
                        renderer.shapes.setColor(0.2f,0.6f,0.2f,1);
                    }
                    renderer.shapes.rect(x+10, y, laneW-30, 16);
                }
            }
        }

        float progressX = 24f;
        float progressY = Gdx.graphics.getHeight() - 18f;
        float progressW = Gdx.graphics.getWidth() - 48f;
        float progressH = 8f;
        renderer.shapes.setColor(0.22f, 0.22f, 0.22f, 1f);
        renderer.shapes.rect(progressX, progressY, progressW, progressH);
        float progress = Math.min(1f, Math.max(0f, now / (float) chartEndTimeMs));
        renderer.shapes.setColor(0.20f, 0.72f, 0.42f, 1f);
        renderer.shapes.rect(progressX, progressY, progressW * progress, progressH);
        renderer.shapes.end();

        renderer.batch.begin();
        String title = beatmap != null && beatmap.title != null ? beatmap.title : "Untitled";
        String artist = beatmap != null && beatmap.artist != null ? beatmap.artist : "Unknown";
        String scoreText = "Score: " + scoreSystem.getScore();
        String comboText = "Combo: " + scoreSystem.getCombo();
        String accText = String.format("Accuracy: %.2f%%", scoreSystem.getAccuracyPercent());
        String judgeText = String.format("P:%d G:%d GD:%d M:%d H:%d",
                scoreSystem.getPerfectCount(),
                scoreSystem.getGreatCount(),
                scoreSystem.getGoodCount(),
                scoreSystem.getMissCount(),
                scoreSystem.getHoldTailSuccessCount());
        String progressText = scoreSystem.getJudgedCount() + " / " + scoreSystem.getTotalJudgements();
        float hudTopY = Gdx.graphics.getHeight() - 34f;
        float hudGap = 20f;
        renderer.font.draw(renderer.batch, title + " - " + artist, 10, hudTopY);
        renderer.font.draw(renderer.batch, scoreText, 10, hudTopY - hudGap);
        renderer.font.draw(renderer.batch, comboText, 10, hudTopY - hudGap * 2f);
        renderer.font.draw(renderer.batch, accText, 10, hudTopY - hudGap * 3f);
        renderer.font.draw(renderer.batch, judgeText, 10, hudTopY - hudGap * 4f);
        renderer.font.draw(renderer.batch, "Progress: " + progressText, 10, hudTopY - hudGap * 5f);
        renderer.font.draw(renderer.batch, "ESC: Back to Library", 10, hudTopY - hudGap * 6f);
        renderer.font.draw(renderer.batch, "R: Finish and show result", 10, hudTopY - hudGap * 7f);
        if (!hasChartNotes) {
            renderer.font.draw(renderer.batch, "This beatmap has no notes yet. Press ESC to return.", 10, hudTopY - hudGap * 8.5f);
        }

        GlyphLayout layout = new GlyphLayout();
        for (JudgementMessage message : judgementMessages) {
            layout.setText(renderer.font, message.text);
            float x = Gdx.graphics.getWidth() / 2f - layout.width / 2f;
            float y = judgeLineY + 60;
            renderer.font.draw(renderer.batch, message.text, x, y);
        }
        renderer.batch.end();

        if (resultVisible) {
            renderResultDialog();
        }
    }

    private void updateActiveHolds(long now) {
        for (int lane = 0; lane < activeHold.length; lane++) {
            HoldNote hold = activeHold[lane];
            if (hold == null || hold.completed) {
                activeHold[lane] = null;
                continue;
            }

            // If the player keeps holding through the tail, auto-complete successfully.
            if (hold.holding && now >= hold.endTime) {
                completeHoldSuccess(lane, hold);
                continue;
            }

            // Safety path: if hold becomes inactive and is clearly past tail timing, fail it.
            if (!hold.holding && now > hold.endTime + hitWindowMs) {
                completeHoldMiss(lane, hold);
            }
        }
    }

    private void completeHoldSuccess(int lane, HoldNote holdNote) {
        holdNote.completed = true;
        holdNote.hit = true;
        holdNote.holding = false;
        activeHold[lane] = null;
        scoreSystem.applyHoldTailSuccess();
        showJudgement("HOLD OK");
    }

    private void completeHoldMiss(int lane, HoldNote holdNote) {
        holdNote.completed = true;
        holdNote.hit = false;
        holdNote.holding = false;
        activeHold[lane] = null;
        scoreSystem.applyJudgement(JudgeSystem.JudgmentResult.MISS);
        showJudgement("MISS");
    }

    private boolean isTrackCompleted() {
        if (!toSpawn.isEmpty()) {
            return false;
        }
        for (HoldNote hold : activeHold) {
            if (hold != null && !hold.completed) {
                return false;
            }
        }
        for (Lane lane : lanes) {
            for (Note note : lane.active) {
                if (!note.completed) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean shouldFinishGame() {
        // When music exists, result should only appear after playback ends.
        if (MusicPlayer.hasLoadedMusic()) {
            return MusicPlayer.isPlaybackCompleted();
        }
        // Fallback for charts without audio: keep old note-based completion behavior.
        return isTrackCompleted();
    }

    private void finishGame() {
        resultVisible = true;
        MusicPlayer.stop();
    }

    private void renderResultDialog() {
        int px = (Gdx.graphics.getWidth() - resultWidth) / 2;
        int py = (Gdx.graphics.getHeight() - resultHeight) / 2;

        shapesBeginOverlay(px, py, resultWidth, resultHeight);

        int retryX = px + resultWidth - 560;
        int retryY = py + 130;
        int backX = px + resultWidth - 290;
        int backY = py + 130;
        int btnW = 220;
        int btnH = 80;

        renderer.shapes.begin(ShapeRenderer.ShapeType.Filled);
        renderer.shapes.setColor(0.22f, 0.38f, 0.25f, 1f);
        renderer.shapes.rect(retryX, retryY, btnW, btnH);
        renderer.shapes.setColor(0.30f, 0.25f, 0.22f, 1f);
        renderer.shapes.rect(backX, backY, btnW, btnH);
        renderer.shapes.end();

        renderer.batch.begin();
        renderer.font.setColor(Color.WHITE);
        renderer.font.draw(renderer.batch, "Result", px + 60, py + resultHeight - 60);
        String title = beatmap != null && beatmap.title != null ? beatmap.title : "Untitled";
        renderer.font.draw(renderer.batch, "Song: " + title, px + 60, py + resultHeight - 120);
        renderer.font.draw(renderer.batch, "Score: " + scoreSystem.getScore(), px + 60, py + resultHeight - 200);
        renderer.font.draw(renderer.batch, "Max Combo: " + scoreSystem.getMaxCombo(), px + 60, py + resultHeight - 260);
        renderer.font.draw(renderer.batch, String.format("Accuracy: %.2f%%", scoreSystem.getAccuracyPercent()), px + 60, py + resultHeight - 320);
        renderer.font.draw(renderer.batch, String.format("PERFECT: %d", scoreSystem.getPerfectCount()), px + 60, py + resultHeight - 410);
        renderer.font.draw(renderer.batch, String.format("GREAT: %d", scoreSystem.getGreatCount()), px + 60, py + resultHeight - 460);
        renderer.font.draw(renderer.batch, String.format("GOOD: %d", scoreSystem.getGoodCount()), px + 60, py + resultHeight - 510);
        renderer.font.draw(renderer.batch, String.format("MISS: %d", scoreSystem.getMissCount()), px + 60, py + resultHeight - 560);
        renderer.font.draw(renderer.batch, "HOLD OK: " + scoreSystem.getHoldTailSuccessCount(), px + 60, py + resultHeight - 610);

        renderer.font.draw(renderer.batch, "Retry", retryX + 70, retryY + 50);
        renderer.font.draw(renderer.batch, "Back", backX + 78, backY + 50);
        renderer.font.draw(renderer.batch, "Enter: Retry", px + 60, py + 70);
        renderer.font.draw(renderer.batch, "ESC: Back to Library", px + 300, py + 70);
        renderer.batch.end();
    }

    private void shapesBeginOverlay(int px, int py, int popupWidth, int popupHeight) {
        renderer.shapes.begin(ShapeRenderer.ShapeType.Filled);
        renderer.shapes.setColor(0f, 0f, 0f, 0.72f);
        renderer.shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderer.shapes.setColor(0.12f, 0.12f, 0.16f, 1f);
        renderer.shapes.rect(px, py, popupWidth, popupHeight);
        renderer.shapes.end();
    }

    private void restartGame() {
        if (beatmapDirectory != null) {
            game.setScreen(new GameScreen(game, beatmapDirectory));
        } else {
            game.setScreen(new GameScreen(game));
        }
        dispose();
    }

    private void backToLibrary() {
        MusicPlayer.stop();
        game.setScreen(new LibraryScreen(game));
        dispose();
    }

    private boolean isInside(int px, int py, int x, int yTop, int width, int height) {
        return px >= x && px <= x + width && py <= yTop && py >= yTop - height;
    }

    @Override public void resize(int width, int height) {}
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void dispose() {
        renderer.dispose();
    }

    private static class JudgementMessage {
        final String text;
        final long expiryMs;

        JudgementMessage(String text, long expiryMs) {
            this.text = text;
            this.expiryMs = expiryMs;
        }
    }
}
