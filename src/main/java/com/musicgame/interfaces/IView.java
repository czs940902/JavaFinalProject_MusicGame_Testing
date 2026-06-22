package com.musicgame.interfaces;

import javafx.scene.layout.Pane;

public interface IView {
    Pane getRoot();
    void onShow();
    void onHide();
}