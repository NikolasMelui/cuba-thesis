/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.actions;

import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.components.AbstractAction;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.web.App;

/**
 * @author shishov
 * @version $Id$
 */
public class ChangeSubstUserAction extends AbstractAction {
    private User user;

    public ChangeSubstUserAction(User user) {
        super("changeSubstUserAction");
        this.user = user;
    }

    @Override
    public String getIcon() {
        return "icons/ok.png";
    }

    @Override
    public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
        final App app = App.getInstance();
        App.getInstance().getWindowManager().checkModificationsAndCloseAll(
                new Runnable() {
                    @Override
                    public void run() {
                        app.getWindowManager().closeAll();
                        try {
                            app.getConnection().substituteUser(user);
                            doAfterChangeUser();
                        } catch (javax.persistence.NoResultException e) {
                            app.getWindowManager().showNotification(
                                    messages.formatMessage(AppConfig.getMessagesPack(), "userDeleteMsg",
                                            user.getName()),
                                    IFrame.NotificationType.WARNING
                            );
                            doRevert();
                        }
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        doRevert();
                    }
                }
        );
    }

    public void doAfterChangeUser() {
    }

    public void doRevert() {
    }
}