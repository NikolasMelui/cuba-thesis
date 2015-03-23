/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.components.Tree;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.settings.Settings;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.WebWindowManager;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.gui.components.WebFrameActionsHolder;
import com.haulmont.cuba.web.toolkit.VersionedThemeResource;
import com.haulmont.cuba.web.toolkit.ui.VerticalActionsLayout;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.TabSheet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.haulmont.cuba.web.gui.components.WebComponentsHelper.convertAlignment;

/**
 * @author krivopustov
 * @version $Id$
 */
public class WebWindow implements Window, Component.Wrapper, Component.HasXmlDescriptor, WrappedWindow,
                                  Component.SecuredActionsHolder {

    protected Log log = LogFactory.getLog(getClass());

    protected String id;
    protected String debugId;

    protected Map<String, Component> componentByIds = new HashMap<>();
    protected Collection<Component> ownComponents = new LinkedHashSet<>();
    protected Map<String, Component> allComponents = new HashMap<>();

    protected String messagePack;

    protected String focusComponentId;

    protected com.vaadin.ui.Component component;

    protected Element element;

    protected DsContext dsContext;
    protected WindowContext context;

    protected String caption;
    protected String description;

    protected List<CloseListener> listeners = new ArrayList<>();

    protected boolean forceClose;
    protected boolean closing = false;

    protected Runnable doAfterClose;

    protected WebWindowManager windowManager;

    protected WindowDelegate delegate;

    protected WebFrameActionsHolder actionsHolder = new WebFrameActionsHolder();
    protected final ActionsPermissions actionsPermissions = new ActionsPermissions(this);

    protected Configuration configuration = AppBeans.get(Configuration.NAME);
    protected Messages messages = AppBeans.get(Messages.NAME);

    public WebWindow() {
        component = createLayout();
        delegate = createDelegate();
        ((com.vaadin.event.Action.Container) component).addActionHandler(new com.vaadin.event.Action.Handler() {
            @Override
            public com.vaadin.event.Action[] getActions(Object target, Object sender) {
                return actionsHolder.getActionImplementations();
            }

            @Override
            public void handleAction(com.vaadin.event.Action actionImpl, Object sender, Object target) {
                Action action = actionsHolder.getAction(actionImpl);
                if (action != null && action.isEnabled() && action.isVisible()) {
                    action.actionPerform(WebWindow.this);
                }
            }
        });
    }

    protected WindowDelegate createDelegate() {
        return new WindowDelegate(this);
    }

    protected com.vaadin.ui.Component createLayout() {
        VerticalActionsLayout layout = new VerticalActionsLayout();
        layout.setSizeFull();
        return layout;
    }

    protected ComponentContainer getContainer() {
        return (ComponentContainer) component;
    }

    @Override
    public String getMessagesPack() {
        return messagePack;
    }

    @Override
    public void setMessagesPack(String name) {
        messagePack = name;
    }

    @Override
    public void registerComponent(Component component) {
        if (component.getId() != null)
            allComponents.put(component.getId(), component);
    }

    @Nullable
    @Override
    public Component getRegisteredComponent(String id) {
        return allComponents.get(id);
    }

    @Override
    public String getStyleName() {
        return component.getStyleName();
    }

    @Override
    public void setStyleName(String name) {
        component.setStyleName(name);
    }

    @Override
    public void setSpacing(boolean enabled) {
        if (component instanceof Layout.SpacingHandler) {
            ((Layout.SpacingHandler) component).setSpacing(true);
        }
    }

    @Override
    public void setMargin(boolean enable) {
        if (component instanceof Layout.MarginHandler) {
            ((Layout.MarginHandler) component).setMargin(new Layout.MarginInfo(enable));
        }
    }

    @Override
    public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
        if (component instanceof Layout.MarginHandler) {
            ((Layout.MarginHandler) component).setMargin(new Layout.MarginInfo(topEnable, rightEnable, bottomEnable, leftEnable));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void addAction(final com.haulmont.cuba.gui.components.Action action) {
        actionsHolder.addAction(action);
        actionsPermissions.apply(action);
    }

    @Override
    public void removeAction(com.haulmont.cuba.gui.components.Action action) {
        actionsHolder.removeAction(action);
    }

    @Override
    public void removeAction(String id) {
        actionsHolder.removeAction(id);
    }

    @Override
    public void removeAllActions() {
        actionsHolder.removeAllActions();
    }

    @Override
    public Collection<com.haulmont.cuba.gui.components.Action> getActions() {
        return actionsHolder.getActions();
    }

    @Override
    public com.haulmont.cuba.gui.components.Action getAction(String id) {
        return actionsHolder.getAction(id);
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public boolean validate(List<Validatable> fields) {
        ValidationErrors errors = new ValidationErrors();

        for (Validatable field : fields) {
            try {
                field.validate();
            } catch (ValidationException e) {
                if (log.isTraceEnabled())
                    log.trace("Validation failed", e);
                else if (log.isDebugEnabled())
                    log.debug("Validation failed: " + e);

                ComponentsHelper.fillErrorMessages(field, e, errors);
            }
        }

        return handleValidationErrors(errors);
    }

    @Override
    public void validate() throws ValidationException {
        delegate.validate();
    }

    @Override
    public boolean validateAll() {
        ValidationErrors errors = new ValidationErrors();

        Collection<Component> components = ComponentsHelper.getComponents(this);
        for (Component component : components) {
            if (component instanceof Validatable) {
                try {
                    ((Validatable) component).validate();
                } catch (ValidationException e) {
                    if (log.isTraceEnabled())
                        log.trace("Validation failed", e);
                    else if (log.isDebugEnabled())
                        log.debug("Validation failed: " + e);

                    ComponentsHelper.fillErrorMessages((Validatable) component, e, errors);
                }
            }
        }

//                    // TODO validate table columns - smthng like this:
//                    if (impl instanceof com.vaadin.ui.Table) {
//                        Set visibleComponents = ((Table) impl).getVisibleComponents();
//                        for (Object visibleComponent : visibleComponents) {
//                            if (visibleComponent instanceof com.vaadin.ui.Field
//                                    && ((com.vaadin.ui.Field) visibleComponent).isEnabled() &&
//                                    !((com.vaadin.ui.Field) visibleComponent).isReadOnly()) {
//                                try {
//                                    ((com.vaadin.ui.Field) visibleComponent).validate();
//                                } catch (Validator.InvalidValueException e) {
//                                    problems.put(e, ((com.vaadin.ui.Field) visibleComponent));
//                                }
//                            }
//                        }
//                    }
//
//                }
//            });
        return handleValidationErrors(errors);
    }

    protected boolean handleValidationErrors(ValidationErrors errors) {
        delegate.postValidate(errors);

        if (errors.isEmpty())
            return true;

        showValidationErrors(errors);

        focusProblemComponent(errors);

        return false;
    }

    protected void showValidationErrors(ValidationErrors errors) {
        StringBuilder buffer = new StringBuilder();
        for (ValidationErrors.Item error : errors.getAll()) {
            buffer.append(error.description).append("\n");
        }

        showNotification(messages.getMessage(WebWindow.class, "validationFail.caption"),
                buffer.toString(), NotificationType.TRAY);
    }

    protected void focusProblemComponent(ValidationErrors errors) {
        Component component = null;
        if (!errors.getAll().isEmpty()) {
            component = errors.getAll().iterator().next().component;
        }

        if (component != null) {
            try {
                com.vaadin.ui.Component vComponent = WebComponentsHelper.unwrap(component);
                com.vaadin.ui.Component c = vComponent;
                com.vaadin.ui.Component prevC = null;
                while (c != null) {
                    if (c instanceof TabSheet && !((TabSheet) c).getSelectedTab().equals(prevC)) {
                        ((TabSheet) c).setSelectedTab(prevC);
                        break;
                    }
                    prevC = c;
                    c = c.getParent();
                }

                // focus first up component
                c = vComponent;
                while (c != null) {
                    if (c instanceof com.vaadin.ui.Component.Focusable) {
                        ((com.vaadin.ui.Component.Focusable) c).focus();
                        break;
                    }
                    c = c.getParent();
                }
            } catch (Exception e) {
                log.warn("Error while validation handling ", e);
            }
        }
    }

    @Override
    public WebWindowManager getWindowManager() {
        return windowManager;
    }

    @Override
    public void setWindowManager(WindowManager windowManager) {
        this.windowManager = (WebWindowManager) windowManager;
    }

    @Override
    public DialogParams getDialogParams() {
        return getWindowManager().getDialogParams();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public <T extends Window> T openWindow(String windowAlias, WindowManager.OpenType openType, Map<String, Object> params) {
        return delegate.openWindow(windowAlias, openType, params);
    }

    @Override
    public <T extends Window> T openWindow(String windowAlias, WindowManager.OpenType openType) {
        return delegate.openWindow(windowAlias, openType);
    }

    @Override
    public <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType, Map<String, Object> params, Datasource parentDs) {
        return delegate.openEditor(windowAlias, item, openType, params, parentDs);
    }

    @Override
    public <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType, Map<String, Object> params) {
        return delegate.openEditor(windowAlias, item, openType, params);
    }

    @Override
    public <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType, Datasource parentDs) {
        return delegate.openEditor(windowAlias, item, openType, parentDs);
    }

    @Override
    public <T extends Window> T openEditor(String windowAlias, Entity item, WindowManager.OpenType openType) {
        return delegate.openEditor(windowAlias, item, openType);
    }

    @Override
    public <T extends Window> T openLookup(String windowAlias, Window.Lookup.Handler handler, WindowManager.OpenType openType, Map<String, Object> params) {
        return delegate.openLookup(windowAlias, handler, openType, params);
    }

    @Override
    public <T extends Window> T openLookup(String windowAlias, Window.Lookup.Handler handler, WindowManager.OpenType openType) {
        return delegate.openLookup(windowAlias, handler, openType);
    }

    @Override
    public <T extends IFrame> T openFrame(Component parent, String windowAlias) {
        return delegate.openFrame(parent, windowAlias);
    }

    @Override
    public <T extends IFrame> T openFrame(Component parent, String windowAlias, Map<String, Object> params) {
        return delegate.openFrame(parent, windowAlias, params);
    }

    @Override
    public void showMessageDialog(String title, String message, MessageType messageType) {
        getWindowManager().showMessageDialog(title, message, messageType);
    }

    @Override
    public void showOptionDialog(String title, String message, MessageType messageType, Action[] actions) {
        getWindowManager().showOptionDialog(title, message, messageType, actions);
    }

    @Override
    public void showOptionDialog(String title, String message, MessageType messageType, java.util.List<Action> actions) {
        getWindowManager().showOptionDialog(title, message, messageType, actions.toArray(new Action[actions.size()]));
    }

    @Override
    public void showNotification(String caption, NotificationType type) {
        getWindowManager().showNotification(caption, type);
    }

    @Override
    public void showNotification(String caption, String description, NotificationType type) {
        getWindowManager().showNotification(caption, description, type);
    }

    @Override
    public void showWebPage(String url, @Nullable Map<String, Object> params) {
        getWindowManager().showWebPage(url, params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public WindowContext getContext() {
        return context;
    }

    @Override
    public void setContext(FrameContext ctx) {
        this.context = (WindowContext) ctx;
    }

    @Override
    public DsContext getDsContext() {
        return dsContext;
    }

    @Override
    public void setDsContext(DsContext dsContext) {
        this.dsContext = dsContext;
    }

    @Override
    public void setFocusComponent(String componentId) {
        this.focusComponentId = componentId;
        if (componentId != null) {
            Component focusComponent = getComponent(componentId);
            if (focusComponent != null) {
                focusComponent.requestFocus();
            } else {
                log.error("Can't find focus component: " + componentId);
            }
        } else {
            findAndFocusChildComponent();
        }
    }

    protected com.vaadin.ui.Component.Focusable getComponentToFocus(ComponentContainer container) {
        Iterator<com.vaadin.ui.Component> componentIterator = container.getComponentIterator();
        while (componentIterator.hasNext()) {
            com.vaadin.ui.Component child = componentIterator.next();
            if (child instanceof Panel) {
                child = ((Panel) child).getContent();
            }
            if (child instanceof TabSheet) {
                // #PL-3176
                // we don't know about selected tab after request
                // may be focused component lays on not selected tab
                // it may break component tree
                continue;
            }
            if (child instanceof ComponentContainer) {
                com.vaadin.ui.Component.Focusable result = getComponentToFocus((ComponentContainer) child);
                if (result != null) {
                    return result;
                }
            } else {
                if (child instanceof com.vaadin.ui.Component.Focusable
                        && !child.isReadOnly()
                        && child.isVisible()
                        && child.isEnabled()
                        && !(child instanceof Button)) {

                    return (com.vaadin.ui.Component.Focusable) child;
                }
            }
        }
        return null;
    }

    @Override
    public String getFocusComponent() {
        return focusComponentId;
    }

    @Override
    public void addListener(CloseListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    @Override
    public void removeListener(CloseListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void applySettings(Settings settings) {
        delegate.applySettings(settings);
    }

    @Override
    public void addTimer(Timer timer) {
        com.haulmont.cuba.web.toolkit.Timer vTimer = WebComponentsHelper.unwrap(timer);
        App.getInstance().addTimer(vTimer, this);
    }

    @Override
    public Timer getTimer(String id) {
        com.haulmont.cuba.web.toolkit.Timer timer = App.getInstance().getTimers().getTimer(id);
        if (timer instanceof WebTimer.WebTimerImpl) {
            return ((WebTimer.WebTimerImpl) timer).getTimerComponent();
        }

        return null;
    }

    @Override
    public Settings getSettings() {
        return delegate.getSettings();
    }

    @Override
    public Element getXmlDescriptor() {
        return element;
    }

    @Override
    public void setXmlDescriptor(Element element) {
        this.element = element;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void add(Component childComponent) {
        add(childComponent, ownComponents.size());
    }

    @Override
    public void add(Component childComponent, int index) {
        if (ownComponents.contains(childComponent)) {
            com.vaadin.ui.Component composition = WebComponentsHelper.getComposition(childComponent);
            int existingIndex = ((AbstractOrderedLayout)getContainer()).getComponentIndex(composition);
            if (index > existingIndex) {
                index--;
            }

            remove(childComponent);
        }

        ComponentContainer container = getContainer();
        com.vaadin.ui.Component vComponent = WebComponentsHelper.getComposition(childComponent);
        ((AbstractOrderedLayout)container).addComponent(vComponent, index);

        com.vaadin.ui.Alignment alignment = convertAlignment(childComponent.getAlignment());
        ((AbstractOrderedLayout) container).setComponentAlignment(vComponent, alignment);

        if (childComponent.getId() != null) {
            componentByIds.put(childComponent.getId(), childComponent);
        }

        if (childComponent instanceof BelongToFrame
                && ((BelongToFrame) childComponent).getFrame() == null) {
            ((BelongToFrame) childComponent).setFrame(this);
        } else {
            registerComponent(childComponent);
        }

        if (index == ownComponents.size()) {
            ownComponents.add(childComponent);
        } else {
            List<Component> componentsTempList = new ArrayList<>(ownComponents);
            componentsTempList.add(index, childComponent);

            ownComponents.clear();
            ownComponents.addAll(componentsTempList);
        }

        childComponent.setParent(this);
    }

    @Override
    public void remove(Component childComponent) {
        getContainer().removeComponent(WebComponentsHelper.getComposition(childComponent));
        if (childComponent.getId() != null) {
            componentByIds.remove(childComponent.getId());
        }
        ownComponents.remove(childComponent);

        childComponent.setParent(null);
    }

    @Override
    public void removeAll() {
        getContainer().removeAllComponents();
        for (String childId : componentByIds.keySet()) {
            allComponents.remove(childId);
        }
        componentByIds.clear();

        List<Component> childComponents = new ArrayList<>(ownComponents);
        ownComponents.clear();

        for (Component ownComponent : childComponents) {
            ownComponent.setParent(null);
        }
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return Collections.unmodifiableCollection(ownComponents);
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    protected boolean onClose(String actionId) {
        fireWindowClosed(actionId);
        return true;
    }

    protected void fireWindowClosed(String actionId) {
        for (Object listener : listeners) {
            if (listener instanceof CloseListener) {
                ((CloseListener) listener).windowClosed(actionId);
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Component getParent() {
        return null;
    }

    @Override
    public void setParent(Component parent) {
    }

    @Override
    public String getDebugId() {
        return debugId;
    }

    @Override
    public void setDebugId(String debugId) {
        this.debugId = debugId;
    }

    @Override
    public boolean isEnabled() {
        return component.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        component.setEnabled(enabled);
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestFocus() {
    }

    @Override
    public float getHeight() {
        return component.getHeight();
    }

    @Override
    public int getHeightUnits() {
        return component.getHeightUnits();
    }

    @Override
    public void setHeight(String height) {
        component.setHeight(height);
    }

    @Override
    public float getWidth() {
        return component.getWidth();
    }

    @Override
    public int getWidthUnits() {
        return component.getWidthUnits();
    }

    @Override
    public void setWidth(String width) {
        component.setWidth(width);
    }

    @Override
    public <T extends Component> T getOwnComponent(String id) {
        //noinspection unchecked
        return (T) componentByIds.get(id);
    }

    @Nullable
    @Override
    public <T extends Component> T getComponent(String id) {
        return ComponentsHelper.getWindowComponent(this, id);
    }

    @Nonnull
    @Override
    public <T extends Component> T getComponentNN(String id) {
        T component = getComponent(id);
        if (component == null) {
            throw new IllegalArgumentException(String.format("Not found component with id '%s'", id));
        }
        return component;
    }

    @Override
    public Alignment getAlignment() {
        return Alignment.MIDDLE_CENTER;
    }

    @Override
    public void setAlignment(Alignment alignment) {
    }

    @Override
    public void expand(Component component, String height, String width) {
        final com.vaadin.ui.Component expandedComponent = WebComponentsHelper.getComposition(component);
        if (getContainer() instanceof AbstractOrderedLayout) {
            WebComponentsHelper.expand((AbstractOrderedLayout) getContainer(), expandedComponent, height, width);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void expand(Component component) {
        expand(component, "", "");
    }

    @Override
    public boolean isExpanded(Component component) {
        return ownComponents.contains(component) && WebComponentsHelper.isComponentExpanded(component);
    }

    @Override
    public ExpandDirection getExpandDirection() {
        return ExpandDirection.VERTICAL;
    }

    @Override
    public <T> T getComponent() {
        //noinspection unchecked
        return (T) component;
    }

    @Override
    public com.vaadin.ui.Component getComposition() {
        return component;
    }

    @Override
    public void closeAndRun(String actionId, Runnable runnable) {
        this.doAfterClose = runnable;
        close(actionId);
    }

    @Override
    public boolean close(final String actionId, boolean force) {
        forceClose = force;
        return close(actionId);
    }

    @Override
    public boolean close(final String actionId) {
        if (!forceClose) {
            if (!delegate.preClose(actionId))
                return false;
        }

        if (closing)
            return true;

        if (!forceClose && isModified()) {
            final Committable committable = (getWrapper() instanceof Committable) ? (Committable) getWrapper() :
                        (this instanceof Committable) ? (Committable) this : null;
            if ((committable != null) && configuration.getConfig(ClientConfig.class).getUseSaveConfirmation()) {
                windowManager.showOptionDialog(
                        messages.getMainMessage("closeUnsaved.caption"),
                        messages.getMainMessage("saveUnsaved"),
                        MessageType.WARNING,
                        new Action[]{
                                new DialogAction(DialogAction.Type.OK) {
                                    @Override
                                    public String getCaption() {
                                        return messages.getMainMessage("closeUnsaved.save");
                                    }
                                    @Override
                                    public void actionPerform(Component component) {
                                        committable.commitAndClose();
                                    }
                                },
                                new AbstractAction("discard") {
                                    @Override
                                    public String getCaption() {
                                        return messages.getMainMessage("closeUnsaved.discard");
                                    }
                                    @Override
                                    public String getIcon() {
                                        return "icons/cancel.png";
                                    }
                                    @Override
                                    public void actionPerform(Component component) {
                                        close(actionId, true);
                                    }
                                },
                                new DialogAction(DialogAction.Type.CANCEL) {
                                    @Override
                                    public String getIcon() {
                                        return null;
                                    }
                                    @Override
                                    public void actionPerform(Component component) {
                                        doAfterClose = null;
                                        // try to move focus back
                                        findAndFocusChildComponent();
                                    }
                                }
                        }
                );
            } else {
                windowManager.showOptionDialog(
                        messages.getMessage(WebWindow.class, "closeUnsaved.caption"),
                        messages.getMessage(WebWindow.class, "closeUnsaved"),
                        MessageType.WARNING,
                        new Action[]{
                                new DialogAction(DialogAction.Type.YES) {
                                    @Override
                                    public void actionPerform(Component component) {
                                        forceClose = true;
                                        close(actionId);
                                    }
                                },
                                new DialogAction(DialogAction.Type.NO) {
                                    @Override
                                    public void actionPerform(Component component) {
                                        doAfterClose = null;
                                        // try to move focus back
                                        findAndFocusChildComponent();
                                    }
                                }
                        }
                );
            }
            closing = false;
            return false;
        }

        if (getWrapper() != null)
            getWrapper().saveSettings();
        else
            saveSettings();

        delegate.disposeComponents();

        windowManager.close(this);
        boolean res = onClose(actionId);
        if (res && doAfterClose != null) {
            doAfterClose.run();
        }
        closing = res;
        return res;
    }

    public boolean findAndFocusChildComponent() {
        com.vaadin.ui.Component.Focusable focusComponent = getComponentToFocus(getContainer());
        if (focusComponent != null) {
            focusComponent.focus();
            return true;
        }
        return false;
    }

    protected boolean isModified() {
        return getDsContext() != null && getDsContext().isModified();
    }

    @Override
    public void saveSettings() {
        delegate.saveSettings();
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public <A extends IFrame> A getFrame() {
        //noinspection unchecked
        return (A) this;
    }

    @Override
    public void setFrame(IFrame frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Window wrapBy(Class<Window> wrapperClass) {
        return delegate.wrapBy(wrapperClass);
    }

    @Override
    public Window getWrapper() {
        return delegate.getWrapper();
    }

    @Override
    public ActionsPermissions getActionsPermissions() {
        return actionsPermissions;
    }

    public static class Editor extends WebWindow implements Window.Editor {

        @Override
        protected WindowDelegate createDelegate() {
            return new EditorWindowDelegate(this);
        }

        @Override
        public Entity getItem() {
            return ((EditorWindowDelegate) delegate).getItem();
        }

        @Override
        public void setItem(Entity item) {
            ((EditorWindowDelegate) delegate).setItem(item);
        }

        @Override
        protected boolean onClose(String actionId) {
            releaseLock();
            return super.onClose(actionId);
        }

        public void releaseLock() {
            ((EditorWindowDelegate) delegate).releaseLock();
        }

        @Nullable
        @Override
        public Datasource getParentDs() {
            return ((EditorWindowDelegate) delegate).getParentDs();
        }

        @Override
        public void setParentDs(Datasource parentDs) {
            ((EditorWindowDelegate) delegate).setParentDs(parentDs);
        }

        protected Collection<com.vaadin.ui.Field> getFields() {
            return WebComponentsHelper.getComponents(getContainer(), com.vaadin.ui.Field.class);
        }

        protected MetaClass getMetaClass() {
            return getDatasource().getMetaClass();
        }

        protected Datasource getDatasource() {
            return delegate.getDatasource();
        }

        protected MetaClass getMetaClass(Object item) {
            final MetaClass metaClass;
            if (item instanceof Datasource) {
                metaClass = ((Datasource) item).getMetaClass();
            } else {
                metaClass = ((Instance) item).getMetaClass();
            }
            return metaClass;
        }

        protected Instance getInstance(Object item) {
            if (item instanceof Datasource) {
                return ((Datasource) item).getItem();
            } else {
                return (Instance) item;
            }
        }

        @Override
        public boolean isModified() {
            return ((EditorWindowDelegate) delegate).isModified();
        }

        @Override
        public boolean commit() {
            return commit(true);
        }

        @Override
        public boolean commit(boolean validate) {
            if (validate && !getWrapper().validateAll())
                return false;

            return ((EditorWindowDelegate) delegate).commit(false);
        }

        @Override
        public void commitAndClose() {
            if (!getWrapper().validateAll())
                return;

            if (((EditorWindowDelegate) delegate).commit(true))
                close(COMMIT_ACTION_ID);
        }

        @Override
        public boolean isLocked() {
            return ((EditorWindowDelegate) delegate).isLocked();
        }

    }

    public static class Lookup extends WebWindow implements Window.Lookup {

        private Handler handler;

        private Validator validator;

        private Component lookupComponent;
        private VerticalLayout container;
        private Button selectButton;
        private Button cancelButton;
        private SelectAction selectAction;

        public Lookup() {
            Configuration configuration = AppBeans.get(Configuration.NAME);
            ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);

            addAction(new AbstractAction(WindowDelegate.LOOKUP_SELECTED_ACTION_ID, clientConfig.getCommitShortcut()) {
                @Override
                public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                    fireSelectAction();
                }
            });
        }

        @Override
        public com.haulmont.cuba.gui.components.Component getLookupComponent() {
            return lookupComponent;
        }

        @Override
        public void setLookupComponent(Component lookupComponent) {
            this.lookupComponent = lookupComponent;

            if (lookupComponent instanceof com.haulmont.cuba.gui.components.Table) {
                com.haulmont.cuba.gui.components.Table table = (com.haulmont.cuba.gui.components.Table) lookupComponent;
                table.setEnterPressAction(
                        new AbstractAction(WindowDelegate.LOOKUP_ENTER_PRESSED_ACTION_ID) {
                            @Override
                            public void actionPerform(Component component) {
                                fireSelectAction();
                            }
                        });
                table.setItemClickAction(new AbstractAction(WindowDelegate.LOOKUP_ITEM_CLICK_ACTION_ID) {
                    @Override
                    public void actionPerform(Component component) {
                        fireSelectAction();
                    }
                });
            } else if (lookupComponent instanceof Tree) {
                final Tree tree = (Tree) lookupComponent;
                final com.haulmont.cuba.web.toolkit.ui.Tree treeComponent = WebComponentsHelper.unwrap(tree);
                treeComponent.setDoubleClickMode(true);
                treeComponent.addListener(new ItemClickEvent.ItemClickListener() {
                    @Override
                    public void itemClick(ItemClickEvent event) {
                        CollectionDatasource treeCds = tree.getDatasource();
                        if (treeCds != null) {
                            if (event.getItem() != null) {
                                treeComponent.setValue(event.getItemId());
                                fireSelectAction();
                            }
                        }
                    }
                });
            }
        }

        @Override
        public Handler getLookupHandler() {
            return handler;
        }

        @Override
        public void setLookupHandler(Handler handler) {
            this.handler = handler;
        }

        @Override
        protected ComponentContainer getContainer() {
            return container;
        }

        @Override
        public void setLookupValidator(Validator validator) {
            this.validator = validator;
        }

        @Override
        public Validator getLookupValidator() {
            return validator;
        }

        protected void fireSelectAction() {
            if (selectAction != null)
                selectAction.buttonClick(null);
        }

        @Override
        public void setSpacing(boolean enabled) {
            container.setSpacing(enabled);
        }

        @Override
        public void setStyleName(String name) {
            container.setStyleName(name);
        }

        @Override
        public String getStyleName() {
            return container.getStyleName();
        }

        @Override
        public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
            container.setMargin(topEnable, rightEnable, bottomEnable, leftEnable);
        }

        @Override
        public void setMargin(boolean enable) {
            container.setMargin(enable);
        }

        @Override
        protected com.vaadin.ui.Component createLayout() {
            final VerticalActionsLayout form = new VerticalActionsLayout();

            container = new VerticalLayout();

            HorizontalLayout okbar = new HorizontalLayout();
            okbar.setHeight(-1, Sizeable.UNITS_PIXELS);
            okbar.setStyleName("Window-actionsPane");
            okbar.setMargin(true, false, false, false);
            okbar.setSpacing(true);

            Messages messages = AppBeans.get(Messages.NAME);

            boolean isTestMode = App.getInstance().isTestMode();

            final String messagesPackage = AppConfig.getMessagesPack();
            selectAction = new SelectAction(this);
            selectButton = WebComponentsHelper.createButton();
            selectButton.setCaption(messages.getMessage(messagesPackage, "actions.Select"));
            selectButton.setIcon(new VersionedThemeResource("icons/ok.png"));
            selectButton.addListener(selectAction);
            selectButton.setStyleName("Window-actionButton");
            if (isTestMode) {
                selectButton.setCubaId("selectButton");
            }

            cancelButton = WebComponentsHelper.createButton();
            cancelButton.setCaption(messages.getMessage(messagesPackage, "actions.Cancel"));
            cancelButton.addListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    close("cancel");
                }
            });
            cancelButton.setStyleName("Window-actionButton");
            cancelButton.setIcon(new VersionedThemeResource("icons/cancel.png"));
            if (isTestMode) {
                cancelButton.setCubaId("cancelButton");
            }

            okbar.addComponent(selectButton);
            okbar.addComponent(cancelButton);

            form.addComponent(container);
            form.addComponent(okbar);

            container.setSizeFull();
            form.setExpandRatio(container, 1);
            form.setComponentAlignment(okbar, com.vaadin.ui.Alignment.MIDDLE_LEFT);
            form.setSizeFull();

            return form;
        }

        @Override
        public void setId(String id) {
            super.setId(id);

            if (App.getInstance().isTestModeRequest()) {
                WebWindowManager windowManager = getWindowManager();
                windowManager.setDebugId(selectButton, id + ".selectButton");
                windowManager.setDebugId(cancelButton, id + ".cancelButton");
            }
        }
    }
}