/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.app.folders;

import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.security.entity.SearchFolder;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.gui.components.WebFilter;

import java.util.*;

/**
 * @author devyatkin
 * @version $Id$
 */
public class SaveSetWindow extends AbstractWindow {

    private Set ids;
    private String componentPath;
    private String componentId;
    private String entityType;
    private String entityClass;
    private FoldersPane foldersPane;
    private LookupField foldersSelect;
    private String query;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        ids = (Set) params.get("items");
        componentPath = (String) params.get("componentPath");
        componentId = (String) params.get("componentId");
        entityType = (String) params.get("entityType");
        foldersPane = (FoldersPane) params.get("foldersPane");
        entityClass = (String) params.get("entityClass");
        query = (String) params.get("query");
        foldersSelect = getComponent("folderSelect");

        Button createBtn = getComponent("createNew");
        Button insertBtn = getComponent("insertBtn");

        insertBtn.setAction(new InsertAction());
        createBtn.setAction(new CreateSetAction());
    }

    private class InsertAction extends AbstractAction {
        protected InsertAction() {
            super("InsertAction");
        }
        public void actionPerform(Component component) {
            SearchFolder folder = foldersSelect.getValue();
            if (folder==null){
                showNotification(getMessage("saveSetWindow.notSelected"),NotificationType.TRAY);
                return; }
            String filterXml = folder.getFilterXml();
                folder.setFilterXml(WebFilter.UserSetHelper.addEntities(filterXml, ids));
                foldersPane.saveFolder(folder);
                foldersPane.refreshFolders();
                close(COMMIT_ACTION_ID,true);
        }
    }

    private class CreateSetAction extends AbstractAction {

        protected CreateSetAction() {
            super("CreateSetAction");
        }

        @Override
        public void actionPerform(Component component) {

            QueryParser parser = QueryTransformerFactory.createParser(query);
            String entityAlias = parser.getEntityAlias(entityType);
            String filterXml = WebFilter.UserSetHelper.generateSetFilter(ids,entityClass,componentId,entityAlias);
            final SearchFolder folder = AppBeans.get(Metadata.class).create(SearchFolder.class);
            folder.setUser(AppBeans.get(UserSessionSource.class).getUserSession().getUser());
            folder.setName("");
            folder.setFilterXml(filterXml);
            folder.setFilterComponentId(componentPath);
            folder.setEntityType(entityType);
            folder.setIsSet(true);

            Runnable commitHandler = new Runnable() {
                public void run() {
                    foldersPane.saveFolder(folder);
                    foldersPane.refreshFolders();
                }
            };

            final FolderEditWindow window = AppFolderEditWindow.create(false, false, folder, null, commitHandler);
            window.addListener(new com.vaadin.ui.Window.CloseListener() {
                public void windowClose(com.vaadin.ui.Window.CloseEvent e) {
                    App.getInstance().getAppWindow().removeWindow(window);
                }
            });
            App.getInstance().getAppWindow().addWindow(window);
            window.addListener(new com.vaadin.ui.Window.CloseListener(){
                public void windowClose(com.vaadin.ui.Window.CloseEvent e) {
                    close(COMMIT_ACTION_ID);
                }
            });
        }
    }
}