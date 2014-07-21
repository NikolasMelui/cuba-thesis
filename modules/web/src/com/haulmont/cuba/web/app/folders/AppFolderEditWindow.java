/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.app.folders;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.app.FoldersService;
import com.haulmont.cuba.core.entity.AppFolder;
import com.haulmont.cuba.core.entity.Folder;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.gui.theme.Theme;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * @author devyatkin
 * @version $Id$
 */
public class AppFolderEditWindow extends FolderEditWindow {

    protected TextArea visibilityScriptField = null;
    protected TextArea quantityScriptField = null;

    public static FolderEditWindow create(boolean isAppFolder, boolean adding,
                                          Folder folder, Presentations presentations, Runnable commitHandler) {
        GlobalConfig globalConfig = AppBeans.get(Configuration.class).getConfig(GlobalConfig.class);
        String className = isAppFolder ? globalConfig.getAppFolderEditWindowClassName()
                                       : globalConfig.getFolderEditWindowClassName();

        if (className != null) {
            Class<FolderEditWindow> aClass = ReflectionHelper.getClass(className);
            try {
                Constructor constructor = aClass.
                        getConstructor(boolean.class, Folder.class, Presentations.class, Runnable.class);
                return (FolderEditWindow) constructor.newInstance(adding, folder, presentations, commitHandler);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else
            return isAppFolder ? new AppFolderEditWindow(adding, folder, presentations, commitHandler)
                               : new FolderEditWindow(adding, folder, presentations, commitHandler);
    }

    public AppFolderEditWindow(boolean adding, Folder folder, Presentations presentations, Runnable commitHandler) {
        super(adding, folder, presentations, commitHandler);
        if (!adding) {
            Theme theme = App.getInstance().getUiTheme();
            setWidth(theme.get("cuba.web.AppFolderEditWindow.width"));

            visibilityScriptField = new TextArea();
            visibilityScriptField.setRows(10);
            visibilityScriptField.setColumns(40);
            visibilityScriptField.setCaption(getMessage("folders.visibilityScript"));
            String vScript = StringUtils.trimToEmpty(((AppFolder) folder).getVisibilityScript());
            visibilityScriptField.setValue(vScript);
            layout.addComponent(visibilityScriptField, 3);

            quantityScriptField = new TextArea();
            String qScript = StringUtils.trimToEmpty(((AppFolder) folder).getQuantityScript());
            quantityScriptField.setValue(qScript);
            quantityScriptField.setRows(10);
            quantityScriptField.setColumns(40);
            quantityScriptField.setCaption(getMessage("folders.quantityScript"));
            layout.addComponent(quantityScriptField, 4);

            if (AppUI.getCurrent().isTestMode()) {
                setCubaId("appFolderEditWindow");

                visibilityScriptField.setCubaId("visibilityScriptField");
                quantityScriptField.setCubaId("quantityScriptField");
            }
        }
    }

    @Override
    protected void initButtonOkListener() {
        okBtn.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                AppFolder folder = (AppFolder) AppFolderEditWindow.this.folder;
                if (StringUtils.trimToNull(nameField.getValue()) == null) {
                    String msg = messages.getMessage(messagesPack, "folders.folderEditWindow.emptyName");
                    App.getInstance().getWindowManager().showNotification(msg, IFrame.NotificationType.TRAY);
                    return;
                }
                folder.setName(nameField.getValue());
                folder.setTabName(tabNameField.getValue());

                if (sortOrderField.getValue() == null || "".equals(sortOrderField.getValue())) {
                    folder.setSortOrder(null);
                } else {
                    Object value = sortOrderField.getValue();
                    int sortOrder;
                    try {
                        sortOrder = Integer.parseInt((String) value);
                    } catch (NumberFormatException e) {
                        String msg = messages.getMessage(messagesPack, "folders.folderEditWindow.invalidSortOrder");
                        App.getInstance().getWindowManager().showNotification(msg, IFrame.NotificationType.TRAY);
                        return;
                    }
                    folder.setSortOrder(sortOrder);
                }

                Object parent = parentSelect.getValue();
                if (parent instanceof Folder) {
                    folder.setParent((Folder) parent);
                } else {
                    folder.setParent(null);
                }

                if (visibilityScriptField != null) {
                    String scriptText = visibilityScriptField.getValue();
                    folder.setVisibilityScript(scriptText);
                }
                if (quantityScriptField != null) {
                    String scriptText = quantityScriptField.getValue();
                    folder.setQuantityScript(scriptText);
                }
                folder.setApplyDefault(Boolean.valueOf(applyDefaultCb.getValue().toString()));

                AppFolderEditWindow.this.commitHandler.run();

                close();
            }
        });
    }

    @Override
    protected void fillParentSelect() {
        parentSelect.removeAllItems();

        String root = getMessage("folders.appFoldersRoot");
        parentSelect.addItem(root);
        parentSelect.setNullSelectionItemId(root);

        FoldersService service = AppBeans.get(FoldersService.NAME);
        List<AppFolder> list = service.loadAppFolders();
        for (AppFolder folder : list) {
            if (!folder.equals(this.folder)) {
                parentSelect.addItem(folder);
                parentSelect.setItemCaption(folder, getMessage(folder.getName()));
            }
        }
    }
}