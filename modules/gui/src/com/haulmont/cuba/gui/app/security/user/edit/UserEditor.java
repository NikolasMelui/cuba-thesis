/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.app.security.user.edit;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowParams;
import com.haulmont.cuba.gui.app.security.user.NameBuilderListener;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.*;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class UserEditor extends AbstractEditor<User> {

    @Inject
    protected DsContext dsContext;

    @Inject
    protected DataSupplier dataSupplier;

    @Inject
    protected Datasource<User> userDs;

    @Inject
    protected CollectionDatasource<UserRole, UUID> rolesDs;

    @Inject
    protected CollectionDatasource<UserSubstitution, UUID> substitutionsDs;

    @Inject
    protected Table rolesTable;

    @Inject
    protected Table substTable;

    @Inject
    protected FieldGroup fieldGroup;

    protected PasswordField passwField;
    protected PasswordField confirmPasswField;
    protected LookupField languageLookup;

    @Inject
    protected Companion companion;

    @Inject
    protected UserSession userSession;

    @Inject
    protected ComponentsFactory factory;

    @Inject
    protected Configuration configuration;

    @Inject
    protected Metadata metadata;

    @Inject
    protected PasswordEncryption passwordEncryption;

    public interface Companion {
        void initPasswordField(PasswordField passwordField);
    }

    @Override
    public void init(Map<String, Object> params) {
        userDs.addListener(new NameBuilderListener<User>(fieldGroup));

        rolesTable.addAction(new AddRoleAction());
        rolesTable.addAction(new EditRoleAction());
        rolesTable.addAction(new RemoveRoleAction(rolesTable, false));

        substTable.addAction(new AddSubstitutedAction());
        substTable.addAction(new EditSubstitutedAction());
        substTable.addAction(new RemoveAction(substTable, false));

        initCustomFields(PersistenceHelper.isNew(WindowParams.ITEM.getEntity(params)));

        dsContext.addListener(
                new DsContext.CommitListenerAdapter() {
                    @Override
                    public void afterCommit(CommitContext context, Set<Entity> result) {
                        for (Entity entity : result) {
                            if (entity.equals(userSession.getUser())) {
                                userSession.setUser((User) entity);
                            }
                            if (entity.equals(userSession.getSubstitutedUser())) {
                                userSession.setSubstitutedUser((User) entity);
                            }
                        }
                    }
                }
        );
    }

    @Override
    protected void postInit() {
        // Do not show roles which are not allowed by security constraints
        LoadContext lc = new LoadContext(Role.class);
        lc.setQueryString("select r from sec$Role r");
        lc.setView(View.MINIMAL);
        List<Role> allowedRoles = dataSupplier.loadList(lc);

        Collection<UserRole> items = rolesDs.getItems();
        for (UserRole userRole : items) {
            if (!allowedRoles.contains(userRole.getRole())) {
                rolesDs.excludeItem(userRole);
            }
        }
    }

    @Override
    protected void initNewItem(User item) {
        addDefaultRoles(item);
        item.setLanguage(messages.getTools().localeToString(userSession.getLocale()));
    }

    private void addDefaultRoles(User user) {
        LoadContext ctx = new LoadContext(Role.class);
        ctx.setQueryString("select r from sec$Role r where r.defaultRole = true");
        List<Role> defaultRoles = dataSupplier.loadList(ctx);

        List<UserRole> newRoles = new ArrayList<>();
        if (user.getUserRoles() != null)
            newRoles.addAll(user.getUserRoles());

        for (Role role : defaultRoles) {
            final MetaClass metaClass = rolesDs.getMetaClass();
            UserRole userRole = dataSupplier.newInstance(metaClass);
            userRole.setRole(role);
            userRole.setUser(user);
            newRoles.add(userRole);
        }

        user.setUserRoles(newRoles);
    }

    private void initCustomFields(boolean isNew) {
        if (isNew) {
            fieldGroup.addCustomField("passw", new FieldGroup.CustomFieldGenerator() {
                @Override
                public Component generateField(Datasource datasource, String propertyId) {
                    passwField = factory.createComponent(PasswordField.NAME);
                    passwField.setRequiredMessage(getMessage("passwMsg"));
                    if (companion != null) {
                        companion.initPasswordField(passwField);
                    } else {
                        passwField.setRequired(true);
                    }
                    return passwField;
                }
            });

            fieldGroup.addCustomField("confirmPassw", new FieldGroup.CustomFieldGenerator() {
                @Override
                public Component generateField(Datasource datasource, String propertyId) {
                    confirmPasswField = factory.createComponent(PasswordField.NAME);
                    confirmPasswField.setRequiredMessage(getMessage("confirmPasswMsg"));
                    if (companion != null) {
                        companion.initPasswordField(confirmPasswField);
                    } else {
                        confirmPasswField.setRequired(true);
                    }
                    return confirmPasswField;
                }
            });
        }

        fieldGroup.addCustomField("language", new FieldGroup.CustomFieldGenerator() {
            @Override
            public Component generateField(Datasource datasource, String propertyId) {
                languageLookup = factory.createComponent(LookupField.NAME);

                languageLookup.setDatasource(datasource, propertyId);
                languageLookup.setRequired(false);

                Map<String, Locale> locales = configuration.getConfig(GlobalConfig.class).getAvailableLocales();
                TreeMap<String, Object> options = new TreeMap<>();
                for (Map.Entry<String, Locale> entry : locales.entrySet()) {
                    options.put(entry.getKey(), messages.getTools().localeToString(entry.getValue()));
                }
                languageLookup.setOptionsMap(options);
                return languageLookup;
            }
        });

        fieldGroup.addCustomField("group", new FieldGroup.CustomFieldGenerator() {
            @Override
            public Component generateField(Datasource datasource, String propertyId) {
                PickerField pickerField = factory.createComponent(PickerField.NAME);
                pickerField.setDatasource(datasource, propertyId);
                pickerField.setRequired(true);
                pickerField.setRequiredMessage(getMessage("groupMsg"));

                PickerField.LookupAction action = pickerField.addLookupAction();
                action.setLookupScreenOpenType(WindowManager.OpenType.DIALOG);

                return pickerField;
            }
        });
    }

    @Override
    protected boolean preCommit() {
        if (rolesDs.isModified()) {
            DatasourceImplementation<UserRole> rolesDsImpl = (DatasourceImplementation) rolesDs;

            CommitContext ctx = new CommitContext(Collections.emptyList(), rolesDsImpl.getItemsToDelete());
            dataSupplier.commit(ctx);

            List<UserRole> modifiedRoles = new ArrayList<>(rolesDsImpl.getItemsToCreate());
            modifiedRoles.addAll(rolesDsImpl.getItemsToUpdate());
            rolesDsImpl.committed(Collections.<Entity>emptySet());
            for (UserRole userRole : modifiedRoles) {
                rolesDsImpl.modified(userRole);
            }
        }

        User user = getItem();

        if (PersistenceHelper.isNew(user)) {
            String passw = passwField.getValue();
            String confPassw = confirmPasswField.getValue();
            if (StringUtils.isBlank(passw) || StringUtils.isBlank(confPassw)) {
                showNotification(getMessage("emptyPassword"), NotificationType.WARNING);
                return false;
            } else {
                if (ObjectUtils.equals(passw, confPassw)) {
                    ClientConfig passwordPolicyConfig = configuration.getConfig(ClientConfig.class);
                    if (passwordPolicyConfig.getPasswordPolicyEnabled()) {
                        String regExp = passwordPolicyConfig.getPasswordPolicyRegExp();
                        if (passw.matches(regExp)) {
                            return true;

                        } else {
                            showNotification(getMessage("simplePassword"), NotificationType.WARNING);
                            return false;
                        }
                    } else {
                        String passwordHash = passwordEncryption.getPasswordHash(user.getId(), passw);

                        user.setPassword(passwordHash);
                        return true;
                    }
                } else {
                    showNotification(getMessage("passwordsDoNotMatch"), NotificationType.WARNING);
                    return false;
                }
            }
        } else {
            return true;
        }
    }

    public void initCopy() {
        for (Entity item : rolesDs.getItems()) {
            ((DatasourceImplementation) rolesDs).modified(item);
        }
    }

    protected class AddRoleAction extends AbstractAction {

        public AddRoleAction() {
            super("add");
            icon = "icons/add.png";

            ClientConfig clientConfig = AppBeans.get(Configuration.class).getConfig(ClientConfig.class);
            setShortcut(clientConfig.getTableAddShortcut());
        }

        @Override
        public void actionPerform(Component component) {
            Map<String, Object> lookupParams = Collections.<String, Object>singletonMap("windowOpener", "sec$User.edit");
            Lookup roleLookupWindow = openLookup("sec$Role.lookup", new Lookup.Handler() {
                @Override
                public void handleLookup(Collection items) {
                    Collection<String> existingRoleNames = getExistingRoleNames();
                    rolesDs.suspendListeners();
                    try {
                        for (Object item : items) {
                            Role role = (Role) item;
                            if (existingRoleNames.contains(role.getName())) continue;

                            final MetaClass metaClass = rolesDs.getMetaClass();
                            UserRole userRole = dataSupplier.newInstance(metaClass);
                            userRole.setRole(role);
                            userRole.setUser(userDs.getItem());

                            rolesDs.addItem(userRole);
                            existingRoleNames.add(role.getName());
                        }
                    } finally {
                        rolesDs.resumeListeners();
                    }
                }

                private Collection<String> getExistingRoleNames() {
                    User user = userDs.getItem();
                    Collection<String> existingRoleNames = new HashSet<>();
                    if (user.getUserRoles() != null) {
                        for (UserRole userRole : user.getUserRoles()) {
                            if (userRole.getRole() != null)
                                existingRoleNames.add(userRole.getRole().getName());
                        }
                    }
                    return existingRoleNames;
                }

            }, WindowManager.OpenType.THIS_TAB, lookupParams);

            roleLookupWindow.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    rolesTable.requestFocus();
                }
            });

            Component lookupComponent = roleLookupWindow.getLookupComponent();
            if (lookupComponent instanceof Table) {
                ((Table) lookupComponent).setMultiSelect(true);
            }
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() &&
                    userSession.isEntityOpPermitted(
                            metadata.getSession().getClass(UserRole.class), EntityOp.CREATE);
        }

        @Override
        public String getCaption() {
            return getMessage("actions.Add");
        }
    }

    protected class EditRoleAction extends ItemTrackingAction {

        public EditRoleAction() {
            super("edit");
            icon = "icons/edit.png";
        }

        @Override
        public void actionPerform(Component component) {
            if (rolesDs.getItem() == null)
                return;
            Window window = openEditor("sec$Role.edit", rolesDs.getItem().getRole(), WindowManager.OpenType.THIS_TAB);
            window.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    if (Window.COMMIT_ACTION_ID.equals(actionId)) {
                        rolesDs.refresh();
                    }
                    rolesTable.requestFocus();
                }
            });
        }

        @Override
        public String getCaption() {
            return getMessage("actions.Edit");
        }
    }

    protected class RemoveRoleAction extends RemoveAction {

        private boolean hasDefaultRole = false;

        public RemoveRoleAction(ListComponent owner, boolean autocommit) {
            super(owner, autocommit);
        }

        @Override
        protected void confirmAndRemove(Set selected) {
            hasDefaultRole = hasDefaultRole(selected);
            super.confirmAndRemove(selected);
        }

        @Override
        public String getConfirmationMessage(String messagesPackage) {
            if (hasDefaultRole)
                return getMessage("dialogs.Confirmation.RemoveDefaultRole");
            else
                return super.getConfirmationMessage(messagesPackage);
        }

        private boolean hasDefaultRole(Set selected) {
            for (Object roleObj : selected) {
                UserRole role = (UserRole) roleObj;
                if (Boolean.TRUE.equals(role.getRole().getDefaultRole()))
                    return true;
            }
            return false;
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() &&
                    userSession.isEntityOpPermitted(
                            metadata.getSession().getClass(UserRole.class), EntityOp.DELETE);
        }
    }

    protected class AddSubstitutedAction extends AbstractAction {

        public AddSubstitutedAction() {
            super("add");
            icon = "icons/add.png";

            ClientConfig clientConfig = AppBeans.get(Configuration.class).getConfig(ClientConfig.class);
            setShortcut(clientConfig.getTableAddShortcut());
        }

        @Override
        public void actionPerform(Component component) {
            final UserSubstitution substitution = metadata.create(UserSubstitution.class);
            substitution.setUser(userDs.getItem());

            Map<String, Object> params = new HashMap<>();

            if (!substitutionsDs.getItemIds().isEmpty()) {
                List<UUID> list = new ArrayList<>();
                for (UUID usId : substitutionsDs.getItemIds()) {
                    list.add(substitutionsDs.getItem(usId).getSubstitutedUser().getId());
                }
                params.put("existingIds", list);
            }

            getDialogParams().setWidth(500);

            Window substitutionEditor = openEditor("sec$UserSubstitution.edit", substitution,
                    WindowManager.OpenType.DIALOG, params, substitutionsDs);
            substitutionEditor.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    substTable.requestFocus();
                }
            });
        }

        @Override
        public boolean isEnabled() {
            return super.isEnabled() &&
                    userSession.isEntityOpPermitted(
                            metadata.getSession().getClass(UserSubstitution.class), EntityOp.CREATE);
        }
    }

    protected class EditSubstitutedAction extends ItemTrackingAction {

        public EditSubstitutedAction() {
            super("edit");
            icon = "icons/edit.png";
        }

        @Override
        public void actionPerform(Component component) {
            getDialogParams().setWidth(500);

            if (substitutionsDs.getItem() != null) {
                Window substitutionEditor = openEditor("sec$UserSubstitution.edit", substitutionsDs.getItem(),
                        WindowManager.OpenType.DIALOG, substitutionsDs);
                substitutionEditor.addListener(new CloseListener() {
                    @Override
                    public void windowClosed(String actionId) {
                        substTable.requestFocus();
                    }
                });
            }
        }
    }
}