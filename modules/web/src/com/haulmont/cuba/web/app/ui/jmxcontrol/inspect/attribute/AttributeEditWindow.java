/*
 * Copyright (c) 2008-2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.app.ui.jmxcontrol.inspect.attribute;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.AbstractEditor;
import com.haulmont.cuba.gui.components.GridLayout;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.jmxcontrol.entity.ManagedBeanAttribute;
import com.haulmont.cuba.web.app.ui.jmxcontrol.util.AttributeEditor;
import com.haulmont.cuba.web.jmx.JmxControlAPI;
import org.apache.commons.lang.ObjectUtils;

import javax.inject.Inject;

/**
 * @author budarov
 * @version $Id$
 */
public class AttributeEditWindow extends AbstractEditor {

    private AttributeEditor valueHolder;

    @Inject
    protected JmxControlAPI jmxControlAPI;

    @Override
    public void setItem(Entity item) {
        super.setItem(item);

        ManagedBeanAttribute mba = (ManagedBeanAttribute) getItem();
        final String type = mba.getType();

        GridLayout layout = getComponent("valueContainer");
        valueHolder = new AttributeEditor(this, type, mba.getValue());

        layout.add(valueHolder.getComponent(), 1, 0);

        if (mba.getName() != null) {
            setCaption(formatMessage("editAttribute.title.format", mba.getName()));
        }
    }

    @Override
    public void commitAndClose() {
        if (assignValue()) {
            close(COMMIT_ACTION_ID, true);
        }
    }

    private boolean assignValue() {
        ManagedBeanAttribute mba = (ManagedBeanAttribute) getItem();

        try {
            Object newValue = valueHolder != null ? valueHolder.getAttributeValue() : null;
            if (newValue != null) {
                if (!ObjectUtils.equals(mba.getValue(), newValue)) {
                    mba.setValue(newValue);
                    jmxControlAPI.saveAttributeValue(mba);
                }
                return true;
            }
        } catch (Exception e) {
            showNotification(String.format(getMessage("editAttribute.exception"), e.getMessage()),
                    IFrame.NotificationType.HUMANIZED);
            return false;
        }
        showNotification(getMessage("editAttribute.conversionError"), IFrame.NotificationType.HUMANIZED);
        return false;
    }
}