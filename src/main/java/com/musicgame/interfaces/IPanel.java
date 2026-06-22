package com.musicgame.interfaces;

import javafx.scene.layout.Pane;

public interface IPanel {
    Pane getRoot();
    void onShow();
    void onHide();
    void onClose();
}