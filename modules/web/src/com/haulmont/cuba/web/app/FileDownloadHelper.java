/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.app;

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.data.CollectionDatasource;

import java.text.NumberFormat;

/**
 * @author krivopustov
 * @version $Id$
 */
public class FileDownloadHelper {

    public static String makeLink(FileDescriptor fd, boolean newWindow, boolean attachment) {
        StringBuilder sb = new StringBuilder();
        sb.append("<a href=\"");
        sb.append(makeUrl(fd, attachment));
        sb.append("\"");
        if (newWindow)
            sb.append(" target=\"_blank\"");
        sb.append(">");
        sb.append(fd.getName());
        sb.append("</a>");
        return sb.toString();
    }

    public static String makeUrl(FileDescriptor fd, boolean attachment) {
        StringBuilder sb = new StringBuilder();
        sb.append("dispatch/download?")
                .append("s=").append(AppBeans.get(UserSessionSource.class).getUserSession().getId()).append("&")
                .append("f=").append(fd.getId());
        if (attachment)
            sb.append("&a=true");
        return sb.toString();
    }

    public static void initGeneratedColumn(final Table table) {
        final CollectionDatasource ds = table.getDatasource();
        MetaPropertyPath nameProperty = ds.getMetaClass().getPropertyPath("name");
        /*final com.vaadin.ui.Table vTable = (com.vaadin.ui.Table) WebComponentsHelper.unwrap(table);

        vTable.addGeneratedColumn(nameProperty, new com.vaadin.ui.Table.ColumnGenerator() {
            private static final long serialVersionUID = -8909453319289476141L;

            @Override
            public Component generateCell(com.vaadin.ui.Table source, final Object itemId, Object columnId) {

                Property prop = source.getItem(itemId).getItemProperty(columnId);
                if (prop.getType().equals(String.class)) {

                    final FileDescriptor fd = (FileDescriptor) ds.getItem(itemId);
                    if (fd == null) {
                        return new Label();
                    }
                    Component component;
                    if (PersistenceHelper.isNew(fd)) {
                        component = new Label(fd.getName());
                    } else {
                        component = new Button(fd.getName(), new Button.ClickListener() {

                            @Override
                            public void buttonClick(Button.ClickEvent event) {
                                new WebExportDisplay().show(fd);
                            }
                        });
                    }
                    ((AbstractComponent) component).setImmediate(true);
                    component.setStyleName("link");
                    return component;
                }
                return null;

            }
        });*/
    }

    public static void initGeneratedColumn(Table table, final String fileProperty) {
        final CollectionDatasource ds = table.getDatasource();
        MetaPropertyPath nameProperty = ds.getMetaClass().getPropertyPath(fileProperty + ".name");
        /*final com.vaadin.ui.Table vTable = (com.vaadin.ui.Table) WebComponentsHelper.unwrap(table);

        vTable.addGeneratedColumn(nameProperty, new com.vaadin.ui.Table.ColumnGenerator() {
            @Override
            public Component generateCell(com.vaadin.ui.Table source, Object itemId, Object columnId) {
                Instance enclosingEntity = ds.getItem(itemId);
                if (enclosingEntity != null) {
                    final FileDescriptor fd = enclosingEntity.getValue(fileProperty);
                    if (fd != null) {
                        Component component;
                        if (PersistenceHelper.isNew(fd)) {
                            component = new Label(fd.getName());
                        } else {
                            component = new Button(fd.getName(),
                                    new Button.ClickListener() {
                                        @Override
                                        public void buttonClick(Button.ClickEvent event) {
                                            new WebExportDisplay().show(fd);
                                        }
                                    });
                        }
                        component.setStyleName("link");
                        return component;
                    }
                }
                return new Label();
            }
        });*/
    }

    public static String formatFileSize(long longSize, int decimalPos) {
        Messages messages = AppBeans.get(Messages.NAME);

        NumberFormat fmt = NumberFormat.getNumberInstance();
        if (decimalPos >= 0) {
            fmt.setMaximumFractionDigits(decimalPos);
        }
        final double size = longSize;
        double val = size / (1024 * 1024);
        if (val > 1) {
            return fmt.format(val).concat(" " + messages.getMessage(FileDownloadHelper.class, "fmtMb"));
        }
        val = size / 1024;
        if (val > 10) {
            return fmt.format(val).concat(" " + messages.getMessage(FileDownloadHelper.class, "fmtKb"));
        }
        return fmt.format(size).concat(" " + messages.getMessage(FileDownloadHelper.class, "fmtB"));
    }

    public static String formatFileSize(long fileSize) {
        return formatFileSize(fileSize, 0);
    }
}
