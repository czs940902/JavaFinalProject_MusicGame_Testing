package com.rhythmgame.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.rhythmgame.gameplay.Lane;
import com.rhythmgame.gameplay.Note;

public class GameRenderer {
    public final ShapeRenderer shapes = new ShapeRenderer();
    public final SpriteBatch batch = new SpriteBatch();
    private FreeTypeFontGenerator fontGenerator;
    public final BitmapFont font = loadFont();

    private BitmapFont loadFont() {
        try {
            FileHandle fontFile = Gdx.files.internal("fonts/NotoSansHK-VF.ttf");
            if (fontFile.exists()) {
                fontGenerator = new FreeTypeFontGenerator(fontFile);
                FreeTypeFontGenerator.FreeTypeFontParameter params = new FreeTypeFontGenerator.FreeTypeFontParameter();
                params.size = 22;
                params.minFilter = Texture.TextureFilter.Linear;
                params.magFilter = Texture.TextureFilter.Linear;
                params.incremental = true;
                return fontGenerator.generateFont(params);
            }
        } catch (Exception e) {
            Gdx.app.error("GameRenderer", "loadFont", e);
        }
        return new BitmapFont();
    }

    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        if (fontGenerator != null) {
            fontGenerator.dispose();
        }
    }
}
