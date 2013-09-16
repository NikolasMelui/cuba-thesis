/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components.filter;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.desktop.gui.components.DesktopComponentsHelper;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.KeyCombination;
import com.haulmont.cuba.gui.components.filter.AbstractRuntimePropConditionEditDlg;
import com.haulmont.cuba.gui.components.filter.ParamFactory;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class RuntimePropConditionEditDlg extends AbstractRuntimePropConditionEditDlg<JDialog> {

    private Editor impl;
    private static final int FIELD_WIDTH = 250;
    private JComponent component;

    public RuntimePropConditionEditDlg(final RuntimePropCondition condition, JComponent component) {
        super(condition);
        this.component = component;
    }

    @Override
    protected void closeDlg() {
        impl.dispose();
        DesktopComponentsHelper.getTopLevelFrame(getImpl()).activate();
    }

    @Override
    public JDialog getImpl() {
        if (impl == null) {
            impl = new Editor();
            initShortcuts();
        }
        return impl;
    }

    protected void initShortcuts() {
        ClientConfig clientConfig = AppBeans.get(Configuration.class).getConfig(ClientConfig.class);
        KeyCombination close = KeyCombination.create(clientConfig.getCloseShortcut());
        KeyCombination commit = KeyCombination.create(clientConfig.getCommitShortcut());

        Action escAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDlg();
            }
        };
        DesktopComponentsHelper.addShortcutAction("close", impl.getRootPane(),
                DesktopComponentsHelper.convertKeyCombination(close), escAction);

        Action commitAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (commit()) {
                    closeDlg();
                }
            }
        };
        DesktopComponentsHelper.addShortcutAction("commit", impl.getRootPane(),
                DesktopComponentsHelper.convertKeyCombination(commit), commitAction);
    }

    @Override
    protected ParamFactory getParamFactory() {
        return new ParamFactoryImpl();
    }

    @Override
    protected void showNotification(String msg, IFrame.NotificationType type) {
        DesktopComponentsHelper.getTopLevelFrame(getImpl()).showNotification(msg, type);
    }

    protected class Editor extends JDialog {
        public Editor() {
            super(DesktopComponentsHelper.getTopLevelFrame(component));
            setLocationRelativeTo(App.getInstance().getMainFrame());
            setTitle(condition.getLocCaption());
            setSize(350, 230);
            setResizable(false);
            MigLayout layout = new MigLayout();
            setLayout(layout);
            JPanel mainPanel = new JPanel(new MigLayout("wrap 2"));
            add(mainPanel, "wrap");

            mainPanel.add(DesktopComponentsHelper.unwrap(categoryLabel));
            JComboBox categories = (JComboBox) DesktopComponentsHelper.unwrap(categorySelect);
            DesktopComponentsHelper.adjustSize(categories);
            Dimension size = categories.getSize();
            size.width = FIELD_WIDTH;
            categories.setPreferredSize(size);

            mainPanel.add(categories);
            mainPanel.add(DesktopComponentsHelper.unwrap(attributeLabel));
            JComboBox attributes = (JComboBox) DesktopComponentsHelper.unwrap(attributeSelect);
            DesktopComponentsHelper.adjustSize(attributes);
            size = attributes.getSize();
            size.width = FIELD_WIDTH;
            attributes.setPreferredSize(size);

            mainPanel.add(attributes);
            mainPanel.add(DesktopComponentsHelper.unwrap(operationLabel));
            JComboBox operations = (JComboBox) DesktopComponentsHelper.unwrap(operationSelect);
            DesktopComponentsHelper.adjustSize(operations);
            size = operations.getSize();
            size.width = FIELD_WIDTH;
            operations.setPreferredSize(size);

            mainPanel.add(operations);

            JPanel buttonsPanel = new JPanel(new MigLayout());
            add(buttonsPanel);
            JButton okButton = (JButton) DesktopComponentsHelper.unwrap(btnOk);
            DesktopComponentsHelper.adjustSize(okButton);
            buttonsPanel.add(okButton);
            JButton cancelButton = (JButton) DesktopComponentsHelper.unwrap(btnCancel);
            DesktopComponentsHelper.adjustSize(cancelButton);
            buttonsPanel.add(cancelButton);

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosed(WindowEvent e) {
                    DesktopComponentsHelper.getTopLevelFrame(getImpl()).activate();
                }
            });
        }
    }
}
