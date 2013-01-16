/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.03.11 9:26
 *
 * $Id$
 */
package com.haulmont.cuba.gui.components.actions;

import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.*;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard list action to create a new entity instance.
 * <p>
 * Action's behaviour can be customized by providing arguments to constructor, setting properties, or overriding
 * methods {@link #afterCommit(com.haulmont.cuba.core.entity.Entity)}, {@link #afterWindowClosed(com.haulmont.cuba.gui.components.Window)}
 *
 * @author krivopustov
 * @version $Id$
 */
public class CreateAction extends AbstractAction {

    public static final String ACTION_ID = ListActionType.CREATE.getId();

    protected final ListComponent owner;
    protected WindowManager.OpenType openType;

    protected String windowId;
    protected Map<String, Object> windowParams;
    protected Map<String, Object> initialValues;

    /**
     * The simplest constructor. The action has default name and opens the editor screen in THIS tab.
     * @param owner    component containing this action
     */
    public CreateAction(ListComponent owner) {
        this(owner, WindowManager.OpenType.THIS_TAB, ACTION_ID);
    }

    /**
     * Constructor that allows to specify how the editor screen opens. The action has default name.
     * @param owner    component containing this action
     * @param openType  how to open the editor screen
     */
    public CreateAction(ListComponent owner, WindowManager.OpenType openType) {
        this(owner, openType, ACTION_ID);
    }

    /**
     * Constructor that allows to specify the action name and how the editor screen opens.
     * @param owner    component containing this action
     * @param openType  how to open the editor screen
     * @param id        action name
     */
    public CreateAction(ListComponent owner, WindowManager.OpenType openType, String id) {
        super(id);
        this.owner = owner;
        this.openType = openType;
        this.caption = messages.getMainMessage("actions.Create");
        this.icon = "icons/create.png";
    }

    /**
     * Whether the action is currently enabled. Override to provide specific behaviour.
     * @return  true if enabled
     */
    public boolean isEnabled() {
        if (!super.isEnabled())
            return false;

        if (!userSession.isEntityOpPermitted(owner.getDatasource().getMetaClass(), EntityOp.CREATE))
            return false;

        if (owner.getDatasource() instanceof PropertyDatasource) {
            MetaProperty metaProperty = ((PropertyDatasource) owner.getDatasource()).getProperty();
            return userSession.isEntityAttrPermitted(
                    metaProperty.getDomain(), metaProperty.getName(), EntityAttrAccess.MODIFY);
        }
        return true;
    }

    /**
     * This method is invoked by action owner component. Don't override it, there are special methods to
     * customize behaviour below.
     * @param component component invoking action
     */
    public void actionPerform(Component component) {
        final CollectionDatasource datasource = owner.getDatasource();
        final DataSupplier dataservice = datasource.getDataSupplier();

        final Entity item = dataservice.newInstance(datasource.getMetaClass());

        if (owner instanceof Tree) {
            String hierarchyProperty = ((Tree) owner).getHierarchyProperty();

            Entity parentItem = datasource.getItem();
            // datasource.getItem() may contain deleted item
            if (parentItem != null && !datasource.containsItem(parentItem.getId())) {
                parentItem = null;
            }

            item.setValue(hierarchyProperty, parentItem);
        }

        if (datasource instanceof NestedDatasource) {
            // Initialize reference to master entity
            Datasource masterDs = ((NestedDatasource) datasource).getMaster();
            MetaProperty metaProperty = ((NestedDatasource) datasource).getProperty();
            if (masterDs != null && metaProperty != null) {
                MetaProperty inverseProp = metaProperty.getInverse();
                if (inverseProp != null && inverseProp.getDomain().equals(datasource.getMetaClass())) {
                    item.setValue(inverseProp.getName(), masterDs.getItem());
                }
            }
        }

        Map<String, Object> values = getInitialValues();
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                final Object value = entry.getValue();
                if (value instanceof Collection) {
                    final Collection collection = (Collection) value;
                    if (!collection.isEmpty()) {
                        if (collection.size() != 1) {
                            throw new UnsupportedOperationException();
                        } else {
                            item.setValue(entry.getKey(), collection.iterator().next());
                        }
                    }
                } else {
                    item.setValue(entry.getKey(), value);
                }
            }
        }

        Datasource parentDs = null;
        if (datasource instanceof PropertyDatasource) {
            MetaProperty metaProperty = ((PropertyDatasource) datasource).getProperty();
            if (metaProperty.getType().equals(MetaProperty.Type.COMPOSITION)) {
                parentDs = datasource;
            }
        }
        final Datasource pDs = parentDs;

        Map<String, Object> params = getWindowParams();
        if (params == null)
            params = new HashMap<String, Object>();

        final Window window = owner.getFrame().openEditor(getWindowId(), item, openType, params, parentDs);

        window.addListener(new Window.CloseListener() {
            public void windowClosed(String actionId) {
                if (Window.COMMIT_ACTION_ID.equals(actionId) && window instanceof Window.Editor) {
                    Object item = ((Window.Editor) window).getItem();
                    if (item instanceof Entity) {
                        if (pDs == null) {
                            boolean modified = datasource.isModified();
                            datasource.addItem((Entity) item);
                            ((DatasourceImplementation) datasource).setModified(modified);
                        }
                        owner.setSelected((Entity) item);
                        afterCommit((Entity) item);
                    }
                }
                afterWindowClosed(window);
            }
        });
    }

    /**
     * @return  editor screen open type
     */
    public WindowManager.OpenType getOpenType() {
        return openType;
    }

    /**
     * @param openType  editor screen open type
     */
    public void setOpenType(WindowManager.OpenType openType) {
        this.openType = openType;
    }

    /**
     * @return  editor screen identifier
     */
    public String getWindowId() {
        if (windowId != null)
            return windowId;
        else
            return owner.getDatasource().getMetaClass().getName() + ".edit";
    }

    /**
     * @param windowId  editor screen identifier
     */
    public void setWindowId(String windowId) {
        this.windowId = windowId;
    }

    /**
     * @return  editor screen parameters
     */
    public Map<String, Object> getWindowParams() {
        return windowParams;
    }

    /**
     * @param windowParams  editor screen parameters
     */
    public void setWindowParams(Map<String, Object> windowParams) {
        this.windowParams = windowParams;
    }

    /**
     * @return  map of initial values for attributes of created entity
     */
    public Map<String, Object> getInitialValues() {
        return initialValues;
    }

    /**
     * @param initialValues map of initial values for attributes of created entity
     */
    public void setInitialValues(Map<String, Object> initialValues) {
        this.initialValues = initialValues;
    }

    /**
     * Hook invoked after the editor was committed and closed
     * @param entity    new committed entity instance
     */
    protected void afterCommit(Entity entity) {
    }

    /**
     * Hook invoked always after the editor was closed
     * @param window    the editor window
     */
    protected void afterWindowClosed(Window window) {
    }
}
