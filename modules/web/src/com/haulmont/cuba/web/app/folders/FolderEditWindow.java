/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.app.folders;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.app.FoldersService;
import com.haulmont.cuba.core.entity.AbstractSearchFolder;
import com.haulmont.cuba.core.entity.Folder;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.security.entity.Presentation;
import com.haulmont.cuba.security.entity.SearchFolder;
import com.haulmont.cuba.web.gui.components.WebButton;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.*;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * @author krivopustov
 * @version $Id$
 */
public class FolderEditWindow extends Window {

    protected Folder folder;
    protected String messagesPack;
    protected TextField nameField;
    protected TextField tabNameField;
    protected Select parentSelect;
    protected TextField sortOrderField;
    protected Select presentation;
    protected CheckBox globalCb;
    protected CheckBox applyDefaultCb;
    protected Runnable commitHandler;
    protected VerticalLayout layout;
    protected Button okBtn;
    protected Messages messages;

    public FolderEditWindow(boolean adding, Folder folder, Presentations presentations, Runnable commitHandler) {
        super();
        this.folder = folder;
        this.commitHandler = commitHandler;

        messages = AppBeans.get(Messages.class);
        messagesPack = AppConfig.getMessagesPack();
        setCaption(adding ? getMessage("folders.folderEditWindow.adding") : getMessage("folders.folderEditWindow"));

        setWidth(300, Sizeable.UNITS_PIXELS);
        setResizable(false);

        layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);

        setContent(layout);
        center();

        nameField = new TextField();
        nameField.setRequired(true);
        nameField.setCaption(getMessage("folders.folderEditWindow.nameField"));
        nameField.setWidth(250, Sizeable.UNITS_PIXELS);
        nameField.setValue(folder.getName());
        layout.addComponent(nameField);

        tabNameField = new TextField();
        tabNameField.setCaption(getMessage("folders.folderEditWindow.tabNameField"));
        tabNameField.setWidth(250, Sizeable.UNITS_PIXELS);
        tabNameField.setValue(StringUtils.trimToEmpty(folder.getTabName()));
        layout.addComponent(tabNameField);

        parentSelect = new Select();
        parentSelect.setCaption(getMessage("folders.folderEditWindow.parentSelect"));
        parentSelect.setWidth(250, Sizeable.UNITS_PIXELS);
        parentSelect.setNullSelectionAllowed(true);
        fillParentSelect();
        parentSelect.setValue(folder.getParent());
        layout.addComponent(parentSelect);

        if (folder instanceof SearchFolder) {
            if (presentations != null) {
                presentation = new Select();
                presentation.setCaption(getMessage("folders.folderEditWindow.presentation"));
                presentation.setWidth("250px");
                presentation.setNullSelectionAllowed(true);
                fillPresentations(presentations);
                presentation.setValue(((SearchFolder) folder).getPresentation());
                layout.addComponent(presentation);
            } else if (((SearchFolder) folder).getPresentation() != null) {
                final TextField selectedPresentation = new TextField();
                selectedPresentation.setWidth("250px");
                selectedPresentation.setCaption(getMessage("folders.folderEditWindow.presentation"));
                selectedPresentation.setValue(((SearchFolder) folder).getPresentation().getName());
                selectedPresentation.setEnabled(false);
                layout.addComponent(selectedPresentation);
            }
        }

        sortOrderField = new TextField();
        sortOrderField.setCaption(getMessage("folders.folderEditWindow.sortOrder"));
        sortOrderField.setWidth(250, Sizeable.UNITS_PIXELS);
        sortOrderField.setValue(folder.getSortOrder() == null ? "" : folder.getSortOrder());
        layout.addComponent(sortOrderField);

        if (UserSessionProvider.getUserSession().isSpecificPermitted("cuba.gui.searchFolder.global")
                && folder instanceof SearchFolder
                && BooleanUtils.isNotTrue(((SearchFolder) folder).getIsSet())) {
            globalCb = new CheckBox(getMessage("folders.folderEditWindow.global"));
            globalCb.setValue(((SearchFolder) folder).getUser() == null);
            layout.addComponent(globalCb);
        }

        applyDefaultCb = new CheckBox(getMessage("folders.folderEditWindow.applyDefault"));
        applyDefaultCb.setValue(BooleanUtils.isTrue(((AbstractSearchFolder)folder).getApplyDefault()));
        applyDefaultCb.setVisible(ConfigProvider.getConfig(ClientConfig.class).getGenericFilterManualApplyRequired()
                && folder instanceof SearchFolder
                && BooleanUtils.isNotTrue(((SearchFolder) folder).getIsSet()));
        layout.addComponent(applyDefaultCb);

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setMargin(true, false, false, false);
        buttonsLayout.setSpacing(true);
        layout.addComponent(buttonsLayout);

        okBtn = new Button(getMessage("actions.Ok"));
        okBtn.setIcon(new ThemeResource("icons/ok.png"));
        okBtn.addStyleName(WebButton.ICON_STYLE);

        initButtonOkListener();
        buttonsLayout.addComponent(okBtn);

        Button cancelBtn = new Button(getMessage("actions.Cancel"));
        cancelBtn.setIcon(new ThemeResource("icons/cancel.png"));
        cancelBtn.addStyleName(WebButton.ICON_STYLE);
        cancelBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close();
            }
        });
        buttonsLayout.addComponent(cancelBtn);
    }

    protected void initButtonOkListener() {
        okBtn.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                SearchFolder folder = (SearchFolder)FolderEditWindow.this.folder;
                if (StringUtils.trimToNull((String) nameField.getValue()) == null) {
                    String msg = messages.getMainMessage("folders.folderEditWindow.emptyName");
                    showNotification(msg, Notification.TYPE_TRAY_NOTIFICATION);
                    return;
                }
                folder.setName((String) nameField.getValue());
                folder.setTabName((String) tabNameField.getValue());

                if (sortOrderField.getValue() == null || "".equals(sortOrderField.getValue())) {
                    folder.setSortOrder(null);
                } else {
                    Object value = sortOrderField.getValue();
                    int sortOrder;
                    if (value instanceof Integer)
                        sortOrder = (Integer) value;
                    else
                        try {
                            sortOrder = Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            String msg = messages.getMainMessage("folders.folderEditWindow.invalidSortOrder");
                            showNotification(msg, Notification.TYPE_WARNING_MESSAGE);
                            return;
                        }
                    folder.setSortOrder(sortOrder);
                }

                Object parent = parentSelect.getValue();
                if (parent instanceof Folder)
                    folder.setParent((Folder) parent);
                else
                    folder.setParent(null);

                folder.setApplyDefault(Boolean.valueOf(applyDefaultCb.getValue().toString()));
                if (globalCb != null) {
                    if (BooleanUtils.isTrue((Boolean) globalCb.getValue())) {
                        folder.setUser(null);
                    } else {
                        folder.setUser(UserSessionProvider.getUserSession().getCurrentOrSubstitutedUser());
                    }
                } else {
                    folder.setUser(UserSessionProvider.getUserSession().getCurrentOrSubstitutedUser());
                }

                if (presentation != null) {
                    folder.setPresentation((Presentation) presentation.getValue());
                }

                FolderEditWindow.this.commitHandler.run();

                close();
            }
        });
    }

    protected void fillParentSelect() {
        parentSelect.removeAllItems();

        String root = getMessage("folders.searchFoldersRoot");
        parentSelect.addItem(root);
        parentSelect.setNullSelectionItemId(root);

        FoldersService service = AppBeans.get(FoldersService.NAME);
        List<SearchFolder> list = service.loadSearchFolders();
        for (SearchFolder folder : list) {
            if (!folder.equals(this.folder)) {
                parentSelect.addItem(folder);
                parentSelect.setItemCaption(folder, folder.getCaption());
            }
        }
    }

    private void fillPresentations(Presentations presentations) {
        presentation.removeAllItems();

        final Collection<Object> availablePresentationIds = presentations.getPresentationIds();
        for (final Object pId : availablePresentationIds) {
            final Presentation p = presentations.getPresentation(pId);
            presentation.addItem(p);
            presentation.setItemCaption(p, presentations.getCaption(pId));
        }
    }

    protected String getMessage(String key) {
        return messages.getMainMessage(key);
    }
}
