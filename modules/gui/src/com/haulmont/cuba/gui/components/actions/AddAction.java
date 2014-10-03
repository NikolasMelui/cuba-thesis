/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.components.actions;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.NestedDatasource;
import com.haulmont.cuba.gui.data.PropertyDatasource;
import com.haulmont.cuba.security.entity.EntityAttrAccess;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard list action adding an entity instance to list from a lookup screen.
 * <p>
 * Action's behaviour can be customized by providing arguments to constructor or setting properties.
 *
 * @author krivopustov
 * @version $Id$
 */
public class AddAction extends AbstractAction implements Action.HasOpenType {

    public static final String ACTION_ID = ListActionType.ADD.getId();

    protected final ListComponent owner;
    protected Window.Lookup.Handler handler;
    protected WindowManager.OpenType openType;

    protected String windowId;
    protected Map<String, Object> windowParams;

    protected boolean permissionFlag = false;

    /**
     * The simplest constructor. The action has default name and opens the lookup screen in THIS tab.
     * Lookup handler can be set by subsequent call to {@link #setHandler(com.haulmont.cuba.gui.components.Window.Lookup.Handler)}.
     * If it is not set, an instance of {@link DefaultHandler} will be used.
     *
     * @param owner    component containing this action
     */
    public AddAction(ListComponent owner) {
        this(owner, null, WindowManager.OpenType.THIS_TAB, ACTION_ID);
    }

    /**
     * The simplest constructor. The action has default name and opens the lookup screen in THIS tab.
     * @param owner    component containing this action
     * @param handler   lookup handler. If null, an instance of {@link DefaultHandler} will be used.
     */
    public AddAction(ListComponent owner, @Nullable Window.Lookup.Handler handler) {
        this(owner, handler, WindowManager.OpenType.THIS_TAB, ACTION_ID);
    }

    /**
     * Constructor that allows to specify how the lookup screen opens. The action has default name.
     * @param owner    component containing this action
     * @param handler   lookup handler. If null, an instance of {@link DefaultHandler} will be used.
     * @param openType  how to open the editor screen
     */
    public AddAction(ListComponent owner, @Nullable Window.Lookup.Handler handler, WindowManager.OpenType openType) {
        this(owner, handler, openType, ACTION_ID);
    }

    /**
     * Constructor that allows to specify the action name and how the lookup screen opens.
     * @param owner    component containing this action
     * @param handler   lookup handler. If null, an instance of {@link DefaultHandler} will be used.
     * @param openType  how to open the editor screen
     * @param id        action's name
     */
    public AddAction(ListComponent owner, @Nullable Window.Lookup.Handler handler, WindowManager.OpenType openType, String id) {
        super(id);
        this.owner = owner;
        this.handler = handler;
        this.openType = openType;
        this.caption = messages.getMainMessage("actions.Add");
        this.icon = "icons/add.png";
        Configuration configuration = AppBeans.get(Configuration.NAME);
        ClientConfig clientConfig = configuration.getConfig(ClientConfig.class);
        setShortcut(clientConfig.getTableAddShortcut());

        refreshState();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(permissionFlag && enabled);
    }

    @Override
    public boolean isEnabled() {
        return permissionFlag && super.isEnabled();
    }

    @Override
    public void refreshState() {
        permissionFlag = true;

        if (owner.getDatasource() instanceof PropertyDatasource) {
            PropertyDatasource datasource = (PropertyDatasource) owner.getDatasource();

            MetaClass parentMetaClass = datasource.getMaster().getMetaClass();
            MetaProperty metaProperty = datasource.getProperty();

            Security security = AppBeans.get(Security.NAME);
            permissionFlag = security.isEntityAttrPermitted(parentMetaClass, metaProperty.getName(), EntityAttrAccess.MODIFY);
        }

        super.setEnabled(permissionFlag);
    }

    /**
     * This method is invoked by action owner component. Don't override it, there are special methods to
     * customize behaviour below.
     * @param component component invoking action
     */
    @Override
    public void actionPerform(Component component) {
        Map<String, Object> params = getWindowParams();
        if (params == null)
            params = new HashMap<>();

        Window.Lookup.Handler itemsHandler = handler != null ? handler : new DefaultHandler();

        Window lookupWindow = owner.getFrame().openLookup(getWindowId(), itemsHandler, openType, params);
        lookupWindow.addListener(new Window.CloseListener() {
            @Override
            public void windowClosed(String actionId) {
                // move focus to owner
                owner.requestFocus();
            }
        });
    }

    /**
     * @return  handler to pass to lookup screen
     */
    @Nullable
    public Window.Lookup.Handler getHandler() {
        return handler;
    }

    /**
     * @param handler   handler to pass to lookup screen
     */
    public void setHandler(Window.Lookup.Handler handler) {
        this.handler = handler;
    }

    /**
     * @return  lookup screen open type
     */
    @Override
    public WindowManager.OpenType getOpenType() {
        return openType;
    }

    /**
     * @param openType  lookup screen open type
     */
    @Override
    public void setOpenType(WindowManager.OpenType openType) {
        this.openType = openType;
    }

    /**
     * @return  lookup screen id
     */
    public String getWindowId() {
        if (windowId != null) {
            return windowId;
        } else {
            WindowConfig windowConfig = AppBeans.get(WindowConfig.NAME);
            MetaClass metaClass = owner.getDatasource().getMetaClass();

            return windowConfig.getAvailableLookupScreenId(metaClass);
        }
    }

    /**
     * @param windowId  lookup screen id
     */
    public void setWindowId(String windowId) {
        this.windowId = windowId;
    }

    /**
     * @return  lookup screen parameters
     */
    public Map<String, Object> getWindowParams() {
        return windowParams;
    }

    /**
     * @param windowParams  lookup screen parameters
     */
    public void setWindowParams(Map<String, Object> windowParams) {
        this.windowParams = windowParams;
    }

    /**
     * The default implementation of <code>Lookup.Handler</code>, adding items to owner's datasource if they are not
     * there yet. <br/>
     * It assumes that a lookup screen returns a collection of entities of the same type as owner's datasource or
     * subtype of owner's datasource class.
     */
    protected class DefaultHandler implements Window.Lookup.Handler {

        @SuppressWarnings("unchecked")
        @Override
        public void handleLookup(Collection items) {
            if (items == null || items.isEmpty()) {
                return;
            }

            final CollectionDatasource ds = owner.getDatasource();
            if (ds == null) {
                return;
            }

            Metadata metadata = AppBeans.get(Metadata.NAME);
            ExtendedEntities extendedEntities = metadata.getExtendedEntities();

            ds.suspendListeners();
            try {
                Entity masterEntity = null;
                MetaProperty inverseProp = null;
                boolean initializeMasterReference = false;

                if (ds instanceof NestedDatasource) {
                    Datasource masterDs = ((NestedDatasource) ds).getMaster();
                    if (masterDs != null) {
                        MetaProperty metaProperty = ((NestedDatasource) ds).getProperty();
                        masterEntity = masterDs.getItem();

                        if (metaProperty != null) {
                            inverseProp = metaProperty.getInverse();

                            if (inverseProp != null && !inverseProp.getRange().getCardinality().isMany()) {
                                Class inversePropClass = extendedEntities.getEffectiveClass(inverseProp.getDomain());
                                Class dsClass = extendedEntities.getEffectiveClass(ds.getMetaClass());

                                initializeMasterReference = inversePropClass.isAssignableFrom(dsClass);
                            }
                        }
                    }
                }

                for (Object item : items) {
                    if (item instanceof Entity) {
                        Entity entity = (Entity) item;
                        if (!ds.containsItem(entity.getId())) {
                            // Initialize reference to master entity
                            if (initializeMasterReference) {
                                entity.setValue(inverseProp.getName(), masterEntity);
                            }
                            ds.addItem(entity);
                        }
                    }
                }
            } finally {
                ds.resumeListeners();
            }
        }
    }
}