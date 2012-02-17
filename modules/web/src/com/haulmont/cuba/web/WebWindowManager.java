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

import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.SilentException;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.web.gui.WebWindow;
import com.haulmont.cuba.web.gui.components.WebButton;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.ui.WindowBreadCrumbs;
import com.vaadin.event.ShortcutListener;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.*;
import com.vaadin.ui.Label;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

public class WebWindowManager extends WindowManager {

    private static final long serialVersionUID = -1212999019760516097L;

    protected static class WindowData implements Serializable {
        private static final long serialVersionUID = -3919777239558187362L;

        protected final Map<Layout, WindowBreadCrumbs> tabs = new HashMap<Layout, WindowBreadCrumbs>();
        protected final Map<WindowBreadCrumbs, Stack<Map.Entry<Window, Integer>>> stacks = new HashMap<WindowBreadCrumbs, Stack<Map.Entry<Window, Integer>>>();
        protected final Map<Window, WindowOpenMode> windowOpenMode = new LinkedHashMap<Window, WindowOpenMode>();
        protected final Map<Window, Integer> windows = new HashMap<Window, Integer>();
        protected final Map<Layout, WindowBreadCrumbs> fakeTabs = new HashMap<Layout, WindowBreadCrumbs>();
    }

    protected App app;

    protected List<ShowStartupLayoutListener> showStartupLayoutListeners = new ArrayList<ShowStartupLayoutListener>();

    protected List<CloseStartupLayoutListener> closeStartupLayoutListeners = new ArrayList<CloseStartupLayoutListener>();

    private Map<AppWindow, WindowData> appWindowMap = new HashMap<AppWindow, WindowData>();

    protected Map<String, Integer> debugIds = new HashMap<String, Integer>();

    private static Log log = LogFactory.getLog(WebWindowManager.class);

    private boolean disableSavingScreenHistory;
    private ScreenHistorySupport screenHistorySupport = new ScreenHistorySupport();

    public WebWindowManager(final App app) {
        this.app = app;
        app.getConnection().addListener(new UserSubstitutionListener() {
            public void userSubstituted(Connection connection) {
                closeStartupScreen(app.getAppWindow());
                showStartupScreen(app.getAppWindow());
            }
        });
    }

    private WindowData getCurrentWindowData() {
        WindowData data = appWindowMap.get(app.getAppWindow());
        if (data == null) {
            data = new WindowData();
            appWindowMap.put(app.getAppWindow(), data);
        }
        return data;
    }

    protected Map<Layout, WindowBreadCrumbs> getTabs() {
        return getCurrentWindowData().tabs;
    }

	protected Map<Layout, WindowBreadCrumbs> getFakeTabs() {
        return getCurrentWindowData().fakeTabs;
    }

    private Map<Window, WindowOpenMode> getWindowOpenMode() {
        return getCurrentWindowData().windowOpenMode;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Collection<Window> getOpenWindows() {
        return new ArrayList<Window>(getWindowOpenMode().keySet());
    }

    protected static class WindowOpenMode implements Serializable {

        private static final long serialVersionUID = 2475930997468013484L;

        protected Window window;
        protected OpenType openType;
        protected Object data;
        protected com.vaadin.ui.Window vaadinWindow;

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

        public Window getWindow() {
            return window;
        }

        public OpenType getOpenType() {
            return openType;
        }

        public com.vaadin.ui.Window getVaadinWindow() {
            return vaadinWindow;
        }

        public void setVaadinWindow(com.vaadin.ui.Window vaadinWindow) {
            this.vaadinWindow = vaadinWindow;
        }
    }

    public void addShowStartupLayoutListener(ShowStartupLayoutListener showStartupLayoutListener) {
        if (!showStartupLayoutListeners.contains(showStartupLayoutListener)) showStartupLayoutListeners.add(showStartupLayoutListener);
    }

    public void removeShowStartupLayoutListener(ShowStartupLayoutListener showStartupLayoutListener) {
        showStartupLayoutListeners.remove(showStartupLayoutListener);
    }

    public void removeAllShowStartupLayoutListeners() {
        showStartupLayoutListeners.clear();
    }

    protected void fireShowStartupLayoutListeners() {
        for (ShowStartupLayoutListener showStartupLayoutListener : showStartupLayoutListeners) {
            showStartupLayoutListener.onShowStartupLayout();
        }
    }

    public void addCloseStartupLayoutListener(CloseStartupLayoutListener closeStartupLayoutListener) {
        if (!closeStartupLayoutListeners.contains(closeStartupLayoutListener)) closeStartupLayoutListeners.add(closeStartupLayoutListener);
    }

    public void removeCloseStartupLayoutListener(CloseStartupLayoutListener closeStartupLayoutListener) {
        closeStartupLayoutListeners.remove(closeStartupLayoutListener);
    }

    public void removeAllCloseStartupLayoutListener() {
        closeStartupLayoutListeners.clear();
    }

    protected void fireCloseStartupLayoutListeners() {
        for (CloseStartupLayoutListener closeStartupLayoutListener : closeStartupLayoutListeners) {
            closeStartupLayoutListener.onCloseStartupLayout();
        }
    }

    protected Layout findTab(Integer hashCode) {
        Set<Map.Entry<Layout, WindowBreadCrumbs>> set = getFakeTabs().entrySet();
        for (Map.Entry<Layout, WindowBreadCrumbs> entry : set) {
            Window currentWindow = entry.getValue().getCurrentWindow();
            if (hashCode.equals(getWindowHashCode(currentWindow)))
                return entry.getKey();
        }
        set = getTabs().entrySet();
        for (Map.Entry<Layout, WindowBreadCrumbs> entry : set) {
            Window currentWindow = entry.getValue().getCurrentWindow();
            if (hashCode.equals(getWindowHashCode(currentWindow)))
                return entry.getKey();
        }
        return null;
    }

    protected Stack<Map.Entry<Window, Integer>> getStack(WindowBreadCrumbs breadCrumbs) {
        return getCurrentWindowData().stacks.get(breadCrumbs);
    }

    protected boolean hasModalWindow() {
        Set<Map.Entry<Window, WindowOpenMode>> windowOpenMode = getCurrentWindowData().windowOpenMode.entrySet();
        for (Map.Entry<Window, WindowOpenMode> openMode : windowOpenMode) {
            if (OpenType.DIALOG.equals(openMode.getValue().getOpenType()))
                return true;
        }
        return false;
    }

    protected void showWindow(final Window window, final String caption, OpenType type, boolean multipleOpen) {
        showWindow(window, caption, null, type, multipleOpen);
    }

    protected void showWindow(final Window window, final String caption, final String description, OpenType type, final boolean multipleOpen) {
        AppWindow appWindow = app.getAppWindow();
        boolean forciblyDialog = false;
        if (type != OpenType.DIALOG && hasModalWindow()) {
            type = OpenType.DIALOG;
            forciblyDialog = true;
        }
        final WindowOpenMode openMode = new WindowOpenMode(window, type);
        Component component;

        window.setCaption(caption);
        window.setDescription(description);


        switch (type) {
            case NEW_TAB:
                closeStartupScreen(appWindow);
                if (AppWindow.Mode.SINGLE.equals(appWindow.getMode())) {

                    VerticalLayout mainLayout = appWindow.getMainLayout();
                    if (mainLayout.getComponentIterator().hasNext()) {
                        Layout oldLayout = (Layout) mainLayout.getComponentIterator().next();
                        WindowBreadCrumbs oldBreadCrumbs = getTabs().get(oldLayout);
                        if (oldBreadCrumbs != null) {
                            Window oldWindow = oldBreadCrumbs.getCurrentWindow();
                            oldWindow.closeAndRun("mainMenu", new Runnable() {
                                public void run() {
                                    showWindow(window, caption, OpenType.NEW_TAB, false);
                                }
                            });
                            return;
                        }
                    }
                } else {
                    final Integer hashCode = getWindowHashCode(window);
                    Layout tab = null;
                    if (hashCode != null && !multipleOpen)
                        tab = findTab(hashCode);
                    Layout oldLayout = tab;
                    final WindowBreadCrumbs oldBreadCrumbs = getTabs().get(oldLayout);

                    if (oldBreadCrumbs != null &&
                            getCurrentWindowData().windowOpenMode.containsKey(oldBreadCrumbs.getCurrentWindow().<IFrame>getFrame()) && !multipleOpen) {
                        final Window oldWindow = oldBreadCrumbs.getCurrentWindow();
                        Layout l = new VerticalLayout();
                        appWindow.getTabSheet().replaceComponent(tab, l);
                        getCurrentWindowData().fakeTabs.put(l, oldBreadCrumbs);
                        oldWindow.closeAndRun("mainMenu", new Runnable() {
                            public void run() {
                                putToWindowMap(oldWindow, hashCode);
                                oldBreadCrumbs.addWindow(oldWindow);
                                showWindow(window, caption, description, OpenType.NEW_TAB, multipleOpen);
                            }
                        });
                        return;
                    }
                }
                component = showWindowNewTab(window, multipleOpen, caption, description, appWindow);
                break;

            case THIS_TAB:
                closeStartupScreen(appWindow);
                component = showWindowThisTab(window, caption, description, appWindow);
                break;

            case DIALOG:
                component = showWindowDialog(window, caption, description, appWindow, forciblyDialog);
                break;

            default:
                throw new UnsupportedOperationException();
        }

        openMode.setData(component);

        if (window instanceof Window.Wrapper) {
            Window wrappedWindow = ((Window.Wrapper) window).getWrappedWindow();
            getWindowOpenMode().put(wrappedWindow, openMode);
        } else {
            getWindowOpenMode().put(window, openMode);
        }

        afterShowWindow(window);
    }

    private void closeStartupScreen(AppWindow appWindow) {
        if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
            TabSheet tabSheet = appWindow.getTabSheet();
            if (tabSheet == null) {
                fireCloseStartupLayoutListeners();
                appWindow.unInitStartupLayout();
                VerticalLayout mainLayout = appWindow.getMainLayout();
                tabSheet = new AppWindow.AppTabSheet();
                tabSheet.setSizeFull();
                mainLayout.addComponent(tabSheet);
                mainLayout.setExpandRatio(tabSheet, 1);
                appWindow.setTabSheet(tabSheet);
            }
        }

        appWindow.getMainLayout().getWindow().addAction(new ShortcutListener("onEscape", com.vaadin.event.ShortcutAction.KeyCode.ESCAPE, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                AppWindow appWindow = app.getAppWindow();
                if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                    TabSheet tabSheet = appWindow.getTabSheet();
                    if (tabSheet != null) {
                        VerticalLayout layout = (VerticalLayout) tabSheet.getSelectedTab();
                        if (layout != null) {
                            WindowBreadCrumbs breadCrumbs = getTabs().get(layout);
                            if (getCurrentWindowData().stacks.get(breadCrumbs).empty()) {
                                ((AppWindow.AppTabSheet) tabSheet).closeTabAndSelectPrevious(layout);
                            } else {
                                breadCrumbs.getCurrentWindow().close("close");
                            }

                        }
                    }
                } else {
                    Iterator<WindowBreadCrumbs> it = getCurrentWindowData().tabs.values().iterator();
                    if (it.hasNext()) {
                        it.next().getCurrentWindow().close("close");
                    }
                }
            }
        });
    }

    protected Layout createNewWinLayout(Window window, Component... components) {

        final VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        if (components != null) {
            for (final Component c : components) {
                layout.addComponent(c);
            }
        }

        final Component component = WebComponentsHelper.getComposition(window);
        component.setSizeFull();
        layout.addComponent(component);
        layout.setExpandRatio(component, 1);

        return layout;
    }

    protected Component showWindowNewTab(final Window window, final boolean multipleOpen, final String caption,
                                         final String description, AppWindow appWindow) {
        final WindowBreadCrumbs breadCrumbs = createWindowBreadCrumbs();
        breadCrumbs.addListener(
                new WindowBreadCrumbs.Listener() {
                    public void windowClick(final Window window) {
                        Runnable op = new Runnable() {
                            public void run() {
                                Window currentWindow = breadCrumbs.getCurrentWindow();

                                if (currentWindow != null && window != currentWindow) {
                                    currentWindow.closeAndRun("close", this);
                                }
                            }
                        };
                        op.run();
                    }
                }
        );
        breadCrumbs.addWindow(window);

        final Layout layout = createNewTabLayout(window, multipleOpen, caption, description, appWindow, breadCrumbs);

        return layout;
    }

    protected Layout createNewTabLayout(final Window window, final boolean multipleOpen, final String caption,
                                        final String description, AppWindow appWindow, Component... components) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        if (components != null) {
            for (final Component c : components) {
                layout.addComponent(c);
            }
        }

        final Component component = WebComponentsHelper.getComposition(window);
        component.setSizeFull();
        layout.addComponent(component);
        layout.setExpandRatio(component, 1);

        if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
            TabSheet tabSheet = appWindow.getTabSheet();
            layout.setMargin(true);
            TabSheet.Tab newTab;
            Integer hashCode = getWindowHashCode(window);
            Layout tab = null;
            if (hashCode != null)
                tab = findTab(hashCode);
            if (tab != null && !multipleOpen) {
                tabSheet.replaceComponent(tab, layout);
                tabSheet.removeComponent(tab);
                getTabs().put(layout, (WindowBreadCrumbs) components[0]);
                removeFromWindowMap(getFakeTabs().get(tab).getCurrentWindow());
                getFakeTabs().remove(tab);
                newTab = tabSheet.getTab(layout);
            } else {
                newTab = tabSheet.addTab(layout);
                getTabs().put(layout, (WindowBreadCrumbs) components[0]);
            }
            newTab.setCaption(formatTabCaption(caption, description));
            //newTab.setDescription(formatTabDescription(caption, description));
            if (tabSheet instanceof AppWindow.AppTabSheet) {
                newTab.setClosable(true);
                ((AppWindow.AppTabSheet) tabSheet).setTabCloseHandler(
                        layout,
                        new AppWindow.AppTabSheet.TabCloseHandler() {
                            public void onClose(TabSheet tabSheet, Component tabContent) {
                                WindowBreadCrumbs breadCrumbs = getTabs().get(tabContent);
                                Runnable closeTask = new TabCloseTask(breadCrumbs);
                                closeTask.run();
                            }
                        });
            }
            tabSheet.setSelectedTab(layout);
        } else {
			getTabs().put(layout, (WindowBreadCrumbs) components[0]);
            layout.addStyleName("single");
            layout.setMargin(true);
            layout.setWidth("99.9%");
            layout.setHeight("99.85%");
            VerticalLayout mainLayout = appWindow.getMainLayout();
            mainLayout.removeAllComponents();
            mainLayout.addComponent(layout);
        }

        return layout;
    }

    public class TabCloseTask implements Runnable {
        private final WindowBreadCrumbs breadCrumbs;

        public TabCloseTask(WindowBreadCrumbs breadCrumbs) {
            this.breadCrumbs = breadCrumbs;
        }

        public void run() {
            Window windowToClose = breadCrumbs.getCurrentWindow();
            if (windowToClose != null) {
                windowToClose.closeAndRun("close", new TabCloseTask(breadCrumbs));
            }
        }
    }

    public void setCurrentWindowCaption(Window window, String caption, String description) {
        TabSheet tabSheet = app.getAppWindow().getTabSheet();
        if (tabSheet == null)
            return; // for SINGLE tabbing mode

        WindowOpenMode openMode = getWindowOpenMode().get(window);
        if (openMode == null || OpenType.DIALOG.equals(openMode.getOpenType()))
            return;

        com.vaadin.ui.Component tabContent = tabSheet.getSelectedTab();
        if (tabContent == null)
            return;

        TabSheet.Tab tab = tabSheet.getTab(tabContent);
        if (tab == null)
            return;

        tab.setCaption(formatTabCaption(caption, description));
    }

    protected String formatTabCaption(final String caption, final String description) {
        String s = formatTabDescription(caption, description);
        int maxLength = ConfigProvider.getConfig(WebConfig.class).getMainTabCaptionLength();
        if (s.length() > maxLength) {
            return s.substring(0, maxLength) + "...";
        } else {
            return s;
        }
    }

    protected String formatTabDescription(final String caption, final String description) {
        if (!StringUtils.isEmpty(description)) {
            return String.format("%s | %s", caption, description);
        } else {
            return caption;
        }
    }

    protected Component showWindowThisTab(final Window window, final String caption, final String description, AppWindow appWindow) {
        VerticalLayout layout;

        if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
            TabSheet tabSheet = appWindow.getTabSheet();
            layout = (VerticalLayout) tabSheet.getSelectedTab();
        } else {
            layout = (VerticalLayout) appWindow.getMainLayout().getComponentIterator().next();
        }

        final WindowBreadCrumbs breadCrumbs = getTabs().get(layout);
        if (breadCrumbs == null)
            throw new IllegalStateException("BreadCrumbs not found");

        final Window currentWindow = breadCrumbs.getCurrentWindow();

        Set<Map.Entry<Window, Integer>> set = getCurrentWindowData().windows.entrySet();
        boolean pushed = false;
        for (Map.Entry<Window, Integer> entry : set) {
            if (entry.getKey().equals(currentWindow)) {
                getCurrentWindowData().windows.remove(currentWindow);
                getStack(breadCrumbs).push(entry);
                pushed = true;
                break;
            }
        }
        if (!pushed) {
            getStack(breadCrumbs).push(new AbstractMap.SimpleEntry<Window, Integer>(currentWindow, null));
        }

        removeFromWindowMap(currentWindow);
        layout.removeComponent(WebComponentsHelper.getComposition(currentWindow));

        final Component component = WebComponentsHelper.getComposition(window);
        component.setSizeFull();
        layout.addComponent(component);
        layout.setExpandRatio(component, 1);

        breadCrumbs.addWindow(window);

        if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
            TabSheet tabSheet = appWindow.getTabSheet();
            TabSheet.Tab tab = tabSheet.getTab(layout);
            tab.setCaption(formatTabCaption(caption, description));
        } else
            appWindow.getMainLayout().requestRepaintAll();

        return layout;
    }

    protected Component showWindowDialog(final Window window, final String caption, final String description, AppWindow appWindow, boolean forciblyDialog) {
        removeWindowsWithName(window.getId());

        final com.vaadin.ui.Window win = createDialogWindow(window);
        win.setName(window.getId());
        setDebugId(win, window.getId());

        Layout layout = (Layout) WebComponentsHelper.getComposition(window);

        // surrond window layout with outer layout to prevent double painting
        VerticalLayout outerLayout = new VerticalLayout();
        outerLayout.addComponent(layout);
        outerLayout.setExpandRatio(layout, 1);

        win.setContent(outerLayout);

        win.addListener(new com.vaadin.ui.Window.CloseListener() {
            public void windowClose(com.vaadin.ui.Window.CloseEvent e) {
                window.close("close", true);
            }
        });

        com.vaadin.event.ShortcutAction exitAction =
                new com.vaadin.event.ShortcutAction(
                        "escapeAction",
                        com.vaadin.event.ShortcutAction.KeyCode.ESCAPE,
                        null);
        Map<com.vaadin.event.Action, Runnable> actions = new HashMap<com.vaadin.event.Action, Runnable>();
        actions.put(exitAction, new Runnable() {
            @Override
            public void run() {
                window.close("close", true);
            }
        });

        WebComponentsHelper.setActions(win, actions);

        final DialogParams dialogParams = getDialogParams();
        boolean dialogParamsIsNull = dialogParams.getHeight() == null && dialogParams.getWidth() == null &&
                dialogParams.getResizable() == null;

        if (forciblyDialog && dialogParamsIsNull) {
            outerLayout.setHeight(100, Sizeable.UNITS_PERCENTAGE);
            win.setWidth(800, Sizeable.UNITS_PIXELS);
            win.setHeight(500, Sizeable.UNITS_PIXELS);
            win.setResizable(true);
            window.setHeight("100%");
        } else {
            if (dialogParams.getWidth() != null)
                win.setWidth(dialogParams.getWidth().floatValue(), Sizeable.UNITS_PIXELS);
            else
                win.setWidth(600, Sizeable.UNITS_PIXELS);

            if (dialogParams.getHeight() != null) {
                win.setHeight(dialogParams.getHeight().floatValue(), Sizeable.UNITS_PIXELS);
                win.getContent().setHeight("100%");
            }

            win.setResizable(BooleanUtils.isTrue(dialogParams.getResizable()));

            dialogParams.reset();
        }
        win.setModal(true);

        App.getInstance().getAppWindow().addWindow(win);
        win.center();

        return win;
    }

    protected WindowBreadCrumbs createWindowBreadCrumbs() {
        WindowBreadCrumbs windowBreadCrumbs = new WindowBreadCrumbs();
        getCurrentWindowData().stacks.put(windowBreadCrumbs, new Stack<Map.Entry<Window, Integer>>());
        return windowBreadCrumbs;
    }

    protected com.vaadin.ui.Window createDialogWindow(Window window) {
        return new com.vaadin.ui.Window(window.getCaption());
    }

    @Override
    public void close(Window window) {
        if (window instanceof Window.Wrapper) {
            window = ((Window.Wrapper) window).getWrappedWindow();
        }

        final WindowOpenMode openMode = getWindowOpenMode().get(window);
        if (openMode == null) {
            log.warn("Problem closing window " + window + " : WindowOpenMode not found");
            return;
        }
        disableSavingScreenHistory = false;
        closeWindow(window, openMode);
        getWindowOpenMode().remove(window);
        removeFromWindowMap(openMode.getWindow());
    }

    public void checkModificationsAndCloseAll(final Runnable runIfOk, final @Nullable Runnable runIfCancel) {
        boolean modified = false;
        for (Window window : getOpenWindows()) {
            if (!disableSavingScreenHistory) {
                screenHistorySupport.saveScreenHistory(window, getWindowOpenMode().get(window).getOpenType());
            }

            if (window instanceof WrappedWindow && ((WrappedWindow) window).getWrapper() != null)
                ((WrappedWindow) window).getWrapper().saveSettings();
            else
                window.saveSettings();

            if (window.getDsContext() != null && window.getDsContext().isModified()) {
                modified = true;
            }
        }
        disableSavingScreenHistory = true;
        if (modified) {
            showOptionDialog(
                    MessageProvider.getMessage(WebWindow.class, "closeUnsaved.caption"),
                    MessageProvider.getMessage(WebWindow.class, "closeUnsaved"),
                    IFrame.MessageType.WARNING,
                    new Action[]{
                            new AbstractAction(MessageProvider.getMessage(WebWindow.class, "actions.Yes")) {
                                public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                                    if (runIfOk != null)
                                        runIfOk.run();
                                }
                                @Override
                                public String getIcon() {
                                    return "icons/ok.png";
                                }
                            },
                            new AbstractAction(MessageProvider.getMessage(WebWindow.class, "actions.No")) {
                                public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                                    if (runIfCancel != null)
                                        runIfCancel.run();
                                }
                                @Override
                                public String getIcon() {
                                    return "icons/cancel.png";
                                }
                            }
                    }
            );
        } else {
            runIfOk.run();
        }
    }

    public void closeAll() {
        List<Map.Entry<Window, WindowOpenMode>> entries = new ArrayList(getWindowOpenMode().entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Window window = entries.get(i).getKey();
            if (window instanceof WebWindow.Editor) {
                ((WebWindow.Editor)window).releaseLock();
            }
            closeWindow(window, entries.get(i).getValue());
        }
        disableSavingScreenHistory = false;
        getWindowOpenMode().clear();
        getCurrentWindowData().windows.clear();
        Collection windows = App.getInstance().getWindows();
        for (Object win : new ArrayList(windows)) {
            if (!win.equals(App.getInstance().getAppWindow())) {
                App.getInstance().removeWindow((com.vaadin.ui.Window) win);
                if (win instanceof AppWindow)
                    appWindowMap.remove(win);
            }
        }
    }

    private void closeWindow(Window window, WindowOpenMode openMode) {
        AppWindow appWindow = app.getAppWindow();

        if (!disableSavingScreenHistory) {
            screenHistorySupport.saveScreenHistory(window, openMode.getOpenType());
        }

        switch (openMode.openType) {
            case DIALOG: {
                final com.vaadin.ui.Window win = (com.vaadin.ui.Window) openMode.getData();
                App.getInstance().getAppWindow().removeWindow(win);
                fireListeners(window, getTabs().size() != 0);
                break;
            }
            case NEW_TAB: {
                final Layout layout = (Layout) openMode.getData();
                layout.removeComponent(WebComponentsHelper.getComposition(window));

                if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                    appWindow.getTabSheet().removeComponent(layout);
                } else {
                    appWindow.getMainLayout().removeComponent(layout);
                }

                WindowBreadCrumbs windowBreadCrumbs = getTabs().get(layout);
                if (windowBreadCrumbs != null) {
                    windowBreadCrumbs.clearListeners();
                    windowBreadCrumbs.removeWindow();
                }

                getTabs().remove(layout);
                getCurrentWindowData().stacks.remove(windowBreadCrumbs);
                fireListeners(window, getTabs().size() != 0);
                showStartupScreen(appWindow);
                break;
            }
            case THIS_TAB: {
                final VerticalLayout layout = (VerticalLayout) openMode.getData();

                final WindowBreadCrumbs breadCrumbs = getTabs().get(layout);
                if (breadCrumbs == null)
                    throw new IllegalStateException("Unable to close screen: breadCrumbs not found");

                breadCrumbs.removeWindow();
                Window currentWindow = breadCrumbs.getCurrentWindow();
                if (!getStack(breadCrumbs).empty()) {
                    Map.Entry<Window, Integer> entry = getStack(breadCrumbs).pop();
                    putToWindowMap(entry.getKey(), entry.getValue());
                }
                final Component component = WebComponentsHelper.getComposition(currentWindow);
                component.setSizeFull();

                layout.removeComponent(WebComponentsHelper.getComposition(window));
                layout.addComponent(component);
                layout.setExpandRatio(component, 1);

                if (AppWindow.Mode.TABBED.equals(appWindow.getMode())) {
                    TabSheet tabSheet = app.getAppWindow().getTabSheet();
                    TabSheet.Tab tab = tabSheet.getTab(layout);
                    tab.setCaption(formatTabCaption(currentWindow.getCaption(), currentWindow.getDescription()));
                }
                fireListeners(window, getTabs().size() != 0);
                showStartupScreen(appWindow);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    public void showFrame(com.haulmont.cuba.gui.components.Component parent, IFrame frame) {
        if (parent instanceof com.haulmont.cuba.gui.components.Component.Container) {
            com.haulmont.cuba.gui.components.Component.Container container =
                    (com.haulmont.cuba.gui.components.Component.Container) parent;
            for (com.haulmont.cuba.gui.components.Component c : container.getComponents()) {
                if (c instanceof com.haulmont.cuba.gui.components.Component.Disposable) {
                    com.haulmont.cuba.gui.components.Component.Disposable disposable =
                            (com.haulmont.cuba.gui.components.Component.Disposable) c;
                    if (!disposable.isDisposed()) {
                        disposable.dispose();
                    }
                }
                container.remove(c);
            }
            container.add(frame);
        } else {
            throw new IllegalStateException(
                    "Parent component must be com.haulmont.cuba.gui.components.Component.Container"
            );
        }
    }

//    @Override
//    protected void initCompanion(Element companionsElem, AbstractWindow window) {
//        Element element = companionsElem.element(AppConfig.getClientType().toString().toLowerCase());
//        if (element != null) {
//            String className = element.attributeValue("class");
//            if (!StringUtils.isBlank(className)) {
//                Class aClass = ScriptingProvider.loadClass(className);
//                Object companion;
//                try {
//                    if (AbstractCompanion.class.isAssignableFrom(aClass)) {
//                        Constructor constructor = aClass.getConstructor(new Class[]{AbstractFrame.class});
//                        companion = constructor.newInstance(window);
//                    } else {
//                        companion = aClass.newInstance();
//                        window.setCompanion(companion);
//                    }
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }

    private void showStartupScreen(AppWindow appWindow) {
        if (getTabs().size() == 0) {
            appWindow.getMainLayout().removeAllComponents();
            appWindow.setTabSheet(null);
            appWindow.initStartupLayout();
            fireShowStartupLayoutListeners();
            appWindow.focus();
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void showNotification(String caption, IFrame.NotificationType type) {
        app.getAppWindow().showNotification(caption, WebComponentsHelper.convertNotificationType(type));
    }

    @Override
    public void showNotification(String caption, String description, IFrame.NotificationType type) {
        com.vaadin.ui.Window.Notification notify =
                new com.vaadin.ui.Window.Notification(caption, description, WebComponentsHelper.convertNotificationType(type));
        if(type.equals(IFrame.NotificationType.HUMANIZED))
            notify.setDelayMsec(3000);
        app.getAppWindow().showNotification(notify);
    }

    @Override
    public void showMessageDialog(
            String title,
            String message,
            IFrame.MessageType messageType
    ) {
        removeWindowsWithName("cuba-message-dialog");

        final com.vaadin.ui.Window window = new com.vaadin.ui.Window(title);
        window.setName("cuba-message-dialog");
        setDebugId(window, "cuba-message-dialog");

        window.addListener(new com.vaadin.ui.Window.CloseListener() {
            public void windowClose(com.vaadin.ui.Window.CloseEvent e) {
                App.getInstance().getAppWindow().removeWindow(window);
            }
        });

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        window.setContent(layout);

        Label desc = new Label(message, Label.CONTENT_XHTML);
        layout.addComponent(desc);

        float width;
        if (getDialogParams().getWidth() != null) {
            width = getDialogParams().getWidth().floatValue();
        } else {
            width = 400;
        }
        getDialogParams().reset();

        window.setWidth(width, Sizeable.UNITS_PIXELS);
        window.setResizable(false);
        window.setModal(true);

        App.getInstance().getAppWindow().addWindow(window);
        window.center();
    }

    @Override
    public void showOptionDialog(
            String title,
            String message,
            IFrame.MessageType messageType,
            Action[] actions
    ) {
        removeWindowsWithName("cuba-option-dialog");

        final com.vaadin.ui.Window window = new com.vaadin.ui.Window(title);
        window.setName("cuba-option-dialog");
        setDebugId(window, "cuba-option-dialog");
        window.setClosable(false);

        window.addListener(new com.vaadin.ui.Window.CloseListener() {
            public void windowClose(com.vaadin.ui.Window.CloseEvent e) {
                app.getAppWindow().removeWindow(window);
            }
        });

        Label messageBox = new Label(message, Label.CONTENT_XHTML);

        float width;
        if (getDialogParams().getWidth() != null) {
            width = getDialogParams().getWidth().floatValue();
        } else {
            width = 400;
        }
        getDialogParams().reset();

        window.setWidth(width, Sizeable.UNITS_PIXELS);
        window.setResizable(false);
        window.setModal(true);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        window.setContent(layout);

        HorizontalLayout actionsBar = new HorizontalLayout();
        actionsBar.setHeight(-1, Sizeable.UNITS_PIXELS);

        HorizontalLayout buttonsContainer = new HorizontalLayout();
        buttonsContainer.setSpacing(true);

        for (final Action action : actions) {
            final Button button = WebComponentsHelper.createButton();
            button.setCaption(action.getCaption());
            button.addListener(new Button.ClickListener() {
                public void buttonClick(Button.ClickEvent event) {
                    action.actionPerform(null);
                    AppWindow appWindow = app.getAppWindow();
                    if (appWindow != null) // possible appWindow is null after logout
                        appWindow.removeWindow(window);
                }
            });

            if (action instanceof DialogAction) {
                switch (((DialogAction) action).getType()) {
                    case OK:
                    case YES:
                        button.setClickShortcut(ShortcutAction.Key.ENTER.getCode(), ShortcutAction.Modifier.CTRL.getCode());
                        break;
                    case NO:
                    case CANCEL:
                    case CLOSE:
                        button.setClickShortcut(ShortcutAction.Key.ESCAPE.getCode());
                        break;
                }
            }

            if (action.getIcon() != null) {
                button.setIcon(new ThemeResource(action.getIcon()));
                button.addStyleName(WebButton.ICON_STYLE);
            }
            setDebugId(button, action.getId());
            buttonsContainer.addComponent(button);
        }
        if (buttonsContainer.getComponentCount() > 0)
            ((Button) buttonsContainer.getComponent(0)).focus();

        actionsBar.addComponent(buttonsContainer);

        layout.addComponent(messageBox);
        layout.addComponent(actionsBar);

        messageBox.setSizeFull();
        layout.setExpandRatio(messageBox, 1);
        layout.setComponentAlignment(actionsBar, com.vaadin.ui.Alignment.BOTTOM_RIGHT);

        App.getInstance().getAppWindow().addWindow(window);
        window.center();
    }

    private void removeWindowsWithName(String name) {
        final com.vaadin.ui.Window mainWindow = app.getAppWindow();

        for (com.vaadin.ui.Window childWindow : new ArrayList<com.vaadin.ui.Window>(mainWindow.getChildWindows())) {
            if (name.equals(childWindow.getName())) {
                String msg = new StrBuilder("Another " + name + " window exists, removing it\n")
                        //.appendWithSeparators(Thread.currentThread().getStackTrace(), "\n")
                        .toString();
                log.warn(msg);
                mainWindow.removeWindow(childWindow);
                Set<Map.Entry<Window, WindowOpenMode>> openModeSet = getWindowOpenMode().entrySet();
                for (Map.Entry<Window, WindowOpenMode> entry : openModeSet) {
                    WindowOpenMode openMode = entry.getValue();
                    if (ObjectUtils.equals(openMode.data, childWindow)) {
                        getWindowOpenMode().remove(entry.getKey());
                        return;
                    }
                }
            }
        }
    }

    public void reloadBreadCrumbs() {
        Layout layout;

        final AppWindow appWindow = App.getInstance().getAppWindow();
        final AppWindow.Mode viewMode = appWindow.getMode();

        if (viewMode == AppWindow.Mode.SINGLE) {
            final Layout mainLayout = appWindow.getMainLayout();
            layout = (Layout) mainLayout.getComponentIterator().next();
        } else {
            layout = (Layout) appWindow.getTabSheet().getSelectedTab();
        }

        if (layout != null) {
            WindowBreadCrumbs breadCrumbs = getTabs().get(layout);
            if (breadCrumbs != null) {
                breadCrumbs.update();
            }
        }
    }

    @Override
    protected void initDebugIds(final Window window) {
        if (app.isTestModeRequest()) {
            com.haulmont.cuba.gui.ComponentsHelper.walkComponents(window, new ComponentVisitor() {
                public void visit(com.haulmont.cuba.gui.components.Component component, String name) {
                    final String id = window.getId() + "." + name;
                    if (ConfigProvider.getConfig(WebConfig.class).getAllowIdSuffix()) {
                        component.setDebugId(generateDebugId(id));
                    } else {
                        if (component.getId() != null) {
                            component.setDebugId(id);
                        } else {
                            component.setDebugId(generateDebugId(id));
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void putToWindowMap(Window window, Integer hashCode) {
        if (window != null) {
            getCurrentWindowData().windows.put(window, hashCode);
        }
    }

    protected void removeFromWindowMap(Window window) {
        getCurrentWindowData().windows.remove(window);
    }

    private Integer getWindowHashCode(Window window){
       return getCurrentWindowData().windows.get(window);
    }

    @Override
    protected Window getWindow(Integer hashCode) {
        if (AppWindow.Mode.SINGLE.equals(app.getAppWindow().getMode()))
            return null;
        Set<Map.Entry<Window, Integer>> set = getCurrentWindowData().windows.entrySet();
        for (Map.Entry<Window, Integer> entry : set) {
            if (hashCode.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    protected void checkCanOpenWindow(WindowInfo windowInfo, OpenType openType, Map<String, Object> params) {
        if (OpenType.NEW_TAB.equals(openType)) {
            if (!windowInfo.getMultipleOpen() && getWindow(getHash(windowInfo, params)) != null) {
                //window already opened
            } else {
                int maxCount = ConfigProvider.getConfig(WebConfig.class).getMaxTabCount();
                if (maxCount > 0 && maxCount <= getCurrentWindowData().tabs.size()) {
                    app.getAppWindow().showNotification(
                            MessageProvider.formatMessage(AppConfig.getMessagesPack(), "tooManyOpenTabs.message", maxCount),
                            com.vaadin.ui.Window.Notification.TYPE_WARNING_MESSAGE);
                    throw new SilentException();
                }
            }
        }
    }

    public void setDebugId(Component component, String id) {
        if (app.isTestModeRequest()) {
            if (ConfigProvider.getConfig(WebConfig.class).getAllowIdSuffix()) {
                component.setDebugId(generateDebugId(id));
            } else {
                component.setDebugId(id);
            }
        }
    }

    private String generateDebugId(String id) {
        Integer count = debugIds.get(id);
        if (count == null) {
            count = 0;
        }
        debugIds.put(id, ++count);
        return id + "." + count;
    }

    public void reset() {
        appWindowMap.clear();
    }

    public interface ShowStartupLayoutListener extends Serializable {
        void onShowStartupLayout();
    }

    public interface CloseStartupLayoutListener extends Serializable {
        void onCloseStartupLayout();
    }
}