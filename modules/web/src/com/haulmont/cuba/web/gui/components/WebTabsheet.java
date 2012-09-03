/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 02.02.2009 17:05:00
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.ComponentVisitor;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Tabsheet;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.data.impl.DsContextImplementation;
import com.haulmont.cuba.gui.settings.Settings;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.vaadin.ui.TabSheet;
import org.dom4j.Element;

import java.util.*;

public class WebTabsheet
    extends
        WebAbstractComponent<TabSheet>
    implements
        Tabsheet, Component.Wrapper, Component.Container
{
    private boolean componentTabChangeListenerInitialized;

    private ComponentLoader.Context context;
    
    private static final long serialVersionUID = -2920295325234843920L;

    public WebTabsheet() {
        component = new TabSheetEx(this);
        component.setCloseHandler(new MyCloseHandler());
    }

    protected Map<String, Tab> tabs = new HashMap<String, Tab>();

    protected Map<Component, String> components = new HashMap<Component, String>();

    protected Set<com.vaadin.ui.Component> lazyTabs = new HashSet<com.vaadin.ui.Component>();

    protected Set<TabChangeListener> listeners = new HashSet<TabChangeListener>();

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
        for (Tab tab : tabs.values()) {
            if (tab.getComponent() instanceof Container) {
                final Component component = WebComponentsHelper.getComponent((Container) tab.getComponent(), id);
                if (component != null) return (T) component;
            }
        }

        return null;
    }

    @Override
    public <T extends Component> T getComponent(String id) {
        return WebComponentsHelper.<T>getComponent(this, id);
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return components.keySet();
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    protected class Tab implements com.haulmont.cuba.gui.components.Tabsheet.Tab {

        private String name;
        private Component component;
        private TabCloseHandler closeHandler;

        public Tab(String name, Component component) {
            this.name = name;
            this.component = component;
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
            return WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component)).getCaption();
        }

        @Override
        public void setCaption(String caption) {
            WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component)).setCaption(caption);
        }

        @Override
        public boolean isEnabled() {
            return WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component)).isEnabled();
        }

        @Override
        public void setEnabled(boolean enabled) {
            WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component)).setEnabled(enabled);
        }

        @Override
        public boolean isVisible() {
            return WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component)).isVisible();
        }

        @Override
        public void setVisible(boolean visible) {
            WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component)).setVisible(visible);
        }

        @Override
        public boolean isClosable() {
            TabSheet.Tab tab = WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            return tab.isClosable();
        }

        @Override
        public void setClosable(boolean closable) {
            TabSheet.Tab tab = WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            tab.setClosable(closable);
        }

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

        @Override
        public void setCaptionStyleName(String styleName) {
            TabSheet.Tab vaadinTab = WebTabsheet.this.component.getTab(WebComponentsHelper.unwrap(component));
            vaadinTab.setCaptionStyle(styleName);
        }
    }

    @Override
    public com.haulmont.cuba.gui.components.Tabsheet.Tab addTab(String name, Component component) {
        final Tab tab = new Tab(name, component);

        this.tabs.put(name, tab);
        this.components.put(component, name);

        final com.vaadin.ui.Component tabComponent = WebComponentsHelper.unwrap(component);
        tabComponent.setSizeFull();
        
        this.component.addTab(tabComponent);

        return tab;
    }

    @Override
    public com.haulmont.cuba.gui.components.Tabsheet.Tab addLazyTab(String name,
                                                                    Element descriptor,
                                                                    ComponentLoader loader)
    {
        WebVBoxLayout tabContent = new WebVBoxLayout();
        tabContent.setSizeFull();
        
        final Tab tab = new Tab(name, tabContent);

        tabs.put(name, tab);
        components.put(tabContent, name);

        final com.vaadin.ui.Component tabComponent = WebComponentsHelper.unwrap(tabContent);
        tabComponent.setSizeFull();

        this.component.addTab(tabComponent);
        lazyTabs.add(tabComponent);

        this.component.addListener(new LazyTabChangeListener(tabContent, descriptor, loader));
        context = loader.getContext();

        return tab;
    }

    @Override
    public void removeTab(String name) {
        final Tab tab = tabs.get(name);
        if (tab == null) throw new IllegalStateException(String.format("Can't find tab '%s'", name));

        this.components.remove(tab.getComponent());
        this.component.removeComponent(WebComponentsHelper.unwrap(tab.getComponent()));
    }

    @Override
    public Tab getTab() {
        final com.vaadin.ui.Component component = this.component.getSelectedTab();
        final String name = components.get(component);
        return tabs.get(name);
    }

    @Override
    public void setTab(com.haulmont.cuba.gui.components.Tabsheet.Tab tab) {
        this.component.setSelectedTab(WebComponentsHelper.unwrap(((Tab) tab).getComponent()));
    }

    @Override
    public void setTab(String name) {
        Tab tab = tabs.get(name);
        if (tab == null) throw new IllegalStateException(String.format("Can't find tab '%s'", name));

        this.component.setSelectedTab(WebComponentsHelper.unwrap(tab.getComponent()));
    }

    @Override
    public Tabsheet.Tab getTab(String name) {
        return tabs.get(name);
    }

    @Override
    public Collection<com.haulmont.cuba.gui.components.Tabsheet.Tab> getTabs() {
        return (Collection)tabs.values();
    }

    @Override
    public void addListener(TabChangeListener listener) {
        // init component SelectedTabChangeListener only when needed, making sure it is
        // after all lazy tabs listeners
        if (!componentTabChangeListenerInitialized) {
            component.addListener(new TabSheet.SelectedTabChangeListener() {
                @Override
                public void selectedTabChange(TabSheet.SelectedTabChangeEvent event) {
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

        listeners.add(listener);
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

    private static class TabSheetEx extends TabSheet implements WebComponentEx {
        private Component component;

        private TabSheetEx(Component component) {
            this.component = component;
        }

        @Override
        public Component asComponent() {
            return component;
        }
    }

    private class LazyTabChangeListener implements TabSheet.SelectedTabChangeListener {

        private WebAbstractBox tabContent;
        private Element descriptor;
        private ComponentLoader loader;

        public LazyTabChangeListener(WebAbstractBox tabContent, Element descriptor, ComponentLoader loader) {
            this.tabContent = tabContent;
            this.descriptor = descriptor;
            this.loader = loader;
        }

        @Override
        public void selectedTabChange(TabSheet.SelectedTabChangeEvent event) {
            com.vaadin.ui.Component selectedTab = WebTabsheet.this.component.getSelectedTab();
            if (selectedTab == tabContent && lazyTabs.remove(tabContent)) {
                Component comp;
                try {
                    comp = loader.loadComponent(AppConfig.getFactory(), descriptor, null);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                tabContent.add(comp);
                com.vaadin.ui.Component impl = WebComponentsHelper.getComposition(comp);
                impl.setSizeFull();

                final Window window = com.haulmont.cuba.gui.ComponentsHelper.getWindow(WebTabsheet.this);
                if (window != null) {
                    com.haulmont.cuba.gui.ComponentsHelper.walkComponents(
                            tabContent,
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
                }
            }
        }
    }

    private class MyCloseHandler implements TabSheet.CloseHandler {
        private static final long serialVersionUID = -6766617382191585632L;

        @Override
        public void onTabClose(TabSheet tabsheet, com.vaadin.ui.Component tabContent) {
            // have no other way to get tab from tab content
            for (Tab tab: tabs.values()) {
                com.vaadin.ui.Component tabComponent = WebComponentsHelper.unwrap(tab.getComponent());
                if (tabComponent == tabContent) {
                    if (tab.isClosable()) {
                        doHandleCloseTab(tab);
                        return;
                    }
                }
            }
        }

        private void doHandleCloseTab(Tab tab) {
            if (tab.getCloseHandler() != null) {
                tab.getCloseHandler().onTabClose(tab);
            }
            else {
                removeTab(tab.getName());
            }
        }
    }
}
