/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.toolkit.VersionedThemeResource;
import com.vaadin.ui.NativeButton;
import org.apache.commons.lang.StringUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author abramov
 * @version $Id$
 */
public class WebButton
        extends
            WebAbstractComponent<com.vaadin.ui.Button>
        implements
            Button {

    protected Action action;
    protected String icon;

    public static final String ICON_STYLE = "icon";

    public WebButton() {
        if (AppBeans.get(Configuration.class).getConfig(WebConfig.class).getUseNativeButtons()) {
            component = new NativeButton();
        } else {
            component = new com.vaadin.ui.Button();
        }
        component.addClickListener(new com.vaadin.ui.Button.ClickListener() {
            @Override
            public void buttonClick(com.vaadin.ui.Button.ClickEvent event) {
                if (action != null) {
                    action.actionPerform(WebButton.this);
                }
            }
        });
    }

    @Override
    public String getCaption() {
        return component.getCaption();
    }

    @Override
    public void setCaption(String caption) {
        component.setCaption(caption);
    }

    @Override
    public String getDescription() {
        return component.getDescription();
    }

    @Override
    public void setDescription(String description) {
        component.setDescription(description);
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public void setAction(Action action) {
        this.action = action;

        final String caption = action.getCaption();
        if (!StringUtils.isEmpty(caption) && StringUtils.isEmpty(component.getCaption())) {
            component.setCaption(caption);
        }

        component.setEnabled(action.isEnabled());
        component.setVisible(action.isVisible());

        if (action.getIcon() != null) {
            setIcon(action.getIcon());
        }

        action.addOwner(this);

        action.addPropertyChangeListener(
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (Action.PROP_ICON.equals(evt.getPropertyName())) {
                            setIcon(WebButton.this.action.getIcon());
                        } else if (Action.PROP_CAPTION.equals(evt.getPropertyName())) {
                            setCaption(WebButton.this.action.getCaption());
                        } else if (Action.PROP_ENABLED.equals(evt.getPropertyName())) {
                            setEnabled(WebButton.this.action.isEnabled());
                        } else if (Action.PROP_VISIBLE.equals(evt.getPropertyName())) {
                            setVisible(WebButton.this.action.isVisible());
                        }
                    }
                }
        );

        AppUI ui = AppUI.getCurrent();
        if (ui.isTestMode() && frame != null && frame.getId() != null
                && (StringUtils.isEmpty(getDebugId()) || StringUtils.isEmpty(getId()))) {
            String id = getId();

            if (StringUtils.isEmpty(id))
                id = action.getId();

            if (StringUtils.isNotEmpty(id)) {
                setDebugId(ui.getTestIdManager().getTestId(ComponentsHelper.getFullFrameId(frame) + "." + id));
            }
        }
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setStyleName(String name) {
        super.setStyleName(name);
        if (getIcon() != null)
            component.addStyleName(ICON_STYLE);
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
        if (!StringUtils.isEmpty(icon)) {
            component.setIcon(new VersionedThemeResource(icon));
            component.addStyleName(ICON_STYLE);
        } else {
            component.setIcon(null);
            component.removeStyleName(ICON_STYLE);
        }
    }

    @Override
    protected void assignAutoDebugId(IFrame frame) {
        if (frame.getId() == null)
            return;

        AppUI ui = AppUI.getCurrent();

        if (ui.isTestMode() && StringUtils.isEmpty(getDebugId())) {
            String id = getId();

            if (StringUtils.isEmpty(id) && action != null)
                id = action.getId();

            if (StringUtils.isNotEmpty(id)) {
                setDebugId(ui.getTestIdManager().getTestId(ComponentsHelper.getFullFrameId(frame) + "." + id));
            }
        }
    }
}