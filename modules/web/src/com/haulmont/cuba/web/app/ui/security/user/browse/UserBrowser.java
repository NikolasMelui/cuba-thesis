/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.01.2009 10:15:26
 * $Id$
 */
package com.haulmont.cuba.web.app.ui.security.user.browse;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.UserSessionClient;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.ExcelAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import com.haulmont.cuba.web.app.ui.security.user.edit.UserEditor;
import com.haulmont.cuba.web.rpt.WebExportDisplay;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserBrowser extends AbstractLookup {

    public UserBrowser(Window frame) {
        super(frame);
    }

    protected void init(Map<String, Object> params) {
        final Table table  = getComponent("users");

        ComponentsHelper.createActions(table);

        final Action removeAction = table.getAction(RemoveAction.ACTION_ID);
        table.getDatasource().addListener(new DsListenerAdapter() {
            @Override
            public void itemChanged(Datasource ds, Entity prevItem, Entity item) {
                super.itemChanged(ds, prevItem, item);
                User user = (User) item;
                if (removeAction != null)
                    removeAction.setEnabled(!(UserSessionClient.getUserSession().getUser().equals(user) ||
                            UserSessionClient.getUserSession().getCurrentOrSubstitutedUser().equals(user)));
            }
        });

        table.addAction(new ExcelAction(table, new WebExportDisplay()));

        table.addAction(
                new AbstractAction("changePassw")
                {
                    public void actionPerform(Component component)  {
                        if (!table.getSelected().isEmpty()) {
                            openEditor (
                                    "sec$User.changePassw",
                                    (Entity) table.getSelected().iterator().next(),
                                    WindowManager.OpenType.DIALOG
                            );
                        }
                    }
                }
        );

        String multiSelect = (String)params.get("multiselect");
        if ("true".equals(multiSelect)) {
            table.setMultiSelect(true);
        }

        table.addAction(
                new AbstractAction("copy"){
                    public void actionPerform(Component component){
                        if (!table.getSelected().isEmpty()){
                            User selectedUser = (User) table.getSelected().iterator().next();
                            selectedUser = getDsContext().getDataService().reload(selectedUser, "user.edit");
                            User newUser = new User();
                            if(selectedUser.getUserRoles()!=null){
                                Set<UserRole> roles = new HashSet<UserRole>();
                                for(UserRole oldRole : selectedUser.getUserRoles()){
                                    UserRole role = new UserRole();
                                    role.setUser(newUser);
                                    role.setRole(oldRole.getRole());
                                    roles.add(role);
                                }
                                newUser.setUserRoles(roles);
                            }
                            newUser.setGroup(selectedUser.getGroup());
                            UserEditor editor = openEditor("sec$User.edit", newUser, WindowManager.OpenType.THIS_TAB);
                            editor.initCopy();
                        }
                    }
                }
        );
//        getDsContext().get("users").refresh();
    }
}
