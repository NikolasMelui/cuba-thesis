/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.bali.datastruct.Node;
import com.haulmont.cuba.core.entity.AbstractSearchFolder;
import com.haulmont.cuba.core.entity.Folder;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.Tree;
import com.haulmont.cuba.gui.components.filter.ConditionsTree;
import com.haulmont.cuba.gui.components.filter.FilterHelper;
import com.haulmont.cuba.gui.components.filter.condition.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.condition.GroupCondition;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.app.folders.AppFolderEditWindow;
import com.haulmont.cuba.web.app.folders.FolderEditWindow;
import com.haulmont.cuba.web.app.folders.FoldersPane;
import com.haulmont.cuba.web.toolkit.ui.FilterSelect;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.Not;
import com.vaadin.event.dd.acceptcriteria.Or;
import com.vaadin.terminal.gwt.client.ui.dd.VerticalDropLocation;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Select;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.util.*;

/**
 * @author gorbunkov
 * @version $Id$
 */
@ManagedBean(FilterHelper.NAME)
public class Web6FilterHelper implements FilterHelper {

    @Inject
    protected Metadata metadata;

    @Inject
    protected Messages messages;

    @Inject
    protected UserSessionSource userSessionSource;

    @Override
    public void setLookupNullSelectionAllowed(LookupField lookupField, boolean value) {
        Select vSelect = WebComponentsHelper.unwrap(lookupField);
        vSelect.setNullSelectionAllowed(value);
    }

    @Override
    public void setLookupTextInputAllowed(LookupField lookupField, boolean value) {
        FilterSelect vSelect = WebComponentsHelper.unwrap(lookupField);
        vSelect.setTextInputAllowed(value);
    }

    @Override
    public AbstractSearchFolder saveFolder(AbstractSearchFolder folder) {
        FoldersPane foldersPane = App.getInstance().getAppWindow().getFoldersPane();
        AbstractSearchFolder savedFolder = (AbstractSearchFolder) foldersPane.saveFolder(folder);
        foldersPane.refreshFolders();
        return savedFolder;
    }

    @Override
    public void openFolderEditWindow(boolean isAppFolder, AbstractSearchFolder folder, Presentations presentations, Runnable commitHandler) {
        final FolderEditWindow window = AppFolderEditWindow.create(isAppFolder, false, folder, presentations, commitHandler);
        App.getInstance().addWindow(window);
    }

    @Override
    public boolean isFolderActionsEnabled() {
        return true;
    }

    @Override
    public void initConditionsDragAndDrop(final Tree tree, final ConditionsTree conditions) {
        final com.vaadin.ui.Tree vTree = WebComponentsHelper.unwrap(tree);
        vTree.setDragMode(com.vaadin.ui.Tree.TreeDragMode.NODE);
        vTree.setDropHandler(new DropHandler() {
            @Override
            public void drop(DragAndDropEvent event) {
                Transferable t = event.getTransferable();

                if (t.getSourceComponent() != vTree)
                    return;

                com.vaadin.ui.Tree.TreeTargetDetails target = (com.vaadin.ui.Tree.TreeTargetDetails) event.getTargetDetails();

                VerticalDropLocation location = target.getDropLocation();
                Object sourceItemId = t.getData("itemId");
                Object targetItemId = target.getItemIdOver();

                CollectionDatasource datasource = tree.getDatasource();

                AbstractCondition sourceCondition = (AbstractCondition) datasource.getItem(sourceItemId);
                AbstractCondition targetCondition = (AbstractCondition) datasource.getItem(targetItemId);

                Node<AbstractCondition> sourceNode = conditions.getNode(sourceCondition);
                Node<AbstractCondition> targetNode = conditions.getNode(targetCondition);

                if (location == VerticalDropLocation.MIDDLE) {
                    if (sourceNode.getParent() == null) {
                        conditions.getRootNodes().remove(sourceNode);
                    } else {
                        sourceNode.getParent().getChildren().remove(sourceNode);
                    }
                    targetNode.addChild(sourceNode);
                    refreshConditionsDs();
                    tree.expand(targetCondition.getId());
                } else {
                    List<Node<AbstractCondition>> siblings;
                    if (targetNode.getParent() == null)
                        siblings = conditions.getRootNodes();
                    else
                        siblings = targetNode.getParent().getChildren();

                    int targetIndex = siblings.indexOf(targetNode);
                    if (location == VerticalDropLocation.BOTTOM)
                        targetIndex++;


                    if (sourceNode.getParent() == null) {
                        conditions.getRootNodes().remove(sourceNode);
                    } else {
                        sourceNode.getParent().getChildren().remove(sourceNode);
                    }

                    if (targetNode.getParent() == null) {
                        sourceNode.parent = null;
                        conditions.getRootNodes().add(targetIndex, sourceNode);
                    } else {
                        targetNode.getParent().insertChildAt(targetIndex, sourceNode);
                    }

                    refreshConditionsDs();
                }
            }

            protected void refreshConditionsDs() {
                tree.getDatasource().refresh(Collections.<String, Object>singletonMap("conditions", conditions));
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return new Or(new AbstractSelect.TargetItemIs(vTree, getGroupConditionIds().toArray()), new Not(AbstractSelect.VerticalLocationIs.MIDDLE));
            }

            protected List<UUID> getGroupConditionIds() {
                List<UUID> groupConditions = new ArrayList<>();
                List<AbstractCondition> list = conditions.toConditionsList();
                for (AbstractCondition condition : list) {
                    if (condition instanceof GroupCondition)
                        groupConditions.add(condition.getId());
                }
                return groupConditions;
            }
        });
    }

    @Override
    public Object getFoldersPane() {
        return App.getInstance().getAppWindow().getFoldersPane();
    }

    @Override
    public void removeFolderFromFoldersPane(Folder folder) {
        FoldersPane foldersPane = App.getInstance().getAppWindow().getFoldersPane();
        if (foldersPane != null) {
            foldersPane.removeFolder(folder);
            foldersPane.refreshFolders();
        }
    }

    @Override
    public boolean isTableActionsEnabled() {
        return true;
    }

    @Override
    public void initTableFtsTooltips(Table table, final Map<UUID, String> tooltips) {
        com.haulmont.cuba.web.toolkit.ui.Table vTable = WebComponentsHelper.unwrap(table);
        vTable.setItemDescriptionGenerator(new com.haulmont.cuba.web.toolkit.ui.Table.ItemDescriptionGenerator() {
            @Override
            public String generateDescription(Component source, Object itemId, Object propertyId) {
                if (tooltips.keySet().contains(itemId)) {
                    return tooltips.get(itemId);
                }
                return null;
            }
        });
    }

    @Override
    public void removeTableFtsTooltips(Table table) {
        com.haulmont.cuba.web.toolkit.ui.Table vTable = WebComponentsHelper.unwrap(table);
        vTable.setItemDescriptionGenerator(null);
    }

    @Override
    public void setFieldReadOnlyFocusable(TextField textField, boolean readOnlyFocusable) {
        com.haulmont.cuba.web.toolkit.ui.TextField vTextField = WebComponentsHelper.unwrap(textField);
        vTextField.setAllowFocusReadonly(readOnlyFocusable);
    }

    @Override
    public void setComponentFocusable(com.haulmont.cuba.gui.components.Component component, boolean focusable) {
        com.vaadin.ui.Component vComponent = WebComponentsHelper.unwrap(component);
        if (vComponent instanceof Component.Focusable) {
            ((Component.Focusable) vComponent).setTabIndex(focusable ? 0 : -1);
        }
    }

    @Override
    public void setLookupCaptions(LookupField lookupField, Map<Object, String> captions) {
        ComboBox vLookupField = WebComponentsHelper.unwrap(lookupField);
        for (Map.Entry<Object, String> entry : captions.entrySet()) {
            vLookupField.setItemCaption(entry.getKey(), entry.getValue());
        }
    }
}