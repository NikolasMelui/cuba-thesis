/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.web.toolkit.VersionedThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.BaseTheme;

import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class WindowBreadCrumbs extends HorizontalLayout {

    public interface Listener {
        void windowClick(Window window);
    }

    protected boolean tabbedMode;

    protected LinkedList<Window> windows = new LinkedList<>();

    protected HorizontalLayout logoLayout;
    protected HorizontalLayout linksLayout;
    protected Button closeBtn;

    protected Map<Button, Window> btn2win = new HashMap<>();

    protected Set<Listener> listeners = new HashSet<>();

    public WindowBreadCrumbs() {
        setWidth(100, Unit.PERCENTAGE);
        setHeight(-1, Unit.PIXELS); // TODO (abramov) This is a bit tricky
        setStyleName("cuba-headline-container");

        tabbedMode = AppWindow.Mode.TABBED.equals(App.getInstance().getAppWindow().getMode());

        if (tabbedMode)
            setVisible(false);

        logoLayout = new HorizontalLayout();
        logoLayout.setMargin(true);
        logoLayout.setSpacing(true);

        linksLayout = new HorizontalLayout();
        linksLayout.setStyleName("cuba-breadcrumbs");

        if (!tabbedMode) {
            closeBtn = new Button("", new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    final Window window = getCurrentWindow();
                    window.close(Window.CLOSE_ACTION_ID);
                }
            });
            closeBtn.setIcon(new VersionedThemeResource("icons/close.png"));
            closeBtn.setStyleName("cuba-closetab-button");
//            vaadin7 Test ids
//            AppUI.getInstance().getWindowManager()
//                    .setDebugId(closeBtn, "closeBtn");
        }

        HorizontalLayout enclosingLayout = new HorizontalLayout();
        enclosingLayout.addComponent(linksLayout);
        enclosingLayout.setComponentAlignment(linksLayout, Alignment.MIDDLE_LEFT);

        addComponent(logoLayout);
        addComponent(enclosingLayout);

        if (closeBtn != null)
            addComponent(closeBtn);

        linksLayout.setSizeFull();
        setExpandRatio(enclosingLayout, 1);
    }

    public Window getCurrentWindow() {
        if (windows.isEmpty())
            return null;
        else
            return windows.getLast();
    }

    public void addWindow(Window window) {
        windows.add(window);
        update();
        if (windows.size() > 1 && tabbedMode)
            setVisible(true);
    }

    public void removeWindow() {
        if (!windows.isEmpty()) {
            windows.removeLast();
            update();
        }
        if (windows.size() <= 1 && tabbedMode)
            setVisible(false);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    private void fireListeners(Window window) {
        for (Listener listener : listeners) {
            listener.windowClick(window);
        }
    }

    protected void setSubTitleContents() {

    }

    public void update() {
        linksLayout.removeAllComponents();
        btn2win.clear();
        for (Iterator<Window> it = windows.iterator(); it.hasNext();) {
            Window window = it.next();
            Button button = new Button(window.getCaption().trim(), new BtnClickListener());
            button.setStyleName(BaseTheme.BUTTON_LINK);
            button.setTabIndex(-1);

            btn2win.put(button, window);

            if (it.hasNext()) {
                linksLayout.addComponent(button);
                Label separatorLab = new Label("&nbsp;&gt;&nbsp;");
                separatorLab.setContentMode(ContentMode.HTML);
                linksLayout.addComponent(separatorLab);
            } else {
                linksLayout.addComponent(new Label(window.getCaption()));
            }
        }
    }

    public class BtnClickListener implements Button.ClickListener {
        @Override
        public void buttonClick(Button.ClickEvent event) {
            Window win = btn2win.get(event.getButton());
            if (win != null)
                fireListeners(win);
        }
    }
}