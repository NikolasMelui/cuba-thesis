/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.xml;

import com.haulmont.cuba.gui.components.*;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DeclarativeAction extends AbstractAction {

    private IFrame frame;
    private String methodName;

    public DeclarativeAction(String id, String caption, String icon, String enable, String visible,
                             String methodName, Component.ActionsHolder holder)
    {
        super(id);
        this.caption = caption;
        this.icon = icon;
        this.enabled = enable == null ? true : Boolean.valueOf(enable);
        this.visible = visible == null ? true : Boolean.valueOf(visible);
        this.methodName = methodName;
        if (holder instanceof IFrame)
            frame = (IFrame) holder;
        else if (holder instanceof Component.BelongToFrame)
            frame = ((Component.BelongToFrame) holder).getFrame();
        else
            throw new IllegalStateException("Component " + holder + " can't contain DeclarativeAction");
    }

    @Override
    public void actionPerform(Component component) {
        if (StringUtils.isEmpty(methodName))
            return;

        Object controller;
        if (frame instanceof WrappedFrame) {
            controller = ((WrappedFrame) frame).getWrapper();
        } else if (frame instanceof WrappedWindow) {
            controller = ((WrappedWindow) frame).getWrapper();
        } else {
            controller = frame;
        }
        Method method;
        try {
            method = controller.getClass().getMethod(methodName, Component.class);
            try {
                method.invoke(controller, component);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (NoSuchMethodException e) {
            try {
                method = controller.getClass().getMethod(methodName);
                try {
                    method.invoke(controller);
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            } catch (NoSuchMethodException e1) {
                throw new IllegalStateException("No suitable methods named " + methodName + " for action " + id);
            }
        }
    }
}
