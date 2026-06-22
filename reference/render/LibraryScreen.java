package com.rhythmgame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.rhythmgame.RhythmGame;
import com.rhythmgame.gameplay.Beatmap;
import com.rhythmgame.gameplay.BeatmapIO;
import com.rhythmgame.gameplay.BeatmapRepository;
import com.rhythmgame.gameplay.BeatmapRepository.BeatmapEntry;

import java.io.IOException;
import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryScreen implements Screen {
    private final RhythmGame game;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font;
    private FreeTypeFontGenerator fontGenerator;
    private final GlyphLayout layout = new GlyphLayout();
    private final BeatmapRepository repository;
    private final Map<String, Texture> coverTextureCache = new HashMap<>();

    private final List<BeatmapEntry> entries = new ArrayList<>();
    private final List<BeatmapEntry> filtered = new ArrayList<>();
    private String filterText = "";
    private float scrollY = 0;
    private float contentHeight = 0;
    private boolean showContextMenu = false;
    private int menuX;
    private int menuY;
    private BeatmapEntry contextEntry;

    private PopupMode popupMode = PopupMode.NONE;
    private BeatmapEntry editEntry;
    private String popupTitle = "";
    private String popupArtist = "";
    private String popupAuthor = "";
    private String popupAudio = "";
    private String popupCover = "";
    private ActiveField activeField = ActiveField.NONE;
    private String messageText = "";
    private Texture popupCoverPreview;
    private String popupCoverPreviewPath = "";
    private Beatmap popupImportedBeatmap;
    private Path popupImportTempDir;

    private final int coverWidth = 400;
    private final int coverHeight = 225;
    private final int margin = 40;
    private final int columns = 4;
    private final int itemSpacing = 30;
    private final int topBarHeight = 120;
    private final int itemBlockHeight = coverHeight + 100;
    private final int popupWidth = 1600;
    private final int popupHeight = 900;

    private enum PopupMode {NONE, NEW, EDIT}
    private enum ActiveField {NONE, TITLE, ARTIST, AUTHOR, AUDIO, COVER}

    public LibraryScreen(RhythmGame game) {
        this.game = game;
        this.repository = new BeatmapRepository(Paths.get("beatmaps"));
        this.font = loadFont();
        loadEntries();
        Gdx.input.setInputProcessor(new LibraryInput());
    }

    private BitmapFont loadFont() {
        try {
            FileHandle fontFile = Gdx.files.internal("fonts/NotoSansHK-VF.ttf");
            if (fontFile.exists()) {
                fontGenerator = new FreeTypeFontGenerator(fontFile);
                FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
                params.size = 32;
                params.minFilter = Texture.TextureFilter.Linear;
                params.magFilter = Texture.TextureFilter.Linear;
                params.incremental = true;
                return fontGenerator.generateFont(params);
            }
        } catch (Exception e) {
            Gdx.app.error("LibraryScreen", "loadFont", e);
        }
        return new BitmapFont();
    }

    private void loadEntries() {
        entries.clear();
        try {
            entries.addAll(repository.loadEntries());
            filtered.clear();
            filtered.addAll(entries);
            calculateContentHeight();
            trimCoverTextureCache();
        } catch (Exception e) {
            Gdx.app.error("LibraryScreen", "loadEntries", e);
        }
    }

    private void trimCoverTextureCache() {
        Set<String> activeCovers = new HashSet<>();
        for (BeatmapEntry entry : entries) {
            String cover = entry.beatmap().cover;
            if (cover != null && !cover.isBlank()) {
                activeCovers.add(cover);
            }
        }
        coverTextureCache.entrySet().removeIf(cacheEntry -> {
            if (!activeCovers.contains(cacheEntry.getKey())) {
                cacheEntry.getValue().dispose();
                return true;
            }
            return false;
        });
    }

    private Texture getCoverTexture(BeatmapEntry entry) {
        String coverPath = entry.beatmap().cover;
        if (coverPath == null || coverPath.isBlank()) {
            return null;
        }
        Texture cached = coverTextureCache.get(coverPath);
        if (cached != null) {
            return cached;
        }
        try {
            Path cover = Paths.get(coverPath);
            if (!Files.exists(cover)) {
                return null;
            }
            Texture texture = new Texture(Gdx.files.absolute(cover.toString()));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            coverTextureCache.put(coverPath, texture);
            return texture;
        } catch (Exception e) {
            Gdx.app.error("LibraryScreen", "getCoverTexture", e);
            return null;
        }
    }

    private void calculateContentHeight() {
        int rows = (filtered.size() + 1 + columns - 1) / columns;
        contentHeight = rows * itemBlockHeight + margin;
        float minHeight = Gdx.graphics.getHeight() - topBarHeight - margin;
        if (contentHeight < minHeight) {
            contentHeight = minHeight;
        }
        clampScroll();
    }

    private void updateFilter(String typed) {
        filterText = typed.toLowerCase();
        filtered.clear();
        for (BeatmapEntry entry : entries) {
            String title = entry.beatmap().title == null ? "" : entry.beatmap().title.toLowerCase();
            String artist = entry.beatmap().artist == null ? "" : entry.beatmap().artist.toLowerCase();
            if (title.contains(filterText) || artist.contains(filterText)) {
                filtered.add(entry);
            }
        }
        calculateContentHeight();
    }

    private void openNewPopup() {
        popupMode = PopupMode.NEW;
        editEntry = null;
        cleanupImportTempDir();
        popupImportedBeatmap = null;
        popupTitle = "";
        popupArtist = "";
        popupAuthor = "";
        popupAudio = "";
        popupCover = "";
        activeField = ActiveField.TITLE;
        messageText = "";
        clearCoverPreview();
    }

    private void openEditPopup(BeatmapEntry entry) {
        popupMode = PopupMode.EDIT;
        editEntry = entry;
        popupTitle = entry.beatmap().title == null ? "" : entry.beatmap().title;
        popupArtist = entry.beatmap().artist == null ? "" : entry.beatmap().artist;
        popupAuthor = entry.beatmap().author == null ? "" : entry.beatmap().author;
        popupAudio = entry.beatmap().audio == null ? "" : entry.beatmap().audio;
        popupCover = entry.beatmap().cover == null ? "" : entry.beatmap().cover;
        activeField = ActiveField.TITLE;
        messageText = "";
        updateCoverPreviewFromPath(popupCover);
    }

    private void closePopup() {
        if (popupMode == PopupMode.NEW) {
            cleanupImportTempDir();
            popupImportedBeatmap = null;
        }
        popupMode = PopupMode.NONE;
        editEntry = null;
        activeField = ActiveField.NONE;
        messageText = "";
        clearCoverPreview();
    }

    private void savePopupData(boolean next) {
        if (popupTitle.isBlank()) {
            messageText = "Please enter a song title.";
            return;
        }
        try {
            Beatmap beatmap = new Beatmap();
            beatmap.title = popupTitle;
            beatmap.artist = popupArtist;
            beatmap.author = popupAuthor;
            beatmap.audio = popupAudio.isBlank() ? null : popupAudio;
            beatmap.cover = popupCover.isBlank() ? null : popupCover;
            if (popupMode == PopupMode.EDIT && editEntry != null && editEntry.beatmap() != null) {
                Beatmap existing = editEntry.beatmap();
                beatmap.bpm = existing.bpm;
                beatmap.offset = existing.offset;
                beatmap.notes = existing.notes != null ? new ArrayList<>(existing.notes) : new ArrayList<>();
            } else if (popupMode == PopupMode.NEW && popupImportedBeatmap != null) {
                beatmap.bpm = popupImportedBeatmap.bpm;
                beatmap.offset = popupImportedBeatmap.offset;
                beatmap.notes = popupImportedBeatmap.notes != null ? new ArrayList<>(popupImportedBeatmap.notes) : new ArrayList<>();
            } else {
                beatmap.bpm = 120.0;
                beatmap.offset = 0.0;
                beatmap.notes = new ArrayList<>();
            }

            Path saveDir;
            if (popupMode == PopupMode.EDIT && editEntry != null) {
                saveDir = editEntry.directory();
            } else {
                String folderName = sanitizeFolderName(popupTitle);
                if (folderName.isBlank()) folderName = "beatmap";
                Path base = Paths.get("beatmaps");
                saveDir = base.resolve(folderName);
                int suffix = 1;
                while (Files.exists(saveDir)) {
                    saveDir = base.resolve(folderName + "-" + suffix);
                    suffix++;
                }
            }
            Path audioPath = resolvePath(popupAudio);
            Path coverPath = resolvePath(popupCover);
            BeatmapIO.saveToDirectory(beatmap, saveDir, audioPath, coverPath);
            cleanupImportTempDir();
            popupImportedBeatmap = null;
            loadEntries();
            if (next) {
                game.setScreen(new EditorScreen(game, saveDir));
                dispose();
            } else {
                closePopup();
            }
        } catch (IOException e) {
            messageText = "Failed to save: " + e.getMessage();
            Gdx.app.error("LibraryScreen", "savePopupData", e);
        }
    }

    private Path resolvePath(String pathText) {
        if (pathText == null || pathText.isBlank()) return null;
        Path path = Paths.get(pathText);
        return Files.exists(path) ? path : null;
    }

    private void chooseAudioFile() {
        String selected = chooseFile("Select Audio", "*.mp3;*.wav;*.ogg");
        if (selected != null) {
            popupAudio = selected;
            activeField = ActiveField.AUDIO;
        }
    }

    private void chooseCoverFile() {
        String selected = chooseFile("Select Cover", "*.png;*.jpg;*.jpeg;*.webp");
        if (selected != null) {
            popupCover = selected;
            activeField = ActiveField.COVER;
            updateCoverPreviewFromPath(selected);
        }
    }

    private String chooseFile(String title, String filter) {
        try {
            Frame frame = null;
            FileDialog dialog = new FileDialog(frame, title, FileDialog.LOAD);
            dialog.setFile(filter);
            dialog.setVisible(true);
            if (dialog.getFile() == null || dialog.getDirectory() == null) {
                return null;
            }
            return Paths.get(dialog.getDirectory(), dialog.getFile()).toString();
        } catch (Exception e) {
            messageText = "File picker unavailable: " + e.getMessage();
            Gdx.app.error("LibraryScreen", "chooseFile", e);
            return null;
        }
    }

    private void importFromZip() {
        String selected = chooseFile("Import Beatmap Zip", "*.zip");
        if (selected == null) {
            return;
        }

        try {
            cleanupImportTempDir();
            Path zipFile = Paths.get(selected);
            if (!Files.exists(zipFile)) {
                messageText = "Import failed: zip file not found.";
                return;
            }

            Path tempDir = Files.createTempDirectory("rhythmgame-import-");
            BeatmapIO.importZipToDirectory(zipFile, tempDir);
            Beatmap imported = BeatmapIO.loadFromDirectory(tempDir);

            popupImportTempDir = tempDir;
            popupImportedBeatmap = imported;
            popupTitle = imported.title == null ? "" : imported.title;
            popupArtist = imported.artist == null ? "" : imported.artist;
            popupAuthor = imported.author == null ? "" : imported.author;
            popupAudio = imported.audio == null ? "" : imported.audio;
            popupCover = imported.cover == null ? "" : imported.cover;
            activeField = ActiveField.TITLE;
            updateCoverPreviewFromPath(popupCover);
            messageText = "Import loaded. Review fields and click Next.";
        } catch (IOException e) {
            cleanupImportTempDir();
            popupImportedBeatmap = null;
            messageText = "Import failed: " + e.getMessage();
            Gdx.app.error("LibraryScreen", "importFromZip", e);
        }
    }

    private void cleanupImportTempDir() {
        if (popupImportTempDir == null || !Files.exists(popupImportTempDir)) {
            popupImportTempDir = null;
            return;
        }
        try {
            Files.walk(popupImportTempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
        popupImportTempDir = null;
    }

    private void updateCoverPreviewFromPath(String pathText) {
        clearCoverPreview();
        if (pathText == null || pathText.isBlank()) {
            return;
        }
        try {
            Path path = Paths.get(pathText);
            if (!Files.exists(path)) {
                return;
            }
            popupCoverPreview = new Texture(Gdx.files.absolute(path.toString()));
            popupCoverPreviewPath = path.toString();
        } catch (Exception e) {
            popupCoverPreview = null;
            popupCoverPreviewPath = "";
            messageText = "Cover preview failed.";
            Gdx.app.error("LibraryScreen", "updateCoverPreviewFromPath", e);
        }
    }

    private void clearCoverPreview() {
        if (popupCoverPreview != null) {
            popupCoverPreview.dispose();
            popupCoverPreview = null;
        }
        popupCoverPreviewPath = "";
    }

    private String sanitizeFolderName(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9_-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private void exportEntry(BeatmapEntry entry) {
        try {
            Path exports = Paths.get("exports");
            Files.createDirectories(exports);
            String fileName = sanitizeFolderName(entry.beatmap().title == null ? "beatmap" : entry.beatmap().title);
            if (fileName.isBlank()) fileName = "beatmap";
            Path zip = exports.resolve(fileName + ".zip");
            int suffix = 1;
            while (Files.exists(zip)) {
                zip = exports.resolve(fileName + "-" + suffix + ".zip");
                suffix++;
            }
            BeatmapIO.exportBeatmapToZip(entry.directory(), zip);
            messageText = "Exported to " + zip.toString();
        } catch (IOException e) {
            messageText = "Export failed: " + e.getMessage();
            Gdx.app.error("LibraryScreen", "exportEntry", e);
        }
    }

    private void deleteEntry(BeatmapEntry entry) {
        try {
            Files.walk(entry.directory())
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            loadEntries();
            messageText = "Deleted " + entry.beatmap().title;
        } catch (IOException e) {
            messageText = "Delete failed: " + e.getMessage();
            Gdx.app.error("LibraryScreen", "deleteEntry", e);
        }
    }

    private void openGameFor(BeatmapEntry entry) {
        game.setScreen(new GameScreen(game, entry.directory()));
        dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new LibraryInput());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderBackground();
        renderTopBar();
        renderGrid();

        if (showContextMenu && contextEntry != null) {
            renderContextMenu();
        }
        if (popupMode != PopupMode.NONE) {
            renderPopup();
        }
        renderFooter();
    }

    private void renderBackground() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.08f, 0.12f, 1);
        shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes.end();
    }

    private void renderTopBar() {
        float topBarY = Gdx.graphics.getHeight() - topBarHeight - margin;
        float searchBoxWidth = 520f;
        float searchBoxHeight = 60f;
        float searchBoxX = Gdx.graphics.getWidth() - margin - 20f - searchBoxWidth;
        float searchBoxY = Gdx.graphics.getHeight() - topBarHeight / 2f - 20f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.18f, 1);
        shapes.rect(margin, topBarY, Gdx.graphics.getWidth() - margin * 2, topBarHeight);
        shapes.setColor(Color.BLACK);
        shapes.rect(searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.8f);
        font.draw(batch, "Library", margin + 20, Gdx.graphics.getHeight() - margin - 30);
        font.getData().setScale(1.0f);
        font.draw(batch, "Search:", searchBoxX + 16, Gdx.graphics.getHeight() - topBarHeight / 2f + 12);
        font.draw(batch, filterText.isEmpty() ? "type to search..." : filterText,
            searchBoxX + 120, Gdx.graphics.getHeight() - topBarHeight / 2f + 12);
        batch.end();
    }

    private void renderGrid() {
        int startX = margin;
        int startY = Gdx.graphics.getHeight() - topBarHeight - margin;
        int row = 0;
        int col = 0;

        for (int index = 0; index < filtered.size() + 1; index++) {
            int itemX = startX + col * (coverWidth + itemSpacing);
            int itemY = startY - row * itemBlockHeight + (int) scrollY;
            if (itemY < -coverHeight) {
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
                continue;
            }

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            if (index == 0) {
                shapes.setColor(0.16f, 0.16f, 0.22f, 1);
            } else {
                shapes.setColor(0.18f, 0.18f, 0.26f, 1);
            }
            shapes.rect(itemX, itemY - coverHeight, coverWidth, coverHeight);
            shapes.end();

            batch.begin();
            font.setColor(Color.WHITE);
            if (index == 0) {
                font.getData().setScale(3f);
                font.draw(batch, "+", itemX + coverWidth / 2f - 10, itemY - coverHeight / 2f + 25);
                font.getData().setScale(1f);
                font.draw(batch, "Add new beatmap", itemX + 10, itemY - coverHeight - 15);
            } else {
                BeatmapEntry entry = filtered.get(index - 1);
                Texture coverTexture = getCoverTexture(entry);
                if (coverTexture != null) {
                    float targetX = itemX;
                    float targetY = itemY - coverHeight;
                    float textureWidth = coverTexture.getWidth();
                    float textureHeight = coverTexture.getHeight();
                    float scale = Math.min(coverWidth / textureWidth, coverHeight / textureHeight);
                    float drawWidth = textureWidth * scale;
                    float drawHeight = textureHeight * scale;
                    float drawX = targetX + (coverWidth - drawWidth) / 2f;
                    float drawY = targetY + (coverHeight - drawHeight) / 2f;
                    batch.draw(coverTexture, drawX, drawY, drawWidth, drawHeight);
                }
                font.getData().setScale(1.2f);
                font.draw(batch, entry.beatmap().title == null ? "Untitled" : entry.beatmap().title,
                        itemX, itemY - coverHeight - 10);
                font.getData().setScale(1.0f);
                font.draw(batch, entry.beatmap().artist == null ? "Unknown" : entry.beatmap().artist,
                    itemX, itemY - coverHeight - 42);
            }
            batch.end();

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
    }

    private void renderContextMenu() {
        int width = 220;
        int height = 140;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.18f, 1);
        shapes.rect(menuX, menuY - height, width, height);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.0f);
        font.draw(batch, "Export", menuX + 20, menuY - 20);
        font.draw(batch, "Edit", menuX + 20, menuY - 60);
        font.draw(batch, "Delete", menuX + 20, menuY - 100);
        batch.end();
    }

    private void renderPopup() {
        int px = (Gdx.graphics.getWidth() - popupWidth) / 2;
        int py = (Gdx.graphics.getHeight() - popupHeight) / 2;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0, 0, 0, 0.8f);
        shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes.setColor(0.12f, 0.12f, 0.16f, 1);
        shapes.rect(px, py, popupWidth, popupHeight);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);
        font.draw(batch, popupMode == PopupMode.NEW ? "新增譜面" : "編輯譜面", px + 40, py + popupHeight - 50);
        font.getData().setScale(1f);
        batch.end();

        int leftX = px + 60;
        int rightX = px + popupWidth - 720;
        int fieldY = py + popupHeight - 150;

        drawTextbox(leftX, fieldY, 500, 50, "歌曲標題", popupTitle, activeField == ActiveField.TITLE);
        drawTextbox(leftX, fieldY - 90, 500, 50, "歌手", popupArtist, activeField == ActiveField.ARTIST);
        drawTextbox(leftX, fieldY - 180, 500, 50, "譜面作者", popupAuthor, activeField == ActiveField.AUTHOR);

        drawButton(rightX, fieldY, 640, 100, "選擇音源", popupAudio.isBlank() ? "(type path)" : popupAudio, activeField == ActiveField.AUDIO);
        drawBox(rightX, fieldY - 140, 640, 360, popupCover.isBlank() ? "選擇封面圖片" : popupCover, activeField == ActiveField.COVER);

        int buttonY = py + 80;
        if (popupMode == PopupMode.NEW) {
            drawButton(px + 80, buttonY, 200, 60, "匯入", "", false);
        }
        drawButton(px + popupWidth - 520, buttonY, 200, 60, "取消", "", false);
        drawButton(px + popupWidth - 280, buttonY, 200, 60, "下一步", "", false);

        if (!messageText.isBlank()) {
            batch.begin();
            font.setColor(Color.YELLOW);
            font.draw(batch, messageText, px + 60, py + 60);
            batch.end();
        }
    }

    private void drawTextbox(int x, int y, int width, int height, String label, String value, boolean active) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(active ? 0.18f : 0.14f, 0.14f, 0.18f, 1);
        shapes.rect(x, y - height, width, height);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.draw(batch, label, x + 10, y - 18);
        font.draw(batch, value.isBlank() ? "" : value, x + 10, y - 40);
        batch.end();
    }

    private void drawButton(int x, int y, int width, int height, String label, String value, boolean active) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(active ? 0.24f : 0.18f, 0.18f, 0.22f, 1);
        shapes.rect(x, y - height, width, height);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.draw(batch, label, x + 20, y - 30);
        if (!value.isBlank()) {
            font.draw(batch, value, x + 20, y - 60);
        }
        batch.end();
    }

    private void drawBox(int x, int y, int width, int height, String label, boolean active) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(active ? 0.18f : 0.14f, 0.14f, 0.18f, 1);
        shapes.rect(x, y - height, width, height);
        shapes.end();

        if (popupCoverPreview != null) {
            float boxX = x + 12f;
            float boxY = y - height + 12f;
            float availW = width - 24f;
            float availH = height - 24f;
            float texW = popupCoverPreview.getWidth();
            float texH = popupCoverPreview.getHeight();
            float scale = Math.min(availW / texW, availH / texH);
            float drawW = texW * scale;
            float drawH = texH * scale;
            float drawX = boxX + (availW - drawW) / 2f;
            float drawY = boxY + (availH - drawH) / 2f;
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(popupCoverPreview, drawX, drawY, drawW, drawH);
            batch.end();
        }

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.draw(batch, label, x + 10, y - 20);
        if (!popupCoverPreviewPath.isBlank()) {
            font.draw(batch, Paths.get(popupCoverPreviewPath).getFileName().toString(), x + 10, y - height + 24);
        }
        batch.end();
    }

    private void renderFooter() {
        batch.begin();
        font.setColor(Color.LIGHT_GRAY);
        font.getData().setScale(1f);
        font.draw(batch, "Right-click beatmap cover for actions. Use the search box above.", margin, 30);
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
        for (Texture texture : coverTextureCache.values()) {
            texture.dispose();
        }
        coverTextureCache.clear();
        clearCoverPreview();
        cleanupImportTempDir();
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
    }

    private class LibraryInput extends com.badlogic.gdx.InputAdapter {
        @Override
        public boolean keyTyped(char character) {
            if (popupMode != PopupMode.NONE && activeField != ActiveField.NONE) {
                if (character == '\b') {
                    String current = getFieldValue(activeField);
                    if (!current.isEmpty()) {
                        setFieldValue(activeField, current.substring(0, current.length() - 1));
                    }
                } else if (character >= 32 && character < 127) {
                    String current = getFieldValue(activeField);
                    setFieldValue(activeField, current + character);
                }
                return true;
            }

            if (character == '\b' && !filterText.isEmpty()) {
                filterText = filterText.substring(0, filterText.length() - 1);
            } else if (character >= 32 && character < 127) {
                filterText += character;
            }
            updateFilter(filterText);
            return true;
        }

        @Override
        public boolean keyDown(int keycode) {
            if ((keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) && popupMode != PopupMode.NONE) {
                savePopupData(true);
                return true;
            }
            if (keycode == Input.Keys.ESCAPE && popupMode != PopupMode.NONE) {
                closePopup();
                return true;
            }
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            if (popupMode == PopupMode.NONE) {
                scrollY -= amountY * 40;
                clampScroll();
            }
            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            int y = Gdx.graphics.getHeight() - screenY;
            if (popupMode != PopupMode.NONE) {
                if (handlePopupTouch(screenX, y, button)) {
                    return true;
                }
                closePopup();
                return true;
            }
            if (showContextMenu) {
                if (handleContextMenuTouch(screenX, y, button)) {
                    return true;
                }
                showContextMenu = false;
                return true;
            }
            if (button == Input.Buttons.RIGHT) {
                BeatmapEntry entry = hitTestEntry(screenX, y);
                if (entry != null) {
                    contextEntry = entry;
                    showContextMenu = true;
                    menuX = screenX;
                    menuY = y;
                    return true;
                }
            }
            if (button == Input.Buttons.LEFT) {
                if (hitTestAddButton(screenX, y)) {
                    openNewPopup();
                    return true;
                }
                BeatmapEntry entry = hitTestEntry(screenX, y);
                if (entry != null) {
                    openGameFor(entry);
                    return true;
                }
            }
            return true;
        }

        private BeatmapEntry hitTestEntry(int x, int y) {
            int startX = margin;
            int startY = Gdx.graphics.getHeight() - topBarHeight - margin;
            int row = 0;
            int col = 0;
            for (int index = 0; index < filtered.size() + 1; index++) {
                int itemX = startX + col * (coverWidth + itemSpacing);
                int itemY = startY - row * itemBlockHeight + (int) scrollY;
                if (index > 0 && isInside(itemX, itemY, coverWidth, coverHeight, x, y)) {
                    return filtered.get(index - 1);
                }
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
            return null;
        }

        private boolean hitTestAddButton(int x, int y) {
            int itemX = margin;
            int itemY = Gdx.graphics.getHeight() - topBarHeight - margin + (int) scrollY;
            return isInside(itemX, itemY, coverWidth, coverHeight, x, y);
        }

        private boolean handleContextMenuTouch(int screenX, int y, int button) {
            if (button != Input.Buttons.LEFT) return false;
            int width = 220;
            int itemHeight = 40;
            for (int idx = 0; idx < 3; idx++) {
                int itemTop = menuY - idx * itemHeight;
                if (isInside(menuX + 10, itemTop, width - 20, itemHeight, screenX, y)) {
                    switch (idx) {
                        case 0 -> exportEntry(contextEntry);
                        case 1 -> openEditPopup(contextEntry);
                        case 2 -> deleteEntry(contextEntry);
                    }
                    showContextMenu = false;
                    return true;
                }
            }
            return false;
        }

        private boolean handlePopupTouch(int x, int y, int button) {
            if (button != Input.Buttons.LEFT) return false;
            int px = (Gdx.graphics.getWidth() - popupWidth) / 2;
            int py = (Gdx.graphics.getHeight() - popupHeight) / 2;
            if (!isInside(px, py + popupHeight, popupWidth, popupHeight, x, y)) {
                return false;
            }
            int leftX = px + 60;
            int rightX = px + popupWidth - 720;
            int fieldY = py + popupHeight - 150;
            if (isInside(leftX, fieldY, 500, 50, x, y)) { activeField = ActiveField.TITLE; return true; }
            if (isInside(leftX, fieldY - 90, 500, 50, x, y)) { activeField = ActiveField.ARTIST; return true; }
            if (isInside(leftX, fieldY - 180, 500, 50, x, y)) { activeField = ActiveField.AUTHOR; return true; }
            if (isInside(rightX, fieldY, 640, 100, x, y)) {
                chooseAudioFile();
                return true;
            }
            if (isInside(rightX, fieldY - 140, 640, 360, x, y)) {
                chooseCoverFile();
                return true;
            }
            int buttonY = py + 80;
            if (popupMode == PopupMode.NEW && isInside(px + 80, buttonY, 200, 60, x, y)) {
                importFromZip();
                return true;
            }
            if (isInside(px + popupWidth - 520, buttonY, 200, 60, x, y)) {
                closePopup();
                return true;
            }
            if (isInside(px + popupWidth - 280, buttonY, 200, 60, x, y)) {
                savePopupData(true);
                return true;
            }
            return true;
        }
    }

    private void clampScroll() {
        float minScroll = Math.min(0, Gdx.graphics.getHeight() - topBarHeight - margin - contentHeight);
        if (scrollY < minScroll) scrollY = minScroll;
        if (scrollY > 0) scrollY = 0;
    }

    private String getFieldValue(ActiveField field) {
        return switch (field) {
            case TITLE -> popupTitle;
            case ARTIST -> popupArtist;
            case AUTHOR -> popupAuthor;
            case AUDIO -> popupAudio;
            case COVER -> popupCover;
            default -> "";
        };
    }

    private void setFieldValue(ActiveField field, String value) {
        switch (field) {
            case TITLE -> popupTitle = value;
            case ARTIST -> popupArtist = value;
            case AUTHOR -> popupAuthor = value;
            case AUDIO -> popupAudio = value;
            case COVER -> popupCover = value;
            default -> {}
        }
    }

    private boolean isInside(int x, int y, int width, int height, int px, int py) {
        return px >= x && px <= x + width && py <= y && py >= y - height;
    }
}
