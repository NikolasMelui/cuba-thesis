/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.app.security.group.browse;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.CreateAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;
import com.haulmont.cuba.security.app.UserManagementService;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class GroupBrowser extends AbstractWindow {

    @Inject
    protected DataSupplier dataSupplier;

    @Inject
    protected UserManagementService userManagementService;

    @Named("groupsTree.create")
    protected CreateAction groupCreateAction;

    @Named("groupsTree.copy")
    protected Action groupCopyAction;

    @Named("groupsTree.edit")
    protected EditAction groupEditAction;

    @Inject
    protected PopupButton groupCreateButton;

    @Inject
    protected HierarchicalDatasource<Group, UUID> groupsDs;

    @Inject
    protected Tree groupsTree;

    @Inject
    protected Table usersTable;

    @Inject
    protected TabSheet tabsheet;

    @Inject
    protected UserSession userSession;

    @Inject
    protected Metadata metadata;

    private boolean constraintsTabInitialized, attributesTabInitialized;

    private GroupPropertyCreateAction attributeCreateAction;
    private GroupPropertyCreateAction constraintCreateAction;
    private GroupPropertyCreateAction userCreateAction;

    public void init(final Map<String, Object> params) {
        groupCreateAction.setCaption(getMessage("action.create"));

        groupCreateAction.setOpenType(WindowManager.OpenType.DIALOG);
        groupEditAction.setOpenType(WindowManager.OpenType.DIALOG);

        groupCreateButton.addAction(groupCreateAction);
        groupCreateButton.addAction(groupCopyAction);

        userCreateAction = new GroupPropertyCreateAction(usersTable) {
            @Override
            protected void afterCommit(Entity entity) {
                usersTable.getDatasource().refresh();
            }
        };
        usersTable.addAction(userCreateAction);
        usersTable.addAction(new ItemTrackingAction("moveToGroup") {

            {
                setEnabled(usersTable.getSingleSelected() != null);
            }

            @Override
            public String getIcon() {
                return "icons/move.png";
            }

            @Override
            public void actionPerform(Component component) {
                final Set<User> selected = usersTable.getSelected();
                if (!selected.isEmpty()) {
                    getDialogParams().setResizable(false);
                    getDialogParams().setHeight(400);
                    openLookup("sec$Group.lookup", new Lookup.Handler() {
                        @Override
                        public void handleLookup(Collection items) {
                            if (items.size() == 1) {
                                Group group = (Group) items.iterator().next();
                                List<UUID> usersForModify = new ArrayList<UUID>();
                                for (User user : selected) {
                                    usersForModify.add(user.getId());
                                }
                                userManagementService.moveUsersToGroup(usersForModify, group.getId());

                                usersTable.getDatasource().refresh();
                            }
                        }
                    }, WindowManager.OpenType.DIALOG);
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled && userSession.isEntityOpPermitted(metadata.getSession().getClass(User.class),
                        EntityOp.UPDATE));
            }
        });

        tabsheet.addListener(
                new TabSheet.TabChangeListener() {
                    @Override
                    public void tabChanged(TabSheet.Tab newTab) {
                        if ("constraintsTab".equals(newTab.getName()))
                            initConstraintsTab();
                        else if ("attributesTab".equals(newTab.getName()))
                            initAttributesTab();
                    }
                }
        );

        // enable actions if group is selected
        groupsDs.addListener(new DsListenerAdapter<Group>() {
            @Override
            public void itemChanged(Datasource<Group> ds, Group prevItem, Group item) {
                if (userCreateAction != null)
                    userCreateAction.setEnabled(item != null);
                if (attributeCreateAction != null)
                    attributeCreateAction.setEnabled(item != null);
                if (constraintCreateAction != null)
                    constraintCreateAction.setEnabled(item != null);
                groupCopyAction.setEnabled(item != null);
            }
        });

        groupsDs.refresh();
        groupsTree.expandTree();

        final Collection<UUID> itemIds = groupsDs.getItemIds();
        if (!itemIds.isEmpty()) {
            groupsTree.setSelected(groupsDs.getItem(itemIds.iterator().next()));
        }

        boolean hasPermissionsToCreateGroup =
                userSession.isEntityOpPermitted(metadata.getSession().getClass(Group.class),
                        EntityOp.CREATE);

        if (groupCreateButton != null) {
            groupCreateButton.setEnabled(hasPermissionsToCreateGroup);
        }
    }

    public void copyGroup() {
        Group group = groupsDs.getItem();
        if (group != null) {
            userManagementService.copyAccessGroup(group.getId());
            groupsDs.refresh();
        }
    }

    private void initConstraintsTab() {
        if (constraintsTabInitialized)
            return;

        final Table constraintsTable = getComponent("constraintsTable");
        constraintCreateAction = new GroupPropertyCreateAction(constraintsTable);
        constraintsTable.addAction(constraintCreateAction);

        constraintsTabInitialized = true;
    }

    private void initAttributesTab() {
        if (attributesTabInitialized)
            return;

        final Table attributesTable = getComponent("attributesTable");
        attributeCreateAction = new GroupPropertyCreateAction(attributesTable);
        attributesTable.addAction(attributeCreateAction);

        attributesTabInitialized = true;
    }

    /**
     * Create action for the objects associated with the group
     */
    private class GroupPropertyCreateAction extends CreateAction {

        public GroupPropertyCreateAction(ListComponent owner) {
            super(owner);
            Set<Group> selected = groupsTree.getSelected();
            setEnabled(selected != null && selected.size() == 1);
        }

        @Override
        public Map<String, Object> getInitialValues() {
            return Collections.<String, Object>singletonMap("group", groupsTree.getSelected());
        }

        @Override
        public void actionPerform(Component component) {
            Set<Group> selected = groupsTree.getSelected();
            if (selected == null || selected.size() != 1) {
                return;
            }

            super.actionPerform(component);
        }
    }
}
