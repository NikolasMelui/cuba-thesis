/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 11.12.2008 17:33:12
 *
 * $Id$
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.data.DataService;
import com.haulmont.cuba.gui.data.impl.GenericDataService;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.web.gui.components.ComponentsHelper;
import com.haulmont.cuba.web.ui.WindowBreadCrumbs;
import com.haulmont.cuba.web.xml.layout.WebComponentsFactory;
import com.itmill.toolkit.terminal.ExternalResource;
import com.itmill.toolkit.terminal.Sizeable;
import com.itmill.toolkit.ui.*;

import java.util.*;

public class WindowManager extends com.haulmont.cuba.gui.WindowManager
{
    private App app;

    private Map<Layout, WindowBreadCrumbs> tabs = new HashMap<Layout, WindowBreadCrumbs>();

    public WindowManager(App app) {
        this.app = app;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected DataService createDefaultDataService() {
        return new GenericDataService(false);
    }

    protected Map<Window, WindowOpenMode> windowOpenMode = new LinkedHashMap<Window, WindowOpenMode>();

    protected static class WindowOpenMode {
        protected Window window;
        protected OpenType openType;
        protected Object data;

        public WindowOpenMode(Window window, OpenType openType) {
            this.window = window;
            this.openType = openType;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    public void showWindow(Window window, String caption, OpenType type) {
        AppWindow appWindow = app.getAppWindow();
        final WindowOpenMode openMode = new WindowOpenMode(window, type);

        if (OpenType.NEW_TAB.equals(type)) {
            if (AppWindow.Mode.SINGLE.equals(appWindow.getMode())) {
                VerticalLayout mainLayout = appWindow.getMainLayout();
                if (mainLayout.getComponentIterator().hasNext()) {
                    Layout oldLayout = (Layout) mainLayout.getComponentIterator().next();
                    WindowBreadCrumbs oldBreadCrumbs = tabs.get(oldLayout);
                    if (oldBreadCrumbs == null)
                        throw new IllegalStateException("BreadCrumbs not found");
                    Window oldWindow = oldBreadCrumbs.getCurrentWindow();
                    com.haulmont.cuba.web.gui.Window webWindow;
                    if (oldWindow instanceof Window.Wrapper) {
                        webWindow = ((Window.Wrapper) oldWindow).getWrappedWindow();
                    } else {
                        webWindow = (com.haulmont.cuba.web.gui.Window) oldWindow;
                    }
                    webWindow.replace("replacingWindow", window, caption);
                    return;
                }
            }

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();

            final WindowBreadCrumbs breadCrumbs = createWindowBreadCrumbs();
            breadCrumbs.addListener(
                    new WindowBreadCrumbs.Listener()
                    {
                        public void windowClick(Window window) {
                            Window currentWindow = breadCrumbs.getCurrentWindow();
                            while (currentWindow != null && window != currentWindow) {
                                close(currentWindow);
                                currentWindow = breadCrumbs.getCurrentWindow();
                            }
                        }
                    }
            );
            breadCrumbs.addWindow(window);

            final Component component = ComponentsHelper.unwrap(window);
            component.setSizeFull();

            layout.addComponent(breadCrumbs);
            layout.addComponent(component);
            layout.setExpandRatio(component, 1);

            if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                TabSheet tabSheet = appWindow.getTabSheet();
                tabSheet.addTab(layout, caption, null);
                tabSheet.setSelectedTab(layout);
            } else {
                VerticalLayout mainLayout = appWindow.getMainLayout();
                mainLayout.removeAllComponents();
                mainLayout.addComponent(layout);
            }
            tabs.put(layout, breadCrumbs);

            openMode.setData(layout);

        } else if (OpenType.THIS_TAB.equals(type)) {
            VerticalLayout layout;

            if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                TabSheet tabSheet = appWindow.getTabSheet();
                layout = (VerticalLayout) tabSheet.getSelectedTab();
            } else {
                layout = (VerticalLayout) appWindow.getMainLayout().getComponentIterator().next();
            }

            final WindowBreadCrumbs breadCrumbs = tabs.get(layout);
            if (breadCrumbs == null)
                throw new IllegalStateException("BreadCrumbs not found");

            final Window currentWindow = breadCrumbs.getCurrentWindow();
            layout.removeComponent(ComponentsHelper.unwrap(currentWindow));

            final Component component = ComponentsHelper.unwrap(window);
            layout.addComponent(component);
            component.setSizeFull();
            layout.setExpandRatio(component, 1);

            breadCrumbs.addWindow(window);

            openMode.setData(layout);

            if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                TabSheet tabSheet = appWindow.getTabSheet();
                tabSheet.setTabCaption(layout, caption);
                tabSheet.requestRepaintAll();
            } else {
                appWindow.getMainLayout().requestRepaintAll();
            }

        } else if (OpenType.DIALOG.equals(type)) {
            final com.itmill.toolkit.ui.Window win = createDialogWindow(window);

            win.setLayout((Layout) ComponentsHelper.unwrap(window));

            win.setWidth(600, Sizeable.UNITS_PIXELS);
            win.setResizable(false);
            win.setModal(true);

            App.getInstance().getMainWindow().addWindow(win);

            openMode.setData(win);

        } else {
            throw new UnsupportedOperationException();
        }

        if (window instanceof Window.Wrapper) {
            window = ((Window.Wrapper) window).getWrappedWindow();
            windowOpenMode.put(window, openMode);
        } else {
            windowOpenMode.put(window, openMode);
        }
    }

    protected WindowBreadCrumbs createWindowBreadCrumbs() {
        return new WindowBreadCrumbs();
    }

    protected com.itmill.toolkit.ui.Window createDialogWindow(Window window) {
        return new com.itmill.toolkit.ui.Window(window.getCaption());
    }

    protected Locale getLocale() {
        return App.getInstance().getLocale();
    }

    public void close(com.haulmont.cuba.gui.components.Window window) {
        if (window instanceof Window.Wrapper) {
            window = ((Window.Wrapper) window).getWrappedWindow();
        }

        final WindowOpenMode openMode = windowOpenMode.get(window);
        if (openMode == null) throw new IllegalStateException();

        boolean needRefresh = closeWindow(window, openMode);
        windowOpenMode.remove(window);

        // TODO (krivopustov) fix TabSheet repaint
        if (needRefresh)
            app.getMainWindow().open(new ExternalResource(app.getURL()));
    }

    public void closeAll() {
        boolean needRefresh = false;
        List<Map.Entry<Window,WindowOpenMode>> entries = new ArrayList(windowOpenMode.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            boolean res = closeWindow(entries.get(i).getKey(), entries.get(i).getValue());
            needRefresh = needRefresh || res;
        }
        windowOpenMode.clear();
        // TODO (krivopustov) fix TabSheet repaint
        if (needRefresh)
            app.getMainWindow().open(new ExternalResource(app.getURL()));
    }

    private boolean closeWindow(Window window, WindowOpenMode openMode) {
        AppWindow appWindow = app.getAppWindow();
        switch (openMode.openType) {
            case DIALOG: {
                final com.itmill.toolkit.ui.Window win = (com.itmill.toolkit.ui.Window) openMode.getData();
                App.getInstance().getMainWindow().removeWindow(win);
                return false;
            }
            case NEW_TAB: {
                final Layout layout = (Layout) openMode.getData();
                layout.removeComponent(ComponentsHelper.unwrap(window));

                if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                    appWindow.getTabSheet().removeComponent(layout);
                } else {
                    appWindow.getMainLayout().removeComponent(layout);
                }

                WindowBreadCrumbs windowBreadCrumbs = tabs.get(layout);
                if (windowBreadCrumbs != null)
                    windowBreadCrumbs.clearListeners();

                tabs.remove(layout);

                return AppWindow.Mode.TABBED.equals(appWindow.getMode());
            }
            case THIS_TAB: {
                final VerticalLayout layout = (VerticalLayout) openMode.getData();

                final WindowBreadCrumbs breadCrumbs = tabs.get(layout);
                if (breadCrumbs == null) throw new IllegalStateException("Unable to close screen: breadCrumbs not found");

                breadCrumbs.removeWindow();
                Window currentWindow = breadCrumbs.getCurrentWindow();

                final Component component = ComponentsHelper.unwrap(currentWindow);
                component.setSizeFull();

                layout.removeComponent(ComponentsHelper.unwrap(window));
                layout.addComponent(component);
                layout.setExpandRatio(component, 1);

                if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                    TabSheet tabSheet = app.getAppWindow().getTabSheet();
                    tabSheet.setTabCaption(layout, currentWindow.getCaption());
                    tabSheet.requestRepaintAll();
                }

                return false;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    protected ComponentsFactory createComponentFactory() {
        return new WebComponentsFactory();
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void showMessageDialog(String title, String message, IFrame.MessageType messageType) {
        final com.itmill.toolkit.ui.Window window = new com.itmill.toolkit.ui.Window(title);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        window.setLayout(layout);

        Label desc = new Label(message, Label.CONTENT_XHTML);
        layout.addComponent(desc);

        window.addComponent(layout);

        window.setWidth(400, Sizeable.UNITS_PIXELS);
        window.setResizable(false);
        window.setModal(true);

        App.getInstance().getMainWindow().addWindow(window);
    }

    public void showOptionDialog(String title, String message, IFrame.MessageType messageType, Action[] actions) {
        final com.itmill.toolkit.ui.Window window = new com.itmill.toolkit.ui.Window(title);

        Label messageBox = new Label(message, Label.CONTENT_XHTML);

        window.setWidth(400, Sizeable.UNITS_PIXELS);
        window.setResizable(false);
        window.setModal(true);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        window.setLayout(layout);

        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setHeight(-1, Sizeable.UNITS_PIXELS);

        HorizontalLayout buttonsContainer = new HorizontalLayout();

        for (final Action action : actions) {
            buttonsContainer.addComponent(new Button(action.getCaption(), new Button.ClickListener() {
                public void buttonClick(Button.ClickEvent event) {
                    action.actionPerform(null);
                    App.getInstance().getMainWindow().removeWindow(window);
                }
            }));
        }

        actionsBar.addComponent(buttonsContainer);

        layout.addComponent(messageBox);
        layout.addComponent(actionsBar);

        messageBox.setSizeFull();
        layout.setExpandRatio(messageBox, 1);
        layout.setComponentAlignment(actionsBar, com.itmill.toolkit.ui.Alignment.BOTTOM_RIGHT);

        App.getInstance().getMainWindow().addWindow(window);
        window.center();
    }
}
