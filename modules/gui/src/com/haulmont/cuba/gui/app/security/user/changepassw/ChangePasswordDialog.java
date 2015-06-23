/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.app.security.user.changepassw;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.PasswordEncryption;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.security.app.UserManagementService;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang.ObjectUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.UUID;

/**
 * @author krivopustov
 * @version $Id$
 */
public class ChangePasswordDialog extends AbstractWindow {

    @Inject
    protected PasswordField passwField;

    @Inject
    protected PasswordField confirmPasswField;

    @Inject
    protected PasswordField currentPasswordField;

    @Inject
    protected Label currentPasswordLabel;

    @Named("windowClose")
    protected Button closeBtn;

    @Inject
    protected ClientConfig clientConfig;

    @Inject
    protected PasswordEncryption passwordEncryption;

    @Inject
    protected UserManagementService userManagementService;

    @Inject
    protected UserSession userSession;

    @WindowParam
    protected User user;

    protected boolean cancelEnabled = true;
    protected boolean currentPasswordRequired = false;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        getDialogParams().setWidthAuto();

        Boolean cancelEnabled = (Boolean) params.get("cancelEnabled");
        if (Boolean.FALSE.equals(cancelEnabled)) {
            this.cancelEnabled = false;
        }

        Boolean currentPasswordRequired = (Boolean) params.get("currentPasswordRequired");
        if (Boolean.TRUE.equals(currentPasswordRequired)) {
            currentPasswordField.setVisible(true);
            currentPasswordLabel.setVisible(true);

            this.currentPasswordRequired = true;
        }

        addAction(new AbstractAction("windowCommit", clientConfig.getCommitShortcut()) {
            @Override
            public void actionPerform(Component component) {
                changePassword();
            }
        });
    }

    @Override
    public void ready() {
        super.ready();

        if (currentPasswordField.isVisible() && currentPasswordField.isEnabled()) {
            currentPasswordField.requestFocus();
        }

        if (!cancelEnabled) {
            closeBtn.setVisible(false);
        }
    }

    @Override
    protected void postValidate(ValidationErrors errors) {
        super.postValidate(errors);

        String password = passwField.getValue();

        if (errors.isEmpty()) {
            String currentPassword = currentPasswordField.getValue();
            String passwordConfirmation = confirmPasswField.getValue();

            UUID targetUserId;
            if (user == null) {
                targetUserId = userSession.getUser().getId();
            } else {
                targetUserId = user.getId();
            }

            if (currentPasswordRequired
                    && !userManagementService.checkPassword(targetUserId, passwordEncryption.getPlainHash(currentPassword))) {
                errors.add(currentPasswordField, getMessage("wrongCurrentPassword"));

            } else if (userManagementService.checkPassword(targetUserId, passwordEncryption.getPlainHash(password))) {
                errors.add(passwField, getMessage("currentPasswordWarning"));

            } else if (!ObjectUtils.equals(password, passwordConfirmation)) {
                errors.add(confirmPasswField, getMessage("passwordsDoNotMatch"));

            } else {
                if (clientConfig.getPasswordPolicyEnabled()) {
                    String regExp = clientConfig.getPasswordPolicyRegExp();
                    if (!password.matches(regExp)) {
                        errors.add(passwField, getMessage("simplePassword"));
                    }
                }
            }
        }
    }

    public void windowClose() {
        close(CLOSE_ACTION_ID);
    }

    public void changePassword() {
        if (validateAll()) {
            UUID targetUserId;
            if (user == null) {
                targetUserId = userSession.getUser().getId();
            } else {
                targetUserId = user.getId();
            }

            String passwordHash = passwordEncryption.getPasswordHash(targetUserId, passwField.<String>getValue());
            userManagementService.changeUserPassword(targetUserId, passwordHash);

            showNotification(getMessage("passwordChanged"), NotificationType.HUMANIZED);

            close(COMMIT_ACTION_ID);
        }
    }
}