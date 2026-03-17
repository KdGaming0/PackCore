package com.github.kd_gaming1.packcore.gui.screen.config;

import com.daqem.uilib.gui.component.EmptyComponent;

public abstract class BaseConfigPage extends EmptyComponent {

    public BaseConfigPage(int width, int height) {
        super(0, 0, width, height);
    }

    /** Called when this page becomes visible. Build all child components here. */
    public abstract void onEnter();

    /** Called when navigating away. Override for cleanup if needed. */
    public void onExit() {}
}