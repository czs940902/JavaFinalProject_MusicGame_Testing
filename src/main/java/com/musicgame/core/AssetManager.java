package com.musicgame.core;

import java.io.IOException;
import java.io.InputStream;

import javafx.scene.image.Image;

public class AssetManager {

	private static final AssetManager INSTANCE = new AssetManager();

	private Image backgroundImage;

	private AssetManager() {
		// private constructor to enforce singleton
	}

	public static AssetManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the library background image loaded from resources/images/LibraryBackground.png
	 * Caches the image after first load.
	 */
	public Image getBackgroundImage() {
		if (backgroundImage == null) {
			try (InputStream is = getClass().getResourceAsStream("/images/LibraryBackground.png")) {
				if (is == null) {
					throw new IOException("Resource not found: /images/LibraryBackground.png");
				}
				backgroundImage = new Image(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return backgroundImage;
	}
}
