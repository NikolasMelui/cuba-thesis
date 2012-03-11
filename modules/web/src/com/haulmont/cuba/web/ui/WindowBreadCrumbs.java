/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 12.12.2008 13:17:46
 *
 * $Id$
 */
package com.haulmont.cuba.web.ui;

import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppWindow;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.BaseTheme;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class WindowBreadCrumbs extends HorizontalLayout {

    public interface Listener extends Serializable {
        void windowClick(Window window);
    }

    protected boolean tabbedMode;

    protected LinkedList<Window> windows = new LinkedList<Window>();

    protected HorizontalLayout logoLayout;
    protected HorizontalLayout linksLayout;
    protected Button closeBtn;

    protected Map<Button, Window> btn2win = new HashMap<Button, Window>();

    protected Set<Listener> listeners = new HashSet<Listener>();

    public WindowBreadCrumbs() {
        setMargin(true);
        setWidth(100, Sizeable.UNITS_PERCENTAGE);
        setHeight(-1, Sizeable.UNITS_PIXELS); // TODO (abramov) This is a bit tricky
        setStyleName("headline-container");

        tabbedMode = AppWindow.Mode.TABBED.equals(App.getInstance().getAppWindow().getMode());

        if (tabbedMode)
            setVisible(false);

        logoLayout = new HorizontalLayout();
        logoLayout.setMargin(true);
        logoLayout.setSpacing(true);

        linksLayout = new HorizontalLayout();
        linksLayout.setStyleName("breadcrumbs");

        if (!tabbedMode) {
            closeBtn = new Button("", new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    final Window window = getCurrentWindow();
                    window.close("close");
                }
            });
            closeBtn.setIcon(new ThemeResource("images/close.png"));
            closeBtn.setStyleName("closetab-button");
            App.getInstance().getWindowManager()
                    .setDebugId(closeBtn, "closeBtn");
        }

        HorizontalLayout enclosingLayout = new HorizontalLayout();
        enclosingLayout.addComponent(linksLayout);
        enclosingLayout.setComponentAlignment(linksLayout, Alignment.MIDDLE_LEFT);

        addComponent(logoLayout);
        setComponentAlignment(logoLayout, Alignment.MIDDLE_LEFT);
        addComponent(enclosingLayout);

        if (closeBtn != null)
            addComponent(closeBtn);

        setComponentAlignment(enclosingLayout, Alignment.MIDDLE_LEFT);
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

            btn2win.put(button, window);

            if (it.hasNext()) {
                linksLayout.addComponent(button);
                Label separatorLab = new Label("&nbsp;&gt;&nbsp;");
                separatorLab.setContentMode(Label.CONTENT_XHTML);
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
