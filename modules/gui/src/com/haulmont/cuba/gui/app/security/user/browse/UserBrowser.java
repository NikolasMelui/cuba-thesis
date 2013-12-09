/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.security.user.browse;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.app.security.user.resetpasswords.ResetPasswordsDialog;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;
import com.haulmont.cuba.security.app.UserManagementService;
import com.haulmont.cuba.security.entity.*;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.lang.BooleanUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class UserBrowser extends AbstractLookup {

    @Inject
    protected Table usersTable;

    @Inject
    protected CollectionDatasource<User, UUID> usersDs;

    @Named("usersTable.remove")
    protected RemoveAction removeAction;

    @Named("usersTable.copySettings")
    protected Action copySettingsAction;

    @Named("usersTable.changePassw")
    protected Action changePasswAction;

    @Named("usersTable.changePasswAtLogon")
    protected Action changePasswAtLogonAction;

    @Inject
    protected Button userTableCopyButton;

    @Inject
    protected Button changePasswordButton;

    @Inject
    protected UserSession userSession;

    @Inject
    protected Metadata metadata;

    @Inject
    protected DataSupplier dataSupplier;

    @Inject
    protected UserManagementService userManagementService;

    @Override
    public void init(Map<String, Object> params) {
        final boolean hasPermissionsToCreateUsers =
                userSession.isEntityOpPermitted(metadata.getClassNN(User.class), EntityOp.CREATE);

        final boolean hasPermissionsToUpdateUsers =
                userSession.isEntityOpPermitted(metadata.getClassNN(User.class), EntityOp.CREATE);

        changePasswAction.setEnabled(hasPermissionsToUpdateUsers);
        changePasswAtLogonAction.setEnabled(hasPermissionsToUpdateUsers);

        copySettingsAction.setEnabled(
                userSession.isEntityOpPermitted(metadata.getClassNN(UserSetting.class), EntityOp.CREATE));

        usersDs.addListener(new DsListenerAdapter<User>() {
            @Override
            public void itemChanged(Datasource<User> ds, User prevItem, User item) {
                if (usersTable.getSelected().size() > 1) {
                    userTableCopyButton.setEnabled(false);
                    changePasswordButton.setEnabled(false);
                } else {
                    userTableCopyButton.setEnabled(hasPermissionsToCreateUsers && item != null);
                    changePasswordButton.setEnabled(hasPermissionsToUpdateUsers && item != null);
                }
            }
        });

        usersTable.addAction(new RemoveAction(usersTable) {
            @Override
            public boolean isApplicableTo(Datasource.State state, Entity item) {
                return super.isApplicableTo(state, item) && isNotCurrentUserSelected((User) item);
            }
        });

        Boolean multiSelect = BooleanUtils.toBooleanObject((String) params.get("multiselect"));
        if (multiSelect != null) {
            usersTable.setMultiSelect(multiSelect);
        }
    }

    protected boolean isNotCurrentUserSelected(User item) {
        return !(usersTable.getSelected().contains(userSession.getUser()) ||
                userSession.getCurrentOrSubstitutedUser().equals(item));
    }

    public void copy() {
        Set<User> selected = usersTable.getSelected();
        if (!selected.isEmpty()) {
            User selectedUser = selected.iterator().next();
            selectedUser = dataSupplier.reload(selectedUser, "user.edit");
            User newUser = metadata.create(User.class);
            if (selectedUser.getUserRoles() != null) {
                List<UserRole> userRoles = new ArrayList<>();
                for (UserRole oldUserRole : selectedUser.getUserRoles()) {
                    Role oldRole = dataSupplier.reload(oldUserRole.getRole(), "_local");
                    if (BooleanUtils.isTrue(oldRole.getDefaultRole()))
                        continue;
                    UserRole role = new UserRole();
                    role.setUser(newUser);
                    role.setRole(oldRole);
                    userRoles.add(role);
                }
                newUser.setUserRoles(userRoles);
            }
            newUser.setGroup(selectedUser.getGroup());
            UserEditor editor = openEditor("sec$User.edit", newUser, WindowManager.OpenType.THIS_TAB);
            editor.initCopy();
            editor.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    usersDs.refresh();
                }
            });
        }
    }

    public void copySettings() {
        Set<User> selected = usersTable.getSelected();
        if (!selected.isEmpty()) {
            openWindow(
                    "sec$User.copySettings",
                    WindowManager.OpenType.DIALOG,
                    new SingletonMap("users", selected)
            );
        }
    }

    public void changePassword() {
        if (!usersTable.getSelected().isEmpty()) {
            final Editor changePasswordDialog = openEditor(
                    "sec$User.changePassw",
                    usersTable.getSelected().iterator().next(),
                    WindowManager.OpenType.DIALOG
            );

            changePasswordDialog.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    if (COMMIT_ACTION_ID.equals(actionId)) {
                        User item = (User) changePasswordDialog.getItem();
                        usersDs.updateItem(dataSupplier.reload(item, "user.browse"));
                    }
                }
            });
        }
    }

    public void changePasswordAtLogon() {
        if (!usersTable.getSelected().isEmpty()) {
            final ResetPasswordsDialog resetPasswordsDialog = openWindow("sec$User.resetPasswords", WindowManager.OpenType.DIALOG);
            resetPasswordsDialog.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                        boolean sendEmails = resetPasswordsDialog.getSendEmails();
                        boolean generatePasswords = resetPasswordsDialog.getGeneratePasswords();
                        Set<User> users = usersTable.getSelected();
                        resetPasswordsForUsers(users, sendEmails, generatePasswords);
                    }
                }
            });
        }
    }

    private void resetPasswordsForUsers(Set<User> users, boolean sendEmails, boolean generatePasswords) {
        List<UUID> usersForModify = new ArrayList<>();
        for (User user : users) {
            usersForModify.add(user.getId());
        }

        if (sendEmails) {
            Integer modifiedCount = userManagementService.changePasswordsAtLogonAndSendEmails(usersForModify);
            usersDs.refresh();

            showNotification(String.format(getMessage("resetPasswordCompleted"), modifiedCount),
                    NotificationType.HUMANIZED);
        } else {
            Map<UUID, String> changedPasswords = userManagementService.changePasswordsAtLogon(usersForModify, generatePasswords);

            if (generatePasswords) {
                Map<User, String> userPasswords = new LinkedHashMap<>();
                for (Map.Entry<UUID, String> entry : changedPasswords.entrySet()) {
                    userPasswords.put(usersDs.getItem(entry.getKey()), entry.getValue());
                }
                Map<String, Object> params = Collections.singletonMap("passwords", (Object) userPasswords);
                openWindow("sec$User.newPasswords", WindowManager.OpenType.DIALOG, params);
            } else {
                showNotification(String.format(getMessage("changePasswordAtLogonCompleted"), changedPasswords.size()),
                        NotificationType.HUMANIZED);
            }
            usersDs.refresh();
        }
    }
}