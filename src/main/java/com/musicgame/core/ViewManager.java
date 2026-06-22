package com.musicgame.core;

import com.musicgame.Main;
import com.musicgame.interfaces.IPanel;
import com.musicgame.interfaces.IView;
import com.musicgame.view.LibraryView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import java.util.Stack;

public class ViewManager {
    private static final ViewManager INSTANCE = new ViewManager();
    private final StackPane root;
    private final StackPane viewContainer;
    private final StackPane panelContainer;
    private final StackPane interruptContainer;
    private final Stack<IPanel> panelStack;
    private IView currentView;
    
    private ViewManager() {
        // Containers
        root = new StackPane();
        root.setPrefSize(Main.GAME_W, Main.GAME_H);
        viewContainer = new StackPane();
        viewContainer.setPrefSize(Main.GAME_W, Main.GAME_H);
        panelContainer = new StackPane();
        panelContainer.setPrefSize(Main.GAME_W, Main.GAME_H);
        interruptContainer = new StackPane();
        interruptContainer.setPrefSize(Main.GAME_W, Main.GAME_H);
        root.getChildren().addAll(viewContainer, panelContainer, interruptContainer);

        panelStack = new Stack<IPanel>();
        
        // 添加ESC快捷键事件处理
        root.setOnKeyPressed(this::handleKeyPressed);
        root.setFocusTraversable(true);

        // 初始显示 LibraryView
        switchView(new LibraryView());
    }
    
    public static ViewManager getInstance() {
        return INSTANCE;
    }
    
    public Pane getView() {
        return root;
    }
    
    /**
     * 直接切换View（会关闭所有Panel）
     */
    public void switchView(IView newView) {
        // 关闭所有Panel
        while (!panelStack.isEmpty()) {
            closePanel();
        }
        
        // 隐藏旧View
        if (currentView != null) {
            currentView.onHide();
        }
        
        // 显示新View
        currentView = newView;
        viewContainer.getChildren().clear();
        viewContainer.getChildren().add(currentView.getRoot());
        currentView.onShow();
        
        // 确保焦点在root上以接收按键事件
        root.requestFocus();
    }
    
    /**
     * 在当前View上叠加Panel
     */
    public void showPanel(IPanel panel) {
        if (!panelStack.isEmpty()) {
            panelStack.peek().onHide(); // 隐藏当前Panel
        }
        
        panelStack.push(panel);
        panelContainer.getChildren().add(panel.getRoot());
        panel.onShow();
        
        // 确保焦点在root上以接收按键事件
        root.requestFocus();
    }
    
    /**
     * 关闭顶层Panel
     */
    public void closePanel() {
        if (panelStack.isEmpty()) {
            return;
        }
        
        IPanel topPanel = panelStack.pop();
        topPanel.onClose();
        panelContainer.getChildren().remove(topPanel.getRoot());
        
        // 显示下一个Panel（如果有）
        if (!panelStack.isEmpty()) {
            panelStack.peek().onShow();
        }
        
        // 确保焦点在root上
        root.requestFocus();
    }
    
    /**
     * 关闭所有Panel
     */
    public void closeAllPanels() {
        while (!panelStack.isEmpty()) {
            closePanel();
        }
    }
    
    /**
     * 处理按键事件
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            event.consume(); // 消费事件，防止默认行为
            
            // 如果有Panel打开，则关闭顶层Panel
            if (!panelStack.isEmpty()) {
                // 只有当顶层Panel不是ExitPanel时才关闭
                closePanel();
            } else {
                // 如果没有Panel打开，显示ExitPanel
                // com.musicgame.view.ExitPanel panel = new com.musicgame.view.ExitPanel();
                // com.musicgame.view.PausePanel panel = new com.musicgame.view.PausePanel();
                com.musicgame.view.CreatePanel panel = new com.musicgame.view.CreatePanel();
                // com.musicgame.view.EditPanel panel = new com.musicgame.view.EditPanel();
                showPanel(panel);
            }
        }
    }
    
    public IView getCurrentView() {
        return currentView;
    }
    
    public int getPanelCount() {
        return panelStack.size();
    }
}
