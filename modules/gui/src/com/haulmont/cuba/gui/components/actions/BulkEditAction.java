/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.actions;

import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author artamonov
 * @version $Id$
 */
public class BulkEditAction extends AbstractAction {

    protected ListComponent owner;
    protected WindowManager.OpenType openType = WindowManager.OpenType.DIALOG;
    protected String exclude;

    public BulkEditAction(ListComponent owner) {
        super("bulkEdit");

        this.owner = owner;
        this.icon = "icons/bulk-edit.png";
        this.caption = messages.getMessage(getClass(), "actions.BulkEdit");

        boolean permitted = userSession.isSpecificPermitted(BulkEditor.PERMISSION);

        setVisible(permitted);
        setEnabled(permitted);
    }

    public WindowManager.OpenType getOpenType() {
        return openType;
    }

    public void setOpenType(WindowManager.OpenType openType) {
        this.openType = openType;
    }

    public String getExcludePropertyRegex() {
        return exclude;
    }

    public void setExcludePropertyRegex(String exclude) {
        this.exclude = exclude;
    }

    @Override
    public void actionPerform(Component component) {
        if (!userSession.isSpecificPermitted(BulkEditor.PERMISSION)) {
            owner.getFrame().showNotification(messages.getMainMessage("accessDenied.message"), IFrame.NotificationType.ERROR);
            return;
        }

        if (owner.getSelected().isEmpty()) {
            owner.getFrame().showNotification(messages.getMainMessage("actions.BulkEdit.emptySelection"),
                    IFrame.NotificationType.HUMANIZED);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("metaClass", owner.getDatasource().getMetaClass());
        params.put("view", owner.getDatasource().getView());
        params.put("selected", owner.getSelected());
        params.put("exclude", exclude);

        owner.getFrame().getDialogParams()
                .setWidth(800)
                .setHeight(600)
                .setResizable(true);

        Window bulkEditor = owner.getFrame().openWindow("bulkEditor", openType, params);
        bulkEditor.addListener(new Window.CloseListener() {
            @Override
            public void windowClosed(String actionId) {
                if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                    owner.getDatasource().refresh();
                }
                owner.requestFocus();
            }
        });
    }
}