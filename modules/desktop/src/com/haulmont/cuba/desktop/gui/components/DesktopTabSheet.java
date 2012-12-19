/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.desktop.DetachedFrame;
import com.haulmont.cuba.desktop.sys.ButtonTabComponent;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.ComponentVisitor;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.impl.DsContextImplementation;
import com.haulmont.cuba.gui.settings.Settings;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import org.dom4j.Element;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DesktopTabSheet
        extends DesktopAbstractComponent<JTabbedPane>
        implements TabSheet, Component.Container, AutoExpanding {
    protected Map<Component, String> components = new HashMap<Component, String>();

    protected List<TabImpl> tabs = new ArrayList<TabImpl>();

    protected Map<JComponent, TabImpl> tabContents = new LinkedHashMap<>();
    
    protected Set<LazyTabInfo> lazyTabs = new HashSet<LazyTabInfo>();

    private ComponentLoader.Context context;

    private boolean initLazyTabListenerAdded;
    private boolean postInitTaskAdded;
    private boolean componentTabChangeListenerInitialized;

    protected Set<TabChangeListener> listeners = new HashSet<TabChangeListener>();

    public DesktopTabSheet() {
        impl = new JTabbedPane();
    }

    @Override
    public void add(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Component> T getOwnComponent(String id) {
        for (TabImpl tab : tabs) {
            if (tab.getComponent() instanceof Container) {
                Component component = DesktopComponentsHelper.getComponent((Container) tab.getComponent(), id);
                if (component != null)
                    return (T) component;
            }
        }
        return null;
    }

    @Override
    public <T extends Component> T getComponent(String id) {
        return DesktopComponentsHelper.<T>getComponent(this, id);
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return components.keySet();
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    @Override
    public Tab addTab(String name, Component component) {
        final TabImpl tab = new TabImpl(name, component, false);

        tabs.add(tab);
        components.put(component, name);

        JComponent comp = DesktopComponentsHelper.getComposition(component);

        impl.addTab("", comp);
        int tabIndex = impl.indexOfComponent(comp);
        tabContents.put(comp, tab);
        setTabComponent(tab, tabIndex);
        return tab;
    }

    private void setTabComponent(final TabImpl tab, int componentIndex) {
        ButtonTabComponent.CloseListener closeListener = new ButtonTabComponent.CloseListener(){
            @Override
            public void onTabClose(int tabIndex) {
                if (tab.getCloseHandler() != null) {
                    tab.getCloseHandler().onTabClose(tab);
                } else {
                    removeTab(tab.getName());
                }
            }
        };

        ButtonTabComponent.DetachListener detachListener = new ButtonTabComponent.DetachListener() {
            @Override
            public void onDetach(int tabIndex) {
                detachTab(tabIndex);
            }
        };

        ButtonTabComponent btnTabComponent = new ButtonTabComponent(impl, false, false, closeListener, detachListener);
        tab.setButtonTabComponent(btnTabComponent);
        impl.setTabComponentAt(componentIndex, btnTabComponent);
    }

    @Override
    public Tab addLazyTab(String name, Element descriptor, ComponentLoader loader) {
        DesktopVBox tabContent = new DesktopVBox();
        tabContent.setWidth("100%");

        TabImpl tab = new TabImpl(name, tabContent, true);
        tabs.add(tab);
        components.put(tabContent, name);

        final JComponent comp = DesktopComponentsHelper.getComposition(tabContent);

        impl.addTab("", comp);
        int tabIndex = impl.indexOfComponent(comp);
        tabContents.put(comp, tab);

        setTabComponent(tab, tabIndex);
        lazyTabs.add(new LazyTabInfo(tab, tabContent, descriptor, loader));

        if (!initLazyTabListenerAdded) {
            impl.addChangeListener(
                    new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            initLazyTab((JComponent) impl.getSelectedComponent());
                        }
                    }
            );
            initLazyTabListenerAdded = true;
        }

        context = loader.getContext();

        if (!postInitTaskAdded) {
            context.addPostInitTask(new ComponentLoader.PostInitTask() {
                @Override
                public void execute(ComponentLoader.Context context, IFrame window) {
                    initComponentTabChangeListener();
                }
            });
            postInitTaskAdded = true;
        }
        return tab;
    }

    @Override
    public void removeTab(String name) {
        TabImpl tab = getTabImpl(name);
        components.remove(tab.getComponent());
        impl.remove(DesktopComponentsHelper.getComposition(tab.getComponent()));
    }

    private TabImpl getTabImpl(String name) {
        TabImpl tab = null;
        for (TabImpl t : tabs) {
            if (t.getName().equals(name)) {
                tab = t;
                break;
            }
        }
        if (tab == null)
            throw new IllegalStateException(String.format("Can't find tab '%s'", name));
        return tab;
    }

    @Override
    public Tab getTab() {
        JComponent component = (JComponent) impl.getSelectedComponent();
        if (component == null) {
            return null; // nothing selected
        }
        for (TabImpl tabImpl : tabs) {
            if (DesktopComponentsHelper.getComposition(tabImpl.getComponent()).equals(component))
                return tabImpl;
        }
        return tabs.get(0);
    }

    @Override
    public void setTab(Tab tab) {
        Component component = ((TabImpl) tab).getComponent();
        impl.setSelectedComponent(DesktopComponentsHelper.getComposition(component));
    }

    @Override
    public void setTab(String name) {
        TabImpl tab = getTabImpl(name);
        impl.setSelectedComponent(DesktopComponentsHelper.getComposition(tab.getComponent()));
    }

    @Override
    public Tab getTab(String name) {
        return getTabImpl(name);
    }

    @Override
    public Collection<Tab> getTabs() {
        return Collections.<Tab>unmodifiableCollection(tabs);
    }

    @Override
    public void addListener(TabChangeListener listener) {
        initComponentTabChangeListener();
        listeners.add(listener);
    }

    private void initComponentTabChangeListener() {
        // init component SelectedTabChangeListener only when needed, making sure it is
        // after all lazy tabs listeners
        if (!componentTabChangeListenerInitialized) {
            impl.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    // Init lazy tab if needed
                    initLazyTab((JComponent) impl.getSelectedComponent());
                    // Fire GUI listener
                    fireTabChanged();
                    // Execute outstanding post init tasks after GUI listener.
                    // We suppose that context.executePostInitTasks() executes a task once and then remove it from task list.
                    if (context != null)
                        context.executePostInitTasks();
                }
            });
            componentTabChangeListenerInitialized = true;
        }
    }

    private void initLazyTab(JComponent tab) {
        LazyTabInfo lti = null;
        for (LazyTabInfo lazyTabInfo : lazyTabs) {
            if (lazyTabInfo.getTabComponent() == tab) {
                lti = lazyTabInfo;
                break;
            }
        }
        if (lti == null) // already initialized
            return;

        if (!lti.getTab().isEnabled())
            return;

        lazyTabs.remove(lti);

        Component comp;
        try {
            comp = lti.loader.loadComponent(AppConfig.getFactory(), lti.descriptor, null);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        comp.setWidth("100%");
        lti.tabContent.add(comp);
        lti.tabContent.expand(comp, "", "");

        final Window window = ComponentsHelper.getWindow(DesktopTabSheet.this);
        if (window != null) {
            ComponentsHelper.walkComponents(
                    lti.tabContent,
                    new ComponentVisitor() {
                        @Override
                        public void visit(Component component, String name) {
                            if (component instanceof HasSettings) {
                                Settings settings = window.getSettings();
                                if (settings != null) {
                                    Element e = settings.get(name);
                                    ((HasSettings) component).applySettings(e);
                                }
                            }
                        }
                    }
            );

            ((DsContextImplementation) window.getDsContext()).resumeSuspended();
            lti.getTab().setLazyInitialized(true);
        }
    }

    @Override
    public void removeListener(TabChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireTabChanged() {
        for (TabChangeListener listener : listeners) {
            listener.tabChanged(getTab());
        }
    }

    private void updateTabVisibility(TabImpl tab) {
        // find insert/remove index by visibility of existing tabs
        int currentIndex = tab.getTabIndex();
        int idx = 0;
        for (TabImpl t : tabs) {
            if (t.equals(tab))
                break;
            if (t.isVisible())
                idx++;
        }

        JComponent comp = DesktopComponentsHelper.getComposition(tab.getComponent());
        if (currentIndex >= 0 || tab.isVisible()) {
            if (tab.isVisible()) {
                impl.insertTab(tab.getCaption(), null, comp, null, idx);
                impl.setTabComponentAt(idx, tab.getButtonTabComponent());
            } else {
                impl.removeTabAt(idx);
            }
        } else { //tab is detached
            DetachedFrame detachedFrame = (DetachedFrame) SwingUtilities.getWindowAncestor(comp);
            detachedFrame.dispose();
        }
        // if we just detach component, it will return isVisible() == true
        if (!tab.isVisible())
            comp.setVisible(false);
    }

    @Override
    public boolean expandsWidth() {
        return true;
    }

    @Override
    public boolean expandsHeight() {
        return false;
    }

    protected class TabImpl implements TabSheet.Tab {

        private String name;
        private Component component;
        private String caption;
        private boolean enabled = true;
        private boolean visible = true;
        private boolean closable;
        private boolean detachable;
        private boolean lazy;
        private boolean lazyInitialized;
        private ButtonTabComponent buttonTabComponent;


        private TabCloseHandler closeHandler;

        public TabImpl(String name, Component component, boolean lazy) {
            this.name = name;
            this.component = component;
            this.lazy = lazy;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getCaption() {
            return caption;
        }

        @Override
        public void setCaption(String caption) {
            this.caption = caption;
            DesktopTabSheet.this.impl.setTitleAt(getTabIndex(), caption);
            getButtonTabComponent().setCaption(caption);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            buttonTabComponent.setEnabled(enabled);
            int tabIndex = getTabIndex();

            if (tabIndex >= 0) {
                impl.setEnabledAt(getTabIndex(), enabled);
                impl.setComponentAt(tabIndex, enabled ? DesktopComponentsHelper.getComposition(component) : null);
                if (impl.getSelectedIndex() == tabIndex && isLazy() && enabled) {
                    initLazyTab(DesktopComponentsHelper.getComposition(component));
                }
            } else {
                component.setVisible(enabled);
            }
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible != this.visible) {
                this.visible = visible;
                DesktopTabSheet.this.updateTabVisibility(this);
            }
        }

        @Override
        public boolean isClosable() {
            return closable;
        }

        @Override
        public void setClosable(boolean closable) {
            if (closable != this.closable) {
                getButtonTabComponent().setCloseable(closable);
                this.closable = closable;
            }
        }

        @Override
        public boolean isDetachable() {
            return detachable;
        }

        @Override
        public void setDetachable(boolean detachable) {
            if (detachable != this.detachable) {
                getButtonTabComponent().setDetachable(detachable);
                this.detachable = detachable;
            }
        }

        @Override
        public TabCloseHandler getCloseHandler() {
            return closeHandler;
        }

        @Override
        public void setCloseHandler(TabCloseHandler tabCloseHandler) {
            this.closeHandler = tabCloseHandler;
        }

        public Component getComponent() {
            return component;
        }

        public int getTabIndex() {
            return DesktopTabSheet.this.impl.indexOfTabComponent(buttonTabComponent);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public void setCaptionStyleName(String styleName) {
            ButtonTabComponent buttonTabComponent = getButtonTabComponent();
            App.getInstance().getTheme().applyStyle(buttonTabComponent.getTitleLabel(), styleName);
        }

        public ButtonTabComponent getButtonTabComponent() {
            return buttonTabComponent;
        }

        public void setButtonTabComponent(ButtonTabComponent buttonTabComponent) {
            this.buttonTabComponent = buttonTabComponent;
        }

        public boolean isLazyInitialized(){
            return lazyInitialized;
        }

        public void setLazyInitialized(boolean lazyInitialized){
            this.lazyInitialized = lazyInitialized;
        }

        public boolean isLazy(){
            return lazy;
        }
    }

    private class LazyTabInfo {
        private DesktopAbstractBox tabContent;
        private Element descriptor;
        private ComponentLoader loader;
        private TabImpl tabImpl;

        private LazyTabInfo(TabImpl tabImpl, DesktopAbstractBox tabContent, Element descriptor,
                            ComponentLoader loader) {
            this.descriptor = descriptor;
            this.loader = loader;
            this.tabContent = tabContent;
            this.tabImpl = tabImpl;
        }

        private JComponent getTabComponent() {
            return DesktopComponentsHelper.getComposition(tabContent);
        }

        private TabImpl getTab(){
            return tabImpl;
        }
    }

    protected void detachTab(final int tabIndex) {
        final JComponent tabContent = (JComponent) impl.getComponentAt(tabIndex);
        TabImpl tabAtIndex = null;
        for (TabImpl tab : tabs) {
            if (DesktopComponentsHelper.getComposition(tab.getComponent()) == tabContent) {
                tabAtIndex = tab;
                if (tab.isLazy() && !tab.isLazyInitialized()) {
                    initLazyTab(tabContent);
                }
                break;
            }

        }
        final TabImpl tabToDetach = tabAtIndex;
        final ButtonTabComponent tabComponent = tabToDetach.getButtonTabComponent();
        final JFrame frame = new DetachedFrame(tabComponent.getCaption(), impl);

        frame.setLocationRelativeTo(DesktopComponentsHelper.getTopLevelFrame(this));
        impl.remove(tabContent);
        updateTabsEnabledState();
        frame.setSize(impl.getSize());
        frame.add(tabContent);

        final HierarchyListener listener = new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) == HierarchyEvent.DISPLAYABILITY_CHANGED
                        && !impl.isDisplayable()) {
                    attachTab(frame, tabToDetach);
                    impl.removeHierarchyListener(this);
                }
            }
        };

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attachTab(frame, tabToDetach);
                impl.removeHierarchyListener(listener);
            }
        });

        impl.addHierarchyListener(listener);
        frame.setVisible(true);
    }

    private void attachTab(JFrame frame, TabImpl tab) {
        int tabIndex = 0;
        int attachedBeforeCount = 0;
        JComponent tabContent = DesktopComponentsHelper.getComposition(tab.getComponent());
        String caption = tab.getCaption();
        ButtonTabComponent tabComponent = tab.getButtonTabComponent();
        for (Map.Entry<JComponent, TabImpl> entry : tabContents.entrySet()) {
            if (entry.getKey() == tabContent) {
                tabIndex = attachedBeforeCount;
                break;
            } else if (entry.getKey().getParent() == impl) {
                attachedBeforeCount++;
            }
        }

        impl.add(tabContent, tabIndex);
        if (!tab.isEnabled()) {
            impl.setComponentAt(tabIndex, null);
        }
        impl.setTitleAt(tabIndex, caption);
        impl.setTabComponentAt(tabIndex, tabComponent);
        updateTabsEnabledState();
        tabComponent.revalidate();
        tabComponent.repaint();
        frame.dispose();
    }

    private void updateTabsEnabledState() {
        for (TabImpl tab : tabs) {
            int tabIndex = tab.getTabIndex();
            if (tabIndex >= 0) {
                impl.setEnabledAt(tab.getTabIndex(), tab.isEnabled());
            }
        }
    }
}
